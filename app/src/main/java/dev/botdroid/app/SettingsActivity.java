package dev.botdroid.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class SettingsActivity extends Activity {

    private Switch robloxSwitch;
    private LinearLayout customList;
    private TextView customCount;
    private EditText customNameInput;
    private EditText customResponseInput;
    private TextView customBuilderStatus;
    private TextView refreshButton;
    private Spinner presenceType;
    private EditText presenceText;
    private Spinner presenceStatus;
    private TextView presencePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Store.init(this);

        robloxSwitch = findViewById(R.id.roblox_switch);
        customList = findViewById(R.id.custom_list);
        customCount = findViewById(R.id.custom_count);
        customNameInput = findViewById(R.id.custom_name_input);
        customResponseInput = findViewById(R.id.custom_response_input);
        customBuilderStatus = findViewById(R.id.custom_builder_status);
        refreshButton = findViewById(R.id.refresh_button);
        presenceType = findViewById(R.id.presence_type);
        presenceText = findViewById(R.id.presence_text);
        presenceStatus = findViewById(R.id.presence_status);
        presencePreview = findViewById(R.id.presence_preview);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.presence_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presenceType.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.presence_statuses, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presenceStatus.setAdapter(statusAdapter);

        robloxSwitch.setChecked(BotState.robloxEnabled);
        robloxSwitch.setOnCheckedChangeListener((button, checked) -> {
            BotState.robloxEnabled = checked;
            Prefs.robloxEnabled(this, checked);
            LogBus.log("Roblox commands " + (checked ? "enabled" : "disabled"));
        });

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

        findViewById(R.id.save_presence).setOnClickListener(view -> savePresence());

        findViewById(R.id.back_button).setOnClickListener(view -> finish());

        loadPresenceIntoUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        robloxSwitch.setChecked(BotState.robloxEnabled);
        loadPresenceIntoUi();
        refreshUi();
    }

    private void loadPresenceIntoUi() {
        presenceType.setSelection(indexOfType(BotPresence.type));
        presenceStatus.setSelection(indexOfStatus(BotPresence.status));
        presenceText.setText(BotPresence.text);
        presencePreview.setText(BotPresence.describe());
    }

    private void savePresence() {
        int position = presenceType.getSelectedItemPosition();
        int statusPosition = presenceStatus.getSelectedItemPosition();
        int type = BotPresence.TYPE_VALUES[Math.max(0, Math.min(position, BotPresence.TYPE_VALUES.length - 1))];
        String status = BotPresence.STATUS_VALUES[Math.max(0,
                Math.min(statusPosition, BotPresence.STATUS_VALUES.length - 1))];
        BotPresence.set(type, presenceText.getText().toString(), status);
        Prefs.savePresence(this, BotPresence.type, BotPresence.text, BotPresence.status);
        BotService.applyPresence(this);
        presencePreview.setText(BotPresence.describe());
        LogBus.log("Bot status saved: " + BotPresence.describe());
        toast(BotService.isRunning() ? "Status updated." : "Saved. The bot will use it when started.");
    }

    private static int indexOfType(int value) {
        for (int i = 0; i < BotPresence.TYPE_VALUES.length; i++) {
            if (BotPresence.TYPE_VALUES[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private static int indexOfStatus(String value) {
        for (int i = 0; i < BotPresence.STATUS_VALUES.length; i++) {
            if (BotPresence.STATUS_VALUES[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private void refreshUi() {
        customCount.setText(getString(R.string.custom_count, CustomCommands.count()));
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
