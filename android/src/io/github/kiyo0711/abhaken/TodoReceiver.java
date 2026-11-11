package io.github.kiyo0711.abhaken;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Empfängt den Tipp auf eine Zeile der Benachrichtigung, das Wegwischen und den Neustart des Handys. */
public class TodoReceiver extends BroadcastReceiver {
    static final String ACTION_CHECK = "io.github.kiyo0711.abhaken.CHECK";
    static final String ACTION_DISMISSED = "io.github.kiyo0711.abhaken.DISMISSED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri data = intent.getData();
        if (ACTION_CHECK.equals(intent.getAction()) && data != null && data.getLastPathSegment() != null) {
            Store.setDone(context, data.getLastPathSegment(), true);
            return;
        }
        // Weggewischt, Neustart oder App-Update: Liste wieder anzeigen.
        Notifier.refresh(context);
    }
}
