package dev.botdroid.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the bot again after a reboot when a token is saved and autostart is on. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        String token = Prefs.token(context);
        if (token.isEmpty() || !Prefs.autostart(context)) {
            return;
        }
        LogBus.log("Phone restarted — starting the bot again");
        BotService.start(context, token);
    }
}
