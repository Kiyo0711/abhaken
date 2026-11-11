package io.github.kiyo0711.abhaken;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

/** Zeigt die offenen Todos als eine Benachrichtigung, in der jede Zeile antippbar ist. */
final class Notifier {
    private static final String CHANNEL = "offene-todos";
    private static final int ID = 1;
    /** Mehr Zeilen passen nicht in eine aufgeklappte Benachrichtigung. */
    private static final int MAX_ROWS = 6;

    private Notifier() {}

    static void refresh(Context context) {
        Context c = context.getApplicationContext();
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;

        List<Todo> open = Store.open(c);
        if (open.isEmpty()) {
            nm.cancel(ID);
            return;
        }

        NotificationChannel channel = new NotificationChannel(CHANNEL, "Offene Todos", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Die Liste zum Abhaken auf dem Sperrbildschirm");
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);

        RemoteViews big = new RemoteViews(c.getPackageName(), R.layout.notification_list);
        StringBuilder summary = new StringBuilder();
        int shown = Math.min(open.size(), MAX_ROWS);
        for (int i = 0; i < open.size(); i++) {
            Todo t = open.get(i);
            if (summary.length() > 0) summary.append(", ");
            summary.append(t.text);
            if (i >= shown) continue;

            RemoteViews row = new RemoteViews(c.getPackageName(), R.layout.notification_row);
            row.setTextViewText(R.id.text, t.text);
            // Broadcast statt Fenster: so kann Android den Tipp bei gesperrtem Handy ausführen.
            Intent check = new Intent(c, TodoReceiver.class)
                    .setAction(TodoReceiver.ACTION_CHECK)
                    .setData(Uri.parse("abhaken://todo/" + t.id));
            row.setOnClickPendingIntent(R.id.row, PendingIntent.getBroadcast(
                    c, 0, check, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            big.addView(R.id.rows, row);
        }
        if (open.size() > shown) {
            big.setTextViewText(R.id.more, "und " + (open.size() - shown) + " weitere in der App");
            big.setViewVisibility(R.id.more, View.VISIBLE);
        }

        PendingIntent openApp = PendingIntent.getActivity(
                c, 0, new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent dismissed = PendingIntent.getBroadcast(
                c, 0, new Intent(c, TodoReceiver.class).setAction(TodoReceiver.ACTION_DISMISSED),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_check)
                .setContentTitle(open.size() == 1 ? "1 Todo offen" : open.size() + " Todos offen")
                .setContentText(summary.toString())
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setCustomBigContentView(big)
                .setContentIntent(openApp)
                .setDeleteIntent(dismissed)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        nm.notify(ID, n);
    }
}
