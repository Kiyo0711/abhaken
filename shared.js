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
  const LIST_TAG = 'abhaken-liste';

  const api = {
    tag,
    allTodos: () => run('todos', 'readonly', (s) => s.getAll()).then((l) => l || []),
    getTodo: (id) => run('todos', 'readonly', (s) => s.get(id)),
    putTodo: (todo) => run('todos', 'readwrite', (s) => s.put(todo)),
    deleteTodo: (id) => run('todos', 'readwrite', (s) => s.delete(id)),
    getSetting: (key) => run('settings', 'readonly', (s) => s.get(key)),
    setSetting: (key, value) => run('settings', 'readwrite', (s) => s.put(value, key)),

    /* Offene Todos in der Reihenfolge der App: neueste zuerst. */
    openTodos: () => api.allTodos().then((l) => l.filter((t) => !t.done).sort((a, b) => b.created - a.created)),

    /* Android/Chrome kann Schaltflächen in Benachrichtigungen, iPhone/Safari nicht. */
    hasActions() {
      return typeof Notification !== 'undefined' && Notification.maxActions > 0;
    },

    /* 'list' = eine Benachrichtigung mit der ganzen Liste, 'single' = eine je Todo. */
    getMode: () => api.getSetting('mode').then((m) => (m === 'single' ? 'single' : 'list')),

    /* Das markierte Todo der Listenansicht; fällt auf das erste offene zurück. */
    async currentTodo(open) {
      const id = await api.getSetting('cursor');
      return open.find((t) => t.id === id) || open[0] || null;
    },

    /* Bringt die Benachrichtigungen auf den Stand der Liste. Von Seite und Service Worker genutzt. */
    async refresh(registration) {
      if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return;
      const open = await api.openTodos();
      const mode = await api.getMode();
      const shown = await registration.getNotifications();
      const base = { icon: 'icon-192.png', badge: 'badge.png', silent: true };

      if (mode === 'list') {
        shown.forEach((n) => { if (n.tag && n.tag.startsWith('todo-')) n.close(); });
        if (!open.length) {
          shown.forEach((n) => { if (n.tag === LIST_TAG) n.close(); });
          return;
        }
        const withActions = api.hasActions();
        const current = await api.currentTodo(open);
        const lines = open.map((t) => '☐ ' + t.text + (withActions && open.length > 1 && t.id === current.id ? '  ◀' : ''));
        const options = Object.assign({}, base, {
          tag: LIST_TAG,
          body: lines.join('\n'),
          data: { list: true },
        });
        if (withActions) {
          options.requireInteraction = true;
          options.actions = [{ action: 'done', title: '✓ Abhaken' }];
          if (open.length > 1) options.actions.push({ action: 'next', title: '↓ Nächstes' });
        }
        await registration.showNotification(open.length === 1 ? '1 Todo offen' : open.length + ' Todos offen', options);
        return;
      }

      // Einzeln: fehlende zeigen, überzählige schließen.
      const wanted = new Map(open.map((t) => [tag(t.id), t]));
      const have = new Set();
      shown.forEach((n) => {
        if (n.tag === LIST_TAG) n.close();
        else if (n.tag && n.tag.startsWith('todo-')) {
          if (wanted.has(n.tag)) have.add(n.tag); else n.close();
        }
      });
      for (const [t, todo] of wanted) {
        if (have.has(t)) continue;
        const options = Object.assign({}, base, { tag: t, timestamp: todo.created, data: { id: todo.id } });
        if (api.hasActions()) {
          options.requireInteraction = true;
          options.actions = [{ action: 'done', title: '✓ Erledigt' }];
        } else {
          options.body = 'Tippen, um die Liste zu öffnen und abzuhaken';
        }
        await registration.showNotification(todo.text, options);
      }
    },

    /* Listenansicht: markiertes Todo abhaken, Markierung rückt auf das nächste. */
    async checkCurrent() {
      const open = await api.openTodos();
      const current = await api.currentTodo(open);
      if (!current) return;
      const i = open.indexOf(current);
      const rest = open.filter((t) => t !== current);
      current.done = Date.now();
      await api.putTodo(current);
      const next = rest[Math.min(i, rest.length - 1)];
      await api.setSetting('cursor', next ? next.id : null);
    },

    /* Listenansicht: Markierung eins weiter, am Ende wieder nach oben. */
    async moveCursor() {
      const open = await api.openTodos();
      const current = await api.currentTodo(open);
      if (!current) return;
      const next = open[(open.indexOf(current) + 1) % open.length];
      await api.setSetting('cursor', next.id);
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
