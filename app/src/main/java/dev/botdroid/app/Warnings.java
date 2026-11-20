package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Warnings issued with /warn, stored on the phone (the bot has no database
 * server). Entries are keyed by server and member.
 */
final class Warnings {

    interface Saver {
        void save(String json);
    }

    static final int MAX_ENTRIES = 2000;

    static final class Entry {
        final String guildId;
        final String userId;
        final String byId;
        final String reason;
        final long at;

        Entry(String guildId, String userId, String byId, String reason, long at) {
            this.guildId = guildId;
            this.userId = userId;
            this.byId = byId;
            this.reason = reason;
            this.at = at;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private static volatile Saver saver;

    private Warnings() {
    }

    static void setSaver(Saver value) {
        saver = value;
    }

    static synchronized void load(String json) {
        ENTRIES.clear();
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length() && ENTRIES.size() < MAX_ENTRIES; i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) {
                    continue;
                }
                ENTRIES.add(new Entry(
                        entry.optString("g", ""),
                        entry.optString("u", ""),
                        entry.optString("b", ""),
                        entry.optString("r", ""),
                        entry.optLong("t", 0L)));
            }
        } catch (Exception ignored) {
            // a corrupt store should never stop the bot
        }
    }

    static synchronized String json() {
        JSONArray array = new JSONArray();
        for (Entry entry : ENTRIES) {
            try {
                array.put(new JSONObject()
                        .put("g", entry.guildId)
                        .put("u", entry.userId)
                        .put("b", entry.byId)
                        .put("r", entry.reason)
                        .put("t", entry.at));
            } catch (Exception ignored) {
                // skip malformed rows
            }
        }
        return array.toString();
    }

    static synchronized boolean add(String guildId, String userId, String byId, String reason) {
        if (ENTRIES.size() >= MAX_ENTRIES) {
            return false;
        }
        ENTRIES.add(new Entry(guildId, userId, byId, reason, System.currentTimeMillis()));
        persist();
        return true;
    }

    static synchronized List<Entry> forMember(String guildId, String userId) {
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.guildId.equals(guildId) && entry.userId.equals(userId)) {
                matches.add(entry);
            }
        }
        return matches;
    }

    static synchronized int clear(String guildId, String userId) {
        List<Entry> removed = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.guildId.equals(guildId) && entry.userId.equals(userId)) {
                removed.add(entry);
            }
        }
        ENTRIES.removeAll(removed);
        if (!removed.isEmpty()) {
            persist();
        }
        return removed.size();
    }

    static synchronized int count() {
        return ENTRIES.size();
    }

    private static void persist() {
        Saver current = saver;
        if (current != null) {
            current.save(json());
        }
    }
}
