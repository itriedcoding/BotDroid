package dev.botdroid.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/** Receives the result of an update install session from Android. */
public class UpdateReceiver extends BroadcastReceiver {

    static final String ACTION_RESULT = "dev.botdroid.app.UPDATE_RESULT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_RESULT.equals(intent.getAction())) {
            return;
        }
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // Android wants the user to confirm; the installer intent comes with it.
            Intent confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmation != null) {
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirmation);
                } catch (Exception e) {
                    LogBus.log("Could not open the installer: " + e.getMessage());
                }
            } else {
                LogBus.log("Installer confirmation was missing; open the APK from Files instead");
            }
            return;
        }
        if (status == PackageInstaller.STATUS_SUCCESS) {
            LogBus.log("Update installed. Restart BotDroid to run the new version.");
            return;
        }
        LogBus.log("Update not installed: "
                + (message == null || message.isEmpty() ? "status " + status : message));
    }
}
