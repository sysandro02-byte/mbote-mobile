/** MBoté API backed exclusively by PostgreSQL (no seeded users or fake content). */
require('dotenv').config();
const cors = require('cors');
const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const crypto = require('node:crypto');
const { Pool } = require('pg');
const { WebSocketServer } = require('ws');

const PORT = Number(process.env.PORT || 8080);
const API_VERSION = '1.6.0';
const realtimeHub = {
  sendToUser: () => false,
  isUserConnected: () => false,
};

function createPool() {
  if (!process.env.DATABASE_URL) throw new Error('DATABASE_URL est requis.');
  return new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DATABASE_SSL === 'false' ? false : { rejectUnauthorized: false },
    max: Number(process.env.DATABASE_POOL_MAX || 10),
    connectionTimeoutMillis: Number(process.env.DATABASE_CONNECT_TIMEOUT_MS || 5000),
  });
}

function createApp({ db, jwtSecret = process.env.JWT_SECRET, allowedOrigins = process.env.FRONTEND_URL } = {}) {
  if (!db) throw new Error('Une connexion PostgreSQL est requise.');
  if (!jwtSecret || jwtSecret.length < 32) throw new Error('JWT_SECRET doit contenir au moins 32 caractères.');

  const app = express();
  const rateBuckets = new Map();
  const rateLimit = ({ windowMs, max, keyPrefix }) => (req, res, next) => {
    const now = Date.now();
    const clientKey = `${keyPrefix}:${req.ip || req.socket?.remoteAddress || 'unknown'}`;
    const current = rateBuckets.get(clientKey);
    if (!current || current.resetAt <= now) {
      rateBuckets.set(clientKey, { count: 1, resetAt: now + windowMs });
      return next();
    }
    current.count += 1;
    if (current.count > max) {
      res.set('Retry-After', String(Math.max(1, Math.ceil((current.resetAt - now) / 1000))));
      return res.status(429).json({ success: false, error: 'Trop de requêtes. Réessayez dans quelques instants.' });
    }
    if (rateBuckets.size > 10000) {
      for (const [key, value] of rateBuckets) if (value.resetAt <= now) rateBuckets.delete(key);
    }
    return next();
  };
  app.set('trust proxy', 1);
  app.use((req, res, next) => {
    const requestId = String(req.get('x-request-id') || crypto.randomUUID()).replace(/[^A-Za-z0-9._-]/g, '').slice(0, 80) || crypto.randomUUID();
    const startedAt = Date.now();
    req.requestId = requestId;
    res.set('X-Request-Id', requestId);
    res.set('X-Content-Type-Options', 'nosniff');
    res.set('X-Frame-Options', 'DENY');
    res.set('Referrer-Policy', 'no-referrer');
    res.set('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
    res.on('finish', () => {
      const level = res.statusCode >= 500 ? 'error' : res.statusCode >= 400 ? 'warn' : 'info';
      console.log(JSON.stringify({
        level,
        event: 'http_request',
        requestId,
        method: req.method,
        path: req.path,
        status: res.statusCode,
        durationMs: Date.now() - startedAt,
      }));
    });
    next();
  });
  const origins = (allowedOrigins || '').split(',').map((item) => item.trim()).filter(Boolean);
  const route = (handler) => (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);
  const success = (res, data, status = 200) => res.status(status).json({ success: true, data });
  const failure = (res, status, error) => res.status(status).json({ success: false, error });
  const text = (value, label, max = 10000) => {
    if (typeof value !== 'string' || !value.trim() || value.trim().length > max) throw Object.assign(new Error(`${label} invalide`), { status: 400 });
    return value.trim();
  };
  const publicUser = (user) => ({ id: user.id, name: user.full_name, email: user.email, phone: user.phone || '', avatar: user.avatar_url || '', role: user.role, isVerified: Boolean(user.is_verified) });
  const tokenFor = (user) => jwt.sign({ userId: user.id, email: user.email, role: user.role }, jwtSecret, { expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile' });
  const auth = (req, res, next) => {
    const token = req.get('authorization')?.match(/^Bearer\s+(.+)$/i)?.[1];
    if (!token) return failure(res, 401, 'Authentification requise');
    try { req.user = jwt.verify(token, jwtSecret, { issuer: 'mbote-api', audience: 'mbote-mobile' }); return next(); }
    catch { return failure(res, 401, 'Session expirée ou invalide'); }
  };
  const adminOnly = (req, res, next) => {
    if (!['ADMIN', 'MODERATOR'].includes(String(req.user?.role || '').toUpperCase())) {
      return failure(res, 403, 'Accès administrateur refusé');
    }
    return next();
  };
  const safeSecretEqual = (provided, expected) => {
    const left = Buffer.from(String(provided || ''));
    const right = Buffer.from(String(expected || ''));
    return left.length > 0 && left.length === right.length && crypto.timingSafeEqual(left, right);
  };
  const member = async (chatId, userId) => (await db.query('SELECT 1 FROM chat_participants WHERE chat_id = $1 AND user_id = $2', [chatId, userId])).rowCount > 0;
  const messageDto = (row, userId) => ({ id: row.id, chatId: row.chat_id, senderId: row.sender_id, senderName: row.sender_name, senderAvatar: row.sender_avatar || '', text: row.text || '', timestamp: row.created_at, mediaType: row.media_type || 'NONE', mediaUrl: row.media_url, isStarred: Boolean(row.is_starred), isMine: row.sender_id === userId });

  const groqModel = () => String(process.env.GROQ_MODEL || 'llama-3.3-70b-versatile').trim();
  const lunaProviderConfigured = () => Boolean(
    String(process.env.GROQ_API_KEY || '').trim() ||
    (String(process.env.LUNA_AI_URL || '').trim() && String(process.env.LUNA_AI_SHARED_SECRET || '').trim())
  );
  const groqCompletion = async ({ messages, temperature = 0.3, maxTokens = 1000 }) => {
    const apiKey = String(process.env.GROQ_API_KEY || '').trim();
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), Number(process.env.GROQ_TIMEOUT_MS || 25000));
    try {
      if (!apiKey) {
        const bridgeUrl = String(process.env.LUNA_AI_URL || '').trim();
        const bridgeSecret = String(process.env.LUNA_AI_SHARED_SECRET || '').trim();
        if (!bridgeUrl || !bridgeSecret) throw Object.assign(new Error('Luna est temporairement indisponible'), { status: 503 });
        const upstream = await fetch(bridgeUrl, {
          method: 'POST',
          headers: {
            'x-loukatech-internal-key': bridgeSecret,
            'content-type': 'application/json',
            accept: 'application/json',
          },
          body: JSON.stringify({ messages, temperature, maxTokens }),
          signal: controller.signal,
        });
        const payload = await upstream.json().catch(() => ({}));
        if (!upstream.ok || !payload?.content) {
          console.warn(JSON.stringify({ level: 'warn', event: 'luna_bridge_error', status: upstream.status, code: payload?.error || null }));
          throw Object.assign(new Error('Le service Luna n’a pas répondu'), { status: upstream.status === 503 ? 503 : 502 });
        }
        return String(payload.content).trim();
      }

      const baseUrl = String(process.env.GROQ_BASE_URL || 'https://api.groq.com/openai/v1').replace(/\/$/, '');
      const upstream = await fetch(`${baseUrl}/chat/completions`, {
        method: 'POST',
        headers: {
          authorization: `Bearer ${apiKey}`,
          'content-type': 'application/json',
          accept: 'application/json',
        },
        body: JSON.stringify({
          model: groqModel(),
          messages,
          temperature,
          max_completion_tokens: maxTokens,
        }),
        signal: controller.signal,
      });
      const payload = await upstream.json().catch(() => ({}));
      if (!upstream.ok) {
        console.warn(JSON.stringify({ level: 'warn', event: 'groq_upstream_error', status: upstream.status, code: payload?.error?.code || null }));
        throw Object.assign(new Error('Le fournisseur Luna n’a pas répondu'), { status: 502 });
      }
      const answer = String(payload?.choices?.[0]?.message?.content || '').trim();
      if (!answer) throw Object.assign(new Error('Réponse Luna vide'), { status: 502 });
      return answer;
    } catch (error) {
      if (error?.name === 'AbortError') throw Object.assign(new Error('Luna a dépassé le délai de réponse'), { status: 504 });
      throw error;
    } finally {
      clearTimeout(timer);
    }
  };

  const firebaseServiceAccount = () => {
    const rawJson = String(process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '').trim();
    const rawB64 = String(process.env.FIREBASE_SERVICE_ACCOUNT_B64 || '').trim();
    if (!rawJson && !rawB64) return null;
    try {
      const source = rawJson || Buffer.from(rawB64, 'base64').toString('utf8');
      const parsed = JSON.parse(source);
      if (!parsed?.client_email || !parsed?.private_key || !parsed?.project_id) return null;
      return parsed;
    } catch {
      return null;
    }
  };
  let fcmAccessTokenCache = { token: '', expiresAt: 0 };
  const firebaseAccessToken = async (forceRefresh = false) => {
    if (!forceRefresh && fcmAccessTokenCache.token && fcmAccessTokenCache.expiresAt > Date.now() + 60_000) {
      return fcmAccessTokenCache.token;
    }
    const serviceAccount = firebaseServiceAccount();
    if (!serviceAccount) throw Object.assign(new Error('Firebase FCM n’est pas configuré'), { status: 503 });
    const now = Math.floor(Date.now() / 1000);
    const encode = (value) => Buffer.from(JSON.stringify(value)).toString('base64url');
    const unsigned = `${encode({ alg: 'RS256', typ: 'JWT' })}.${encode({
      iss: serviceAccount.client_email,
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
      aud: 'https://oauth2.googleapis.com/token',
      iat: now,
      exp: now + 3600,
    })}`;
    const signature = crypto.sign('RSA-SHA256', Buffer.from(unsigned), serviceAccount.private_key).toString('base64url');
    const assertion = `${unsigned}.${signature}`;
    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'content-type': 'application/x-www-form-urlencoded', accept: 'application/json' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion,
      }),
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok || !payload.access_token) throw Object.assign(new Error('Authentification Firebase FCM impossible'), { status: 502 });
    fcmAccessTokenCache = {
      token: String(payload.access_token),
      expiresAt: Date.now() + Math.max(300, Number(payload.expires_in || 3600)) * 1000,
    };
    return fcmAccessTokenCache.token;
  };
  const sendFcmToken = async (token, notification, data = {}, retry = true) => {
    const serviceAccount = firebaseServiceAccount();
    if (!serviceAccount) return { ok: false, notConfigured: true };
    const accessToken = await firebaseAccessToken();
    const normalizedData = Object.fromEntries(
      Object.entries(data).filter(([, value]) => value !== undefined && value !== null).map(([key, value]) => [key, String(value)])
    );
    const response = await fetch(
      `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(serviceAccount.project_id)}/messages:send`,
      {
        method: 'POST',
        headers: { authorization: `Bearer ${accessToken}`, 'content-type': 'application/json', accept: 'application/json' },
        body: JSON.stringify({
          message: {
            token,
            notification: notification?.title ? { title: String(notification.title), body: String(notification.body || '') } : undefined,
            data: normalizedData,
            android: { priority: 'high', notification: { channel_id: 'mbote_push_notifications_channel' } },
          },
        }),
      },
    );
    const body = await response.text();
    if (response.status === 401 && retry) {
      fcmAccessTokenCache = { token: '', expiresAt: 0 };
      return sendFcmToken(token, notification, data, false);
    }
    const unregistered = response.status === 404 || /UNREGISTERED/i.test(body);
    if (!response.ok && !unregistered) {
      console.warn(JSON.stringify({ level: 'warn', event: 'fcm_send_failed', status: response.status }));
    }
    return { ok: response.ok, unregistered };
  };
  const sendPushToUser = async (userId, notification, data = {}) => {
    if (!firebaseServiceAccount()) return { sent: 0, configured: false };
    const tokens = await db.query('SELECT token FROM device_push_tokens WHERE user_id=$1', [userId]);
    let sent = 0;
    for (const row of tokens.rows) {
      try {
        const result = await sendFcmToken(row.token, notification, data);
        if (result.ok) sent += 1;
        if (result.unregistered) await db.query('DELETE FROM device_push_tokens WHERE user_id=$1 AND token=$2', [userId, row.token]);
      } catch (error) {
        console.warn(JSON.stringify({ level: 'warn', event: 'fcm_delivery_error', userId: String(userId), error: error?.message || 'unknown' }));
      }
    }
    return { sent, configured: true };
  };

  const integrationProbeCache = new Map();
  const cachedIntegrationProbe = async (name, probe, ttlMs = 60_000) => {
    const now = Date.now();
    const cached = integrationProbeCache.get(name);
    if (cached?.expiresAt > now) return cached.value;
    if (cached?.promise) return cached.promise;
    const promise = Promise.resolve()
      .then(probe)
      .then((value) => Boolean(value))
      .catch((error) => {
        console.warn(JSON.stringify({ level: 'warn', event: 'integration_probe_failed', integration: name, error: error?.message || 'unknown' }));
        return false;
      })
      .then((value) => {
        integrationProbeCache.set(name, { value, expiresAt: Date.now() + ttlMs });
        return value;
      });
    integrationProbeCache.set(name, { promise, expiresAt: now + ttlMs });
    return promise;
  };
  const probeLunaProvider = () => cachedIntegrationProbe('luna', async () => {
    if (String(process.env.GROQ_API_KEY || '').trim()) return true;
    const bridgeUrl = String(process.env.LUNA_AI_URL || '').trim().replace(/\/$/, '');
    const secret = String(process.env.LUNA_AI_SHARED_SECRET || '').trim();
    if (!bridgeUrl || !secret) return false;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 8000);
    try {
      const response = await fetch(`${bridgeUrl}/health`, {
        headers: { 'x-loukatech-internal-key': secret, accept: 'application/json' },
        signal: controller.signal,
      });
      if (!response.ok) return false;
      const payload = await response.json().catch(() => ({}));
      return payload?.ok === true && payload?.provider === 'groq';
    } finally {
      clearTimeout(timer);
    }
  }, 120_000);
  const probeLoukaPayProvider = () => cachedIntegrationProbe('loukapay', async () => {
    const apiKey = String(process.env.PAYMENTS_API_KEY || '').trim();
    const endpoint = String(process.env.PAYMENTS_API_URL || '').trim();
    if (!apiKey || !endpoint) return false;
    const base = String(process.env.LOUKAPAY_BASE_URL || endpoint.replace(/\/v1\/payment-intents\/?$/, '')).replace(/\/$/, '');
    if (!base) return false;

    const probeProviders = async (timeoutMs) => {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), timeoutMs);
      try {
        const response = await fetch(`${base}/v1/providers`, {
          headers: { authorization: `Bearer ${apiKey}`, accept: 'application/json' },
          signal: controller.signal,
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) return false;
        const providers = Array.isArray(payload?.providers) ? payload.providers : [];
        return payload?.mode === 'live'
          && payload?.live_allowed === true
          && payload?.ready === true
          && providers.length > 0;
      } finally {
        clearTimeout(timer);
      }
    };

    try {
      if (await probeProviders(18_000)) return true;
    } catch (error) {
      if (error?.name !== 'AbortError') throw error;
    }

    // Render free services can be cold on the first request. Give LoukaPay one
    // bounded retry rather than marking payments unavailable immediately.
    await new Promise((resolve) => setTimeout(resolve, 1500));
    return probeProviders(18_000);
  }, 120_000);
  const probeFirebaseProvider = () => cachedIntegrationProbe('firebase', async () => {
    if (!firebaseServiceAccount()) return false;
    return Boolean(await firebaseAccessToken());
  }, 10 * 60_000);

  const sendOtpEmail = async (email, code, flow) => {
    if (!process.env.BREVO_API_KEY) throw Object.assign(new Error('Le service e-mail est temporairement indisponible'), { status: 503 });
    const response = await fetch(process.env.BREVO_API_URL || 'https://api.brevo.com/v3/smtp/email', {
      method: 'POST',
      headers: { 'api-key': process.env.BREVO_API_KEY, 'content-type': 'application/json', accept: 'application/json' },
      body: JSON.stringify({
        sender: { name: process.env.BREVO_SENDER_NAME || 'MBoté Sécurité', email: process.env.BREVO_SENDER_EMAIL || 'noreply@loukatech.com' },
        to: [{ email }],
        subject: flow === 'REGISTER' ? 'Confirmez votre compte MBoté' : 'Confirmez votre connexion MBoté',
        htmlContent: `<p>Votre code de sécurité MBoté est <strong>${code}</strong>.</p><p>Il expire dans 10 minutes. Ne le communiquez à personne.</p>`,
      }),
    });
    if (!response.ok) throw Object.assign(new Error('Le code de sécurité n’a pas pu être envoyé'), { status: 502 });
  };
  const createAuthChallenge = async ({ flow, email, userId = null, payload = {} }) => {
    const code = String(crypto.randomInt(0, 1_000_000)).padStart(6, '0');
    const codeHash = crypto.createHash('sha256').update(code).digest('hex');
    const result = await db.query(
      `INSERT INTO auth_challenges (flow, user_id, email, code_hash, payload, expires_at)
       VALUES ($1, $2, $3, $4, $5, NOW() + INTERVAL '10 minutes') RETURNING id`,
      [flow, userId, email, codeHash, payload],
    );
    try { await sendOtpEmail(email, code, flow); }
    catch (error) { await db.query('DELETE FROM auth_challenges WHERE id = $1', [result.rows[0].id]); throw error; }
    return { requiresOtpVerification: true, pendingUserId: result.rows[0].id, deliveryChannel: 'email', target: email.replace(/^(.{2}).*(@.*)$/, '$1***$2'), flow: flow.toLowerCase() };
  };
  const consumeAuthChallenge = async (challengeId, otp, flow) => {
    if (!/^[0-9]{6}$/.test(String(otp || ''))) throw Object.assign(new Error('Code OTP invalide'), { status: 400 });
    const result = await db.query(
      'SELECT * FROM auth_challenges WHERE id = $1 AND flow = $2 AND expires_at > NOW() FOR UPDATE',
      [challengeId, flow],
    );
    const challenge = result.rows[0];
    const submittedHash = crypto.createHash('sha256').update(String(otp)).digest('hex');
    if (!challenge || challenge.attempts >= 5 || challenge.code_hash.length !== submittedHash.length ||
        !crypto.timingSafeEqual(Buffer.from(challenge.code_hash), Buffer.from(submittedHash))) {
      if (challenge) await db.query('UPDATE auth_challenges SET attempts = attempts + 1 WHERE id = $1', [challenge.id]);
      throw Object.assign(new Error('Code OTP expiré ou invalide'), { status: 400 });
    }
    return challenge;
  };

  app.disable('x-powered-by');
  app.use(cors({ origin(origin, callback) { return !origin || origins.includes(origin) ? callback(null, true) : callback(new Error('Origine CORS non autorisée')); } }));
  // Publication videos are streamed directly to PostgreSQL-backed storage metadata.
  // Keep this raw-body middleware before express.json so Android can upload real video bytes.
  app.use('/v1/uploads/:surface', express.raw({ type: ['video/*', 'application/octet-stream'], limit: '50mb' }));
  app.use(express.json({
    limit: '5mb',
    verify(req, _res, buf) {
      req.rawBody = Buffer.from(buf);
    },
  }));
  app.use('/v1', rateLimit({ windowMs: 60_000, max: 600, keyPrefix: 'api' }));
  app.use('/v1/auth', rateLimit({ windowMs: 15 * 60_000, max: 60, keyPrefix: 'auth' }));

  app.post('/v1/uploads/:surface', auth, route(async (req, res) => {
    const surface = req.params.surface;
    if (!['short-videos', 'actus-videos'].includes(surface)) return failure(res, 404, 'Surface de publication inconnue');
    if (!Buffer.isBuffer(req.body) || req.body.length === 0) return failure(res, 400, 'Vidéo vide ou invalide');
    if (req.body.length > 50 * 1024 * 1024) return failure(res, 413, 'Vidéo trop volumineuse');
    const contentType = String(req.get('content-type') || '').split(';')[0].trim().toLowerCase();
    if (!contentType.startsWith('video/')) return failure(res, 415, 'Le fichier sélectionné n’est pas une vidéo');
    const created = await db.query(
      `INSERT INTO publication_uploads (owner_id, surface, content_type, file_size, content)
       VALUES ($1,$2,$3,$4,$5) RETURNING id`,
      [req.user.userId, surface, contentType, req.body.length, req.body],
    );
    success(res, { url: `${process.env.PUBLIC_API_URL || `${req.protocol}://${req.get('host')}`}/v1/uploads/files/${created.rows[0].id}` }, 201);
  }));

  app.get('/v1/uploads/files/:uploadId', route(async (req, res) => {
    const found = await db.query('SELECT content_type, content FROM publication_uploads WHERE id=$1', [req.params.uploadId]);
    if (!found.rowCount) return failure(res, 404, 'Média introuvable');
    res.set('Content-Type', found.rows[0].content_type);
    res.set('Cache-Control', 'public, max-age=31536000, immutable');
    res.send(found.rows[0].content);
  }));

  app.get(['/health', '/v1/health'], route(async (_req, res) => {
    const started = Date.now();
    await db.query('SELECT 1');
    success(res, {
      status: 'online',
      version: API_VERSION,
      databaseLatencyMs: Date.now() - started,
      uptimeSec: Math.floor(process.uptime()),
      timestamp: new Date().toISOString(),
    });
  }));

  app.get('/v1/readiness', route(async (_req, res) => {
    const started = Date.now();
    await db.query('SELECT 1');
    const [aiReady, paymentsReady, pushReady] = await Promise.all([
      probeLunaProvider(),
      probeLoukaPayProvider(),
      probeFirebaseProvider(),
    ]);
    const capabilities = {
      database: true,
      emailOtp: Boolean(process.env.BREVO_API_KEY),
      liveTurn: Boolean(process.env.MBOTE_TURN_URL && process.env.MBOTE_TURN_USERNAME && process.env.MBOTE_TURN_CREDENTIAL),
      ai: aiReady,
      payments: paymentsReady,
      paymentWebhook: Boolean(process.env.PAYMENTS_WEBHOOK_SECRET),
      push: pushReady,
      googleOAuthBackend: Boolean(process.env.GOOGLE_CLIENT_ID),
      githubOAuthBackend: Boolean(process.env.GITHUB_CLIENT_ID),
    };
    const coreRequired = ['database', 'emailOtp', 'liveTurn'];
    const fullRequired = ['database', 'emailOtp', 'liveTurn', 'ai', 'payments', 'paymentWebhook', 'push'];
    const coreReady = coreRequired.every((name) => capabilities[name] === true);
    const releaseReady = fullRequired.every((name) => capabilities[name] === true);
    const missingCapabilities = fullRequired.filter((name) => capabilities[name] !== true);
    success(res, {
      status: releaseReady ? 'ready' : coreReady ? 'partial' : 'degraded',
      coreReady,
      releaseReady,
      missingCapabilities,
      databaseLatencyMs: Date.now() - started,
      capabilities,
      timestamp: new Date().toISOString(),
    });
  }));

  const legalPage = (title, body) => `<!doctype html><html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title} — MBoté</title><style>body{font-family:system-ui,-apple-system,sans-serif;max-width:860px;margin:40px auto;padding:0 20px;line-height:1.6;color:#18181b}h1,h2{color:#5b21b6}small{color:#71717a}</style></head><body><h1>${title}</h1>${body}<hr><small>MBoté — LoukaTech · Mise à jour: 21 septembre 2026 · Contact: contacts@loukatech.com</small></body></html>`;

  app.get('/privacy', (_req,res) => res.type('html').send(legalPage('Politique de confidentialité', `
    <p>MBoté traite les informations nécessaires à la création et à la sécurisation du compte, à la messagerie, aux publications, aux appels, aux réunions et aux fonctionnalités choisies par l’utilisateur.</p>
    <h2>Données traitées</h2><p>Selon les fonctions utilisées: nom, identifiants de compte, adresse e-mail, numéro de téléphone, profil, contenus publiés, messages et métadonnées de communication, jetons de notification, ainsi que les médias transmis volontairement.</p>
    <h2>Permissions de l’appareil</h2><p>La caméra et le microphone sont utilisés uniquement lorsque l’utilisateur lance une fonction qui les nécessite. La localisation et les contacts ne sont utilisés qu’après autorisation pour les fonctions correspondantes. Les notifications servent à signaler les nouveaux événements du compte.</p>
    <h2>Sécurité et conservation</h2><p>Les échanges avec les serveurs utilisent HTTPS/WSS. Les données sont conservées le temps nécessaire au fonctionnement du service et aux obligations applicables. L’utilisateur peut supprimer son compte depuis l’application.</p>
    <h2>Prestataires</h2><p>MBoté peut utiliser des prestataires techniques pour l’hébergement, la base de données, l’envoi d’e-mails, les notifications, l’intelligence artificielle et le relais WebRTC. Ils reçoivent uniquement les données nécessaires au service fourni.</p>
    <h2>Vos choix</h2><p>Vous pouvez modifier vos informations, gérer les permissions Android, ajuster les notifications et demander la suppression du compte et des données associées.</p>
  `)));

  app.get('/account-deletion', (_req,res) => res.type('html').send(legalPage('Suppression du compte MBoté', `
    <p>Pour supprimer votre compte dans l’application: ouvrez <strong>Paramètres → Compte → Supprimer mon compte</strong> et confirmez la demande.</p>
    <p>Si vous n’avez plus accès à l’application, contactez <strong>contacts@loukatech.com</strong> depuis l’adresse e-mail associée au compte. Une vérification d’identité peut être demandée afin d’éviter la suppression frauduleuse d’un compte.</p>
    <p>La suppression retire le compte et les données directement rattachées conformément aux règles de conservation applicables. Certaines données techniques ou obligations légales peuvent nécessiter une conservation limitée.</p>
  `)));

  app.get('/terms', (_req,res) => res.type('html').send(legalPage('Conditions d’utilisation', `
    <p>MBoté est un service de communication et de réseau social. L’utilisateur est responsable du contenu qu’il publie et doit respecter les lois applicables, les droits des autres utilisateurs et les règles de sécurité du service.</p>
    <p>Sont interdits notamment l’usurpation d’identité, le harcèlement, la fraude, la diffusion non autorisée de données personnelles, les contenus illicites et les tentatives de contourner la sécurité du service.</p>
    <p>LoukaTech peut limiter ou suspendre un compte lorsque cela est nécessaire pour la sécurité du service ou le respect des règles applicables.</p>
  `)));

  app.post('/v1/auth/register', route(async (req, res) => {
    const fullName = text(req.body.name || req.body.fullName, 'Nom complet', 255);
    const email = text(req.body.email, 'Email', 255).toLowerCase();
    const password = text(req.body.password, 'Mot de passe', 256);
    if (!/^\S+@\S+\.\S+$/.test(email)) return failure(res, 400, 'Email invalide');
    if (password.length < 12) return failure(res, 400, 'Le mot de passe doit contenir au moins 12 caractères');
    const exists = await db.query('SELECT 1 FROM users WHERE email = $1 OR username = $2', [email, req.body.username?.trim() || null]);
    if (exists.rowCount) return failure(res, 409, 'Un compte existe déjà avec cet email ou ce nom utilisateur');
    const payload = {
      email, passwordHash: await bcrypt.hash(password, 12), fullName,
      username: req.body.username?.trim() || null, phone: req.body.phoneNumber?.trim() || req.body.phone?.trim() || null,
      country: req.body.country?.trim() || 'Congo', city: req.body.city?.trim() || '',
      bio: req.body.bio?.trim() || '', accountType: req.body.accountType || 'personal',
      accountVisibility: req.body.accountVisibility || 'public',
    };
    success(res, await createAuthChallenge({ flow: 'REGISTER', email, payload }), 201);
  }));

  app.post('/v1/auth/verify-registration-otp', route(async (req, res) => {
    // Validate before opening the account-creation transaction so a failed OTP
    // attempt is persisted instead of being undone by the rollback below.
    const challenge = await consumeAuthChallenge(req.body.pendingUserId, req.body.otp, 'REGISTER');
    await db.query('BEGIN');
    try {
      const p = challenge.payload;
      const result = await db.query(
        `INSERT INTO users (email, password_hash, full_name, username, phone, country, city, bio, is_verified)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, true) RETURNING *`,
        [p.email, p.passwordHash, p.fullName, p.username || null, p.phone || null, p.country || 'Congo', p.city || null, p.bio || null],
      );
      await db.query('DELETE FROM auth_challenges WHERE id = $1', [challenge.id]);
      await db.query('COMMIT');
      const user = result.rows[0];
      return success(res, { token: tokenFor(user), user: { ...publicUser(user), username: user.username || '', phone_number: user.phone || '', country: user.country || '', city: user.city || '', account_type: p.accountType, account_visibility: p.accountVisibility } }, 201);
    } catch (error) { await db.query('ROLLBACK'); throw error; }
  }));

  app.post('/v1/auth/login', route(async (req, res) => {
    const email = text(req.body.email, 'Email', 255).toLowerCase();
    const password = text(req.body.password, 'Mot de passe', 256);
    const result = await db.query('SELECT * FROM users WHERE email = $1', [email]);
    const user = result.rows[0];
    if (!user || !user.password_hash || !(await bcrypt.compare(password, user.password_hash))) return failure(res, 401, 'Identifiants invalides');
    success(res, await createAuthChallenge({ flow: 'LOGIN', email, userId: user.id }));
  }));

  app.post('/v1/auth/verify-login-otp', route(async (req, res) => {
    // Keep invalid-attempt accounting outside the transaction rolled back below.
    const challenge = await consumeAuthChallenge(req.body.pendingUserId, req.body.otp, 'LOGIN');
    await db.query('BEGIN');
    try {
      const result = await db.query('SELECT * FROM users WHERE id = $1', [challenge.user_id]);
      if (!result.rowCount) throw Object.assign(new Error('Compte introuvable'), { status: 404 });
      await db.query('DELETE FROM auth_challenges WHERE id = $1', [challenge.id]);
      await db.query('COMMIT');
      const user = result.rows[0];
      return success(res, { token: tokenFor(user), user: { ...publicUser(user), username: user.username || '', phone_number: user.phone || '', country: user.country || '', city: user.city || '' } });
    } catch (error) { await db.query('ROLLBACK'); throw error; }
  }));

  app.get('/v1/auth/me', auth, route(async (req, res) => {
    const result = await db.query('SELECT * FROM users WHERE id = $1', [req.user.userId]);
    return result.rowCount ? success(res, publicUser(result.rows[0])) : failure(res, 401, 'Compte introuvable');
  }));

  app.post('/v1/auth/qr/pairings', rateLimit({ windowMs: 60_000, max: 10, keyPrefix: 'qr-pairing-create' }), route(async (_req, res) => {
    const pairingToken = crypto.randomBytes(32).toString('base64url');
    const tokenHash = crypto.createHash('sha256').update(pairingToken).digest('hex');
    const result = await db.query(
      "INSERT INTO desktop_login_pairings(token_hash) VALUES($1) RETURNING expires_at",
      [tokenHash],
    );
    return success(res, { pairingToken, expiresAt: result.rows[0].expires_at }, 201);
  }));

  app.get('/v1/auth/qr/pairings/:pairingToken', rateLimit({ windowMs: 60_000, max: 120, keyPrefix: 'qr-pairing-poll' }), route(async (req, res) => {
    const pairingToken = text(req.params.pairingToken, 'Jeton de connexion QR', 256);
    if (!/^[A-Za-z0-9_-]{32,256}$/.test(pairingToken)) return failure(res, 400, 'Jeton de connexion QR invalide');
    const tokenHash = crypto.createHash('sha256').update(pairingToken).digest('hex');
    const result = await db.query(
      `UPDATE desktop_login_pairings pairing
       SET consumed_at=NOW()
       FROM users
       WHERE pairing.token_hash=$1
         AND pairing.confirmed_by=users.id
         AND pairing.confirmed_at IS NOT NULL
         AND pairing.consumed_at IS NULL
         AND pairing.expires_at>NOW()
       RETURNING users.*`,
      [tokenHash],
    );
    if (!result.rowCount) {
      const pending = await db.query(
        'SELECT confirmed_at, consumed_at, expires_at FROM desktop_login_pairings WHERE token_hash=$1',
        [tokenHash],
      );
      if (!pending.rowCount || new Date(pending.rows[0].expires_at).getTime() <= Date.now()) return failure(res, 410, 'Session QR expirée');
      if (pending.rows[0].consumed_at) return failure(res, 410, 'Session QR déjà utilisée');
      return success(res, { status: 'PENDING' });
    }
    const user = result.rows[0];
    return success(res, { status: 'CONFIRMED', authToken: tokenFor(user), user: publicUser(user) });
  }));

  app.post('/v1/auth/qr/confirm', auth, rateLimit({ windowMs: 60_000, max: 20, keyPrefix: 'qr-pairing-confirm' }), route(async (req, res) => {
    const pairingToken = text(req.body.pairingToken, 'Jeton de connexion QR', 256);
    if (!/^[A-Za-z0-9_-]{32,256}$/.test(pairingToken)) return failure(res, 400, 'Jeton de connexion QR invalide');
    const tokenHash = crypto.createHash('sha256').update(pairingToken).digest('hex');
    const result = await db.query(
      `UPDATE desktop_login_pairings
       SET confirmed_by=$2, confirmed_at=NOW()
       WHERE token_hash=$1
         AND confirmed_at IS NULL
         AND consumed_at IS NULL
         AND expires_at>NOW()
       RETURNING expires_at`,
      [tokenHash, req.user.userId],
    );
    if (!result.rowCount) return failure(res, 410, 'Session QR expirée ou déjà utilisée');
    return success(res, { confirmed: true, expiresAt: result.rows[0].expires_at });
  }));

  const getAdminStats = async () => {
    const result = await db.query(`
      SELECT
        (SELECT COUNT(*)::int FROM users) AS "activeUsersCount",
        (SELECT COUNT(*)::bigint FROM messages WHERE created_at >= CURRENT_DATE) AS "totalMessagesToday",
        (SELECT COUNT(*)::int FROM group_call_sessions WHERE status = 'ACTIVE') AS "activeCallsCount",
        (SELECT COUNT(*)::int FROM short_videos) AS "shortVideosTotal",
        (SELECT COALESCE(SUM(amount_fcfa), 0)::bigint FROM gift_transactions WHERE status = 'COMPLETED') AS "totalMobileMoneyTipsFcfa"
    `);
    const stats = result.rows[0] || {};
    return {
      activeUsersCount: Number(stats.activeUsersCount || 0),
      onlineNowCount: 0,
      totalMessagesToday: Number(stats.totalMessagesToday || 0),
      activeCallsCount: Number(stats.activeCallsCount || 0),
      shortVideosTotal: Number(stats.shortVideosTotal || 0),
      totalMobileMoneyTipsFcfa: Number(stats.totalMobileMoneyTipsFcfa || 0),
      serverUptimeSec: Math.floor(process.uptime()),
      cpuUsagePercent: 0,
      ramUsageMb: Math.round(process.memoryUsage().rss / 1024 / 1024),
      databaseStatus: 'Opérationnel',
      apiVersion: API_VERSION,
    };
  };

  app.post('/v1/admin/login', rateLimit({ windowMs: 15 * 60_000, max: 10, keyPrefix: 'admin-login' }), route(async (req, res) => {
    const configuredKey = String(process.env.ADMIN_API_KEY || '');
    if (!configuredKey) return failure(res, 503, 'Accès administrateur non configuré');
    if (!safeSecretEqual(req.body.adminKey, configuredKey)) return failure(res, 401, 'Identifiants administrateur invalides');
    const email = text(req.body.email, 'Email', 255).toLowerCase();
    const password = text(req.body.password, 'Mot de passe', 256);
    const result = await db.query('SELECT * FROM users WHERE email = $1', [email]);
    const user = result.rows[0];
    if (!user || !user.password_hash || !['ADMIN', 'MODERATOR'].includes(String(user.role || '').toUpperCase()) || !(await bcrypt.compare(password, user.password_hash))) {
      return failure(res, 401, 'Identifiants administrateur invalides');
    }
    return success(res, await getAdminStats());
  }));

  app.get('/v1/admin/stats', auth, adminOnly, route(async (_req, res) => success(res, await getAdminStats())));
  app.post('/v1/auth/google', route(async (req, res) => {
    const idToken = text(req.body.idToken, 'Jeton Google', 10000);
    if (!process.env.GOOGLE_CLIENT_ID) return failure(res, 503, 'Google OAuth n’est pas configuré');
    const verify = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`);
    if (!verify.ok) return failure(res, 401, 'Jeton Google invalide');
    const identity = await verify.json();
    if (identity.aud !== process.env.GOOGLE_CLIENT_ID || identity.email_verified !== 'true') {
      return failure(res, 401, 'Identité Google non vérifiée');
    }
    const email = String(identity.email).toLowerCase();
    const result = await db.query(
      `INSERT INTO users (email, full_name, avatar_url, is_verified)
       VALUES ($1, $2, $3, true)
       ON CONFLICT (email) DO UPDATE SET
         full_name = EXCLUDED.full_name,
         avatar_url = COALESCE(EXCLUDED.avatar_url, users.avatar_url),
         is_verified = true,
         updated_at = NOW()
       RETURNING *`,
      [email, String(identity.name || email).slice(0, 255), identity.picture || null],
    );
    const user = result.rows[0];
    success(res, { token: tokenFor(user), refreshToken: null, userId: user.id, ...publicUser(user) });
  }));

  app.post('/v1/auth/github', route(async (req, res) => {
    const accessToken = text(req.body.accessToken, 'Jeton GitHub', 1000);
    if (!process.env.GITHUB_CLIENT_ID) return failure(res, 503, 'GitHub OAuth n’est pas configuré');
    const headers = { authorization: `Bearer ${accessToken}`, accept: 'application/vnd.github+json', 'user-agent': 'MBote-Mobile' };
    const [profileResponse, emailsResponse] = await Promise.all([
      fetch('https://api.github.com/user', { headers }),
      fetch('https://api.github.com/user/emails', { headers }),
    ]);
    if (!profileResponse.ok || !emailsResponse.ok) return failure(res, 401, 'Jeton GitHub invalide');
    const profile = await profileResponse.json();
    const emails = await emailsResponse.json();
    const primary = emails.find((item) => item.primary && item.verified) || emails.find((item) => item.verified);
    if (!primary?.email) return failure(res, 401, 'Adresse GitHub vérifiée requise');
    const email = String(primary.email).toLowerCase();
    const result = await db.query(
      `INSERT INTO users (email, full_name, username, avatar_url, is_verified)
       VALUES ($1, $2, $3, $4, true)
       ON CONFLICT (email) DO UPDATE SET
         full_name = EXCLUDED.full_name,
         username = COALESCE(users.username, EXCLUDED.username),
         avatar_url = COALESCE(EXCLUDED.avatar_url, users.avatar_url),
         is_verified = true,
         updated_at = NOW()
       RETURNING *`,
      [email, String(profile.name || profile.login).slice(0, 255), String(profile.login).slice(0, 100), profile.avatar_url || null],
    );
    const user = result.rows[0];
    success(res, { token: tokenFor(user), refreshToken: null, userId: user.id, ...publicUser(user) });
  }));

  app.post('/v1/auth/forgot-password', route(async (req, res) => {
    const email = text(req.body.email, 'Email', 255).toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(email)) return failure(res, 400, 'Email invalide');
    if (!process.env.BREVO_API_KEY) return failure(res, 503, 'Le service e-mail est temporairement indisponible');

    const found = await db.query('SELECT id FROM users WHERE email = $1', [email]);
    // Always return the same public response to prevent account enumeration.
    if (!found.rowCount) return success(res, { message: 'Si ce compte existe, un code a été envoyé.' });

    const code = String(crypto.randomInt(0, 1_000_000)).padStart(6, '0');
    const codeHash = crypto.createHash('sha256').update(code).digest('hex');
    await db.query(
      `INSERT INTO password_reset_tokens (user_id, code_hash, expires_at)
       VALUES ($1, $2, NOW() + INTERVAL '15 minutes')
       ON CONFLICT (user_id) DO UPDATE SET code_hash = EXCLUDED.code_hash, expires_at = EXCLUDED.expires_at, attempts = 0`,
      [found.rows[0].id, codeHash],
    );

    const mailResponse = await fetch(process.env.BREVO_API_URL || 'https://api.brevo.com/v3/smtp/email', {
      method: 'POST',
      headers: { 'api-key': process.env.BREVO_API_KEY, 'content-type': 'application/json', accept: 'application/json' },
      body: JSON.stringify({
        sender: { name: process.env.BREVO_SENDER_NAME || 'MBoté Sécurité', email: process.env.BREVO_SENDER_EMAIL || 'noreply@loukatech.com' },
        to: [{ email }],
        subject: 'Code de réinitialisation MBoté',
        htmlContent: `<p>Votre code MBoté est <strong>${code}</strong>.</p><p>Il expire dans 15 minutes.</p>`,
      }),
    });
    if (!mailResponse.ok) {
      await db.query('DELETE FROM password_reset_tokens WHERE user_id = $1', [found.rows[0].id]);
      throw Object.assign(new Error('Le code n’a pas pu être envoyé'), { status: 502 });
    }
    return success(res, { message: 'Si ce compte existe, un code a été envoyé.' });
  }));

  app.post('/v1/auth/reset-password-confirm', route(async (req, res) => {
    const email = text(req.body.email, 'Email', 255).toLowerCase();
    const code = text(req.body.resetCode || req.body.code, 'Code', 6);
    const newPassword = text(req.body.newPassword, 'Nouveau mot de passe', 256);
    if (!/^\d{6}$/.test(code)) return failure(res, 400, 'Code invalide');
    if (newPassword.length < 12) return failure(res, 400, 'Le mot de passe doit contenir au moins 12 caractères');

    const result = await db.query(
      `SELECT pr.user_id, pr.code_hash, pr.attempts
         FROM password_reset_tokens pr JOIN users u ON u.id = pr.user_id
        WHERE u.email = $1 AND pr.expires_at > NOW()`,
      [email],
    );
    const token = result.rows[0];
    const submittedHash = crypto.createHash('sha256').update(code).digest('hex');
    if (!token || token.attempts >= 5 || token.code_hash.length !== submittedHash.length ||
        !crypto.timingSafeEqual(Buffer.from(token.code_hash), Buffer.from(submittedHash))) {
      if (token) await db.query('UPDATE password_reset_tokens SET attempts = attempts + 1 WHERE user_id = $1', [token.user_id]);
      return failure(res, 400, 'Code expiré ou invalide');
    }

    await db.query('BEGIN');
    try {
      await db.query('UPDATE users SET password_hash = $2, updated_at = NOW() WHERE id = $1', [token.user_id, await bcrypt.hash(newPassword, 12)]);
      await db.query('DELETE FROM password_reset_tokens WHERE user_id = $1', [token.user_id]);
      await db.query('COMMIT');
    } catch (error) {
      await db.query('ROLLBACK');
      throw error;
    }
    return success(res, true);
  }));

  app.get('/v1/chats', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT c.id, COALESCE(c.name, 'Conversation') AS name, COALESCE(c.avatar_url, '') AS avatar,
              c.is_group AS "isGroup", c.is_channel AS "isChannel", COALESCE(last_message.text, '') AS "lastMessage",
              last_message.created_at AS "lastMessageTime", 0 AS "unreadCount", false AS "isOnline"
         FROM chats c JOIN chat_participants cp ON cp.chat_id = c.id AND cp.user_id = $1
         LEFT JOIN LATERAL (SELECT text, created_at FROM messages WHERE chat_id = c.id ORDER BY created_at DESC LIMIT 1) last_message ON true
        ORDER BY last_message.created_at DESC NULLS LAST, c.created_at DESC`, [req.user.userId],
    );
    success(res, result.rows);
  }));
  app.get('/v1/chats/:chatId/messages', auth, route(async (req, res) => {
    if (!(await member(req.params.chatId, req.user.userId))) return failure(res, 403, 'Accès à cette conversation refusé');
    const result = await db.query(
      `SELECT m.*, u.full_name AS sender_name, u.avatar_url AS sender_avatar,
              EXISTS(SELECT 1 FROM message_stars ms WHERE ms.message_id = m.id AND ms.user_id = $2) AS is_starred
         FROM messages m JOIN users u ON u.id = m.sender_id WHERE m.chat_id = $1 ORDER BY m.created_at ASC`, [req.params.chatId, req.user.userId],
    );
    success(res, result.rows.map((row) => messageDto(row, req.user.userId)));
  }));
  const sendMessage = async (req, res) => {
    const { chatId, text: bodyText = '', mediaType = 'NONE', mediaUrl = null, replyToMessageId = null } = req.body;
    if (!chatId || (!String(bodyText).trim() && !mediaUrl)) return failure(res, 400, 'Un message ou un média est requis');
    if (!(await member(chatId, req.user.userId))) return failure(res, 403, 'Accès à cette conversation refusé');
    const inserted = await db.query('INSERT INTO messages (chat_id, sender_id, text, media_type, media_url, reply_to_id) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *', [chatId, req.user.userId, String(bodyText).trim() || null, mediaType, mediaUrl, replyToMessageId]);
    const user = await db.query('SELECT full_name, avatar_url FROM users WHERE id = $1', [req.user.userId]);
    const dto = messageDto({ ...inserted.rows[0], sender_name: user.rows[0].full_name, sender_avatar: user.rows[0].avatar_url }, req.user.userId);
    const recipients = await db.query('SELECT user_id FROM chat_participants WHERE chat_id=$1 AND user_id<>$2', [chatId, req.user.userId]);
    for (const row of recipients.rows) {
      const recipientId = String(row.user_id);
      realtimeHub.sendToUser(recipientId, { type: 'CHAT_MESSAGE', ...dto, isMine: false });
      if (!realtimeHub.isUserConnected(recipientId)) {
        void sendPushToUser(
          recipientId,
          { title: user.rows[0].full_name || 'Nouveau message MBoté', body: dto.text || (dto.mediaUrl ? 'Vous avez reçu un média.' : 'Nouveau message') },
          { type: 'message', chatId: String(chatId), senderId: String(req.user.userId), messageId: String(dto.id) },
        );
      }
    }
    return success(res, dto, 201);
  };
  app.post('/v1/messages/send', auth, route(sendMessage));
  app.post('/v1/chats/:chatId/messages', auth, route((req, res) => sendMessage({ ...req, body: { ...req.body, chatId: req.params.chatId } }, res)));
  app.delete('/v1/messages/:messageId', auth, route(async (req, res) => {
    const result = await db.query('DELETE FROM messages WHERE id = $1 AND sender_id = $2 RETURNING id', [req.params.messageId, req.user.userId]);
    return result.rowCount ? success(res, true) : failure(res, 404, 'Message introuvable ou non modifiable');
  }));
  app.post('/v1/messages/:messageId/star', auth, route(async (req, res) => {
    const changed = await db.query('INSERT INTO message_stars (message_id, user_id) VALUES ($1, $2) ON CONFLICT (message_id, user_id) DO NOTHING RETURNING message_id', [req.params.messageId, req.user.userId]);
    if (!changed.rowCount) await db.query('DELETE FROM message_stars WHERE message_id = $1 AND user_id = $2', [req.params.messageId, req.user.userId]);
    success(res, changed.rowCount > 0);
  }));

  app.get('/v1/publications', auth, route(async (req, res) => {
    const params = [req.user.userId]; let where = '';
    if (req.query.category && req.query.category !== 'TOUS') { params.push(req.query.category); where = `WHERE n.category = $${params.length}`; }
    const result = await db.query(
      `SELECT n.id, u.full_name AS "authorName", COALESCE(u.avatar_url, '') AS "authorAvatar", COALESCE(u.bio, 'Membre MBoté') AS "authorTitle",
              n.content AS "contentText", n.image_url AS "mediaUrl", COALESCE(n.media_type, 'TEXT') AS "mediaType", n.created_at AS timestamp,
              n.likes_count AS "likesCount", n.comments_count AS "commentsCount", n.shares_count AS "sharesCount",
              EXISTS(SELECT 1 FROM news_post_likes nl WHERE nl.news_post_id = n.id AND nl.user_id = $1) AS "isLikedByMe", n.category
         FROM news_posts n JOIN users u ON u.id = n.author_id ${where} ORDER BY n.created_at DESC`, params,
    );
    success(res, result.rows);
  }));
  app.post('/v1/publications', auth, route(async (req, res) => {
    const content = text(req.body.contentText, 'Publication');
    const created = await db.query('INSERT INTO news_posts (author_id, category, title, content, image_url, media_type) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *', [req.user.userId, req.body.category?.trim() || 'GÉNÉRAL', content.slice(0, 255), content, req.body.mediaUrl || null, req.body.mediaType || 'TEXT']);
    const user = await db.query('SELECT full_name, avatar_url, bio FROM users WHERE id = $1', [req.user.userId]);
    const post = created.rows[0];
    success(res, { id: post.id, authorName: user.rows[0].full_name, authorAvatar: user.rows[0].avatar_url || '', authorTitle: user.rows[0].bio || 'Membre MBoté', contentText: post.content, mediaUrl: post.image_url, mediaType: post.media_type, timestamp: post.created_at, likesCount: 0, commentsCount: 0, sharesCount: 0, isLikedByMe: false, category: post.category }, 201);
  }));
  app.post('/v1/publications/:postId/like', auth, route(async (req, res) => {
    const changed = await db.query('INSERT INTO news_post_likes (news_post_id, user_id) VALUES ($1, $2) ON CONFLICT (news_post_id, user_id) DO NOTHING RETURNING news_post_id', [req.params.postId, req.user.userId]);
    if (!changed.rowCount) await db.query('DELETE FROM news_post_likes WHERE news_post_id = $1 AND user_id = $2', [req.params.postId, req.user.userId]);
    const count = await db.query('SELECT COUNT(*)::int AS count FROM news_post_likes WHERE news_post_id = $1', [req.params.postId]);
    await db.query('UPDATE news_posts SET likes_count = $2 WHERE id = $1', [req.params.postId, count.rows[0].count]);
    success(res, { postId: req.params.postId, isLiked: changed.rowCount > 0, totalLikes: count.rows[0].count });
  }));
  app.post('/v1/publications/:postId/comments', auth, route(async (req, res) => {
    const created = await db.query('INSERT INTO news_post_comments (news_post_id, author_id, text) VALUES ($1, $2, $3) RETURNING *', [req.params.postId, req.user.userId, text(req.body.text, 'Commentaire')]);
    const user = await db.query('SELECT full_name, avatar_url FROM users WHERE id = $1', [req.user.userId]);
    await db.query('UPDATE news_posts SET comments_count = (SELECT COUNT(*) FROM news_post_comments WHERE news_post_id = $1) WHERE id = $1', [req.params.postId]);
    success(res, { id: created.rows[0].id, postId: req.params.postId, authorName: user.rows[0].full_name, authorAvatar: user.rows[0].avatar_url || '', commentText: created.rows[0].text, timestamp: created.rows[0].created_at }, 201);
  }));

  app.get('/v1/short-videos', auth, route(async (req, res) => {
    const limit = Math.min(Math.max(Number(req.query.limit) || 12, 1), 50);
    const result = await db.query(
      `SELECT s.id, s.creator_id AS user_id, u.full_name AS user_name,
              COALESCE(u.username, '') AS user_username, COALESCE(u.avatar_url, '') AS user_avatar,
              COALESCE(s.caption, '') AS caption, s.video_url, s.thumbnail_url,
              s.music_track AS music_name, s.duration_seconds,
              (SELECT COUNT(*)::int FROM short_video_reactions r WHERE r.short_video_id = s.id) AS like_count,
              EXISTS(SELECT 1 FROM short_video_reactions r WHERE r.short_video_id = s.id AND r.user_id = $1) AS liked_by_me,
              (SELECT COUNT(*)::int FROM short_video_comments c WHERE c.short_video_id = s.id) AS comment_count,
              (SELECT COUNT(*)::int FROM short_video_shares sh WHERE sh.short_video_id = s.id) AS share_count,
              (SELECT COUNT(*)::int FROM short_video_bookmarks b WHERE b.short_video_id = s.id) AS bookmark_count,
              EXISTS(SELECT 1 FROM short_video_bookmarks b WHERE b.short_video_id = s.id AND b.user_id = $1) AS saved_by_me,
              EXISTS(SELECT 1 FROM user_follows f WHERE f.follower_id = $1 AND f.followed_id = s.creator_id) AS followed_by_me,
              (SELECT COUNT(*)::int FROM short_video_views v WHERE v.short_video_id = s.id) AS view_count,
              s.created_at
         FROM short_videos s JOIN users u ON u.id = s.creator_id
        WHERE s.visibility = 'public' OR s.creator_id = $1
        ORDER BY s.created_at DESC LIMIT $2`, [req.user.userId, limit],
    );
    success(res, result.rows);
  }));

  app.post('/v1/short-videos', auth, route(async (req, res) => {
    const videoUrl = text(req.body.videoUrl, 'URL de vidéo', 2000);
    const result = await db.query(
      `INSERT INTO short_videos
         (creator_id, video_url, thumbnail_url, caption, music_track, duration_seconds, visibility)
       VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *`,
      [req.user.userId, videoUrl, req.body.thumbnailUrl || null, req.body.caption || null,
       req.body.musicName || null, Math.max(Number(req.body.durationSeconds) || 0, 0),
       req.body.visibility === 'private' ? 'private' : 'public'],
    );
    const user = await db.query('SELECT full_name, username, avatar_url FROM users WHERE id = $1', [req.user.userId]);
    const row = result.rows[0];
    success(res, {
      id: row.id, user_id: req.user.userId, user_name: user.rows[0].full_name,
      user_username: user.rows[0].username || '', user_avatar: user.rows[0].avatar_url || '',
      caption: row.caption || '', video_url: row.video_url, thumbnail_url: row.thumbnail_url,
      music_name: row.music_track, duration_seconds: row.duration_seconds, like_count: 0,
      liked_by_me: false, comment_count: 0, share_count: 0, bookmark_count: 0,
      saved_by_me: false, followed_by_me: false, view_count: 0, created_at: row.created_at,
    }, 201);
  }));

  app.post('/v1/short-videos/:videoId/likes', auth, route(async (req, res) => {
    const inserted = await db.query(
      `INSERT INTO short_video_reactions (short_video_id, user_id, emoji)
       VALUES ($1, $2, '❤️') ON CONFLICT (short_video_id, user_id) DO NOTHING RETURNING id`,
      [req.params.videoId, req.user.userId],
    );
    if (!inserted.rowCount) await db.query('DELETE FROM short_video_reactions WHERE short_video_id = $1 AND user_id = $2', [req.params.videoId, req.user.userId]);
    const count = await db.query('SELECT COUNT(*)::int AS count FROM short_video_reactions WHERE short_video_id = $1', [req.params.videoId]);
    await db.query('UPDATE short_videos SET likes_count = $2 WHERE id = $1', [req.params.videoId, count.rows[0].count]);
    success(res, { likeCount: count.rows[0].count, likedByMe: inserted.rowCount > 0 });
  }));

  app.get('/v1/short-videos/:videoId/comments', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT c.id, u.full_name AS user_name, COALESCE(u.avatar_url, '') AS user_avatar,
              c.text AS content, c.created_at
         FROM short_video_comments c JOIN users u ON u.id = c.author_id
        WHERE c.short_video_id = $1 ORDER BY c.created_at ASC LIMIT 100`, [req.params.videoId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/short-videos/:videoId/comments', auth, route(async (req, res) => {
    const content = text(req.body.content, 'Commentaire', 2000);
    const created = await db.query(
      'INSERT INTO short_video_comments (short_video_id, author_id, text) VALUES ($1, $2, $3) RETURNING *',
      [req.params.videoId, req.user.userId, content],
    );
    const user = await db.query('SELECT full_name, avatar_url FROM users WHERE id = $1', [req.user.userId]);
    success(res, { id: created.rows[0].id, user_name: user.rows[0].full_name, user_avatar: user.rows[0].avatar_url || '', content, created_at: created.rows[0].created_at }, 201);
  }));

  app.post('/v1/short-videos/:videoId/bookmarks', auth, route(async (req, res) => {
    const inserted = await db.query(
      'INSERT INTO short_video_bookmarks (short_video_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING RETURNING short_video_id',
      [req.params.videoId, req.user.userId],
    );
    if (!inserted.rowCount) await db.query('DELETE FROM short_video_bookmarks WHERE short_video_id = $1 AND user_id = $2', [req.params.videoId, req.user.userId]);
    const count = await db.query('SELECT COUNT(*)::int AS count FROM short_video_bookmarks WHERE short_video_id = $1', [req.params.videoId]);
    await db.query('UPDATE short_videos SET bookmarks_count = $2 WHERE id = $1', [req.params.videoId, count.rows[0].count]);
    success(res, { bookmarkCount: count.rows[0].count, savedByMe: inserted.rowCount > 0 });
  }));

  app.post('/v1/short-videos/authors/:authorId/follow', auth, route(async (req, res) => {
    const inserted = await db.query(
      'INSERT INTO user_follows (follower_id, followed_id) VALUES ($1, $2) ON CONFLICT DO NOTHING RETURNING followed_id',
      [req.user.userId, req.params.authorId],
    );
    if (!inserted.rowCount) await db.query('DELETE FROM user_follows WHERE follower_id = $1 AND followed_id = $2', [req.user.userId, req.params.authorId]);
    const count = await db.query('SELECT COUNT(*)::int AS count FROM user_follows WHERE followed_id = $1', [req.params.authorId]);
    success(res, { followerCount: count.rows[0].count, followedByMe: inserted.rowCount > 0 });
  }));

  app.post('/v1/short-videos/:videoId/shares', auth, route(async (req, res) => {
    await db.query('INSERT INTO short_video_shares (short_video_id, user_id, target_chat_id) VALUES ($1, $2, $3)', [req.params.videoId, req.user.userId, req.body.targetChatId || null]);
    const count = await db.query('SELECT COUNT(*)::int AS count FROM short_video_shares WHERE short_video_id = $1', [req.params.videoId]);
    await db.query('UPDATE short_videos SET shares_count = $2 WHERE id = $1', [req.params.videoId, count.rows[0].count]);
    success(res, { shareCount: count.rows[0].count });
  }));

  app.post('/v1/short-videos/:videoId/views', auth, route(async (req, res) => {
    await db.query('INSERT INTO short_video_views (short_video_id, viewer_id) VALUES ($1, $2) ON CONFLICT DO NOTHING', [req.params.videoId, req.user.userId]);
    success(res, true);
  }));

  app.get('/v1/shorts/videos', auth, route(async (_req, res) => {
    const result = await db.query(
      `SELECT s.id, s.creator_id AS "creatorId", u.full_name AS "creatorName", COALESCE(u.username, '') AS "creatorUsername",
              COALESCE(u.avatar_url, '') AS "creatorAvatar", COALESCE(u.bio, '') AS "creatorBio", u.is_verified AS "isCreatorVerified",
              false AS "isFollowing", COALESCE(s.thumbnail_url, s.video_url) AS "videoThumbnailUrl", s.video_url AS "videoPlaybackUrl",
              COALESCE(s.caption, '') AS caption, ARRAY[]::text[] AS hashtags, COALESCE(s.music_track, '') AS "musicTitle", '' AS "musicArtist",
              '' AS "musicCoverUrl", s.likes_count AS "likesCount", false AS "isLiked", NULL::text AS "userReaction",
              '{}'::json AS "reactionsCount", s.comments_count AS "commentsCount", s.shares_count AS "sharesCount",
              s.bookmarks_count AS "bookmarksCount", false AS "isBookmarked", 0 AS "viewsCount", '0:00' AS "durationFormatted",
              s.location, s.created_at AS timestamp, 'GÉNÉRAL' AS category, '[]'::json AS comments
         FROM short_videos s JOIN users u ON u.id = s.creator_id ORDER BY s.created_at DESC`,
    );
    success(res, result.rows);
  }));
  app.post('/v1/shorts/create', auth, route(async (req, res) => {
    const created = await db.query('INSERT INTO short_videos (creator_id, video_url, thumbnail_url, caption, location, music_track) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *', [req.user.userId, text(req.body.videoUrl, 'URL de vidéo', 2000), req.body.thumbnailUrl || null, req.body.caption || null, req.body.location || null, req.body.musicTrack || null]);
    success(res, created.rows[0], 201);
  }));
  app.post('/v1/shorts/:videoId/like', auth, route(async (req, res) => {
    const liked = req.query.isLiked !== 'false';
    if (liked) await db.query('INSERT INTO short_video_reactions (short_video_id, user_id, emoji) VALUES ($1, $2, $3) ON CONFLICT (short_video_id, user_id) DO UPDATE SET emoji = EXCLUDED.emoji', [req.params.videoId, req.user.userId, '❤️']);
    else await db.query('DELETE FROM short_video_reactions WHERE short_video_id = $1 AND user_id = $2', [req.params.videoId, req.user.userId]);
    const count = await db.query('SELECT COUNT(*)::int AS count FROM short_video_reactions WHERE short_video_id = $1', [req.params.videoId]);
    await db.query('UPDATE short_videos SET likes_count = $2 WHERE id = $1', [req.params.videoId, count.rows[0].count]);
    success(res, liked);
  }));
  app.post('/v1/shorts/:videoId/comment', auth, route(async (req, res) => {
    const created = await db.query('INSERT INTO short_video_comments (short_video_id, author_id, text) VALUES ($1, $2, $3) RETURNING *', [req.params.videoId, req.user.userId, text(req.body.text, 'Commentaire')]);
    const user = await db.query('SELECT full_name, username, avatar_url FROM users WHERE id = $1', [req.user.userId]);
    await db.query('UPDATE short_videos SET comments_count = (SELECT COUNT(*) FROM short_video_comments WHERE short_video_id = $1) WHERE id = $1', [req.params.videoId]);
    success(res, { id: created.rows[0].id, authorName: user.rows[0].full_name, authorUsername: user.rows[0].username || '', authorAvatar: user.rows[0].avatar_url || '', text: created.rows[0].text, timestamp: created.rows[0].created_at, likesCount: 0, isLiked: false }, 201);
  }));
  app.get('/v1/masta/users', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT u.id, u.full_name AS name, COALESCE(u.avatar_url,'') AS avatar,
              COALESCE(u.city,u.country,'') AS "infoSubtitle", 0 AS "mutualFriendsCount",
              ARRAY[]::text[] AS "mutualFriendsAvatars", false AS "isOnline",
              COALESCE(u.city,'') AS city, NULL::text AS "timeBadge",
              CASE
                WHEN EXISTS(SELECT 1 FROM friend_requests f WHERE f.status='ACCEPTED' AND ((f.sender_id=$1 AND f.receiver_id=u.id) OR (f.receiver_id=$1 AND f.sender_id=u.id))) THEN 'FRIENDS'
                WHEN EXISTS(SELECT 1 FROM friend_requests f WHERE f.status='PENDING' AND f.receiver_id=$1 AND f.sender_id=u.id) THEN 'RECEIVED'
                WHEN EXISTS(SELECT 1 FROM friend_requests f WHERE f.status='PENDING' AND f.sender_id=$1 AND f.receiver_id=u.id) THEN 'SENT'
                ELSE 'SUGGESTIONS'
              END AS "subType"
         FROM users u
        WHERE u.id<>$1
          AND NOT EXISTS(SELECT 1 FROM blocked_users b WHERE (b.blocker_id=$1 AND b.blocked_id=u.id) OR (b.blocker_id=u.id AND b.blocked_id=$1))
        ORDER BY u.created_at DESC LIMIT 100`,
      [req.user.userId],
    );
    success(res, result.rows);
  }));

  app.get('/v1/masta/requests', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT f.id,
              CASE WHEN f.receiver_id=$1 THEN f.sender_id ELSE f.receiver_id END AS "userId",
              u.full_name AS name, COALESCE(u.phone,'') AS phone, COALESCE(u.avatar_url,'') AS "avatarUrl",
              (f.receiver_id=$1) AS "isIncoming", f.created_at AS timestamp, f.status
         FROM friend_requests f
         JOIN users u ON u.id=CASE WHEN f.receiver_id=$1 THEN f.sender_id ELSE f.receiver_id END
        WHERE (f.sender_id=$1 OR f.receiver_id=$1) AND f.status='PENDING'
        ORDER BY f.created_at DESC`,
      [req.user.userId],
    );
    success(res,result.rows);
  }));

  app.post('/v1/masta/requests', auth, route(async (req,res)=>{
    const targetUserId=text(req.body.targetUserId,'Utilisateur',80);
    if(targetUserId===req.user.userId) return failure(res,400,'Vous ne pouvez pas vous ajouter vous-même');
    const target=await db.query('SELECT id FROM users WHERE id=$1',[targetUserId]);
    if(!target.rowCount) return failure(res,404,'Utilisateur introuvable');
    const blocked=await db.query('SELECT 1 FROM blocked_users WHERE (blocker_id=$1 AND blocked_id=$2) OR (blocker_id=$2 AND blocked_id=$1)',[req.user.userId,targetUserId]);
    if(blocked.rowCount) return failure(res,409,'Cette demande ne peut pas être envoyée');
    const existing=await db.query(
      "SELECT id,status FROM friend_requests WHERE ((sender_id=$1 AND receiver_id=$2) OR (sender_id=$2 AND receiver_id=$1)) AND status IN ('PENDING','ACCEPTED')",
      [req.user.userId,targetUserId],
    );
    if(existing.rowCount) return failure(res,409,existing.rows[0].status==='ACCEPTED'?'Vous êtes déjà Masta':'Une demande est déjà en attente');
    const created=await db.query(
      "INSERT INTO friend_requests(sender_id,receiver_id,status) VALUES($1,$2,'PENDING') RETURNING id,created_at",
      [req.user.userId,targetUserId],
    );
    success(res,created.rows[0],201);
  }));

  app.post('/v1/masta/requests/:requestId/accept', auth, route(async (req,res)=>{
    const accepted=await db.query(
      "UPDATE friend_requests SET status='ACCEPTED',responded_at=NOW() WHERE id=$1 AND receiver_id=$2 AND status='PENDING' RETURNING id,sender_id",
      [req.params.requestId,req.user.userId],
    );
    if(!accepted.rowCount) return failure(res,404,'Demande introuvable');
    success(res,{id:accepted.rows[0].id,friendUserId:accepted.rows[0].sender_id});
  }));

  app.delete('/v1/masta/requests/:requestId', auth, route(async (req,res)=>{
    const removed=await db.query(
      "UPDATE friend_requests SET status='DECLINED',responded_at=NOW() WHERE id=$1 AND (sender_id=$2 OR receiver_id=$2) AND status='PENDING' RETURNING id",
      [req.params.requestId,req.user.userId],
    );
    if(!removed.rowCount) return failure(res,404,'Demande introuvable');
    success(res,true);
  }));

  app.get('/v1/users/public', auth, route(async (req, res) => {
    const limit = Math.min(Math.max(Number(req.query.limit) || 50, 1), 100);
    const result = await db.query(
      `SELECT u.id, u.full_name AS name, COALESCE(u.username, '') AS username,
              COALESCE(u.avatar_url, '') AS avatar, COALESCE(u.bio, '') AS bio,
              COALESCE(u.city, '') AS city, COALESCE(u.country, '') AS country,
              EXISTS(SELECT 1 FROM blocked_users b WHERE b.blocker_id = $1 AND b.blocked_id = u.id) AS "blockedByMe"
         FROM users u WHERE u.id <> $1
           AND NOT EXISTS(SELECT 1 FROM blocked_users b WHERE b.blocker_id = u.id AND b.blocked_id = $1)
        ORDER BY u.created_at DESC LIMIT $2`,
      [req.user.userId, limit],
    );
    success(res, result.rows);
  }));

  app.get('/v1/users/me/settings', auth, route(async (req, res) => {
    const result = await db.query('SELECT value FROM user_settings WHERE user_id = $1', [req.user.userId]);
    success(res, { value: result.rows[0]?.value || {} });
  }));
  app.put('/v1/users/me/settings', auth, route(async (req, res) => {
    const value = req.body?.value;
    if (!value || typeof value !== 'object' || Array.isArray(value)) return failure(res, 400, 'Paramètres invalides');
    const result = await db.query(
      `INSERT INTO user_settings (user_id, value) VALUES ($1, $2)
       ON CONFLICT (user_id) DO UPDATE SET value = EXCLUDED.value, updated_at = NOW()
       RETURNING value`, [req.user.userId, value],
    );
    success(res, { value: result.rows[0].value });
  }));

  app.get('/v1/users/me/blocks', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT u.id, u.full_name AS name, COALESCE(u.avatar_url, '') AS avatar
         FROM blocked_users b JOIN users u ON u.id = b.blocked_id
        WHERE b.blocker_id = $1 ORDER BY b.created_at DESC`, [req.user.userId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/users/:userId/block', auth, route(async (req, res) => {
    await db.query('INSERT INTO blocked_users (blocker_id, blocked_id) VALUES ($1, $2) ON CONFLICT DO NOTHING', [req.user.userId, req.params.userId]);
    success(res, true);
  }));
  app.delete('/v1/users/:userId/block', auth, route(async (req, res) => {
    await db.query('DELETE FROM blocked_users WHERE blocker_id = $1 AND blocked_id = $2', [req.user.userId, req.params.userId]);
    success(res, true);
  }));

  app.post('/v1/reports', auth, route(async (req, res) => {
    const targetType = text(req.body.targetType, 'Type de cible', 50);
    const targetId = text(req.body.targetId, 'Cible', 255);
    const reason = text(req.body.reason, 'Motif', 2000);
    const result = await db.query(
      'INSERT INTO reports (reporter_id, target_type, target_id, reason) VALUES ($1, $2, $3, $4) RETURNING *',
      [req.user.userId, targetType, targetId, reason],
    );
    success(res, result.rows[0], 201);
  }));
  app.get('/v1/reports/mine', auth, route(async (req, res) => {
    const result = await db.query('SELECT * FROM reports WHERE reporter_id = $1 ORDER BY created_at DESC', [req.user.userId]);
    success(res, result.rows);
  }));

  app.get('/v1/calls/history', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT c.id, c.peer_user_id AS "peerUserId", COALESCE(u.full_name, 'Utilisateur') AS name,
              COALESCE(u.avatar_url, '') AS avatar, c.direction AS type,
              c.media_type = 'VIDEO' AS "isVideo", c.started_at AS timestamp,
              c.duration_seconds AS "durationSeconds", c.status
         FROM call_history c LEFT JOIN users u ON u.id = c.peer_user_id
        WHERE c.user_id = $1 ORDER BY c.started_at DESC LIMIT 200`, [req.user.userId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/calls/log', auth, route(async (req, res) => {
    const direction = text(req.body.direction || req.body.type, 'Direction', 20).toUpperCase();
    const mediaType = (req.body.isVideo ? 'VIDEO' : String(req.body.mediaType || 'AUDIO')).toUpperCase();
    const status = String(req.body.status || 'COMPLETED').toUpperCase().slice(0, 20);
    const duration = Math.max(0, Number(req.body.durationSeconds) || 0);
    const result = await db.query(
      `INSERT INTO call_history (user_id, peer_user_id, direction, media_type, status, duration_seconds)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [req.user.userId, req.body.peerUserId || null, direction, mediaType, status, duration],
    );
    success(res, result.rows[0], 201);
  }));

  app.get('/v1/meetings', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT m.*, u.full_name AS "hostName",
              (SELECT COUNT(*)::int FROM meeting_participants p WHERE p.meeting_id = m.id) AS "participantsCount"
         FROM meetings m JOIN users u ON u.id = m.host_id
        WHERE m.host_id = $1 OR EXISTS (SELECT 1 FROM meeting_participants p WHERE p.meeting_id = m.id AND p.user_id = $1)
        ORDER BY m.scheduled_at NULLS FIRST, m.created_at DESC`, [req.user.userId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/meetings', auth, route(async (req, res) => {
    const title = text(req.body.title, 'Titre', 255);
    const code = crypto.randomBytes(5).toString('hex').toUpperCase();
    const result = await db.query(
      `INSERT INTO meetings (host_id, title, code, scheduled_at, duration_minutes)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [req.user.userId, title, code, req.body.scheduledAt || null, Math.min(Math.max(Number(req.body.durationMinutes) || 30, 5), 1440)],
    );
    await db.query('INSERT INTO meeting_participants (meeting_id, user_id) VALUES ($1, $2)', [result.rows[0].id, req.user.userId]);
    success(res, result.rows[0], 201);
  }));
  app.post('/v1/meetings/:code/join', auth, route(async (req, res) => {
    const meeting = await db.query('SELECT * FROM meetings WHERE code = $1', [req.params.code.toUpperCase()]);
    if (!meeting.rowCount) return failure(res, 404, 'Réunion introuvable');
    await db.query('INSERT INTO meeting_participants (meeting_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING', [meeting.rows[0].id, req.user.userId]);
    success(res, meeting.rows[0]);
  }));

  app.get('/v1/statuses', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT s.*, u.full_name AS "authorName", COALESCE(u.avatar_url, '') AS "authorAvatar",
              EXISTS(SELECT 1 FROM status_views v WHERE v.status_id = s.id AND v.viewer_id = $1) AS "viewedByMe",
              (SELECT COUNT(*)::int FROM status_views v WHERE v.status_id = s.id) AS "viewsCount"
         FROM statuses s JOIN users u ON u.id = s.author_id
        WHERE s.expires_at > NOW() ORDER BY s.created_at DESC`, [req.user.userId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/statuses', auth, route(async (req, res) => {
    const mediaType = String(req.body.mediaType || 'TEXT').toUpperCase().slice(0, 20);
    const bodyText = typeof req.body.text === 'string' ? req.body.text.trim().slice(0, 5000) : null;
    const mediaUrl = typeof req.body.mediaUrl === 'string' ? req.body.mediaUrl.trim().slice(0, 2000) : null;
    if (!bodyText && !mediaUrl) return failure(res, 400, 'Contenu du statut requis');
    const result = await db.query(
      `INSERT INTO statuses (author_id, media_type, media_url, text, background_color)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [req.user.userId, mediaType, mediaUrl, bodyText, req.body.backgroundColor || null],
    );
    success(res, result.rows[0], 201);
  }));
  app.post('/v1/statuses/:statusId/view', auth, route(async (req, res) => {
    await db.query('INSERT INTO status_views (status_id, viewer_id) VALUES ($1, $2) ON CONFLICT DO NOTHING', [req.params.statusId, req.user.userId]);
    success(res, true);
  }));


  app.get('/v1/gifts/catalog', auth, route(async (_req, res) => {
    const result = await db.query('SELECT id,name,emoji,price_fcfa AS "priceFcfa",description FROM gift_catalog WHERE active=TRUE ORDER BY price_fcfa');
    success(res, result.rows);
  }));

  app.get('/v1/gifts/me', auth, route(async (req, res) => {
    const [inventory, transactions, withdrawals, wallet] = await Promise.all([
      db.query('SELECT gift_id AS "giftId",quantity FROM user_gift_inventory WHERE user_id=$1', [req.user.userId]),
      db.query(`SELECT gt.id,gt.gift_id AS "giftId",gc.name AS "giftName",gc.emoji,gt.amount_fcfa AS "amountFcfa",
        gt.sender_id=$1 AS "isSent",COALESCE(u.full_name,'Utilisateur') AS "counterpartName",gt.status,gt.created_at AS "createdAt"
        FROM gift_transactions gt JOIN gift_catalog gc ON gc.id=gt.gift_id
        LEFT JOIN users u ON u.id=CASE WHEN gt.sender_id=$1 THEN gt.recipient_id ELSE gt.sender_id END
        WHERE gt.sender_id=$1 OR gt.recipient_id=$1 ORDER BY gt.created_at DESC LIMIT 100`, [req.user.userId]),
      db.query('SELECT id,amount_fcfa AS "amountFcfa",provider,destination_account AS "destinationAccount",status,created_at AS "createdAt" FROM wallet_withdrawals WHERE user_id=$1 ORDER BY created_at DESC LIMIT 100', [req.user.userId]),
      db.query('SELECT wallet_balance_fcfa AS "walletBalanceFcfa", gift_earnings_balance_fcfa AS "giftEarningsBalanceFcfa" FROM users WHERE id=$1', [req.user.userId]),
    ]);
    success(res, { inventory: inventory.rows, transactions: transactions.rows, withdrawals: withdrawals.rows, walletBalanceFcfa: Number(wallet.rows[0]?.walletBalanceFcfa || 0), giftEarningsBalanceFcfa: Number(wallet.rows[0]?.giftEarningsBalanceFcfa || 0) });
  }));

  app.post('/v1/gifts/send', auth, route(async (req, res) => {
    const giftId = text(req.body.giftId, 'Cadeau', 80);
    const recipientId = text(req.body.recipientId, 'Destinataire', 80);
    const quantity = Number(req.body.quantity || 1);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 100) return failure(res, 400, 'Quantité invalide');
    await db.query('BEGIN');
    try {
      const gift = await db.query('SELECT id,price_fcfa FROM gift_catalog WHERE id=$1 AND active=TRUE', [giftId]);
      if (!gift.rowCount) throw Object.assign(new Error('Cadeau introuvable'), { status: 404 });
      const recipient = await db.query('SELECT id FROM users WHERE id=$1', [recipientId]);
      if (!recipient.rowCount || recipientId === req.user.userId) throw Object.assign(new Error('Destinataire invalide'), { status: 400 });
      const stock = await db.query('SELECT quantity FROM user_gift_inventory WHERE user_id=$1 AND gift_id=$2 FOR UPDATE', [req.user.userId, giftId]);
      if (Number(stock.rows[0]?.quantity || 0) < quantity) throw Object.assign(new Error('Stock de cadeaux insuffisant'), { status: 409 });
      await db.query('UPDATE user_gift_inventory SET quantity=quantity-$3,updated_at=NOW() WHERE user_id=$1 AND gift_id=$2', [req.user.userId, giftId, quantity]);
      const amount = Number(gift.rows[0].price_fcfa) * quantity;
      const tx = await db.query("INSERT INTO gift_transactions(sender_id,recipient_id,gift_id,quantity,amount_fcfa,status) VALUES($1,$2,$3,$4,$5,'COMPLETED') RETURNING id", [req.user.userId, recipientId, giftId, quantity, amount]);
      await db.query('UPDATE users SET gift_earnings_balance_fcfa=gift_earnings_balance_fcfa+$2 WHERE id=$1', [recipientId, amount]);
      await db.query('COMMIT');
      success(res, { transactionId: tx.rows[0].id, amountFcfa: amount });
    } catch (error) { await db.query('ROLLBACK'); throw error; }
  }));

  app.post('/v1/wallet/withdrawals', auth, route(async (req, res) => {
    const amount = Number(req.body.amountFcfa);
    const provider = text(req.body.provider, 'Opérateur', 80);
    const destination = text(req.body.destinationAccount, 'Compte destinataire', 120);
    if (!Number.isSafeInteger(amount) || amount <= 0) return failure(res, 400, 'Montant invalide');
    await db.query('BEGIN');
    try {
      const wallet = await db.query('SELECT gift_earnings_balance_fcfa FROM users WHERE id=$1 FOR UPDATE', [req.user.userId]);
      if (Number(wallet.rows[0]?.gift_earnings_balance_fcfa || 0) < amount) throw Object.assign(new Error('Gains cadeaux insuffisants'), { status: 409 });
      await db.query('UPDATE users SET gift_earnings_balance_fcfa=gift_earnings_balance_fcfa-$2 WHERE id=$1', [req.user.userId, amount]);
      const withdrawal = await db.query("INSERT INTO wallet_withdrawals(user_id,amount_fcfa,provider,destination_account,status) VALUES($1,$2,$3,$4,'PENDING') RETURNING id,status", [req.user.userId, amount, provider, destination]);
      await db.query('COMMIT');
      success(res, withdrawal.rows[0]);
    } catch (error) { await db.query('ROLLBACK'); throw error; }
  }));

  // Server-side media search and payment connectors keep provider secrets out of the APK.
  app.get('/v1/media/search', auth, route(async (req, res) => {
    if (!process.env.GIPHY_API_KEY) return failure(res, 503, 'La recherche GIF n’est pas configurée');
    const query = text(req.query.q, 'Recherche', 100);
    const type = req.query.type === 'sticker' ? 'stickers' : 'gifs';
    const limit = Math.min(Math.max(Number(req.query.limit) || 20, 1), 50);
    const upstream = await fetch(`https://api.giphy.com/v1/${type}/search?api_key=${encodeURIComponent(process.env.GIPHY_API_KEY)}&q=${encodeURIComponent(query)}&limit=${limit}&rating=pg-13`);
    if (!upstream.ok) throw Object.assign(new Error('Le fournisseur GIF est indisponible'), { status: 502 });
    const payload = await upstream.json();
    const items = (payload.data || []).map((item) => ({
      id: item.id,
      title: item.title || '',
      previewUrl: item.images?.fixed_width_small?.url || item.images?.preview_gif?.url || '',
      originalUrl: item.images?.original?.url || '',
      width: Number(item.images?.original?.width) || 0,
      height: Number(item.images?.original?.height) || 0,
    })).filter((item) => item.originalUrl);
    success(res, { items });
  }));

  const normalizeLoukaPayProvider = (value) => {
    const provider = String(value || '').trim().toLowerCase();
    if (provider.includes('mtn')) return 'mtn';
    if (provider.includes('airtel')) return 'airtel';
    if (provider.includes('gimac')) return 'gimac';
    return null;
  };
  const loukaPayStatus = (value) => {
    switch (String(value || '').trim().toLowerCase()) {
      case 'succeeded': return 'COMPLETED';
      case 'failed': return 'FAILED';
      case 'cancelled':
      case 'refunded': return 'CANCELLED';
      case 'created':
      case 'pending':
      default: return 'PENDING';
    }
  };
  const loukaPayBaseUrl = () => {
    const explicit = String(process.env.LOUKAPAY_BASE_URL || '').trim().replace(/\/$/, '');
    if (explicit) return explicit;
    return String(process.env.PAYMENTS_API_URL || '').trim().replace(/\/v1\/payment-intents\/?$/, '');
  };
  const verifyLoukaPayWebhook = (rawBody, signatureHeader) => {
    const secret = String(process.env.PAYMENTS_WEBHOOK_SECRET || '').trim();
    if (!secret || !signatureHeader) return false;
    const values = Object.fromEntries(String(signatureHeader).split(',').map((part) => part.trim().split('=', 2)));
    const timestamp = Number(values.t);
    const supplied = String(values.v1 || '');
    if (!Number.isFinite(timestamp) || !/^[a-f0-9]{64}$/i.test(supplied)) return false;
    if (Math.abs(Math.floor(Date.now() / 1000) - timestamp) > 300) return false;
    const body = Buffer.isBuffer(rawBody) ? rawBody.toString('utf8') : String(rawBody || '');
    const expected = crypto.createHmac('sha256', secret).update(`${timestamp}.${body}`).digest('hex');
    const a = Buffer.from(supplied, 'hex');
    const b = Buffer.from(expected, 'hex');
    return a.length === b.length && crypto.timingSafeEqual(a, b);
  };

  app.post('/v1/payments/callback', route(async (req, res) => {
    if (!process.env.PAYMENTS_WEBHOOK_SECRET) return failure(res, 503, 'Webhook paiement non configuré');
    const signature = req.get('x-loukapay-signature');
    if (!verifyLoukaPayWebhook(req.rawBody || Buffer.from(JSON.stringify(req.body || {})), signature)) {
      return failure(res, 401, 'Signature webhook LoukaPay invalide');
    }
    const payment = req.body?.data?.payment || {};
    const intentId = String(payment.external_reference || '').trim();
    if (!/^[0-9a-f-]{36}$/i.test(intentId)) return failure(res, 400, 'Référence MBoté invalide');
    const status = loukaPayStatus(payment.status);
    const result = await db.query(
      `UPDATE payment_intents
          SET status=$2,
              provider_reference=COALESCE($3,provider_reference),
              updated_at=NOW()
        WHERE id=$1
        RETURNING id,user_id,status,provider_reference`,
      [intentId, status, payment.id || null],
    );
    if (!result.rowCount) return failure(res, 404, 'Paiement MBoté introuvable');
    const row = result.rows[0];
    if (row.user_id) {
      realtimeHub.sendToUser(String(row.user_id), {
        type: 'PAYMENT_STATUS',
        intentId: String(row.id),
        status: row.status,
        providerReference: row.provider_reference || '',
        timestamp: Date.now(),
      });
      if (status !== 'PENDING') {
        void sendPushToUser(String(row.user_id), {
          title: status === 'COMPLETED' ? 'Paiement confirmé' : status === 'FAILED' ? 'Paiement échoué' : 'Paiement annulé',
          body: status === 'COMPLETED' ? 'Votre paiement LoukaPay a été confirmé.' : 'Le statut de votre paiement LoukaPay a été mis à jour.',
        }, { type: 'payment_status', intentId: String(row.id), status }).catch(() => {});
      }
    }
    success(res, { accepted: true, intentId: row.id, status: row.status });
  }));

  app.get('/v1/payments/intents/:intentId', auth, route(async (req, res) => {
    let result = await db.query(
      'SELECT id,provider,provider_reference AS "providerReference",amount_fcfa AS "amountFcfa",status,created_at AS "createdAt",updated_at AS "updatedAt" FROM payment_intents WHERE id=$1 AND user_id=$2',
      [req.params.intentId, req.user.userId],
    );
    if (!result.rowCount) return failure(res, 404, 'Paiement introuvable');
    const current = result.rows[0];
    if (current.status === 'PENDING' && current.providerReference && process.env.PAYMENTS_API_KEY && loukaPayBaseUrl()) {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 10000);
      try {
        const upstream = await fetch(`${loukaPayBaseUrl()}/v1/payment-intents/${encodeURIComponent(current.providerReference)}`, {
          headers: { authorization: `Bearer ${process.env.PAYMENTS_API_KEY}`, accept: 'application/json' },
          signal: controller.signal,
        });
        if (upstream.ok) {
          const payload = await upstream.json();
          const mapped = loukaPayStatus(payload.status);
          await db.query('UPDATE payment_intents SET status=$2,updated_at=NOW() WHERE id=$1', [current.id, mapped]);
          result.rows[0].status = mapped;
        }
      } finally {
        clearTimeout(timer);
      }
    }
    success(res, result.rows[0]);
  }));

  app.post('/v1/payments/intents', auth, route(async (req, res) => {
    if (!process.env.PAYMENTS_API_URL || !process.env.PAYMENTS_API_KEY) {
      return failure(res, 503, 'LoukaPay n’est pas configuré');
    }
    const provider = normalizeLoukaPayProvider(req.body.provider);
    if (!provider) return failure(res, 400, 'Opérateur non pris en charge par LoukaPay');
    const amount = Number(req.body.amountFcfa);
    const phone = text(req.body.phone, 'Téléphone', 50);
    if (!Number.isSafeInteger(amount) || amount <= 0) return failure(res, 400, 'Montant invalide');

    const local = await db.query(
      'INSERT INTO payment_intents(user_id,provider,amount_fcfa,phone) VALUES($1,$2,$3,$4) RETURNING id',
      [req.user.userId, provider, amount, phone],
    );
    const localIntentId = String(local.rows[0].id);
    const common = {
      amount,
      currency: 'XAF',
      external_reference: localIntentId,
      description: String(req.body.note || 'Paiement MBoté').slice(0, 255),
      customer_reference: String(req.user.userId),
      metadata: { source: 'mbote', mbote_user_id: String(req.user.userId), requested_provider: provider },
    };
    const requestLoukaPay = async (body, idempotencySuffix = '') => {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 20000);
      try {
        const upstream = await fetch(process.env.PAYMENTS_API_URL, {
          method: 'POST',
          headers: {
            authorization: `Bearer ${process.env.PAYMENTS_API_KEY}`,
            'content-type': 'application/json',
            accept: 'application/json',
            'idempotency-key': `mbote_${localIntentId}${idempotencySuffix}`,
          },
          body: JSON.stringify(body),
          signal: controller.signal,
        });
        return { upstream, payload: await upstream.json().catch(() => ({})) };
      } finally {
        clearTimeout(timer);
      }
    };

    try {
      let { upstream, payload } = await requestLoukaPay({ ...common, provider, customer_msisdn: phone });
      if (!upstream.ok && upstream.status >= 500) {
        const fallback = await requestLoukaPay(common, '_checkout');
        upstream = fallback.upstream;
        payload = fallback.payload;
      }
      if (!upstream.ok) {
        await db.query("UPDATE payment_intents SET status='FAILED',updated_at=NOW() WHERE id=$1", [localIntentId]);
        const detail = payload?.error === 'admin_approval_required'
          ? 'Les paiements LoukaPay en production ne sont pas encore autorisés.'
          : 'LoukaPay a refusé la demande de paiement.';
        throw Object.assign(new Error(detail), { status: upstream.status >= 400 && upstream.status < 500 ? 409 : 502 });
      }
      const status = loukaPayStatus(payload.status);
      await db.query(
        'UPDATE payment_intents SET provider_reference=$2,status=$3,updated_at=NOW() WHERE id=$1',
        [localIntentId, payload.id || null, status],
      );
      success(res, {
        intentId: localIntentId,
        status,
        amount,
        currency: 'XAF',
        provider,
        providerReference: payload.id || null,
        checkoutUrl: payload.checkout_url || null,
        receiptUrl: payload.receipt_url || null,
        checkoutRequired: Boolean(payload.checkout_required),
        instructions: payload.checkout_required ? 'Finalisez le paiement dans LoukaPay.' : 'Confirmez la demande Mobile Money sur votre téléphone.',
      }, 201);
    } catch (error) {
      if (error?.name === 'AbortError') {
        await db.query("UPDATE payment_intents SET status='FAILED',updated_at=NOW() WHERE id=$1", [localIntentId]);
        throw Object.assign(new Error('LoukaPay a dépassé le délai de réponse'), { status: 504 });
      }
      throw error;
    }
  }));

  // Session, profile and public configuration contracts consumed by Android.
  app.post('/v1/auth/logout', auth, route(async (_req, res) => success(res, true)));
  app.get('/v1/public-settings', route(async (req, res) => {
    const result = await db.query("SELECT value FROM app_content WHERE content_key = 'registration_config'");
    const origin = process.env.PUBLIC_API_URL || `${req.protocol}://${req.get('host')}`;
    const configured = result.rows[0]?.value || {};
    success(res, {
      ...configured,
      termsOfService: configured.termsOfService || `${origin}/terms`,
      privacyPolicy: configured.privacyPolicy || `${origin}/privacy`,
      accountDeletionUrl: configured.accountDeletionUrl || `${origin}/account-deletion`,
      businessCategories: Array.isArray(configured.businessCategories) ? configured.businessCategories : [],
    });
  }));
  app.put('/v1/users/me/profile', auth, route(async (req, res) => {
    const fields = [
      req.body.name?.trim() || null, req.body.email?.trim()?.toLowerCase() || null,
      req.body.bio?.trim() || null, req.body.avatar?.trim() || null, req.body.coverUrl?.trim() || null,
      req.user.userId,
    ];
    const result = await db.query(
      `UPDATE users SET full_name = COALESCE($1, full_name), email = COALESCE($2, email),
       bio = COALESCE($3, bio), avatar_url = COALESCE($4, avatar_url),
       cover_url = COALESCE($5, cover_url), updated_at = NOW() WHERE id = $6 RETURNING *`, fields,
    );
    if (!result.rowCount) return failure(res, 404, 'Compte introuvable');
    const user = result.rows[0];
    success(res, { ...publicUser(user), username: user.username || '', phone_number: user.phone || '', country: user.country || '', city: user.city || '' });
  }));
  app.delete('/v1/users/me', auth, route(async (req, res) => {
    const result = await db.query('DELETE FROM users WHERE id = $1 RETURNING id', [req.user.userId]);
    return result.rowCount ? success(res, { ok: true }) : failure(res, 404, 'Compte introuvable');
  }));

  // Creation, read receipts and reactions for real conversations.
  app.post('/v1/chats', auth, route(async (req, res) => {
    const participantIds = Array.isArray(req.body.participantIds) ? [...new Set(req.body.participantIds.map(String))] : [];
    if (!participantIds.length) return failure(res, 400, 'Au moins un participant est requis');
    const users = await db.query('SELECT id,full_name,COALESCE(avatar_url,\'\') AS avatar FROM users WHERE id = ANY($1::uuid[])', [participantIds]);
    if (users.rowCount !== participantIds.length) return failure(res, 400, 'Un ou plusieurs participants sont invalides');
    const isGroup = Boolean(req.body.isGroup);
    if (!isGroup && participantIds.length === 1) {
      const targetId=participantIds[0];
      const existing=await db.query(
        `SELECT c.id FROM chats c
          WHERE c.is_group=FALSE
            AND EXISTS(SELECT 1 FROM chat_participants p WHERE p.chat_id=c.id AND p.user_id=$1)
            AND EXISTS(SELECT 1 FROM chat_participants p WHERE p.chat_id=c.id AND p.user_id=$2)
            AND (SELECT COUNT(*) FROM chat_participants p WHERE p.chat_id=c.id)=2
          ORDER BY c.created_at DESC LIMIT 1`,
        [req.user.userId,targetId],
      );
      const target=users.rows.find((u)=>String(u.id)===targetId);
      if(existing.rowCount) return success(res,{id:existing.rows[0].id,name:target?.full_name||'Discussion',avatar:target?.avatar||'',isGroup:false,isChannel:false,participants:users.rows});
    }
    const created = await db.query(
      'INSERT INTO chats (name, is_group, created_by) VALUES ($1, $2, $3) RETURNING *',
      [isGroup ? text(req.body.name, 'Nom du groupe', 255) : null, isGroup, req.user.userId],
    );
    const chat = created.rows[0];
    const members = [...new Set([req.user.userId, ...participantIds])];
    await db.query('INSERT INTO chat_participants (chat_id, user_id, role) SELECT $1, unnest($2::uuid[]), CASE WHEN unnest($2::uuid[]) = $3 THEN \'ADMIN\' ELSE \'MEMBER\' END ON CONFLICT DO NOTHING', [chat.id, members, req.user.userId]);
    const target=!isGroup&&users.rows.length===1?users.rows[0]:null;
    success(res, { id: chat.id, name: chat.name || target?.full_name || 'Discussion', avatar: target?.avatar || '', isGroup, isChannel: false, participants: users.rows }, 201);
  }));
  app.post('/v1/chats/:chatId/read', auth, route(async (req, res) => {
    if (!(await member(req.params.chatId, req.user.userId))) return failure(res, 403, 'Accès refusé');
    await db.query(
      'INSERT INTO chat_reads (chat_id, user_id) VALUES ($1, $2) ON CONFLICT (chat_id, user_id) DO UPDATE SET last_read_at = NOW()',
      [req.params.chatId, req.user.userId],
    );
    await db.query("UPDATE messages SET status = 'READ' WHERE chat_id = $1 AND sender_id <> $2", [req.params.chatId, req.user.userId]);
    success(res, true);
  }));
  app.post('/v1/messages/:messageId/poll-votes', auth, route(async (req, res) => {
    const optionId = text(req.body.optionId, 'Option', 120);
    const allowed = await db.query(
      'SELECT 1 FROM messages m JOIN chat_participants cp ON cp.chat_id = m.chat_id WHERE m.id = $1 AND cp.user_id = $2',
      [req.params.messageId, req.user.userId],
    );
    if (!allowed.rowCount) return failure(res, 404, 'Message introuvable');
    const message = await db.query('SELECT metadata FROM messages WHERE id = $1', [req.params.messageId]);
    const metadata = message.rows[0]?.metadata || {};
    const poll = metadata.pollData;
    if (!poll || !Array.isArray(poll.options)) return failure(res, 409, 'Ce message n’est pas un sondage');
    const option = poll.options.find((item) => String(item.id) === optionId);
    if (!option) return failure(res, 400, 'Option de sondage invalide');
    poll.options = poll.options.map((item) => {
      const voters = Array.isArray(item.voters) ? item.voters.map(String) : [];
      const withoutMe = voters.filter((id) => id !== req.user.userId);
      return { ...item, voters: String(item.id) === optionId ? [...withoutMe, req.user.userId] : withoutMe };
    });
    await db.query("UPDATE messages SET metadata = jsonb_set(COALESCE(metadata, '{}'::jsonb), '{pollData}', $2::jsonb, true) WHERE id = $1", [req.params.messageId, JSON.stringify(poll)]);
    success(res, poll);
  }));

  app.post('/v1/messages/:messageId/reactions', auth, route(async (req, res) => {
    const emoji = text(req.body.emoji, 'Réaction', 16);
    const allowed = await db.query(
      'SELECT 1 FROM messages m JOIN chat_participants cp ON cp.chat_id = m.chat_id WHERE m.id = $1 AND cp.user_id = $2',
      [req.params.messageId, req.user.userId],
    );
    if (!allowed.rowCount) return failure(res, 404, 'Message introuvable');
    const inserted = await db.query(
      'INSERT INTO message_reactions (message_id, user_id, emoji) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING RETURNING emoji',
      [req.params.messageId, req.user.userId, emoji],
    );
    if (!inserted.rowCount) await db.query('DELETE FROM message_reactions WHERE message_id = $1 AND user_id = $2 AND emoji = $3', [req.params.messageId, req.user.userId, emoji]);
    const counts = await db.query('SELECT emoji, COUNT(*)::int AS count FROM message_reactions WHERE message_id = $1 GROUP BY emoji', [req.params.messageId]);
    success(res, Object.fromEntries(counts.rows.map((row) => [row.emoji, row.count])));
  }));

  // Channels are conversations with a persisted public profile and subscriptions.
  app.get('/v1/channels', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT c.id, c.name, cp.description, cp.category, c.avatar_url AS "avatarUrl",
       cp.banner_url AS "bannerUrl", (SELECT COUNT(*)::int FROM channel_subscriptions cs WHERE cs.channel_id = c.id) AS "subscriberCount",
       EXISTS(SELECT 1 FROM channel_subscriptions cs WHERE cs.channel_id = c.id AND cs.user_id = $1) AS "subscribedByMe",
       (c.created_by = $1) AS "canPublish" FROM chats c JOIN channel_profiles cp ON cp.chat_id = c.id
       WHERE cp.privacy = 'public' OR c.created_by = $1 OR EXISTS(SELECT 1 FROM channel_subscriptions cs WHERE cs.channel_id = c.id AND cs.user_id = $1)
       ORDER BY c.created_at DESC`, [req.user.userId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/channels', auth, route(async (req, res) => {
    const name = text(req.body.name, 'Nom de la chaîne', 255);
    const slug = text(req.body.slug, 'Identifiant de chaîne', 120).toLowerCase();
    await db.query('BEGIN');
    try {
      const created = await db.query('INSERT INTO chats (name, is_group, is_channel, created_by) VALUES ($1, true, true, $2) RETURNING *', [name, req.user.userId]);
      await db.query('INSERT INTO channel_profiles (chat_id, description, slug, privacy, category) VALUES ($1, $2, $3, $4, $5)', [created.rows[0].id, req.body.description?.trim() || '', slug, req.body.privacy === 'private' ? 'private' : 'public', req.body.category?.trim() || null]);
      await db.query("INSERT INTO chat_participants (chat_id, user_id, role) VALUES ($1, $2, 'OWNER')", [created.rows[0].id, req.user.userId]);
      await db.query('INSERT INTO channel_subscriptions (channel_id, user_id) VALUES ($1, $2)', [created.rows[0].id, req.user.userId]);
      await db.query('COMMIT');
      success(res, { id: created.rows[0].id }, 201);
    } catch (error) { await db.query('ROLLBACK'); throw error; }
  }));
  app.post('/v1/channels/:channelId/posts', auth, route(async (req, res) => {
    const owner = await db.query('SELECT 1 FROM chats WHERE id = $1 AND created_by = $2 AND is_channel = true', [req.params.channelId, req.user.userId]);
    if (!owner.rowCount) return failure(res, 403, 'Publication réservée au propriétaire');
    return sendMessage({ ...req, body: { chatId: req.params.channelId, text: req.body.content } }, res);
  }));
  const setChannelSubscription = async (req, res, subscribe) => {
    if (subscribe) await db.query('INSERT INTO channel_subscriptions (channel_id, user_id) VALUES ($1, $2) ON CONFLICT DO NOTHING', [req.params.channelId, req.user.userId]);
    else await db.query('DELETE FROM channel_subscriptions WHERE channel_id = $1 AND user_id = $2', [req.params.channelId, req.user.userId]);
    success(res, true);
  };
  app.post('/v1/channels/:channelId/subscribe', auth, route((req, res) => setChannelSubscription(req, res, true)));
  app.delete('/v1/channels/:channelId/subscribe', auth, route((req, res) => setChannelSubscription(req, res, false)));

  // Actus API aliases with fully persisted reactions, comments and shares.
  app.get('/v1/actus/posts', auth, route(async (req, res) => {
    const limit = Math.min(Math.max(Number(req.query.limit) || 20, 1), 100);
    const result = await db.query(
      `SELECT n.id, n.author_id, u.full_name AS author_name, COALESCE(u.avatar_url, '') AS author_avatar,
       lower(COALESCE(n.media_type, 'text')) AS type, n.content, n.image_url AS thumbnail, n.category AS visibility,
       (SELECT COUNT(*)::int FROM news_post_comments nc WHERE nc.news_post_id=n.id) AS comment_count,
       (SELECT COUNT(*)::int FROM news_post_shares ns WHERE ns.news_post_id=n.id) AS share_count,
       (SELECT COUNT(*)::int FROM news_post_likes nl WHERE nl.news_post_id=n.id) AS reaction_count,
       CASE WHEN EXISTS(SELECT 1 FROM news_post_likes nl WHERE nl.news_post_id=n.id AND nl.user_id=$1) THEN '❤️' END AS my_reaction,
       n.created_at FROM news_posts n JOIN users u ON u.id=n.author_id ORDER BY n.created_at DESC LIMIT $2`,
      [req.user.userId, limit],
    );
    success(res, result.rows);
  }));
  app.post('/v1/actus/posts', auth, route(async (req, res) => {
    const rawType = String(req.body.type || 'text').trim().toLowerCase();
    const type = rawType.startsWith('image/') || rawType === 'photo' ? 'image'
      : rawType.startsWith('video/') ? 'video'
      : rawType.startsWith('audio/') || rawType === 'voice' || rawType === 'vocal' ? 'audio'
      : rawType === 'texte' ? 'text' : rawType;
    if (!['text','image','audio','video'].includes(type)) return failure(res, 400, 'Type de publication invalide');
    const content = text(req.body.content, 'Publication');
    const description = type === 'text' ? content : String(req.body.thumbnail || '').trim();
    const created = await db.query(
      'INSERT INTO news_posts (author_id, category, title, content, image_url, media_type) VALUES ($1,$2,$3,$4,$5,$6) RETURNING *',
      [req.user.userId, req.body.visibility || 'public', (description || content).slice(0,255), description || content, type === 'text' ? null : content, type.toUpperCase()],
    );
    const user = await db.query('SELECT full_name, avatar_url FROM users WHERE id=$1', [req.user.userId]);
    success(res, { id: created.rows[0].id, author_id: req.user.userId, author_name: user.rows[0].full_name, author_avatar: user.rows[0].avatar_url || '', type, content: type === 'text' ? content : created.rows[0].image_url, thumbnail: type === 'text' ? null : description, visibility: req.body.visibility || 'public', comment_count: 0, share_count: 0, reaction_count: 0, my_reaction: null, created_at: created.rows[0].created_at }, 201);
  }));
  app.post('/v1/actus/posts/:postId/reactions', auth, route(async (req, res) => {
    const existing = await db.query('SELECT 1 FROM news_post_likes WHERE news_post_id=$1 AND user_id=$2', [req.params.postId, req.user.userId]);
    if (existing.rowCount) await db.query('DELETE FROM news_post_likes WHERE news_post_id=$1 AND user_id=$2', [req.params.postId, req.user.userId]);
    else await db.query('INSERT INTO news_post_likes (news_post_id,user_id) VALUES ($1,$2)', [req.params.postId, req.user.userId]);
    success(res, true);
  }));
  app.get('/v1/actus/posts/:postId/comments', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT c.id, u.full_name AS user_name, COALESCE(u.avatar_url,'') AS user_avatar, c.text AS content, c.created_at
       FROM news_post_comments c JOIN users u ON u.id=c.author_id WHERE c.news_post_id=$1 ORDER BY c.created_at ASC LIMIT 100`,
      [req.params.postId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/actus/posts/:postId/comments', auth, route(async (req, res) => {
    await db.query('INSERT INTO news_post_comments (news_post_id,author_id,text) VALUES ($1,$2,$3)', [req.params.postId, req.user.userId, text(req.body.content || req.body.text, 'Commentaire', 2000)]);
    success(res, true, 201);
  }));
  app.post('/v1/actus/posts/:postId/shares', auth, route(async (req, res) => {
    await db.query('INSERT INTO news_post_shares (news_post_id,user_id) VALUES ($1,$2)', [req.params.postId, req.user.userId]);
    const count = await db.query('SELECT COUNT(*)::int AS count FROM news_post_shares WHERE news_post_id=$1', [req.params.postId]);
    await db.query('UPDATE news_posts SET shares_count=$2 WHERE id=$1', [req.params.postId, count.rows[0].count]);
    success(res, { shareCount: count.rows[0].count });
  }));

  // Jobs and applications are sourced exclusively from PostgreSQL.
  app.get('/v1/jobs', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT id,title,company,location,contract_type AS type,description,domain AS "activityDomain",
       contract_type AS duration,COALESCE(salary,'') AS salary,j.created_at AS "publishedAt",
       '' AS "expiresAt",'' AS url,company_logo AS "imageUrl",
       (SELECT COUNT(*)::int FROM job_applications a WHERE a.job_id=j.id) AS "applicantsCount",
       (SELECT COUNT(*)::int FROM job_likes l WHERE l.job_id=j.id) AS "likesCount",
       EXISTS(SELECT 1 FROM job_likes l WHERE l.job_id=j.id AND l.user_id=$1) AS "isLiked",
       EXISTS(SELECT 1 FROM job_bookmarks b WHERE b.job_id=j.id AND b.user_id=$1) AS "isSaved"
       FROM job_offers j ORDER BY j.created_at DESC`,
      [req.user.userId],
    );
    success(res, { jobs: result.rows });
  }));
  app.post('/v1/jobs', auth, route(async (req, res) => {
    const created = await db.query(
      `INSERT INTO job_offers (title,company,company_logo,location,domain,contract_type,work_mode,salary,description)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING id,title,company,location,contract_type AS type,
       description,domain AS "activityDomain",contract_type AS duration,COALESCE(salary,'') AS salary,
       created_at AS "publishedAt",'' AS "expiresAt",'' AS url,company_logo AS "imageUrl"`,
      [text(req.body.title,'Titre',255),text(req.body.company,'Entreprise',255),req.body.imageUrl||null,text(req.body.location,'Lieu',255),req.body.activityDomain||req.body.domain||'Autre',req.body.duration||req.body.type||'CDI',req.body.workMode||'Sur site',req.body.salary||null,text(req.body.description,'Description')],
    );
    success(res, { job: created.rows[0] }, 201);
  }));
  app.post('/v1/jobs/:jobId/apply', auth, route(async (req, res) => {
    await db.query('INSERT INTO job_applications (job_id,applicant_id,cv_url) VALUES ($1,$2,$3) ON CONFLICT (job_id,applicant_id) DO UPDATE SET cv_url=EXCLUDED.cv_url, created_at=NOW()', [req.params.jobId, req.user.userId, req.body.cvUrl || null]);
    success(res, true, 201);
  }));

  app.post('/v1/jobs/:jobId/like', auth, route(async (req,res)=>{
    const inserted=await db.query('INSERT INTO job_likes(job_id,user_id) VALUES($1,$2) ON CONFLICT DO NOTHING RETURNING job_id',[req.params.jobId,req.user.userId]);
    if(!inserted.rowCount)await db.query('DELETE FROM job_likes WHERE job_id=$1 AND user_id=$2',[req.params.jobId,req.user.userId]);
    const count=await db.query('SELECT COUNT(*)::int AS count FROM job_likes WHERE job_id=$1',[req.params.jobId]);
    success(res,{liked:inserted.rowCount>0,count:count.rows[0].count});
  }));
  app.post('/v1/jobs/:jobId/bookmark', auth, route(async (req,res)=>{
    const inserted=await db.query('INSERT INTO job_bookmarks(job_id,user_id) VALUES($1,$2) ON CONFLICT DO NOTHING RETURNING job_id',[req.params.jobId,req.user.userId]);
    if(!inserted.rowCount)await db.query('DELETE FROM job_bookmarks WHERE job_id=$1 AND user_id=$2',[req.params.jobId,req.user.userId]);
    success(res,{saved:inserted.rowCount>0});
  }));

  // Status interactions and lifecycle.
  app.get('/v1/status', auth, route(async (req, res) => {
    const result = await db.query(
      `SELECT s.id,s.author_id AS user_id,u.full_name AS user_name,COALESCE(u.avatar_url,'') AS user_avatar,
       lower(s.media_type) AS type,COALESCE(s.text,s.media_url,'') AS content,s.background_color AS background,
       s.text AS caption,s.created_at,(SELECT COUNT(*)::int FROM status_reactions r WHERE r.status_id=s.id) AS reaction_count,
       (SELECT emoji FROM status_reactions r WHERE r.status_id=s.id AND r.user_id=$1) AS my_reaction,
       (SELECT COUNT(*)::int FROM status_comments c WHERE c.status_id=s.id) AS comment_count,
       (SELECT COUNT(*)::int FROM status_shares sh WHERE sh.status_id=s.id) AS share_count,
       (SELECT COUNT(*)::int FROM status_views v WHERE v.status_id=s.id) AS view_count
       FROM statuses s JOIN users u ON u.id=s.author_id WHERE s.expires_at>NOW() ORDER BY s.created_at DESC`,
      [req.user.userId],
    );
    success(res, result.rows);
  }));
  app.post('/v1/status/publications', auth, route(async (req, res) => {
    const rawType = String(req.body.type || 'text').trim().toLowerCase();
    const typeAliases = { text: 'TEXT', texte: 'TEXT', image: 'IMAGE', photo: 'IMAGE', audio: 'AUDIO', voice: 'AUDIO', vocal: 'AUDIO', video: 'VIDEO', 'vidéo': 'VIDEO' };
    const type = typeAliases[rawType] || (rawType.startsWith('image/') ? 'IMAGE' : rawType.startsWith('audio/') ? 'AUDIO' : rawType.startsWith('video/') ? 'VIDEO' : null);
    if (!type) return failure(res, 400, 'Type de statut invalide');
    const content = text(req.body.content,'Statut',5000);
    const created = await db.query(
      `INSERT INTO statuses (author_id,media_type,media_url,text,background_color,expires_at)
       VALUES ($1,$2,$3,$4,$5,NOW()+($6::int * INTERVAL '1 hour')) RETURNING *`,
      [req.user.userId,type,type==='TEXT'?null:content,type==='TEXT'?content:(req.body.caption||null),req.body.background||null,Math.min(Math.max(Number(req.body.durationHours)||24,1),168)],
    );
    const user=await db.query('SELECT full_name,avatar_url FROM users WHERE id=$1',[req.user.userId]);
    success(res,{id:created.rows[0].id,user_id:req.user.userId,user_name:user.rows[0].full_name,user_avatar:user.rows[0].avatar_url||'',type:type.toLowerCase(),content,background:req.body.background||null,caption:req.body.caption||null,created_at:created.rows[0].created_at,reaction_count:0,my_reaction:null,comment_count:0,share_count:0,view_count:0},201);
  }));
  app.post('/v1/status/:statusId/views', auth, route(async (req,res)=>{await db.query('INSERT INTO status_views(status_id,viewer_id) VALUES($1,$2) ON CONFLICT DO NOTHING',[req.params.statusId,req.user.userId]);success(res,true);}));
  app.post('/v1/status/:statusId/reactions', auth, route(async (req,res)=>{
    const emoji=text(req.body.reaction,'Réaction',16);
    await db.query('INSERT INTO status_reactions(status_id,user_id,emoji) VALUES($1,$2,$3) ON CONFLICT(status_id,user_id) DO UPDATE SET emoji=EXCLUDED.emoji',[req.params.statusId,req.user.userId,emoji]);
    success(res,true);
  }));
  app.post('/v1/status/:statusId/comments', auth, route(async (req,res)=>{await db.query('INSERT INTO status_comments(status_id,author_id,text) VALUES($1,$2,$3)',[req.params.statusId,req.user.userId,text(req.body.content,'Commentaire',2000)]);success(res,true,201);}));
  app.post('/v1/status/:statusId/shares', auth, route(async (req,res)=>{await db.query('INSERT INTO status_shares(status_id,user_id) VALUES($1,$2)',[req.params.statusId,req.user.userId]);success(res,true,201);}));
  app.delete('/v1/status/:statusId', auth, route(async (req,res)=>{const result=await db.query('DELETE FROM statuses WHERE id=$1 AND author_id=$2 RETURNING id',[req.params.statusId,req.user.userId]);return result.rowCount?success(res,true):failure(res,404,'Statut introuvable');}));

  // LIVE: persisted sessions and viewer membership. Signaling events travel over Socket.IO below.
  app.get('/v1/live/ice-servers', auth, route(async (_req,res)=>{
    const servers = [
      { urls: ['stun:stun.l.google.com:19302', 'stun:stun1.l.google.com:19302'] }
    ];
    const turnUrl = String(process.env.MBOTE_TURN_URL || '').trim();
    const username = String(process.env.MBOTE_TURN_USERNAME || '').trim();
    const credential = String(process.env.MBOTE_TURN_CREDENTIAL || '').trim();
    if (turnUrl && username && credential) {
      servers.push({ urls: [turnUrl], username, credential });
    }
    success(res, { iceServers: servers, turnConfigured: Boolean(turnUrl && username && credential) });
  }));

  app.get('/v1/live', auth, route(async (req,res)=>{
    const result=await db.query(
      `SELECT l.id,l.title,l.host_id,u.full_name AS host_name,COALESCE(u.avatar_url,'') AS host_avatar,
       l.status,l.started_at,l.ended_at,
       (SELECT COUNT(*)::int FROM live_stream_viewers v WHERE v.stream_id=l.id AND v.left_at IS NULL) AS viewer_count
       FROM live_streams l JOIN users u ON u.id=l.host_id
       WHERE l.status='LIVE' ORDER BY l.started_at DESC LIMIT 50`
    );
    success(res,result.rows);
  }));
  app.post('/v1/live', auth, route(async(req,res)=>{
    const title=text(req.body.title,'Titre du Live',255);
    await db.query("UPDATE live_streams SET status='ENDED',ended_at=NOW() WHERE host_id=$1 AND status='LIVE'",[req.user.userId]);
    const created=await db.query(
      "INSERT INTO live_streams(host_id,title,status,started_at) VALUES($1,$2,'LIVE',NOW()) RETURNING id,title,status,started_at",
      [req.user.userId,title],
    );
    success(res,created.rows[0],201);
  }));
  app.post('/v1/live/:streamId/join',auth,route(async(req,res)=>{
    const live=await db.query("SELECT id,host_id,title,status FROM live_streams WHERE id=$1 AND status='LIVE'",[req.params.streamId]);
    if(!live.rowCount)return failure(res,404,'Live introuvable ou terminé');
    if(String(live.rows[0].host_id)!==String(req.user.userId)) await db.query('INSERT INTO live_stream_viewers(stream_id,user_id) VALUES($1,$2) ON CONFLICT(stream_id,user_id) DO UPDATE SET left_at=NULL,joined_at=NOW()',[req.params.streamId,req.user.userId]);
    success(res,live.rows[0]);
  }));
  app.post('/v1/live/:streamId/leave',auth,route(async(req,res)=>{
    await db.query('UPDATE live_stream_viewers SET left_at=NOW() WHERE stream_id=$1 AND user_id=$2',[req.params.streamId,req.user.userId]);
    success(res,true);
  }));
  app.post('/v1/live/:streamId/end',auth,route(async(req,res)=>{
    const ended=await db.query("UPDATE live_streams SET status='ENDED',ended_at=NOW() WHERE id=$1 AND host_id=$2 AND status='LIVE' RETURNING id",[req.params.streamId,req.user.userId]);
    if(!ended.rowCount)return failure(res,404,'Live actif introuvable');
    await db.query('UPDATE live_stream_viewers SET left_at=COALESCE(left_at,NOW()) WHERE stream_id=$1',[req.params.streamId]);
    success(res,true);
  }));

  // Group call sessions never fall back to fabricated local participants.
  const groupCallDto = async (roomCode, viewerId) => {
    const sessionResult = await db.query('SELECT * FROM group_call_sessions WHERE room_code=$1',[roomCode.toUpperCase()]);
    if(!sessionResult.rowCount) return null;
    const session=sessionResult.rows[0];
    const participants=await db.query(
      `SELECT u.id,u.full_name AS name,COALESCE(u.avatar_url,'') AS avatar,
       NOT p.audio_enabled AS "isMuted",NOT p.video_enabled AS "isVideoOff",
       (u.id=$2) AS "isHost",p.screen_sharing AS "isScreenSharing",0::float AS "audioVolumeLevel"
       FROM group_call_participants p JOIN users u ON u.id=p.user_id
       WHERE p.session_id=$1 AND p.left_at IS NULL ORDER BY p.joined_at`,[session.id,session.host_id],
    );
    return {roomCode:session.room_code,roomTitle:session.title,isVideoCall:session.is_video,hostUserId:String(session.host_id),participants:participants.rows,connectionQuality:'SERVER_CONNECTED',encryptionStandard:'WebRTC E2EE',createdAtTimestamp:new Date(session.created_at).getTime()};
  };
  app.post('/v1/calls/group/create',auth,route(async(req,res)=>{
    const code=crypto.randomBytes(4).toString('hex').toUpperCase();
    const title=text(req.body.roomTitle,'Titre',255);
    const isVideo=req.body.isVideoCall!==false;
    const invitees=Array.isArray(req.body.participantIds)?[...new Set(req.body.participantIds.map(String).filter((id)=>id!==String(req.user.userId)))]:[];
    if(invitees.length) {
      const valid=await db.query('SELECT id FROM users WHERE id=ANY($1::uuid[])',[invitees]);
      if(valid.rowCount!==invitees.length)return failure(res,400,'Un ou plusieurs participants sont invalides');
    }
    const created=await db.query('INSERT INTO group_call_sessions(room_code,host_id,title,is_video) VALUES($1,$2,$3,$4) RETURNING id',[code,req.user.userId,title,isVideo]);
    await db.query('INSERT INTO group_call_participants(session_id,user_id,video_enabled) VALUES($1,$2,$3)',[created.rows[0].id,req.user.userId,isVideo]);
    if(invitees.length) await db.query('INSERT INTO group_call_participants(session_id,user_id,video_enabled) SELECT $1,unnest($2::uuid[]),$3 ON CONFLICT DO NOTHING',[created.rows[0].id,invitees,isVideo]);
    const host=await db.query("SELECT full_name,COALESCE(avatar_url,'') AS avatar FROM users WHERE id=$1",[req.user.userId]);
    const invite={type:'CALL_INVITE',roomCode:code,callerUserId:String(req.user.userId),callerName:host.rows[0]?.full_name||'Utilisateur MBoté',callerAvatar:host.rows[0]?.avatar||'',isVideo,title,timestamp:Date.now()};
    for(const userId of invitees) {
      realtimeHub.sendToUser(userId,invite);
      if (!realtimeHub.isUserConnected(userId)) {
        void sendPushToUser(
          userId,
          { title: isVideo ? 'Appel vidéo MBoté' : 'Appel audio MBoté', body: `${invite.callerName} vous appelle.` },
          { type:'incoming_call',roomCode:code,callerUserId:String(req.user.userId),callerName:invite.callerName,isVideo:String(isVideo),title },
        );
      }
    }
    success(res,await groupCallDto(code,req.user.userId),201);
  }));
  app.post('/v1/calls/group/join/:roomCode',auth,route(async(req,res)=>{
    const session=await db.query("SELECT id,is_video FROM group_call_sessions WHERE room_code=$1 AND status='ACTIVE'",[req.params.roomCode.toUpperCase()]);
    if(!session.rowCount)return failure(res,404,'Appel de groupe introuvable');
    await db.query('INSERT INTO group_call_participants(session_id,user_id,video_enabled,left_at) VALUES($1,$2,$3,NULL) ON CONFLICT(session_id,user_id) DO UPDATE SET left_at=NULL',[session.rows[0].id,req.user.userId,session.rows[0].is_video]);
    success(res,await groupCallDto(req.params.roomCode,req.user.userId));
  }));
  app.put('/v1/calls/group/update-state',auth,route(async(req,res)=>{
    const session=await db.query('SELECT id FROM group_call_sessions WHERE room_code=$1',[text(req.body.roomCode,'Code',32).toUpperCase()]);
    if(!session.rowCount)return failure(res,404,'Appel introuvable');
    await db.query('UPDATE group_call_participants SET audio_enabled=$3,video_enabled=$4,screen_sharing=$5 WHERE session_id=$1 AND user_id=$2',[session.rows[0].id,req.user.userId,!req.body.isMuted,!req.body.isVideoOff,Boolean(req.body.isScreenSharing)]);
    success(res,true);
  }));
  app.post('/v1/calls/group/leave/:roomCode',auth,route(async(req,res)=>{
    const session=await db.query('SELECT id,host_id FROM group_call_sessions WHERE room_code=$1',[req.params.roomCode.toUpperCase()]);
    if(!session.rowCount)return failure(res,404,'Appel introuvable');
    await db.query('UPDATE group_call_participants SET left_at=NOW() WHERE session_id=$1 AND user_id=$2',[session.rows[0].id,req.user.userId]);
    if(String(session.rows[0].host_id)===String(req.user.userId))await db.query("UPDATE group_call_sessions SET status='ENDED',ended_at=NOW() WHERE id=$1",[session.rows[0].id]);
    success(res,true);
  }));

  // Parent/child links accept only short-lived tokens generated by the child account.
  app.post('/v1/parental/link-token',auth,route(async(req,res)=>{
    const result=await db.query("INSERT INTO parental_link_tokens(child_id) VALUES($1) RETURNING token,expires_at",[req.user.userId]);
    success(res,{qrPayload:`mbote://child-link?token=${result.rows[0].token}`,expiresAt:result.rows[0].expires_at},201);
  }));
  app.post('/v1/parental/links/consume',auth,route(async(req,res)=>{
    const payload=text(req.body.qrPayload,'QR code',2000);
    let token;
    try { token=new URL(payload).searchParams.get('token'); } catch { token=null; }
    if(!token)return failure(res,400,'QR code enfant invalide');
    const found=await db.query(
      `UPDATE parental_link_tokens SET consumed_at=NOW() WHERE token=$1 AND expires_at>NOW() AND consumed_at IS NULL
       RETURNING child_id`,[token],
    );
    if(!found.rowCount)return failure(res,400,'QR code expiré ou déjà utilisé');
    await db.query('INSERT INTO parental_links(parent_id,child_id) VALUES($1,$2) ON CONFLICT DO NOTHING',[req.user.userId,found.rows[0].child_id]);
    const child=await db.query('SELECT id,full_name,username,avatar_url,created_at FROM users WHERE id=$1',[found.rows[0].child_id]);
    const u=child.rows[0];
    success(res,{id:u.id,name:u.full_name,username:u.username||'',avatar:u.avatar_url||'',age:0,schoolName:'',deviceModel:'',batteryLevel:0,isOnline:false,lastActive:'',linkToken:'',installedApps:[],lastPanicAlert:null});
  }));
  app.get('/v1/parental/children',auth,route(async(req,res)=>{
    const result=await db.query(
      `SELECT u.id,u.full_name AS name,COALESCE(u.username,'') AS username,COALESCE(u.avatar_url,'') AS avatar,
       0 AS age,'' AS "schoolName",'' AS "deviceModel",0 AS "batteryLevel",false AS "isOnline",'' AS "lastActive",
       '' AS "linkToken",'[]'::json AS "installedApps",NULL::json AS "lastPanicAlert"
       FROM parental_links p JOIN users u ON u.id=p.child_id WHERE p.parent_id=$1 ORDER BY p.linked_at DESC`,[req.user.userId],
    );
    success(res,result.rows);
  }));
  app.delete('/v1/parental/children/:childId',auth,route(async(req,res)=>{
    await db.query('DELETE FROM parental_links WHERE parent_id=$1 AND child_id=$2',[req.user.userId,req.params.childId]);success(res,true);
  }));
  app.post('/v1/parental/sos',auth,route(async(req,res)=>{
    const parent=await db.query('SELECT parent_id FROM parental_links WHERE child_id=$1 ORDER BY linked_at DESC LIMIT 1',[req.user.userId]);
    if(!parent.rowCount)return failure(res,409,'Aucun compte parent lié');
    const created=await db.query(
      `INSERT INTO panic_alerts(child_id,parent_id,reason,latitude,longitude,address,battery_level)
       VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING *`,
      [req.user.userId,parent.rows[0].parent_id,text(req.body.reason,'Motif',2000),req.body.latitude||null,req.body.longitude||null,req.body.address||null,req.body.batteryLevel||null],
    );
    success(res,created.rows[0],201);
  }));

  // Dynamic application content and push-token registration.
  app.get('/v1/content/aron-questions',auth,route(async(_req,res)=>{
    const result=await db.query("SELECT value FROM app_content WHERE content_key='aron_questions'");
    success(res,result.rows[0]?.value||[]);
  }));
  app.put('/v1/devices/push-token',auth,route(async(req,res)=>{
    const token=text(req.body.token,'Jeton FCM',4096);
    await db.query('INSERT INTO device_push_tokens(user_id,token) VALUES($1,$2) ON CONFLICT(user_id,token) DO UPDATE SET updated_at=NOW()',[req.user.userId,token]);
    success(res,true);
  }));

  app.post('/v1/ai/translate', auth, route(async (req, res) => {
    const source = text(req.body.text, 'Texte', 5000);
    const targetLanguage = text(req.body.targetLanguage, 'Langue cible', 80);
    const translatedText = await groqCompletion({
      messages: [
        { role: 'system', content: `Tu es Luna, l’assistante IA de MBoté. Traduis fidèlement vers ${targetLanguage}. Réponds uniquement avec la traduction, sans explication ni guillemets ajoutés.` },
        { role: 'user', content: source },
      ],
      temperature: 0.1,
      maxTokens: 1800,
    });
    success(res, { translatedText, targetLanguage, provider: 'groq', model: groqModel() });
  }));

  app.post('/v1/ai/smart-replies', auth, route(async (req, res) => {
    const messages = Array.isArray(req.body.messages) ? req.body.messages.slice(-6) : [];
    if (!messages.length) return failure(res, 400, 'Historique de conversation requis');
    const history = messages.map((item) => {
      const body = typeof item?.text === 'string' ? item.text.trim().slice(0, 1000) : '';
      const sender = item?.isMine ? 'Moi' : String(item?.senderName || 'Contact').slice(0, 80);
      return body ? `${sender}: ${body}` : '';
    }).filter(Boolean).join('\n');
    if (!history) return failure(res, 400, 'Historique de conversation invalide');
    const style = ['Brief', 'Balanced', 'Elaborate'].includes(req.body.conciseness) ? req.body.conciseness : 'Balanced';
    const raw = await groqCompletion({
      messages: [
        { role: 'system', content: `Tu es Luna, l’assistante de MBoté. Style: ${style}. Réponds uniquement avec un tableau JSON contenant exactement trois chaînes de réponse naturelles et adaptées à la conversation.` },
        { role: 'user', content: `Conversation récente :\n${history}\n\nGénère les trois réponses.` },
      ],
      temperature: 0.45,
      maxTokens: 500,
    });
    let suggestions = [];
    try {
      const cleaned = raw.replace(/^\`\`\`(?:json)?\s*/i, '').replace(/\s*\`\`\`$/i, '').trim();
      const parsed = JSON.parse(cleaned);
      suggestions = Array.isArray(parsed) ? parsed : Array.isArray(parsed?.suggestions) ? parsed.suggestions : [];
    } catch {
      suggestions = raw.split('\n').map((line) => line.replace(/^[-*\d.\s]+/, '').trim()).filter(Boolean);
    }
    suggestions = suggestions.filter((item) => typeof item === 'string' && item.trim()).map((item) => item.trim()).slice(0, 3);
    if (suggestions.length !== 3) throw Object.assign(new Error('Réponse Luna invalide'), { status: 502 });
    success(res, { suggestions, provider: 'groq', model: groqModel() });
  }));

  app.post('/v1/ai/luna', auth, route(async (req, res) => {
    const message = text(req.body.message, 'Message', 6000);
    const history = Array.isArray(req.body.history) ? req.body.history.slice(-10) : [];
    const safeHistory = history.map((item) => ({
      role: item?.role === 'assistant' ? 'assistant' : 'user',
      content: String(item?.content || '').trim().slice(0, 3000),
    })).filter((item) => item.content);
    const answer = await groqCompletion({
      messages: [
        {
          role: 'system',
          content: 'Tu es Luna, l’assistante IA de MBoté. Aide de façon claire, utile et concise. Respecte la langue de l’utilisateur. N’invente pas de données personnelles, de résultats d’action, de paiements ou de faits non vérifiés. Pour les fonctions MBoté, explique uniquement ce qui est réellement disponible.',
        },
        ...safeHistory,
        { role: 'user', content: message },
      ],
      temperature: 0.35,
      maxTokens: 1600,
    });
    success(res, { answer, provider: 'groq', model: groqModel() });
  }));

  app.use((error, req, res, _next) => {
    if (error.status) return failure(res, error.status, error.message);
    console.error(JSON.stringify({
      level: 'error',
      event: 'unhandled_request_error',
      requestId: req.requestId,
      method: req.method,
      path: req.path,
      error: error?.message || 'Unknown error',
    }));
    return failure(res, 500, 'Erreur interne du serveur');
  });
  return app;
}

if (require.main === module) {
  const db = createPool();
  const app = createApp({ db });
  const server = app.listen(PORT, () => console.log(`MBoté API v${API_VERSION} écoute sur ${PORT}`));
  const wss = new WebSocketServer({ server, path: '/ws', maxPayload: 64 * 1024 });
  const heartbeat = setInterval(() => {
    for (const socket of wss.clients) {
      if (socket.isAlive === false) {
        socket.terminate();
        continue;
      }
      socket.isAlive = false;
      socket.ping();
    }
  }, 30_000);
  const liveSockets = new Map();
  const rtcRooms = new Map();
  const userSockets = new Map();
  realtimeHub.sendToUser = (userId, event) => {
    const encoded = JSON.stringify(event);
    let delivered = false;
    for (const client of (userSockets.get(String(userId)) || new Set())) {
      if (client.readyState === 1) {
        client.send(encoded);
        delivered = true;
      }
    }
    return delivered;
  };
  realtimeHub.isUserConnected = (userId) =>
    [...(userSockets.get(String(userId)) || new Set())].some((client) => client.readyState === 1);
  const broadcastLive = (streamId,event,except=null) => {
    const message=JSON.stringify(event);
    for(const client of (liveSockets.get(streamId)||new Set())) {
      if(client!==except && client.readyState===1) client.send(message);
    }
  };
  const viewerCount=async(streamId)=>{
    const count=await db.query(`SELECT COUNT(*)::int AS count FROM live_stream_viewers v JOIN live_streams l ON l.id=v.stream_id WHERE v.stream_id=$1 AND v.left_at IS NULL AND v.user_id<>l.host_id`,[streamId]);
    return count.rows[0]?.count||0;
  };
  wss.on('connection',(socket,request)=>{
    socket.isAlive = true;
    socket.on('pong', () => { socket.isAlive = true; });
    let identity=null;
    let joinedStream=null;
    socket.on('message',async(raw)=>{
      let message; try { message=JSON.parse(raw.toString()); } catch { return; }
      if(!identity) {
        if(message.type!=='AUTH') return socket.close(1008,'Authentification requise');
        try {
          identity=jwt.verify(String(message.token||''),process.env.JWT_SECRET,{issuer:'mbote-api',audience:'mbote-mobile'});
          const user = await db.query('SELECT full_name FROM users WHERE id=$1',[identity.userId]);
          if(!user.rowCount) return socket.close(1008,'Compte introuvable');
          socket.mboteUserId=identity.userId;
          socket.mboteUserName=String(user.rows[0].full_name||'Utilisateur MBoté').slice(0,120);
          const userId=String(identity.userId);
          if(!userSockets.has(userId)) userSockets.set(userId,new Set());
          userSockets.get(userId).add(socket);
          socket.send(JSON.stringify({type:'AUTH_OK',userId:identity.userId}));
        }
        catch { socket.close(1008,'Session invalide'); }
        return;
      }
      if(message.type==='CHAT_TYPING') {
        const chatId=String(message.chatId||'');
        if(!chatId || !(await member(chatId,identity.userId))) return;
        const recipients=await db.query('SELECT user_id FROM chat_participants WHERE chat_id=$1 AND user_id<>$2',[chatId,identity.userId]);
        const packet={type:'CHAT_TYPING',chatId,userName:socket.mboteUserName||'Utilisateur MBoté',isTyping:Boolean(message.isTyping),timestamp:Date.now()};
        for(const row of recipients.rows) realtimeHub.sendToUser(String(row.user_id),packet);
        return;
      }
      if(message.type==='CALL_RESPONSE') {
        const roomCode=String(message.roomCode||'').trim().toUpperCase();
        const status=String(message.status||'').toUpperCase();
        if(!roomCode || !['ACCEPTED','REJECTED'].includes(status)) return;
        const session=await db.query(
          `SELECT s.id,s.host_id FROM group_call_sessions s
            JOIN group_call_participants p ON p.session_id=s.id
           WHERE s.room_code=$1 AND s.status='ACTIVE' AND p.user_id=$2`,
          [roomCode,identity.userId],
        );
        if(!session.rowCount)return;
        if(status==='REJECTED') {
          await db.query('UPDATE group_call_participants SET left_at=NOW() WHERE session_id=$1 AND user_id=$2',[session.rows[0].id,identity.userId]);
        }
        realtimeHub.sendToUser(String(session.rows[0].host_id),{
          type:'CALL_RESPONSE',roomCode,status,userId:String(identity.userId),userName:socket.mboteUserName||'Utilisateur MBoté',timestamp:Date.now()
        });
        return;
      }
      if(message.type==='CALL_END') {
        const roomCode=String(message.roomCode||'').trim().toUpperCase();
        if(!roomCode)return;
        const session=await db.query(
          `SELECT s.id,s.host_id FROM group_call_sessions s
            JOIN group_call_participants p ON p.session_id=s.id
           WHERE s.room_code=$1 AND s.status='ACTIVE' AND p.user_id=$2`,
          [roomCode,identity.userId],
        );
        if(!session.rowCount)return;
        const participants=await db.query('SELECT user_id FROM group_call_participants WHERE session_id=$1 AND left_at IS NULL',[session.rows[0].id]);
        await db.query('UPDATE group_call_participants SET left_at=NOW() WHERE session_id=$1 AND user_id=$2',[session.rows[0].id,identity.userId]);
        if(String(session.rows[0].host_id)===String(identity.userId)) {
          await db.query("UPDATE group_call_sessions SET status='ENDED',ended_at=NOW() WHERE id=$1",[session.rows[0].id]);
        }
        for(const row of participants.rows) {
          if(String(row.user_id)!==String(identity.userId)) realtimeHub.sendToUser(String(row.user_id),{type:'CALL_END',roomCode,userId:String(identity.userId),timestamp:Date.now()});
        }
        return;
      }
      if(message.type==='RTC_JOIN') {
        const roomCode=String(message.roomCode||'').trim().toUpperCase();
        if(!roomCode)return;
        const session=await db.query(
          `SELECT s.id FROM group_call_sessions s
            JOIN group_call_participants p ON p.session_id=s.id
           WHERE s.room_code=$1 AND s.status='ACTIVE' AND p.user_id=$2 AND p.left_at IS NULL`,
          [roomCode,identity.userId],
        );
        if(!session.rowCount)return;
        if(!socket.mboteRtcRooms)socket.mboteRtcRooms=new Set();
        const room=rtcRooms.get(roomCode)||new Set();
        const peers=[...room].filter((client)=>client!==socket&&client.readyState===1).map((client)=>({
          userId:String(client.mboteUserId||''),userName:String(client.mboteUserName||'Utilisateur MBoté')
        })).filter((peer)=>peer.userId);
        room.add(socket);rtcRooms.set(roomCode,room);socket.mboteRtcRooms.add(roomCode);
        socket.send(JSON.stringify({type:'RTC_PEERS',roomCode,peers,timestamp:Date.now()}));
        const joined=JSON.stringify({type:'RTC_PEER_JOINED',roomCode,userId:String(identity.userId),userName:socket.mboteUserName||'Utilisateur MBoté',timestamp:Date.now()});
        for(const client of room)if(client!==socket&&client.readyState===1)client.send(joined);
        return;
      }
      if(message.type==='RTC_LEAVE') {
        const roomCode=String(message.roomCode||'').trim().toUpperCase();
        const room=rtcRooms.get(roomCode);
        if(!room||!room.has(socket))return;
        room.delete(socket);socket.mboteRtcRooms?.delete(roomCode);
        if(!room.size)rtcRooms.delete(roomCode);
        const left=JSON.stringify({type:'RTC_PEER_LEFT',roomCode,userId:String(identity.userId),timestamp:Date.now()});
        for(const client of room)if(client.readyState===1)client.send(left);
        return;
      }
      if(message.type==='RTC_SIGNAL') {
        const roomCode=String(message.roomCode||'').trim().toUpperCase();
        const targetUserId=String(message.targetUserId||'');
        const signalType=String(message.signalType||'').toUpperCase();
        const room=rtcRooms.get(roomCode);
        if(!room||!room.has(socket)||!targetUserId||!['OFFER','ANSWER','ICE'].includes(signalType))return;
        const packet=JSON.stringify({
          type:'RTC_SIGNAL',roomCode,signalType,fromUserId:String(identity.userId),
          sdp:message.sdp||null,candidate:message.candidate||null,sdpMid:message.sdpMid||null,
          sdpMLineIndex:Number.isInteger(message.sdpMLineIndex)?message.sdpMLineIndex:null,timestamp:Date.now()
        });
        for(const client of room) {
          if(client!==socket&&client.readyState===1&&String(client.mboteUserId)===targetUserId)client.send(packet);
        }
        return;
      }
      const streamId=String(message.streamId||'');
      if(message.type==='LIVE_JOIN' && streamId) {
        const live=await db.query("SELECT host_id FROM live_streams WHERE id=$1 AND status='LIVE'",[streamId]);
        if(!live.rowCount)return;
        joinedStream=streamId;
        if(!liveSockets.has(streamId))liveSockets.set(streamId,new Set());
        liveSockets.get(streamId).add(socket);
        if(String(live.rows[0].host_id)!==String(identity.userId)) {
          await db.query('INSERT INTO live_stream_viewers(stream_id,user_id) VALUES($1,$2) ON CONFLICT(stream_id,user_id) DO UPDATE SET left_at=NULL,joined_at=NOW()',[streamId,identity.userId]);
        }
        broadcastLive(streamId,{type:'LIVE_VIEWER_COUNT',streamId,viewerCount:await viewerCount(streamId),timestamp:Date.now()});
        return;
      }
      if(message.type==='LIVE_LEAVE' && streamId && joinedStream===streamId) {
        liveSockets.get(streamId)?.delete(socket);
        await db.query('UPDATE live_stream_viewers SET left_at=NOW() WHERE stream_id=$1 AND user_id=$2',[streamId,identity.userId]).catch(()=>{});
        joinedStream=null;
        broadcastLive(streamId,{type:'LIVE_VIEWER_COUNT',streamId,viewerCount:await viewerCount(streamId),timestamp:Date.now()});
        return;
      }
      if(!streamId || joinedStream!==streamId)return;
      const base={streamId,senderName:socket.mboteUserName||'Utilisateur MBoté',timestamp:Date.now()};
      if(message.type==='LIVE_COMMENT') {
        const liveText=String(message.text||'').trim().slice(0,1000);
        if(liveText) broadcastLive(streamId,{...base,type:'LIVE_COMMENT',payloadText:liveText,badgeType:message.badgeType||null});
      }
      if(message.type==='LIVE_REACTION') {
        const emoji=String(message.emoji||'').slice(0,16);
        if(emoji) broadcastLive(streamId,{...base,type:'LIVE_REACTION',emoji});
      }
      if(message.type==='LIVE_GIFT') broadcastLive(streamId,{...base,type:'LIVE_GIFT',giftId:message.giftId||null,giftName:message.giftName||null,giftEmoji:message.emoji||null,giftValueFcfa:Number(message.valueFcfa)||0});
      if(message.type==='LIVE_SIGNAL') {
        const signalType=String(message.signalType||'');
        if(!['OFFER','ANSWER','ICE'].includes(signalType))return;
        const packet={...message,type:'LIVE_SIGNAL',signalType,fromUserId:identity.userId};
        const targetUserId=String(message.targetUserId||'');
        if(targetUserId) {
          const encoded=JSON.stringify(packet);
          for(const client of (liveSockets.get(streamId)||new Set())) {
            if(client!==socket && client.readyState===1 && client.mboteUserId===targetUserId) client.send(encoded);
          }
        } else broadcastLive(streamId,packet,socket);
      }
      if(message.type==='LIVE_STATUS') broadcastLive(streamId,{...base,type:'LIVE_STATUS',status:String(message.status||'')});
    });
    socket.on('close',async()=>{
      if(identity) {
        const userId=String(identity.userId);
        userSockets.get(userId)?.delete(socket);
        if(userSockets.get(userId)?.size===0) userSockets.delete(userId);
      }
      if(socket.mboteRtcRooms) {
        for(const roomCode of [...socket.mboteRtcRooms]) {
          const room=rtcRooms.get(roomCode);
          room?.delete(socket);
          if(room && !room.size)rtcRooms.delete(roomCode);
          const left=JSON.stringify({type:'RTC_PEER_LEFT',roomCode,userId:String(identity?.userId||''),timestamp:Date.now()});
          for(const client of (room||new Set()))if(client.readyState===1)client.send(left);
        }
      }
      if(!identity||!joinedStream)return;
      liveSockets.get(joinedStream)?.delete(socket);
      await db.query('UPDATE live_stream_viewers SET left_at=NOW() WHERE stream_id=$1 AND user_id=$2',[joinedStream,identity.userId]).catch(()=>{});
      broadcastLive(joinedStream,{type:'LIVE_VIEWER_COUNT',streamId:joinedStream,viewerCount:await viewerCount(joinedStream),timestamp:Date.now()});
    });
  });
  const close = async () => {
    clearInterval(heartbeat);
    for (const socket of wss.clients) socket.close(1001, 'Serveur en arrêt');
    wss.close();
    server.close();
    await db.end();
  };
  process.on('SIGINT', close); process.on('SIGTERM', close);
}
module.exports = { createApp, createPool };
