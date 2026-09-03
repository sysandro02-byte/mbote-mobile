const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:3010';
const PASSWORD = 'Test^12345';
const runId = Date.now().toString(36);

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

const registerAndVerify = async (suffix) => {
  const username = `mobile_pub_${suffix}_${runId}`.toLowerCase();
  const challenge = await request('/api/auth/register', {
    method: 'POST',
    body: {
      email: `${username}@mbote.local`,
      password: PASSWORD,
      name: `Mobile Test ${suffix}`,
      username,
      birthDate: '1992-01-15',
      country: 'Congo',
      city: 'Brazzaville',
      address: `Avenue Test ${suffix}`,
      phoneNumber: `+24206${Math.floor(1000000 + Math.random() * 8999999)}`,
      gender: 'other',
      bio: `Compte smoke mobile ${suffix}`,
      accountType: 'personal',
      accountVisibility: 'public',
    },
  });

  expect(challenge.pendingUserId, `creation compte ${suffix}`, JSON.stringify(challenge));
  if (!challenge.devOtp) {
    const admin = await request('/api/admin/register', {
      method: 'POST',
      body: {
        name: `Mobile Admin ${suffix}`,
        email: `${username}_admin@mbote.local`,
        password: PASSWORD,
      },
    });
    expect(admin.token, `token admin fallback ${suffix}`);
    expect(admin.user?.id, `admin user id fallback ${suffix}`, `user=${admin.user?.id || ''}`);
    return admin;
  }

  const verified = await request('/api/auth/verify-registration-otp', {
    method: 'POST',
    body: { pendingUserId: challenge.pendingUserId, otp: challenge.devOtp },
  });
  expect(verified.token, `token auth ${suffix}`);
  expect(verified.user?.id, `user id ${suffix}`, JSON.stringify(verified.user || {}));
  return verified;
};

const main = async () => {
  const health = await request('/api/health');
  expect(health.status === 'ok', 'health backend');

  const author = await registerAndVerify('author');
  const recipient = await registerAndVerify('recipient');

  const status = await request('/api/status/publications', {
    token: author.token,
    method: 'POST',
    body: {
      type: 'text',
      content: `Statut smoke mobile ${runId}`,
      background: '#6D28D9',
      visibility: 'public',
      durationHours: 24,
    },
  });
  expect(status.id, 'publication statut', JSON.stringify(status));

  const actus = await request('/api/actus/posts', {
    token: author.token,
    method: 'POST',
    body: {
      type: 'text',
      content: `Actus smoke mobile ${runId}`,
      visibility: 'public',
      allowComments: true,
      allowShares: true,
    },
  });
  expect(actus.id, 'publication actus', JSON.stringify(actus));

  const shortVideo = await request('/api/short-videos', {
    token: author.token,
    method: 'POST',
    body: {
      caption: `Courte video smoke mobile ${runId}`,
      videoUrl: `https://mbote.loukatech.com/e2e/short-${runId}.mp4`,
      durationSeconds: 15,
      musicName: 'Smoke test MBote',
      visibility: 'everyone',
    },
  });
  expect(shortVideo.id, 'publication courte video', JSON.stringify(shortVideo));

  await request('/api/contacts', {
    token: author.token,
    method: 'POST',
    body: { contactId: recipient.user.id },
  });
  await request(`/api/contacts/${author.user.id}/accept`, {
    token: recipient.token,
    method: 'POST',
  });

  const chat = await request('/api/chats', {
    token: author.token,
    method: 'POST',
    body: { isGroup: false, participantIds: [recipient.user.id] },
  });
  expect(chat.id, 'creation discussion message', JSON.stringify(chat));

  const message = await request(`/api/chats/${chat.id}/messages`, {
    token: author.token,
    method: 'POST',
    body: {
      type: 'text',
      content: `Message smoke mobile ${runId}`,
      metadata: { source: 'codex-smoke-test' },
    },
  });
  expect(message.id, 'envoi message', JSON.stringify(message));

  const [publicActus, publicStatuses, publicShorts, messages] = await Promise.all([
    request('/api/actus/posts?limit=20'),
    request('/api/status'),
    request('/api/short-videos?limit=20'),
    request(`/api/chats/${chat.id}/messages`, { token: recipient.token }),
  ]);

  expect(Array.isArray(publicActus) && publicActus.some((item) => String(item.id) === String(actus.id)), 'actus visible en GET public');
  expect(Array.isArray(publicStatuses) && publicStatuses.some((item) => String(item.id) === String(status.id)), 'statut visible en GET public');
  expect(Array.isArray(publicShorts) && publicShorts.some((item) => String(item.id) === String(shortVideo.id)), 'courte video visible en GET public');
  expect(Array.isArray(messages) && messages.some((item) => String(item.id) === String(message.id)), 'message recu visible par destinataire');

  console.log(JSON.stringify({
    ok: true,
    baseUrl: BASE_URL,
    runId,
    statusId: status.id,
    actusId: actus.id,
    shortVideoId: shortVideo.id,
    chatId: chat.id,
    messageId: message.id,
    checks,
  }, null, 2));
};

main().catch((error) => {
  console.error(JSON.stringify({ ok: false, baseUrl: BASE_URL, runId, error: error.message, checks }, null, 2));
  process.exit(1);
});
