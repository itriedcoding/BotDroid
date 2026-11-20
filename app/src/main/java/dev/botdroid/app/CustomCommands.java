package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User-created slash commands. Kept in memory and persisted through a {@link Saver}
 * so the logic stays free of Android classes (and testable on the JVM).
 */
final class CustomCommands {

    interface Saver {
        void save(String json);
    }

    static final int MAX = 40;
    static final int MAX_RESPONSE = 1500;

    private static final LinkedHashMap<String, String> COMMANDS = new LinkedHashMap<>();

    private static volatile Saver saver;
    private static volatile Runnable onChange;

    private CustomCommands() {
    }

    static void setSaver(Saver value) {
        saver = value;
    }

    static void setOnChange(Runnable value) {
        onChange = value;
    }

    static synchronized void load(String json) {
        COMMANDS.clear();
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) {
                    continue;
                }
                String name = entry.optString("name", "");
                String response = entry.optString("response", "");
                if (isValidName(name) && !response.isEmpty() && COMMANDS.size() < MAX) {
                    COMMANDS.put(name, response);
                }
            }
        } catch (Exception ignored) {
            // a corrupt store should never stop the bot
        }
    }

    static synchronized String json() {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, String> entry : COMMANDS.entrySet()) {
            try {
                JSONObject object = new JSONObject();
                object.put("name", entry.getKey());
                object.put("response", entry.getValue());
                array.put(object);
            } catch (Exception ignored) {
                // skip malformed rows
            }
        }
        return array.toString();
    }

    static synchronized JSONArray definitions() {
        JSONArray array = new JSONArray();
        for (String name : COMMANDS.keySet()) {
            try {
                JSONObject object = new JSONObject();
                object.put("name", name);
                object.put("description", "Custom command");
                array.put(object);
            } catch (Exception ignored) {
                // skip malformed rows
            }
        }
        return array;
    }

    static synchronized String response(String name) {
        return COMMANDS.get(name);
    }

    static synchronized boolean contains(String name) {
        return COMMANDS.containsKey(name);
    }

    static synchronized List<String> names() {
        return new ArrayList<>(COMMANDS.keySet());
    }

    static synchronized int count() {
        return COMMANDS.size();
    }

    /** Returns an error message, or null when the command was stored. */
    static synchronized String add(String name, String response) {
        if (!isValidName(name)) {
            return "Names must be 1-32 characters using lowercase letters, numbers, `-` or `_`.";
        }
        if (Commands.isPreset(name)) {
            return "`/" + name + "` is already a built-in command.";
        }
        if (response == null || response.trim().isEmpty()) {
            return "Give the command something to reply with.";
        }
        if (response.length() > MAX_RESPONSE) {
            return "Responses are limited to " + MAX_RESPONSE + " characters.";
        }
        boolean existing = COMMANDS.containsKey(name);
        if (!existing && COMMANDS.size() >= MAX) {
            return "You already have the maximum of " + MAX + " custom commands.";
        }
        COMMANDS.put(name, response);
        persist();
        return null;
    }

    static synchronized boolean remove(String name) {
        if (COMMANDS.remove(name) == null) {
            return false;
        }
        persist();
        return true;
    }

    static void clear() {
        synchronized (CustomCommands.class) {
            COMMANDS.clear();
        }
        persist();
    }

    private static void persist() {
        Saver currentSaver = saver;
        if (currentSaver != null) {
            currentSaver.save(json());
        }
        Runnable listener = onChange;
        if (listener != null) {
            listener.run();
        }
    }

    static boolean isValidName(String name) {
        return name != null && name.matches("[a-z0-9_-]{1,32}");
    }
}
