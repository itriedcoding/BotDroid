package dev.botdroid.app;

import android.content.Context;

/** Local, app-private storage for the bot token. */
final class Prefs {

    private static final String FILE = "botdroid";
    private static final String KEY_TOKEN = "token";

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
}
