package dev.botdroid.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends Activity {

    private Switch robloxSwitch;
    private Switch autostartSwitch;
    private LinearLayout customList;
    private TextView customCount;
    private EditText customNameInput;
    private EditText customResponseInput;
    private TextView customBuilderStatus;
    private TextView refreshButton;
    private TextView updateVersion;
    private TextView updateStatus;
    private TextView updateCheckButton;
    private TextView updateInstallButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Store.init(this);

        robloxSwitch = findViewById(R.id.roblox_switch);
        autostartSwitch = findViewById(R.id.autostart_switch);
        customList = findViewById(R.id.custom_list);
        customCount = findViewById(R.id.custom_count);
        customNameInput = findViewById(R.id.custom_name_input);
        customResponseInput = findViewById(R.id.custom_response_input);
        customBuilderStatus = findViewById(R.id.custom_builder_status);
        refreshButton = findViewById(R.id.refresh_button);
        updateVersion = findViewById(R.id.update_version);
        updateStatus = findViewById(R.id.update_status);
        updateCheckButton = findViewById(R.id.update_check_button);
        updateInstallButton = findViewById(R.id.update_install_button);

        updateVersion.setText(getString(R.string.update_current, BotState.VERSION, BotState.code()));
        updateStatus.setText(Prefs.lastUpdateTag(this).isEmpty()
                ? "Last check failed or has not run yet."
                : "Last check: " + Prefs.lastUpdateTag(this) + " was the newest release.");
        updateCheckButton.setOnClickListener(view -> checkForUpdates());
        updateInstallButton.setOnClickListener(view -> downloadUpdate());
        renderUpdate();

        robloxSwitch.setChecked(BotState.robloxEnabled);
        robloxSwitch.setOnCheckedChangeListener((button, checked) -> {
            BotState.robloxEnabled = checked;
            Prefs.robloxEnabled(this, checked);
            LogBus.log("Roblox commands " + (checked ? "enabled" : "disabled"));
        });

        autostartSwitch.setChecked(Prefs.autostart(this));
        autostartSwitch.setOnCheckedChangeListener((button, checked) -> {
            Prefs.autostart(this, checked);
            LogBus.log("Start on boot " + (checked ? "enabled" : "disabled"));
        });

        findViewById(R.id.commands_button).setOnClickListener(view ->
                startActivity(new Intent(this, CommandsActivity.class)));
        findViewById(R.id.presence_button).setOnClickListener(view ->
                startActivity(new Intent(this, PresenceActivity.class)));
        findViewById(R.id.builder_button).setOnClickListener(view ->
                startActivity(new Intent(this, BuilderActivity.class)));

        findViewById(R.id.add_custom_button).setOnClickListener(view -> addCustomCommand());

        findViewById(R.id.clear_custom_button).setOnClickListener(view -> {
            if (CustomCommands.count() == 0) {
                toast("There are no custom commands to clear.");
                return;
            }
            CustomCommands.clear();
            refreshUi();
            LogBus.log("Cleared all custom commands");
            toast("Custom commands cleared.");
        });

        refreshButton.setOnClickListener(view -> {
            if (!BotService.isRunning()) {
                toast("Start the bot first.");
                return;
            }
            BotService.refreshCommands(this);
            toast("Re-registering commands in Discord…");
        });

        findViewById(R.id.forget_button).setOnClickListener(view -> {
            Prefs.token(this, "");
            BotService.stop(this);
            LogBus.log("Bot token removed from this device");
            toast("Token removed and bot stopped.");
        });

        findViewById(R.id.github_link).setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/itriedcoding/BotDroid")));
            } catch (Exception e) {
                toast("No app can open that link");
            }
        });

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        robloxSwitch.setChecked(BotState.robloxEnabled);
        autostartSwitch.setChecked(Prefs.autostart(this));
        refreshUi();
    }

    private void refreshUi() {
        customCount.setText(getString(R.string.custom_count, CustomCommands.count(), CustomCommands.limit()));
        refreshCustomList();
        refreshButton.setEnabled(BotService.isRunning());
    }

    private void refreshCustomList() {
        customList.removeAllViews();
        List<String> names = CustomCommands.names();
        if (names.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.custom_empty);
            empty.setTextColor(getColor(R.color.muted));
            empty.setTextSize(12);
            customList.addView(empty);
            return;
        }
        for (String name : names) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            TextView label = new TextView(this);
            label.setText("/" + name);
            label.setTextColor(getColor(R.color.log));
            label.setTextSize(13);
            label.setTypeface(Typeface.MONOSPACE);
            label.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView remove = new TextView(this);
            remove.setText(R.string.custom_remove);
            remove.setTextColor(getColor(R.color.accent));
            remove.setTextSize(12);
            remove.setPadding(dp(10), dp(6), dp(2), dp(6));
            remove.setOnClickListener(view -> {
                CustomCommands.remove(name);
                LogBus.log("Custom command removed from the app: /" + name);
                refreshUi();
            });

            row.addView(label);
            row.addView(remove);
            customList.addView(row);
        }
    }

    private void renderUpdate() {
        Updater.Release release = Updater.latest;
        boolean newer = release != null && release.isNewer();
        updateInstallButton.setEnabled(newer);
        updateInstallButton.setAlpha(newer ? 1f : 0.45f);
        updateInstallButton.setText(newer
                ? getString(R.string.update_install) + " " + release.tag
                : getString(R.string.update_install));
    }

    private void checkForUpdates() {
        updateCheckButton.setEnabled(false);
        updateStatus.setText("Checking github.com…");
        new Thread(() -> {
            Updater.Release found = null;
            String failure = null;
            try {
                found = Updater.check();
            } catch (Exception e) {
                failure = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            final Updater.Release release = found;
            final String error = failure;
            runOnUiThread(() -> {
                updateCheckButton.setEnabled(true);
                if (error != null) {
                    updateStatus.setText("Could not check for updates: " + error);
                    return;
                }
                Prefs.updateChecked(this, release.tag);
                updateStatus.setText(release.isNewer()
                        ? "BotDroid " + release.tag + " is available."
                        : "BotDroid " + release.tag + " is the newest release — this app is up to date.");
                renderUpdate();
            });
        }, "botdroid-update-check").start();
    }

    private void downloadUpdate() {
        final Updater.Release release = Updater.latest;
        if (release == null || !release.isNewer()) {
            return;
        }
        updateInstallButton.setEnabled(false);
        new Thread(() -> {
            String result = "Android's installer will ask you to confirm the update.";
            try {
                File apk = Updater.download(this, release,
                        message -> runOnUiThread(() -> updateStatus.setText(message)));
                Updater.install(this, apk,
                        message -> runOnUiThread(() -> updateStatus.setText(message)));
            } catch (Exception e) {
                result = "Update failed: "
                        + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
            final String message = result;
            runOnUiThread(() -> {
                updateStatus.setText(message);
                renderUpdate();
            });
        }, "botdroid-update-download").start();
    }

    private void addCustomCommand() {
        String name = customNameInput.getText().toString().toLowerCase(Locale.US).trim().replace(' ', '-');
        String response = customResponseInput.getText().toString().trim();
        String error = CustomCommands.add(name, response);
        if (error != null) {
            customBuilderStatus.setText(error);
            return;
        }
        customNameInput.setText("");
        customResponseInput.setText("");
        customBuilderStatus.setText(BotService.isRunning()
                ? "Added /" + name + " — registering it in Discord now."
                : "Added /" + name + ". It is registered when the bot starts.");
        LogBus.log("Custom command added from the app: /" + name);
        refreshUi();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
