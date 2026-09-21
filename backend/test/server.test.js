const assert = require('node:assert/strict');
const test = require('node:test');
const jwt = require('jsonwebtoken');
const { createApp, cleanupOrphanedPublicationUploads } = require('../server');

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


test('RTC ICE configuration is authenticated and reads TURN secrets only on the server', async () => {
  const previous = {
    url: process.env.MBOTE_TURN_URL,
    urls: process.env.MBOTE_TURN_URLS,
    username: process.env.MBOTE_TURN_USERNAME,
    credential: process.env.MBOTE_TURN_CREDENTIAL,
  };
  process.env.MBOTE_TURN_URL = 'turn:relay.example.test:443';
  delete process.env.MBOTE_TURN_URLS;
  process.env.MBOTE_TURN_USERNAME = 'runtime-user';
  process.env.MBOTE_TURN_CREDENTIAL = 'runtime-credential';

  const userId = '22222222-2222-4222-8222-222222222222';
  const token = jwt.sign({ userId, email: 'rtc@example.com', role: 'USER' }, secret, {
    expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile',
  });
  const db = { query: async () => ({ rowCount: 1, rows: [] }) };

  try {
    await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
      const anonymous = await fetch(`${baseUrl}/v1/rtc/ice-servers`);
      assert.equal(anonymous.status, 401);

      const response = await fetch(`${baseUrl}/v1/rtc/ice-servers`, {
        headers: { authorization: `Bearer ${token}` },
      });
      assert.equal(response.status, 200);
      assert.match(response.headers.get('cache-control') || '', /no-store/);
      const body = await response.json();
      const turn = body.data.iceServers.find((server) => server.urls.some((url) => url.startsWith('turn:')));
      assert.equal(turn.username, 'runtime-user');
      assert.equal(turn.credential, 'runtime-credential');
    });
  } finally {
    if (previous.url === undefined) delete process.env.MBOTE_TURN_URL; else process.env.MBOTE_TURN_URL = previous.url;
    if (previous.urls === undefined) delete process.env.MBOTE_TURN_URLS; else process.env.MBOTE_TURN_URLS = previous.urls;
    if (previous.username === undefined) delete process.env.MBOTE_TURN_USERNAME; else process.env.MBOTE_TURN_USERNAME = previous.username;
    if (previous.credential === undefined) delete process.env.MBOTE_TURN_CREDENTIAL; else process.env.MBOTE_TURN_CREDENTIAL = previous.credential;
  }
});


test('orphaned media cleanup deletes only old unreferenced upload candidates in bounded batches', async () => {
  const calls = [];
  const db = {
    query: async (sql, params) => {
      calls.push({ sql, params });
      return { rowCount: 3, rows: [{ id: 'a' }, { id: 'b' }, { id: 'c' }] };
    },
  };

  const removed = await cleanupOrphanedPublicationUploads(db, { retentionDays: 14, batchSize: 25 });
  assert.equal(removed, 3);
  assert.equal(calls.length, 1);
  assert.deepEqual(calls[0].params, [14, 25]);
  assert.match(calls[0].sql, /publication_uploads/);
  assert.match(calls[0].sql, /news_posts/);
  assert.match(calls[0].sql, /short_videos/);
  assert.match(calls[0].sql, /messages/);
  assert.match(calls[0].sql, /statuses/);
  assert.match(calls[0].sql, /LIMIT \$2/);
});
