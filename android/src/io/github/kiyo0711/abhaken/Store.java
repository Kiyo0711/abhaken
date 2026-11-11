package io.github.kiyo0711.abhaken;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Speichert die Todos auf dem Gerät. Jede Änderung gleicht die Benachrichtigung an. */
final class Store {
    private static final String PREFS = "abhaken";
    private static final String KEY = "todos";

    /** Die geöffnete App trägt sich hier ein, um Änderungen sofort anzuzeigen. */
    static Runnable listener;

    private Store() {}

    static synchronized List<Todo> all(Context c) {
        List<Todo> out = new ArrayList<>();
        String raw = prefs(c).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Todo t = new Todo();
                t.id = o.getString("id");
                t.text = o.getString("text");
                t.created = o.getLong("created");
                t.done = o.optLong("done", 0);
                out.add(t);
            }
        } catch (JSONException e) {
            // Unlesbare Daten: mit leerer Liste weiterarbeiten.
        }
        return out;
    }

    /** Offene Todos, neueste zuerst. */
    static List<Todo> open(Context c) {
        List<Todo> out = new ArrayList<>();
        for (Todo t : all(c)) if (t.done == 0) out.add(t);
        Collections.sort(out, (a, b) -> Long.compare(b.created, a.created));
        return out;
    }

    /** Erledigte Todos, zuletzt abgehakte zuerst. */
    static List<Todo> done(Context c) {
        List<Todo> out = new ArrayList<>();
        for (Todo t : all(c)) if (t.done != 0) out.add(t);
        Collections.sort(out, (a, b) -> Long.compare(b.done, a.done));
        return out;
    }

    static void add(Context c, String text) {
        List<Todo> list = all(c);
        Todo t = new Todo();
        t.id = UUID.randomUUID().toString();
        t.text = text;
        t.created = System.currentTimeMillis();
        list.add(t);
        save(c, list);
    }

    static void setDone(Context c, String id, boolean isDone) {
        List<Todo> list = all(c);
        for (Todo t : list) {
            if (t.id.equals(id)) t.done = isDone ? System.currentTimeMillis() : 0;
        }
        save(c, list);
    }

    static void delete(Context c, String id) {
        List<Todo> list = all(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(id)) list.remove(i);
        }
        save(c, list);
    }

    static void clearDone(Context c) {
        List<Todo> list = all(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).done != 0) list.remove(i);
        }
        save(c, list);
    }

    private static synchronized void save(Context c, List<Todo> list) {
        JSONArray a = new JSONArray();
        try {
            for (Todo t : list) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("text", t.text);
                o.put("created", t.created);
                o.put("done", t.done);
                a.put(o);
            }
        } catch (JSONException e) {
            return;
        }
        prefs(c).edit().putString(KEY, a.toString()).commit();
        Notifier.refresh(c);
        Runnable l = listener;
        if (l != null) l.run();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
