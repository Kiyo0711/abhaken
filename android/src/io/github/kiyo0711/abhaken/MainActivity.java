package io.github.kiyo0711.abhaken;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Die Liste in der App: Todos eintragen, abhaken, löschen, zurückholen. */
public class MainActivity extends Activity {
    private static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    private LinearLayout list;
    private TextView count;
    private TextView banner;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list = findViewById(R.id.list);
        count = findViewById(R.id.count);
        banner = findViewById(R.id.banner);
        input = findViewById(R.id.input);

        findViewById(R.id.add).setOnClickListener(v -> addFromInput());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addFromInput();
                return true;
            }
            return false;
        });
        banner.setOnClickListener(v -> openNotificationSettings());

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {POST_NOTIFICATIONS}, 1);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Store.listener = this::render;
        render();
        Notifier.refresh(this);
    }

    @Override
    protected void onStop() {
        Store.listener = null;
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Notifier.refresh(this);
        render();
    }

    private void addFromInput() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        Store.add(this, text);
    }

    private void openNotificationSettings() {
        startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
    }

    private void render() {
        List<Todo> open = Store.open(this);
        List<Todo> done = Store.done(this);

        count.setText(open.isEmpty() && done.isEmpty() ? ""
                : open.size() + " offen" + (done.isEmpty() ? "" : ", " + done.size() + " erledigt"));

        NotificationManager nm = getSystemService(NotificationManager.class);
        banner.setVisibility(nm != null && nm.areNotificationsEnabled() ? View.GONE : View.VISIBLE);

        list.removeAllViews();
        if (open.isEmpty()) {
            list.addView(label("Nichts offen. Trag unten dein erstes Todo ein.", 16, R.color.muted));
        }
        for (Todo t : open) list.addView(openRow(t));

        if (!done.isEmpty()) {
            LinearLayout head = row();
            head.setPadding(0, dp(20), 0, 0);
            TextView title = label("Erledigt", 15, R.color.muted);
            head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            head.addView(action("Erledigte entfernen", () -> Store.clearDone(this)));
            list.addView(head);
            for (Todo t : done) list.addView(doneRow(t));
        }
    }

    private View openRow(Todo t) {
        LinearLayout row = row();
        row.setBackgroundResource(R.drawable.slip);
        row.setPadding(dp(8), dp(6), dp(4), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);

        CheckBox box = new CheckBox(this);
        box.setContentDescription("Erledigt: " + t.text);
        box.setOnClickListener(v -> Store.setDone(this, t.id, true));
        row.addView(box);

        TextView text = label(t.text, 18, R.color.ink);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(action("Löschen", () -> Store.delete(this, t.id)));
        return row;
    }

    private View doneRow(Todo t) {
        LinearLayout row = row();
        TextView text = label(t.text, 16, R.color.muted);
        text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(action("Zurückholen", () -> Store.setDone(this, t.id, false)));
        return row;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView label(String text, int sp, int colorRes) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(getColor(colorRes));
        return v;
    }

    private TextView action(String text, Runnable onClick) {
        TextView v = label(text, 15, R.color.muted);
        v.setPadding(dp(12), dp(12), dp(12), dp(12));
        v.setOnClickListener(x -> onClick.run());
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
