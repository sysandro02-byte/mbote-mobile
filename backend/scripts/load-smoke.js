'use strict';

const WebSocket = require('ws');

const origin = (process.env.API_ORIGIN || 'https://mbote-backend.onrender.com').replace(/\/$/, '');
const tokenA = (process.env.MBOTE_LOAD_TEST_TOKEN_A || '').trim();
const tokenB = (process.env.MBOTE_LOAD_TEST_TOKEN_B || '').trim();
const chatId = (process.env.MBOTE_LOAD_TEST_CHAT_ID || '').trim();

async function request(path, token) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 15000);
  try {
    const response = await fetch(origin + path, {
      headers: token ? { authorization: `Bearer ${token}` } : {},
      signal: controller.signal,
    });
    const text = await response.text();
    return { status: response.status, text };
  } finally {
    clearTimeout(timer);
  }
}

async function concurrentRead(path, expected, count = 25, token = '') {
  const started = Date.now();
  const results = await Promise.all(Array.from({ length: count }, () => request(path, token)));
  const bad = results.filter((r) => r.status !== expected);
  if (bad.length) {
    throw new Error(`${path}: ${bad.length}/${count} requests failed; first status=${bad[0].status}`);
  }
  return { path, count, durationMs: Date.now() - started };
}

function wsUrl() {
  return origin.replace(/^https:/, 'wss:').replace(/^http:/, 'ws:') + '/ws';
}

function invalidWebSocketProbe() {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(wsUrl());
    const timer = setTimeout(() => { ws.terminate(); reject(new Error('invalid WebSocket auth timeout')); }, 12000);
    ws.on('open', () => ws.send(JSON.stringify({ type: 'AUTH', token: 'mbote-load-invalid-token' })));
    ws.on('close', (code) => {
      clearTimeout(timer);
      if (code !== 1008) return reject(new Error(`invalid WebSocket expected close 1008, got ${code}`));
      resolve();
    });
    ws.on('error', (error) => { clearTimeout(timer); reject(error); });
  });
}

function authenticatedWebSocketProbe(token, label) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(wsUrl());
    const timer = setTimeout(() => { ws.terminate(); reject(new Error(`${label} WebSocket AUTH_OK timeout`)); }, 12000);
    ws.on('open', () => ws.send(JSON.stringify({ type: 'AUTH', token })));
    ws.on('message', (raw) => {
      let message;
      try { message = JSON.parse(raw.toString()); } catch { return; }
      if (message.type === 'AUTH_OK') {
        if (chatId) ws.send(JSON.stringify({ type: 'CHAT_TYPING', chatId, isTyping: true }));
        clearTimeout(timer);
        ws.close(1000, 'load probe complete');
        resolve();
      }
    });
    ws.on('error', (error) => { clearTimeout(timer); reject(error); });
  });
}

(async () => {
  const checks = [];
  checks.push(await concurrentRead('/v1/health', 200, 30));
  checks.push(await concurrentRead('/v1/readiness', 200, 30));
  await Promise.all(Array.from({ length: 8 }, invalidWebSocketProbe));

  if (tokenA) {
    for (const path of ['/v1/chats', '/v1/publications', '/v1/statuses', '/v1/short-videos', '/v1/live']) {
      checks.push(await concurrentRead(path, 200, 5, tokenA));
    }
    await authenticatedWebSocketProbe(tokenA, 'account A');
  }

  if (tokenB) {
    checks.push(await concurrentRead('/v1/chats', 200, 5, tokenB));
    await authenticatedWebSocketProbe(tokenB, 'account B');
  }

  console.log(JSON.stringify({
    ok: true,
    origin,
    authenticatedAccounts: Number(Boolean(tokenA)) + Number(Boolean(tokenB)),
    checks,
  }, null, 2));
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
