/* Service Worker: hakt Todos direkt aus der Benachrichtigung ab – ohne die App zu öffnen. */
importScripts('shared.js');

const CACHE = 'abhaken-v3';
const FILES = ['./', 'index.html', 'shared.js', 'manifest.webmanifest', 'icon-192.png', 'icon-512.png', 'badge.png'];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((c) => c.addAll(FILES)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

/* Netz zuerst, Cache als Rückfall – so läuft die App auch offline. */
self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET' || new URL(req.url).origin !== self.location.origin) return;
  event.respondWith(
    fetch(req)
      .then((res) => {
        if (res.ok) {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put(req, copy));
        }
        return res;
      })
      .catch(() => caches.match(req, { ignoreSearch: true }).then((hit) => hit || caches.match('index.html')))
  );
});

self.addEventListener('notificationclick', (event) => {
  const data = event.notification.data || {};

  if (event.action === 'done' || event.action === 'next') {
    // Abhaken im Hintergrund: kein Fenster öffnen, damit das Handy gesperrt bleiben kann.
    event.waitUntil((async () => {
      if (data.list) {
        if (event.action === 'done') await Abhaken.checkCurrent();
        else await Abhaken.moveCursor();
      } else if (event.action === 'done') {
        event.notification.close();
        const todo = await Abhaken.getTodo(data.id);
        if (todo && !todo.done) {
          todo.done = Date.now();
          await Abhaken.putTodo(todo);
        }
      }
      await Abhaken.refresh(self.registration);
      Abhaken.announce();
    })());
    return;
  }

  // Tipp auf die Benachrichtigung selbst: App öffnen, Todos bleiben stehen.
  event.waitUntil((async () => {
    const wins = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    if (wins.length) return wins[0].focus();
    return self.clients.openWindow('./');
  })());
});

/* Weggewischt? Wenn „Anheften“ an ist, kommen offene Todos zurück. */
self.addEventListener('notificationclose', (event) => {
  event.waitUntil((async () => {
    if (await Abhaken.getSetting('pin')) await Abhaken.refresh(self.registration);
  })());
});
