# Abhaken

Todos, die als Benachrichtigung auf dem Sperrbildschirm stehen. Auf Android lassen sie sich dort mit „✓ Erledigt“ abhaken; auf dem iPhone öffnet ein Tipp die Liste zum Abhaken (Apple erlaubt Web-Apps keine Schaltflächen in Benachrichtigungen).

## Einrichten (Android)

1. Alle Dateien dieses Ordners unter einer **https-Adresse** ablegen (z. B. GitHub Pages, Netlify, Cloudflare Pages oder eigener Webspace). Als lokale Datei geöffnet funktioniert der Sperrbildschirm-Teil nicht.
2. Die Adresse auf dem Handy in **Chrome** öffnen.
3. Auf „Benachrichtigungen erlauben“ tippen.
4. Optional: Chrome-Menü → „Zum Startbildschirm hinzufügen“.
5. In den Android-Einstellungen unter „Benachrichtigungen“ für den Sperrbildschirm „Alle Inhalte anzeigen“ wählen.

## Einrichten (iPhone, ab iOS 16.4)

1. Die Adresse in **Safari** öffnen.
2. „Teilen“ → „Zum Home-Bildschirm“.
3. Abhaken über das neue Symbol starten und auf „Benachrichtigungen erlauben“ tippen.

## Dateien

- `index.html` – die App
- `sw.js` – Service Worker, hakt Todos aus der Benachrichtigung ab
- `shared.js` – Speicher und Benachrichtigungen (von beiden genutzt)
- `manifest.webmanifest`, `icon-192.png`, `icon-512.png`, `badge.png` – Installation und Symbole

Die Todos liegen nur im Browser dieses Handys (IndexedDB). Es gibt keinen Server und kein Konto.
