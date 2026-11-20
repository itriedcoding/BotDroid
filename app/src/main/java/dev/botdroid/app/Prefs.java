package dev.botdroid.app;

import android.content.Context;

/** Local, app-private storage for the token, settings and custom commands. */
final class Prefs {

    private static final String FILE = "botdroid";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROBLOX = "roblox_enabled";
    private static final String KEY_CUSTOM = "custom_commands";
    private static final String KEY_PRESENCE_TYPE = "presence_type";
    private static final String KEY_PRESENCE_TEXT = "presence_text";
    private static final String KEY_PRESENCE_STATUS = "presence_status";

    private Prefs() {
    }

    static String token(Context context) {
        String value = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
        return value == null ? "" : value;
    }

    static void token(Context context, String value) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, value)
                .apply();
    }

    static boolean robloxEnabled(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ROBLOX, true);
    }

    static void robloxEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ROBLOX, enabled)
                .apply();
    }

    static void savePresence(Context context, int type, String text, String status) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_PRESENCE_TYPE, type)
                .putString(KEY_PRESENCE_TEXT, text)
                .putString(KEY_PRESENCE_STATUS, status)
                .apply();
    }

    static void loadPresence(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        BotPresence.set(
                prefs.getInt(KEY_PRESENCE_TYPE, BotPresence.PLAYING),
                prefs.getString(KEY_PRESENCE_TEXT, ""),
                prefs.getString(KEY_PRESENCE_STATUS, "online"));
    }

    static String customCommands(Context context) {
        String value = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_CUSTOM, "[]");
        return value == null ? "[]" : value;
    }

    static void customCommands(Context context, String json) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM, json)
                .apply();
    }
}
