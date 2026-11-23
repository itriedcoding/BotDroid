package dev.botdroid.app;

/** Process-wide bot state shared by commands, the service and the UI. */
final class BotState {

    static final String VERSION = "1.9";

    static volatile long startedAt = 0L;
    static volatile String applicationId = "";
    static volatile int guildCount = 0;
    static volatile boolean robloxEnabled = true;

    private BotState() {
    }

    /** Build number used to compare against release tags such as v1.7. */
    static int code() {
        return versionCode(VERSION);
    }

    /**
     * "v1.7" becomes 10700 and "v1.10.2" becomes 11002, so tags can be compared
     * numerically instead of by string. Returns 0 for anything that is not a
     * version number.
     */
    static int versionCode(String version) {
        if (version == null) {
            return 0;
        }
        String text = version.trim();
        if (text.startsWith("v") || text.startsWith("V")) {
            text = text.substring(1);
        }
        String[] parts = text.split("[.+-]");
        int major = number(parts, 0);
        int minor = number(parts, 1);
        int patch = number(parts, 2);
        if (major < 0) {
            return 0;
        }
        return major * 10000 + Math.max(minor, 0) * 100 + Math.max(patch, 0);
    }

    private static int number(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            int value = Integer.parseInt(parts[index]);
            return value < 0 || value > 99 ? -1 : value;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
