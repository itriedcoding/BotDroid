package dev.botdroid.app;

import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/** Tiny in-memory log shared between the bot service and the activity. */
final class LogBus {

    interface Listener {
        void onLog(String line);

        void onStatus(String status);
    }

    private static final int MAX_LINES = 400;
    private static final List<String> LINES = new ArrayList<>();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private static volatile String status = "Stopped";

    private LogBus() {
    }

    static void addListener(Listener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    static List<String> lines() {
        synchronized (LINES) {
            return new ArrayList<>(LINES);
        }
    }

    static String status() {
        return status;
    }

    static void log(String line) {
        final String stamped = time() + "  " + line;
        synchronized (LINES) {
            LINES.add(stamped);
            while (LINES.size() > MAX_LINES) {
                LINES.remove(0);
            }
        }
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : LISTENERS) {
                    listener.onLog(stamped);
                }
            }
        });
    }

    private static String time() {
        // SimpleDateFormat is not thread safe and log() is called from several
        // bot threads at once.
        synchronized (CLOCK) {
            return CLOCK.format(new Date());
        }
    }

    static void setStatus(final String value) {
        status = value;
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : LISTENERS) {
                    listener.onStatus(value);
                }
            }
        });
    }
}
