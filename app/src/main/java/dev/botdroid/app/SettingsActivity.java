package dev.botdroid.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class SettingsActivity extends Activity {

    private Switch robloxSwitch;
    private TextView customList;
    private TextView customCount;
    private TextView refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        robloxSwitch = findViewById(R.id.roblox_switch);
        customList = findViewById(R.id.custom_list);
        customCount = findViewById(R.id.custom_count);
        refreshButton = findViewById(R.id.refresh_button);

        robloxSwitch.setChecked(BotState.robloxEnabled);
        robloxSwitch.setOnCheckedChangeListener((button, checked) -> {
            BotState.robloxEnabled = checked;
            Prefs.robloxEnabled(this, checked);
            LogBus.log("Roblox commands " + (checked ? "enabled" : "disabled"));
        });

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
        refreshUi();
    }

    private void refreshUi() {
        customCount.setText(getString(R.string.custom_count, CustomCommands.count()));
        List<String> names = CustomCommands.names();
        if (names.isEmpty()) {
            customList.setText(R.string.custom_empty);
        } else {
            StringBuilder builder = new StringBuilder();
            for (String name : names) {
                builder.append('/').append(name).append("   ");
            }
            customList.setText(builder.toString().trim());
        }
        refreshButton.setEnabled(BotService.isRunning());
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
