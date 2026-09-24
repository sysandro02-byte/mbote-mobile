const assert = require('node:assert/strict');
const test = require('node:test');
const jwt = require('jsonwebtoken');
const { createApp } = require('../server');

const secret = 'test-only-secret-that-is-long-enough-to-sign-jwts';

async function withServer(app, run) {
  const server = app.listen(0, '127.0.0.1');
  await new Promise((resolve) => server.once('listening', resolve));
  try {
    return await run(`http://127.0.0.1:${server.address().port}`);
  } finally {
    await new Promise((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
  }
}

test('refuses to create an API without a database connection', () => {
  assert.throws(() => createApp({ jwtSecret: secret }), /PostgreSQL/);
});

test('health is backed by a database probe and private routes reject anonymous users', async () => {
  let probes = 0;
  const db = { query: async (sql) => {
    if (sql === 'SELECT 1') probes += 1;
    return { rowCount: 1, rows: [] };
  } };
  await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
    const health = await fetch(`${baseUrl}/v1/health`);
    assert.equal(health.status, 200);
    assert.equal((await health.json()).data.status, 'online');

    const chats = await fetch(`${baseUrl}/v1/chats`);
    assert.equal(chats.status, 401);
  });
  assert.equal(probes, 1);
});

test('registration validates password strength before querying PostgreSQL', async () => {
  const db = { query: async () => { throw new Error('database must not be queried'); } };
  await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
    const response = await fetch(`${baseUrl}/v1/auth/register`, {
      method: 'POST', headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ fullName: 'Test User', email: 'test@example.com', password: 'short' }),
    });
    assert.equal(response.status, 400);
  });
});

test('password recovery never simulates email delivery when Brevo is unavailable', async () => {
  const previousKey = process.env.BREVO_API_KEY;
  delete process.env.BREVO_API_KEY;
  const db = { query: async () => { throw new Error('database must not be queried without email configuration'); } };
  try {
    await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
      const response = await fetch(`${baseUrl}/v1/auth/forgot-password`, {
        method: 'POST', headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ email: 'test@example.com' }),
      });
      assert.equal(response.status, 503);
      assert.equal((await response.json()).success, false);
    });
  } finally {
    if (previousKey === undefined) delete process.env.BREVO_API_KEY;
    else process.env.BREVO_API_KEY = previousKey;
  }
});

test('admin login refuses access when the server key is not configured', async () => {
  const previousKey = process.env.ADMIN_API_KEY;
  delete process.env.ADMIN_API_KEY;
  const db = { query: async () => { throw new Error('database must not be queried without admin configuration'); } };
  try {
    await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
      const response = await fetch(`${baseUrl}/v1/admin/login`, {
        method: 'POST', headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ adminKey: 'not-configured', email: 'admin@example.com', password: 'not-a-real-password' }),
      });
      assert.equal(response.status, 503);
      assert.equal((await response.json()).success, false);
    });
  } finally {
    if (previousKey === undefined) delete process.env.ADMIN_API_KEY;
    else process.env.ADMIN_API_KEY = previousKey;
  }
});

test('desktop QR confirmation is authenticated, hashed and single-use', async () => {
  const userId = '11111111-1111-4111-8111-111111111111';
  const token = jwt.sign({ userId, email: 'qr@example.com', role: 'USER' }, secret, {
    expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile',
  });
  const pairingToken = 'a'.repeat(43);
  const expectedHash = require('node:crypto').createHash('sha256').update(pairingToken).digest('hex');
  const queries = [];
  const db = { query: async (sql, params) => {
    queries.push({ sql, params });
    if (sql.includes('UPDATE desktop_login_pairings')) {
      return { rowCount: 1, rows: [{ expires_at: new Date(Date.now() + 60_000).toISOString() }] };
    }
    return { rowCount: 0, rows: [] };
  } };

  await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
    const anonymous = await fetch(`${baseUrl}/v1/auth/qr/confirm`, {
      method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ pairingToken }),
    });
    assert.equal(anonymous.status, 401);

    const confirmed = await fetch(`${baseUrl}/v1/auth/qr/confirm`, {
      method: 'POST',
      headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
      body: JSON.stringify({ pairingToken }),
    });
    assert.equal(confirmed.status, 200);
    assert.equal((await confirmed.json()).data.confirmed, true);
  });

  const update = queries.find(({ sql }) => sql.includes('UPDATE desktop_login_pairings'));
  assert.equal(update.params[0], expectedHash);
  assert.equal(update.params[1], userId);
  assert.ok(!JSON.stringify(queries).includes(pairingToken));
});


test('gift state and withdrawals use server-side gift earnings', async () => {
  const userId = '11111111-1111-4111-8111-111111111111';
  const token = jwt.sign({ userId, email: 'gift@example.com', role: 'USER' }, secret, {
    expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile',
  });
  const executed = [];
  const db = { query: async (sql) => {
    executed.push(sql);
    if (sql.includes('FROM user_gift_inventory')) return { rowCount: 1, rows: [{ giftId: 'g_bronze', quantity: 2 }] };
    if (sql.includes('FROM gift_transactions')) return { rowCount: 0, rows: [] };
    if (sql.includes('FROM wallet_withdrawals WHERE')) return { rowCount: 0, rows: [] };
    if (sql.includes('SELECT wallet_balance_fcfa')) return { rowCount: 1, rows: [{ walletBalanceFcfa: 5000, giftEarningsBalanceFcfa: 7000 }] };
    if (sql.includes('SELECT gift_earnings_balance_fcfa')) return { rowCount: 1, rows: [{ gift_earnings_balance_fcfa: 7000 }] };
    if (sql.includes('INSERT INTO wallet_withdrawals')) return { rowCount: 1, rows: [{ id: 'withdraw-1', status: 'PENDING' }] };
    return { rowCount: 1, rows: [] };
  } };

  await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
    const state = await fetch(`${baseUrl}/v1/gifts/me`, { headers: { authorization: `Bearer ${token}` } });
    assert.equal(state.status, 200);
    const body = await state.json();
    assert.equal(body.data.giftEarningsBalanceFcfa, 7000);
    assert.equal(body.data.walletBalanceFcfa, 5000);

    const withdrawal = await fetch(`${baseUrl}/v1/wallet/withdrawals`, {
      method: 'POST',
      headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
      body: JSON.stringify({ amountFcfa: 2000, provider: 'MTN Mobile Money', destinationAccount: '+242060000000' }),
    });
    assert.equal(withdrawal.status, 200);
  });

  assert.ok(executed.some((sql) => sql.includes('UPDATE users SET gift_earnings_balance_fcfa=gift_earnings_balance_fcfa-$2')));
  assert.ok(!executed.some((sql) => sql.includes('UPDATE users SET wallet_balance_fcfa=wallet_balance_fcfa-$2')));
});

test('status, actus, short video and message publish then read through the API', async () => {
  const userId = '11111111-1111-4111-8111-111111111111';
  const chatId = '22222222-2222-4222-8222-222222222222';
  const authToken = jwt.sign({ userId, email: 'publisher@example.com', role: 'USER' }, secret, {
    expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile',
  });
  const createdAt = new Date().toISOString();
  const state = { statuses: [], actus: [], shorts: [], messages: [] };
  let sequence = 0;
  const nextId = () => `00000000-0000-4000-8000-${String(++sequence).padStart(12, '0')}`;
  const db = { query: async (sql, params = []) => {
    if (sql.includes('SELECT 1 FROM chat_participants')) return { rowCount: 1, rows: [{ '?column?': 1 }] };
    if (sql.includes('INSERT INTO statuses')) {
      const row = { id: nextId(), author_id: userId, media_type: params[1], media_url: params[2], text: params[3], background_color: params[4], created_at: createdAt };
      state.statuses.unshift(row); return { rowCount: 1, rows: [row] };
    }
    if (sql.includes('FROM statuses s JOIN users') && sql.includes('reaction_count')) {
      return { rowCount: state.statuses.length, rows: state.statuses.map((row) => ({ id: row.id, user_id: userId, user_name: 'Test Publisher', user_avatar: '', type: row.media_type.toLowerCase(), content: row.text || row.media_url, background: row.background_color, caption: row.text, created_at: row.created_at, reaction_count: 0, my_reaction: null, comment_count: 0, share_count: 0, view_count: 0 })) };
    }
    if (sql.includes('INSERT INTO news_posts')) {
      const row = { id: nextId(), author_id: userId, category: params[1], content: params[3], image_url: params[4], media_type: params[5], created_at: createdAt };
      state.actus.unshift(row); return { rowCount: 1, rows: [row] };
    }
    if (sql.includes('FROM news_posts n JOIN users') && sql.includes('reaction_count')) {
      return { rowCount: state.actus.length, rows: state.actus.map((row) => ({ id: row.id, author_id: userId, author_name: 'Test Publisher', author_avatar: '', type: row.media_type.toLowerCase(), content: row.content, thumbnail: row.image_url, visibility: row.category, comment_count: 0, share_count: 0, reaction_count: 0, my_reaction: null, created_at: row.created_at })) };
    }
    if (sql.includes('INSERT INTO short_videos')) {
      const row = { id: nextId(), creator_id: userId, video_url: params[1], thumbnail_url: params[2], caption: params[3], music_track: params[4], duration_seconds: params[5], visibility: params[6], created_at: createdAt };
      state.shorts.unshift(row); return { rowCount: 1, rows: [row] };
    }
    if (sql.includes('FROM short_videos s JOIN users')) {
      return { rowCount: state.shorts.length, rows: state.shorts.map((row) => ({ id: row.id, user_id: userId, user_name: 'Test Publisher', user_username: 'publisher', user_avatar: '', caption: row.caption, video_url: row.video_url, thumbnail_url: row.thumbnail_url, music_name: row.music_track, duration_seconds: row.duration_seconds, like_count: 0, liked_by_me: false, comment_count: 0, share_count: 0, bookmark_count: 0, saved_by_me: false, followed_by_me: false, view_count: 0, created_at: row.created_at })) };
    }
    if (sql.includes('INSERT INTO messages ')) {
      const row = { id: nextId(), chat_id: params[0], sender_id: userId, text: params[2], media_type: params[3], media_url: params[4], created_at: createdAt };
      state.messages.push(row); return { rowCount: 1, rows: [row] };
    }
    if (sql.includes('FROM messages m JOIN users')) {
      return { rowCount: state.messages.length, rows: state.messages.map((row) => ({ ...row, sender_name: 'Test Publisher', sender_avatar: '', is_starred: false })) };
    }
    if (sql.includes('SELECT full_name, username, avatar_url')) return { rowCount: 1, rows: [{ full_name: 'Test Publisher', username: 'publisher', avatar_url: '' }] };
    if (sql.includes('SELECT full_name, avatar_url') || sql.includes('SELECT full_name,avatar_url')) return { rowCount: 1, rows: [{ full_name: 'Test Publisher', avatar_url: '' }] };
    if (sql.includes('SELECT user_id FROM chat_participants')) return { rowCount: 0, rows: [] };
    throw new Error(`Unexpected SQL in publication integration test: ${sql}`);
  } };

  const api = (baseUrl, path, options = {}) => fetch(`${baseUrl}${path}`, {
    ...options,
    headers: { authorization: `Bearer ${authToken}`, 'content-type': 'application/json', ...(options.headers || {}) },
  });

  await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
    const creations = await Promise.all([
      api(baseUrl, '/v1/status/publications', { method: 'POST', body: JSON.stringify({ type: 'text', content: 'Statut réel', durationHours: 24 }) }),
      api(baseUrl, '/v1/actus/posts', { method: 'POST', body: JSON.stringify({ type: 'text', content: 'Actualité réelle', visibility: 'public' }) }),
      api(baseUrl, '/v1/short-videos', { method: 'POST', body: JSON.stringify({ caption: 'Vidéo réelle', videoUrl: 'https://cdn.example.org/video.mp4', durationSeconds: 15 }) }),
      api(baseUrl, `/v1/chats/${chatId}/messages`, { method: 'POST', body: JSON.stringify({ text: 'Message réel', mediaType: 'NONE' }) }),
    ]);
    for (const response of creations) assert.equal(response.status, 201);
    const created = await Promise.all(creations.map((response) => response.json()));
    for (const body of created) assert.ok(body.success && body.data.id);

    const reads = await Promise.all([
      api(baseUrl, '/v1/status'), api(baseUrl, '/v1/actus/posts'),
      api(baseUrl, '/v1/short-videos'), api(baseUrl, `/v1/chats/${chatId}/messages`),
    ]);
    for (const response of reads) assert.equal(response.status, 200);
    const bodies = await Promise.all(reads.map((response) => response.json()));
    bodies.forEach((body, index) => assert.equal(body.data[0].id, created[index].data.id));
  });
});
