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
    return success(res, messageDto({ ...inserted.rows[0], sender_name: user.rows[0].full_name, sender_avatar: user.rows[0].avatar_url }, req.user.userId), 201);
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
    const result = await db.query('SELECT id, full_name AS name, COALESCE(avatar_url, \'\') AS avatar, COALESCE(city, country, \'\') AS "infoSubtitle", 0 AS "mutualFriendsCount", ARRAY[]::text[] AS "mutualFriendsAvatars", false AS "isOnline", COALESCE(city, \'\') AS city, NULL::text AS "timeBadge", \'FRIENDS\' AS "subType" FROM users WHERE id <> $1 ORDER BY created_at DESC LIMIT 100', [req.user.userId]);
    success(res, result.rows);
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
