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


test('LoukaPay badge purchase uses server-priced merchant contract', async () => {
  const userId = '22222222-2222-4222-8222-222222222222';
  const token = jwt.sign({ userId, email: 'pay@example.com', role: 'USER' }, secret, {
    expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile',
  });
  const intentId = '33333333-3333-4333-8333-333333333333';
  const previousFetch = global.fetch;
  const previousUrl = process.env.PAYMENTS_API_URL;
  const previousKey = process.env.PAYMENTS_API_KEY;
  let upstreamRequest;
  process.env.PAYMENTS_API_URL = 'https://loukapay.test/v1/payment-intents';
  process.env.PAYMENTS_API_KEY = 'lp_sk_test_secret';
  global.fetch = async (url, options = {}) => {
    upstreamRequest = { url: String(url), options };
    return { ok: true, status: 201, json: async () => ({
      id: '44444444-4444-4444-8444-444444444444',
      status: 'pending',
      checkout_url: 'https://checkout.test/x',
      checkout_required: false,
    }) };
  };
  const db = { query: async (sql) => {
    if (sql.includes('SELECT id,title,price_fcfa FROM badge_catalog')) {
      return { rowCount: 1, rows: [{ id: 'badge_vip', title: 'Badge VIP Prestige', price_fcfa: 10000 }] };
    }
    if (sql.includes('SELECT 1 FROM user_badges')) return { rowCount: 0, rows: [] };
    if (sql.includes('INSERT INTO payment_intents')) {
      return { rowCount: 1, rows: [{
        id: intentId, user_id: userId, provider: 'mtn', amount_fcfa: 10000,
        phone: '242060000000', purpose: 'BADGE_PURCHASE',
        payload: { badgeId: 'badge_vip' }, status: 'PENDING', fulfilled_at: null,
      }] };
    }
    if (sql.includes('UPDATE payment_intents SET provider_reference')) {
      return { rowCount: 1, rows: [{
        id: intentId, user_id: userId, provider: 'mtn', provider_reference: '44444444-4444-4444-8444-444444444444',
        amount_fcfa: 10000, purpose: 'BADGE_PURCHASE', payload: { badgeId: 'badge_vip' },
        status: 'PENDING', checkout_url: 'https://checkout.test/x', fulfilled_at: null,
      }] };
    }
    return { rowCount: 1, rows: [] };
  } };

  try {
    await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
      const response = await previousFetch(\`${baseUrl}/v1/payments/intents\`, {
        method: 'POST',
        headers: { authorization: \`Bearer ${token}\`, 'content-type': 'application/json' },
        body: JSON.stringify({ provider: 'mtn', phone: '242060000000', purpose: 'BADGE_PURCHASE', badgeId: 'badge_vip' }),
      });
      assert.equal(response.status, 201);
      const body = await response.json();
      assert.equal(body.data.intentId, intentId);
      assert.equal(body.data.amount, 10000);
      assert.equal(body.data.fulfilled, false);
    });
    assert.equal(upstreamRequest.url, 'https://loukapay.test/v1/payment-intents');
    assert.equal(upstreamRequest.options.headers['idempotency-key'], \`mbote_${intentId}\`);
    const upstreamBody = JSON.parse(upstreamRequest.options.body);
    assert.equal(upstreamBody.provider, 'mtn');
    assert.equal(upstreamBody.amount, 10000);
    assert.equal(upstreamBody.external_reference, intentId);
    assert.equal(upstreamBody.customer_msisdn, '242060000000');
    assert.equal(upstreamBody.metadata.mbote_purpose, 'BADGE_PURCHASE');
  } finally {
    global.fetch = previousFetch;
    if (previousUrl === undefined) delete process.env.PAYMENTS_API_URL; else process.env.PAYMENTS_API_URL = previousUrl;
    if (previousKey === undefined) delete process.env.PAYMENTS_API_KEY; else process.env.PAYMENTS_API_KEY = previousKey;
  }
});

test('confirmed LoukaPay badge payment is fulfilled exactly once by MBote', async () => {
  const userId = '55555555-5555-4555-8555-555555555555';
  const intentId = '66666666-6666-4666-8666-666666666666';
  const token = jwt.sign({ userId, email: 'badge@example.com', role: 'USER' }, secret, {
    expiresIn: '30d', issuer: 'mbote-api', audience: 'mbote-mobile',
  });
  const previousFetch = global.fetch;
  const previousKey = process.env.PAYMENTS_API_KEY;
  const previousBase = process.env.LOUKAPAY_BASE_URL;
  process.env.PAYMENTS_API_KEY = 'lp_sk_test_secret';
  process.env.LOUKAPAY_BASE_URL = 'https://loukapay.test';
  global.fetch = async () => ({ ok: true, status: 200, json: async () => ({ id: 'lp-1', status: 'succeeded' }) });

  let badgeInsertCount = 0;
  let fulfilled = false;
  const pending = {
    id: intentId, user_id: userId, provider: 'mtn', provider_reference: 'lp-1',
    amount_fcfa: 10000, status: 'PENDING', purpose: 'BADGE_PURCHASE',
    payload: { badgeId: 'badge_vip' }, fulfilled_at: null,
  };
  const db = { query: async (sql) => {
    if (sql === 'BEGIN' || sql === 'COMMIT' || sql === 'ROLLBACK') return { rowCount: 0, rows: [] };
    if (sql.includes('SELECT * FROM payment_intents WHERE id=$1 AND user_id=$2')) {
      return { rowCount: 1, rows: [pending] };
    }
    if (sql.includes('UPDATE payment_intents SET status=$2,checkout_url=')) {
      return { rowCount: 1, rows: [{ ...pending, status: 'COMPLETED' }] };
    }
    if (sql.includes('SELECT * FROM payment_intents WHERE id=$1 FOR UPDATE')) {
      return { rowCount: 1, rows: [{ ...pending, status: 'COMPLETED', fulfilled_at: fulfilled ? new Date().toISOString() : null }] };
    }
    if (sql.includes('INSERT INTO user_badges')) {
      badgeInsertCount += 1;
      return { rowCount: 1, rows: [] };
    }
    if (sql.includes('UPDATE payment_intents SET fulfilled_at')) {
      fulfilled = true;
      return { rowCount: 1, rows: [{ ...pending, status: 'COMPLETED', fulfilled_at: new Date().toISOString() }] };
    }
    return { rowCount: 1, rows: [] };
  } };

  try {
    await withServer(createApp({ db, jwtSecret: secret }), async (baseUrl) => {
      const first = await previousFetch(\`${baseUrl}/v1/payments/intents/${intentId}\`, {
        headers: { authorization: \`Bearer ${token}\` },
      });
      assert.equal(first.status, 200);
      const firstBody = await first.json();
      assert.equal(firstBody.data.status, 'COMPLETED');
      assert.equal(firstBody.data.fulfilled, true);

      const second = await previousFetch(\`${baseUrl}/v1/payments/intents/${intentId}\`, {
        headers: { authorization: \`Bearer ${token}\` },
      });
      assert.equal(second.status, 200);
    });
    assert.equal(badgeInsertCount, 1);
  } finally {
    global.fetch = previousFetch;
    if (previousKey === undefined) delete process.env.PAYMENTS_API_KEY; else process.env.PAYMENTS_API_KEY = previousKey;
    if (previousBase === undefined) delete process.env.LOUKAPAY_BASE_URL; else process.env.LOUKAPAY_BASE_URL = previousBase;
  }
});
