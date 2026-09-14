/** MBoté API backed exclusively by PostgreSQL (no seeded users or fake content). */
require('dotenv').config();
const cors = require('cors');
const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const crypto = require('node:crypto');
const { Pool } = require('pg');

const PORT = Number(process.env.PORT || 8080);
const API_VERSION = '1.5.0';

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
  const member = async (chatId, userId) => (await db.query('SELECT 1 FROM chat_participants WHERE chat_id = $1 AND user_id = $2', [chatId, userId])).rowCount > 0;
  const messageDto = (row, userId) => ({ id: row.id, chatId: row.chat_id, senderId: row.sender_id, senderName: row.sender_name, senderAvatar: row.sender_avatar || '', text: row.text || '', timestamp: row.created_at, mediaType: row.media_type || 'NONE', mediaUrl: row.media_url, isStarred: Boolean(row.is_starred), isMine: row.sender_id === userId });

  app.disable('x-powered-by');
  app.use(cors({ origin(origin, callback) { return !origin || origins.includes(origin) ? callback(null, true) : callback(new Error('Origine CORS non autorisée')); } }));
  app.use(express.json({ limit: '1mb' }));

  app.get(['/health', '/v1/health'], route(async (_req, res) => { await db.query('SELECT 1'); success(res, { status: 'online', version: API_VERSION, timestamp: new Date().toISOString() }); }));

  app.post('/v1/auth/register', route(async (req, res) => {
    const fullName = text(req.body.fullName, 'Nom complet', 255);
    const email = text(req.body.email, 'Email', 255).toLowerCase();
    const password = text(req.body.password, 'Mot de passe', 256);
    if (!/^\S+@\S+\.\S+$/.test(email)) return failure(res, 400, 'Email invalide');
    if (password.length < 12) return failure(res, 400, 'Le mot de passe doit contenir au moins 12 caractères');
    try {
      const result = await db.query(
        'INSERT INTO users (email, password_hash, full_name, phone, country, city) VALUES ($1, $2, $3, $4, $5, $6) RETURNING *',
        [email, await bcrypt.hash(password, 12), fullName, req.body.phone?.trim() || null, req.body.country?.trim() || 'Congo', req.body.city?.trim() || 'Brazzaville'],
      );
      const user = result.rows[0];
      return success(res, { token: tokenFor(user), refreshToken: null, userId: user.id, ...publicUser(user) }, 201);
    } catch (error) { if (error.code === '23505') return failure(res, 409, 'Un compte existe déjà pour cet email'); throw error; }
  }));

  app.post('/v1/auth/login', route(async (req, res) => {
    const email = text(req.body.email, 'Email', 255).toLowerCase();
    const password = text(req.body.password, 'Mot de passe', 256);
    const result = await db.query('SELECT * FROM users WHERE email = $1', [email]);
    const user = result.rows[0];
    if (!user || !user.password_hash || !(await bcrypt.compare(password, user.password_hash))) return failure(res, 401, 'Identifiants invalides');
    return success(res, { token: tokenFor(user), refreshToken: null, userId: user.id, ...publicUser(user) });
  }));
  app.get('/v1/auth/me', auth, route(async (req, res) => {
    const result = await db.query('SELECT * FROM users WHERE id = $1', [req.user.userId]);
    return result.rowCount ? success(res, publicUser(result.rows[0])) : failure(res, 401, 'Compte introuvable');
  }));
  // OAuth remains unavailable until a server-side provider is configured.
  app.post('/v1/auth/google', (_req, res) => failure(res, 501, 'Google OAuth doit être configuré côté serveur'));

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
    return success(res, messageDto({ ...inserted.rows[0], sender_name: user.rows[0].full_name, sender_avatar: user.rows[0].avatar_url }, req.user.userId), 201);
  };
  app.post('/v1/messages/send', auth, route(sendMessage));
  app.post('/v1/chats/:chatId/messages', auth, route((req, res) => sendMessage({ ...req, body: { ...req.body, chatId: req.params.chatId } }, res)));
  app.delete('/v1/messages/:messageId', auth, route(async (req, res) => {
    const result = await db.query('DELETE FROM messages WHERE id = $1 AND sender_id = $2 RETURNING id', [req.params.messageId, req.user.userId]);
    return result.rowCount ? success(res, true) : failure(res, 404, 'Message introuvable ou non modifiable');
  }));
  app.post('/v1/messages/:messageId/star', auth, route(async (req, res) => {
    const changed = await db.query('INSERT INTO message_stars (message_id, user_id) VALUES ($1, $2) ON CONFLICT (message_id, user_id) DO DELETE RETURNING message_id', [req.params.messageId, req.user.userId]);
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
    const changed = await db.query('INSERT INTO news_post_likes (news_post_id, user_id) VALUES ($1, $2) ON CONFLICT (news_post_id, user_id) DO DELETE RETURNING news_post_id', [req.params.postId, req.user.userId]);
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
    const result = await db.query('SELECT id, full_name AS name, COALESCE(avatar_url, \'\') AS avatar, COALESCE(city, country, \'\') AS "infoSubtitle", 0 AS "mutualFriendsCount", ARRAY[]::text[] AS "mutualFriendsAvatars", false AS "isOnline", COALESCE(city, \'\') AS city, NULL::text AS "timeBadge", \'FRIENDS\' AS "subType" FROM users WHERE id <> $1 ORDER BY created_at DESC LIMIT 100', [req.user.userId]);
    success(res, result.rows);
  }));

  app.post('/v1/ai/smart-replies', auth, route(async (req, res) => {
    if (!process.env.GEMINI_API_KEY || !process.env.GEMINI_MODEL) {
      return failure(res, 503, 'L’assistant IA est temporairement indisponible');
    }
    const messages = Array.isArray(req.body.messages) ? req.body.messages.slice(-6) : [];
    if (!messages.length) return failure(res, 400, 'Historique de conversation requis');
    const history = messages.map((item) => {
      const body = typeof item?.text === 'string' ? item.text.trim().slice(0, 1000) : '';
      const sender = item?.isMine ? 'Moi' : String(item?.senderName || 'Contact').slice(0, 80);
      return body ? `${sender}: ${body}` : '';
    }).filter(Boolean).join('\n');
    if (!history) return failure(res, 400, 'Historique de conversation invalide');

    const style = ['Brief', 'Balanced', 'Elaborate'].includes(req.body.conciseness)
      ? req.body.conciseness : 'Balanced';
    const upstream = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(process.env.GEMINI_MODEL)}:generateContent?key=${encodeURIComponent(process.env.GEMINI_API_KEY)}`,
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: `Conversation récente :\n${history}\n\nPropose exactement trois réponses adaptées.` }] }],
          systemInstruction: { parts: [{ text: `Tu aides un utilisateur de MBoté. Style: ${style}. Réponds uniquement avec un tableau JSON de trois chaînes courtes.` }] },
          generationConfig: { temperature: 0.5, responseMimeType: 'application/json' },
        }),
      },
    );
    if (!upstream.ok) throw Object.assign(new Error('Le fournisseur IA n’a pas répondu'), { status: 502 });
    const payload = await upstream.json();
    const raw = payload?.candidates?.[0]?.content?.parts?.[0]?.text;
    let suggestions;
    try { suggestions = JSON.parse(raw); } catch { suggestions = []; }
    if (!Array.isArray(suggestions) || !suggestions.length) {
      throw Object.assign(new Error('Réponse IA invalide'), { status: 502 });
    }
    success(res, { suggestions: suggestions.filter((item) => typeof item === 'string' && item.trim()).slice(0, 3) });
  }));

  app.use((error, _req, res, _next) => { if (error.status) return failure(res, error.status, error.message); console.error('[mbote-api]', error); return failure(res, 500, 'Erreur interne du serveur'); });
  return app;
}

if (require.main === module) {
  const db = createPool();
  const server = createApp({ db }).listen(PORT, () => console.log(`MBoté API v${API_VERSION} écoute sur ${PORT}`));
  const close = async () => { server.close(); await db.end(); };
  process.on('SIGINT', close); process.on('SIGTERM', close);
}
module.exports = { createApp, createPool };
