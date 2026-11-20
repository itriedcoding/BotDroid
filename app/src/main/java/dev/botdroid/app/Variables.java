package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Variables used by scripts: global, per user and per server. Keys look like
 * {@code global::name}, {@code user:<id>:name} and {@code server:<id>:name}.
 * Persisted through a {@link Saver} so the store stays free of Android classes.
 */
final class Variables {

    interface Saver {
        void save(String json);
    }

    static final int MAX_VARIABLES = 500;
    static final int MAX_VALUE = 900;

    private static final LinkedHashMap<String, String> VALUES = new LinkedHashMap<>();

    private static volatile Saver saver;

    private Variables() {
    }

    static void setSaver(Saver value) {
        saver = value;
    }

    static synchronized void load(String json) {
        VALUES.clear();
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length() && VALUES.size() < MAX_VARIABLES; i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) {
                    continue;
                }
                String key = entry.optString("k", "");
                if (!key.isEmpty()) {
                    VALUES.put(key, entry.optString("v", ""));
                }
            }
        } catch (Exception ignored) {
            // a corrupt store should never stop the bot
        }
    }

    static synchronized String json() {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, String> entry : VALUES.entrySet()) {
            try {
                array.put(new JSONObject().put("k", entry.getKey()).put("v", entry.getValue()));
            } catch (Exception ignored) {
                // skip malformed rows
            }
        }
        return array.toString();
    }

    /** Missing variables read as an empty string, like Bot Designer's functions. */
    static synchronized String get(String key) {
        String value = key == null ? null : VALUES.get(key);
        return value == null ? "" : value;
    }

    /** Returns false when the store is full and the key is new. */
    static synchronized boolean set(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        String trimmed = key.length() > 110 ? key.substring(0, 110) : key;
        String stored = value == null ? "" : (value.length() > MAX_VALUE ? value.substring(0, MAX_VALUE) : value);
        if (!VALUES.containsKey(trimmed) && VALUES.size() >= MAX_VARIABLES) {
            return false;
        }
        VALUES.put(trimmed, stored);
        persist();
        return true;
    }

    static synchronized boolean remove(String key) {
        if (VALUES.remove(key) == null) {
            return false;
        }
        persist();
        return true;
    }

    static synchronized void clear() {
        VALUES.clear();
        persist();
    }

    static synchronized int count() {
        return VALUES.size();
    }

    static synchronized List<String> keys() {
        return new ArrayList<>(VALUES.keySet());
    }

    static synchronized String peek(String key) {
        String value = VALUES.get(key);
        return value == null ? "" : value;
    }

    private static void persist() {
        Saver current = saver;
        if (current != null) {
            current.save(json());
        }
    }
}
