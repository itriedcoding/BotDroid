package dev.botdroid.app;

/** Process-wide bot state shared by commands, the service and the UI. */
final class BotState {

    static final String VERSION = "1.5";

    static volatile long startedAt = 0L;
    static volatile String applicationId = "";
    static volatile int guildCount = 0;
    static volatile boolean robloxEnabled = true;

    private BotState() {
    }
}
