package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which built-in commands are enabled. Disabled commands are neither
 * registered in Discord nor dispatched, which frees slots under Discord's
 * 100 command limit for custom commands.
 */
final class CommandToggles {

    interface Saver {
        void save(String json);
    }

    /**
     * Niche commands that start switched off so a fresh install still has room
     * for custom commands under Discord's 100 command limit.
     */
    static final List<String> DEFAULT_OFF = java.util.Arrays.asList(
            // Niche built-ins from the first release.
            "announce", "dm", "webhooks", "invites", "stickers", "events", "threads",
            "emojiinfo", "serverbanner", "memberroles",
            // The later batch: pick them in Settings → Commands when you want them.
            "serverstats", "channels", "channelperms", "auditlog", "thread", "warn", "warnings",
            "topinvites", "mock", "rot13", "caesar", "sortlines", "dedupe", "hex", "charinfo",
            "repeat", "embed", "weekday", "percentage");

    private static final Set<String> DISABLED = new HashSet<>();

    private static volatile Saver saver;
    private static volatile Runnable onChange;

    private CommandToggles() {
    }

    static void setSaver(Saver value) {
        saver = value;
    }

    static void setOnChange(Runnable value) {
        onChange = value;
    }

    static synchronized void load(String json) {
        DISABLED.clear();
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String name = array.optString(i, "");
                if (!name.isEmpty()) {
                    DISABLED.add(name);
                }
            }
        } catch (Exception ignored) {
            // a corrupt list should never stop the bot
        }
    }

    static synchronized String json() {
        JSONArray array = new JSONArray();
        for (String name : DISABLED) {
            array.put(name);
        }
        return array.toString();
    }

    static synchronized void loadDefaults() {
        DISABLED.clear();
        DISABLED.addAll(DEFAULT_OFF);
    }

    static synchronized boolean isEnabled(String name) {
        return !DISABLED.contains(name);
    }

    static synchronized boolean setEnabled(String name, boolean enabled) {
        boolean changed = enabled ? DISABLED.remove(name) : DISABLED.add(name);
        if (changed) {
            persist();
        }
        return changed;
    }

    static synchronized void setAllEnabled(List<String> names, boolean enabled) {
        for (String name : names) {
            if (enabled) {
                DISABLED.remove(name);
            } else {
                DISABLED.add(name);
            }
        }
        persist();
    }

    static synchronized int disabledCount() {
        return DISABLED.size();
    }

    static synchronized int enabledCount() {
        int total = Commands.presetCount();
        int disabled = 0;
        for (String name : DISABLED) {
            if (Commands.isPreset(name)) {
                disabled++;
            }
        }
        return Math.max(0, total - disabled);
    }

    static synchronized List<String> disabledNames() {
        return new ArrayList<>(DISABLED);
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
}
