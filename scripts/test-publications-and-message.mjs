const BASE_URL = (process.env.E2E_BASE_URL || 'https://mbote-backend.onrender.com').replace(/\/$/, '');
const TOKEN = String(process.env.E2E_AUTH_TOKEN || '').trim();
const CHAT_ID = String(process.env.E2E_CHAT_ID || '').trim();
const runId = Date.now().toString(36);

if (!TOKEN || !CHAT_ID) {
  console.error('E2E_AUTH_TOKEN et E2E_CHAT_ID sont requis. Le chat doit appartenir au compte de test.');
  process.exit(2);
}

const checks = [];

const expect = (condition, label, details = '') => {
  checks.push({ ok: Boolean(condition), label, details });
  if (!condition) {
    throw new Error(`${label}${details ? `: ${details}` : ''}`);
  }
};

const request = async (path, { token, method = 'GET', body } = {}) => {
  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await response.text();
  let data = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = { raw: text };
  }
  if (!response.ok) {
    throw new Error(`${method} ${path} failed ${response.status}: ${data.error || text}`);
  }
  return data;
};

const main = async () => {
  const health = await request('/v1/health');
  expect(health.success === true && health.data?.status === 'online', 'health backend');

  const status = await request('/v1/status/publications', {
    token: TOKEN,
    method: 'POST',
    body: {
      type: 'text',
      content: `Statut smoke mobile ${runId}`,
      background: '#6D28D9',
      visibility: 'public',
      durationHours: 24,
    },
  });
  expect(status.data?.id, 'publication statut', JSON.stringify(status));

  const actus = await request('/v1/actus/posts', {
    token: TOKEN,
    method: 'POST',
    body: {
      type: 'text',
      content: `Actus smoke mobile ${runId}`,
      visibility: 'public',
      allowComments: true,
      allowShares: true,
    },
  });
  expect(actus.data?.id, 'publication actus', JSON.stringify(actus));

  const shortVideo = await request('/v1/short-videos', {
    token: TOKEN,
    method: 'POST',
    body: {
      caption: `Courte video smoke mobile ${runId}`,
      videoUrl: `https://mbote.loukatech.com/e2e/short-${runId}.mp4`,
      durationSeconds: 15,
      musicName: 'Smoke test MBote',
      visibility: 'everyone',
    },
  });
  expect(shortVideo.data?.id, 'publication courte video', JSON.stringify(shortVideo));

  const message = await request(`/v1/chats/${CHAT_ID}/messages`, {
    token: TOKEN,
    method: 'POST',
    body: {
      text: `Message smoke mobile ${runId}`,
      mediaType: 'NONE',
    },
  });
  expect(message.data?.id, 'envoi message', JSON.stringify(message));

  const [publicActus, publicStatuses, publicShorts, messages] = await Promise.all([
    request('/v1/actus/posts?limit=100', { token: TOKEN }),
    request('/v1/status', { token: TOKEN }),
    request('/v1/short-videos?limit=50', { token: TOKEN }),
    request(`/v1/chats/${CHAT_ID}/messages`, { token: TOKEN }),
  ]);

  expect(publicActus.data?.some((item) => String(item.id) === String(actus.data?.id)), 'actus visible après publication');
  expect(publicStatuses.data?.some((item) => String(item.id) === String(status.data?.id)), 'statut visible après publication');
  expect(publicShorts.data?.some((item) => String(item.id) === String(shortVideo.data?.id)), 'courte vidéo visible après publication');
  expect(messages.data?.some((item) => String(item.id) === String(message.data?.id)), 'message visible après envoi');

  console.log(JSON.stringify({
    ok: true,
    baseUrl: BASE_URL,
    runId,
    statusId: status.data?.id,
    actusId: actus.data?.id,
    shortVideoId: shortVideo.data?.id,
    chatId: CHAT_ID,
    messageId: message.data?.id,
    checks,
  }, null, 2));
};

main().catch((error) => {
  console.error(JSON.stringify({ ok: false, baseUrl: BASE_URL, runId, error: error.message, checks }, null, 2));
  process.exit(1);
});
