const CACHE_VERSION = 'mbote-app-shell-v9';
const STATIC_CACHE = 'mbote-static-v1';
const MEDIA_CACHE = 'mbote-media-v1';
const DEFAULT_NOTIFICATION_ICON = '/logo.png';
const DEFAULT_NOTIFICATION_BADGE = '/favicon.svg';
const APP_SHELL = [
  '/',
  '/index.html',
  '/manifest.json',
  '/favicon.svg',
  '/mbote-login-watermark.png',
  '/offline.html',
];

const isApiRequest = (url) => (
  url.pathname.startsWith('/api/')
  || url.pathname.startsWith('/socket.io/')
);

const isEmojiAssetRequest = (url) => (
  url.hostname === 'cdn.jsdelivr.net'
  && url.pathname.startsWith('/npm/emoji-datasource-')
  && url.pathname.endsWith('.png')
);

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_VERSION)
      .then((cache) => cache.addAll(APP_SHELL))
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => {
        const current = [CACHE_VERSION, STATIC_CACHE, MEDIA_CACHE];
        return Promise.all(keys.filter((key) => !current.includes(key)).map((key) => caches.delete(key)));
      })
      .then(() => self.clients.claim()),
  );
});

self.addEventListener('message', (event) => {
  if (event.data?.type === 'MBOTE_SKIP_WAITING') {
    self.skipWaiting();
  }
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  if (isEmojiAssetRequest(url)) {
    event.respondWith(
      caches.match(request).then((cached) => {
        if (cached) return cached;
        return fetch(request).then((response) => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_VERSION).then((cache) => cache.put(request, clone));
          }
          return response;
        });
      }),
    );
    return;
  }

  if (url.origin !== self.location.origin) return;
  if (isApiRequest(url)) {
    event.respondWith(
      fetch(request).catch(() => new Response(JSON.stringify({
        offline: true,
        error: 'Cette fonction nécessite une connexion Internet.',
      }), {
        status: 503,
        headers: { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' },
      })),
    );
    return;
  }

  if (request.mode === 'navigate') {
    event.respondWith(
      caches.match('/index.html').then((cached) => cached || fetch(request).catch(() => caches.match('/offline.html'))),
    );
    return;
  }

  if (request.destination === 'video' || request.destination === 'audio') {
    event.respondWith(fetch(request).catch(() => caches.match(request).then((cached) => cached || Response.error())));
    return;
  }

  const cacheName = request.destination === 'image' ? MEDIA_CACHE : STATIC_CACHE;
  event.respondWith(caches.match(request).then((cached) => {
    const network = fetch(request).then((response) => {
      if (response.ok) {
        const clone = response.clone();
        void caches.open(cacheName).then((cache) => cache.put(request, clone));
      }
      return response;
    });
    if (cached) {
      event.waitUntil(network.catch(() => undefined));
      return cached;
    }
    return network.catch(() => Response.error());
  }));
});

const normalizeNotificationPayload = async (event) => {
  if (!event.data) return { title: 'MBote', body: 'Nouvelle notification MBote' };
  try {
    return await event.data.json();
  } catch {
    return { title: 'MBote', body: await event.data.text() };
  }
};

self.addEventListener('push', (event) => {
  event.waitUntil((async () => {
    const payload = await normalizeNotificationPayload(event);
    const title = payload.title || payload.senderName || 'MBote';
    const body = payload.body || payload.message || 'Nouvelle activité sur MBote';
    const category = payload.category || payload.type || 'message';
    const notificationId = payload.notificationId || payload.id || '';
    const threadId = payload.threadId || payload.chatId || payload.targetId || payload.senderId || notificationId || category;
    const count = Number(payload.count || payload.unreadCount || 1);
    const targetUrl = payload.targetUrl || payload.url || payload.clickUrl || (notificationId ? `/app/notifications/open/${encodeURIComponent(notificationId)}` : '/app/notifications');
    const actions = category === 'call'
      ? [
          { action: 'answer', title: 'Repondre' },
          { action: 'reject', title: 'Refuser' },
        ]
      : [
          { action: 'open', title: 'Ouvrir' },
          { action: 'dismiss', title: 'Ignorer' },
        ];

    await self.registration.showNotification(title, {
      body,
      icon: payload.icon || payload.avatar || DEFAULT_NOTIFICATION_ICON,
      badge: payload.badge || DEFAULT_NOTIFICATION_BADGE,
      image: payload.image,
      tag: `mbote-${category}-${threadId}`,
      renotify: true,
      requireInteraction: Boolean(payload.requireInteraction || category === 'call'),
      silent: Boolean(payload.silent),
      timestamp: payload.timestamp || Date.now(),
      vibrate: category === 'call' ? [180, 80, 180, 80, 220] : [80, 40, 80],
      data: {
        url: targetUrl,
        targetUrl,
        deepLink: payload.deepLink,
        notificationId,
        category,
        threadId,
        count,
      },
      actions,
    });
  })());
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  if (event.action === 'dismiss' || event.action === 'reject') return;

  const rawTargetUrl = event.notification.data?.targetUrl || event.notification.data?.url || '/app/notifications';
  const targetUrl = new URL(rawTargetUrl, self.location.origin);
  if (event.action === 'answer') targetUrl.searchParams.set('callAction', 'answer');
  const targetHref = targetUrl.href;
  event.waitUntil((async () => {
    const clientList = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    for (const client of clientList) {
      if ('focus' in client) {
        await client.focus();
        if ('navigate' in client) await client.navigate(targetHref);
        return;
      }
    }
    await self.clients.openWindow(targetHref);
  })());
});
