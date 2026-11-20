package dev.botdroid.app;

import android.content.Context;

/** Loads persisted settings into memory and keeps them in sync with prefs. */
final class Store {

    private Store() {
    }

    static void init(Context context) {
        Context app = context.getApplicationContext();
        BotState.robloxEnabled = Prefs.robloxEnabled(app);
        Prefs.loadPresence(app);
        String disabled = Prefs.disabledCommands(app);
        if (disabled.isEmpty()) {
            CommandToggles.loadDefaults();
            Prefs.disabledCommands(app, CommandToggles.json());
        } else {
            CommandToggles.load(disabled);
        }
        CommandToggles.setSaver(json -> Prefs.disabledCommands(app, json));
        CommandToggles.setOnChange(() -> {
            if (BotService.isRunning()) {
                BotService.refreshCommands(app);
            }
        });
        CustomCommands.load(Prefs.customCommands(app));
        CustomCommands.setSaver(json -> Prefs.customCommands(app, json));
        CustomCommands.setOnChange(() -> {
            if (BotService.isRunning()) {
                BotService.refreshCommands(app);
            }
        });
    }
}
