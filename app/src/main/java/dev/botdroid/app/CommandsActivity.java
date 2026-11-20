package dev.botdroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/** Turns individual built-in commands on or off; disabled ones free command slots. */
public class CommandsActivity extends Activity {

    private LinearLayout container;
    private TextView summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commands);
        Store.init(this);

        container = findViewById(R.id.command_container);
        summary = findViewById(R.id.command_summary);

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
        findViewById(R.id.enable_all).setOnClickListener(view -> {
            CommandToggles.setAllEnabled(Commands.presetNames(), true);
            LogBus.log("All built-in commands turned on");
            refresh();
        });
        findViewById(R.id.disable_all).setOnClickListener(view -> {
            CommandToggles.setAllEnabled(Commands.presetNames(), false);
            LogBus.log("All built-in commands turned off");
            refresh();
        });

        refresh();
    }

    private void refresh() {
        container.removeAllViews();
        JSONArray presets = Commands.presetDefinitions();
        for (int i = 0; i < presets.length(); i++) {
            JSONObject entry = presets.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            final String name = entry.optString("name", "");
            if (name.isEmpty()) {
                continue;
            }
            container.addView(buildRow(name, entry.optString("description", "")));
        }
        int enabled = CommandToggles.enabledCount();
        int custom = CustomCommands.count();
        int free = Math.max(0, Commands.MAX_COMMANDS - enabled - custom);
        summary.setText(enabled + " of " + Commands.presetCount() + " built-ins on · " + custom
                + " custom · " + free + " of " + Commands.MAX_COMMANDS + " slots free");
    }

    private View buildRow(final String name, String description) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("/" + name);
        title.setTextColor(getColor(R.color.text));
        title.setTextSize(14);

        TextView subtitle = new TextView(this);
        subtitle.setText(description);
        subtitle.setTextColor(getColor(R.color.muted));
        subtitle.setTextSize(11);

        text.addView(title);
        text.addView(subtitle);

        Switch toggle = new Switch(this);
        toggle.setChecked(CommandToggles.isEnabled(name));
        toggle.setOnCheckedChangeListener((button, checked) -> {
            CommandToggles.setEnabled(name, checked);
            LogBus.log("Command /" + name + (checked ? " turned on" : " turned off"));
            refreshSummaryOnly();
        });

        row.addView(text);
        row.addView(toggle);
        return row;
    }

    private void refreshSummaryOnly() {
        int enabled = CommandToggles.enabledCount();
        int custom = CustomCommands.count();
        int free = Math.max(0, Commands.MAX_COMMANDS - enabled - custom);
        summary.setText(enabled + " of " + Commands.presetCount() + " built-ins on · " + custom
                + " custom · " + free + " of " + Commands.MAX_COMMANDS + " slots free");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
