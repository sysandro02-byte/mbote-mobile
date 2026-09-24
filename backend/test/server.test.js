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


test('publication contracts accept Android media types and keep jobs on the backend flow', async () => {
  const userId = '22222222-2222-4222-8222-222222222222';
  const token = jwt.sign({ userId, email: 'publisher@example.com', role: 'USER' }, secret, {
    expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile',
  });
  const executed = [];
  const createdAt = new Date().toISOString();
  const db = {
    query: async (sql, params = []) => {
      executed.push({ sql, params });
      if (sql.includes('INSERT INTO publication_uploads')) {
        return { rowCount: 1, rows: [{ id: '33333333-3333-4333-8333-333333333333' }] };
      }
      if (sql.includes('INSERT INTO statuses')) {
        return { rowCount: 1, rows: [{ id: 'status-1', created_at: createdAt }] };
      }
      if (sql.includes('INSERT INTO news_posts')) {
        return {
          rowCount: 1,
          rows: [{
            id: 'post-1',
            category: params[1] || 'public',
            content: params[3] || params[2] || '',
            image_url: params[4] || null,
            media_type: params[5] || 'TEXT',
            created_at: createdAt,
          }],
        };
      }
      if (sql.includes('INSERT INTO short_videos')) {
        return {
          rowCount: 1,
          rows: [{
            id: 'short-1',
            caption: params[3] || '',
            video_url: params[1],
            thumbnail_url: params[2] || null,
            music_track: params[4] || null,
            duration_seconds: params[5] || 0,
            created_at: createdAt,
          }],
        };
      }
      if (sql.includes('INSERT INTO job_offers')) {
        return {
          rowCount: 1,
          rows: [{
            id: 'job-1',
            title: params[0],
            company: params[1],
            location: params[3],
            type: params[5],
            description: params[8],
            activityDomain: params[4],
            duration: params[5],
            salary: params[7] || '',
            publishedAt: createdAt,
            expiresAt: '',
            url: '',
            imageUrl: params[2] || null,
          }],
        };
      }
      if (sql.includes('INSERT INTO job_applications')) return { rowCount: 1, rows: [] };
      if (sql.includes('SELECT full_name')) {
        return {
          rowCount: 1,
          rows: [{ full_name: 'MBoté Test', username: 'mbote-test', avatar_url: '', bio: 'Membre MBoté' }],
        };
      }
      return { rowCount: 1, rows: [] };
    },
  };

  await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
    const authHeaders = { authorization: `Bearer ${token}` };

    const upload = await fetch(`${baseUrl}/v1/uploads/status-media`, {
      method: 'POST',
      headers: { ...authHeaders, 'content-type': 'image/jpeg' },
      body: Buffer.from([0xff, 0xd8, 0xff, 0xd9]),
    });
    assert.equal(upload.status, 201);
    const uploadedUrl = (await upload.json()).data.url;
    assert.match(uploadedUrl, /\/v1\/uploads\/files\/33333333-3333-4333-8333-333333333333$/);

    const status = await fetch(`${baseUrl}/v1/status/publications`, {
      method: 'POST',
      headers: { ...authHeaders, 'content-type': 'application/json' },
      body: JSON.stringify({ type: 'photo', content: uploadedUrl, caption: 'Photo statut' }),
    });
    assert.equal(status.status, 201);
    assert.equal((await status.json()).data.type, 'image');

    const actus = await fetch(`${baseUrl}/v1/actus/posts`, {
      method: 'POST',
      headers: { ...authHeaders, 'content-type': 'application/json' },
      body: JSON.stringify({ type: 'image/jpeg', content: uploadedUrl, thumbnail: 'Photo Actus', visibility: 'public' }),
    });
    assert.equal(actus.status, 201);
    assert.equal((await actus.json()).data.type, 'image');

    const short = await fetch(`${baseUrl}/v1/short-videos`, {
      method: 'POST',
      headers: { ...authHeaders, 'content-type': 'application/json' },
      body: JSON.stringify({ videoUrl: 'https://media.example/short.mp4', caption: 'Short test', durationSeconds: 8 }),
    });
    assert.equal(short.status, 201);

    const job = await fetch(`${baseUrl}/v1/jobs`, {
      method: 'POST',
      headers: { ...authHeaders, 'content-type': 'application/json' },
      body: JSON.stringify({
        title: 'Développeur',
        company: 'LoukaTech',
        location: 'Brazzaville',
        activityDomain: 'Technologie',
        duration: 'CDI',
        workMode: 'Hybride',
        salary: 'Selon profil',
        description: 'Construire des fonctionnalités MBoté.',
      }),
    });
    assert.equal(job.status, 201);

    const apply = await fetch(`${baseUrl}/v1/jobs/job-1/apply`, {
      method: 'POST',
      headers: { ...authHeaders, 'content-type': 'application/json' },
      body: JSON.stringify({ cvUrl: 'https://example.com/cv.pdf' }),
    });
    assert.equal(apply.status, 201);
  });

  assert.ok(executed.some(({ sql, params }) =>
    sql.includes('INSERT INTO statuses') && params[1] === 'IMAGE' && String(params[2]).includes('/v1/uploads/files/')
  ));
  assert.ok(executed.some(({ sql, params }) =>
    sql.includes('INSERT INTO news_posts') && params[5] === 'IMAGE'
  ));
  assert.ok(executed.some(({ sql }) => sql.includes('INSERT INTO short_videos')));
  assert.ok(executed.some(({ sql }) => sql.includes('INSERT INTO job_offers')));
  assert.ok(executed.some(({ sql }) => sql.includes('INSERT INTO job_applications')));
});
