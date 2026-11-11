/* Service Worker: hakt Todos direkt aus der Benachrichtigung ab – ohne die App zu öffnen. */
importScripts('shared.js');

const CACHE = 'abhaken-v2';
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
  const id = event.notification.data && event.notification.data.id;

  if (event.action === 'done') {
    // Abhaken im Hintergrund: kein Fenster öffnen, damit das Handy gesperrt bleiben kann.
    event.notification.close();
    event.waitUntil((async () => {
      const todo = await Abhaken.getTodo(id);
      if (todo && !todo.done) {
        todo.done = Date.now();
        await Abhaken.putTodo(todo);
      }
      Abhaken.announce();
    })());
    return;
  }

  // Tipp auf die Benachrichtigung selbst: App öffnen, Todo bleibt stehen.
  event.waitUntil((async () => {
    const wins = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    if (wins.length) return wins[0].focus();
    return self.clients.openWindow('./');
  })());
});

/* Weggewischt? Wenn „Anheften“ an ist, kommt ein offenes Todo zurück. */
self.addEventListener('notificationclose', (event) => {
  const id = event.notification.data && event.notification.data.id;
  event.waitUntil((async () => {
    if (!(await Abhaken.getSetting('pin'))) return;
    const todo = await Abhaken.getTodo(id);
    if (todo && !todo.done) await Abhaken.notify(self.registration, todo);
  })());
});
