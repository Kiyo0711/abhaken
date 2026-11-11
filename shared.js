/* Gemeinsamer Code für Seite und Service Worker: Speicher + Benachrichtigungen. */
(function (root) {
  const DB_NAME = 'abhaken';

  function openDB() {
    return new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, 1);
      req.onupgradeneeded = () => {
        const db = req.result;
        if (!db.objectStoreNames.contains('todos')) db.createObjectStore('todos', { keyPath: 'id' });
        if (!db.objectStoreNames.contains('settings')) db.createObjectStore('settings');
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  function run(store, mode, fn) {
    return openDB().then((db) => new Promise((resolve, reject) => {
      const t = db.transaction(store, mode);
      const req = fn(t.objectStore(store));
      t.oncomplete = () => { db.close(); resolve(req ? req.result : undefined); };
      t.onerror = t.onabort = () => { db.close(); reject(t.error); };
    }));
  }

  const tag = (id) => 'todo-' + id;

  const api = {
    tag,
    allTodos: () => run('todos', 'readonly', (s) => s.getAll()).then((l) => l || []),
    getTodo: (id) => run('todos', 'readonly', (s) => s.get(id)),
    putTodo: (todo) => run('todos', 'readwrite', (s) => s.put(todo)),
    deleteTodo: (id) => run('todos', 'readwrite', (s) => s.delete(id)),
    getSetting: (key) => run('settings', 'readonly', (s) => s.get(key)),
    setSetting: (key, value) => run('settings', 'readwrite', (s) => s.put(value, key)),

    /* Zeigt ein Todo als Benachrichtigung mit „Erledigt“-Schaltfläche. */
    /* Android/Chrome kann Schaltflächen in Benachrichtigungen, iPhone/Safari nicht. */
    hasActions() {
      return typeof Notification !== 'undefined' && Notification.maxActions > 0;
    },

    /* Zeigt ein Todo als Benachrichtigung – wo möglich mit „Erledigt“-Schaltfläche. */
    notify(registration, todo) {
      const options = {
        tag: tag(todo.id),
        icon: 'icon-192.png',
        badge: 'badge.png',
        timestamp: todo.created,
        silent: true,
        data: { id: todo.id },
      };
      if (api.hasActions()) {
        options.requireInteraction = true;
        options.actions = [{ action: 'done', title: '✓ Erledigt' }];
      } else {
        options.body = 'Tippen, um die Liste zu öffnen und abzuhaken';
      }
      return registration.showNotification(todo.text, options);
    },

    async closeNotification(registration, id) {
      const list = await registration.getNotifications({ tag: tag(id) });
      list.forEach((n) => n.close());
    },

    /* Sagt allen offenen Fenstern/dem Service Worker, dass sich etwas geändert hat. */
    announce() {
      try {
        const ch = new BroadcastChannel('abhaken');
        ch.postMessage({ type: 'changed' });
        ch.close();
      } catch (e) { /* BroadcastChannel fehlt: Seite lädt beim Öffnen neu */ }
    },
  };

  root.Abhaken = api;
})(self);
