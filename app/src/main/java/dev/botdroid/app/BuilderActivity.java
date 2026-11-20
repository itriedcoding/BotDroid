package dev.botdroid.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The bot builder: writes scripts for custom commands, previews them against
 * sample values with the real interpreter, and saves them to the bot.
 */
public class BuilderActivity extends Activity {

    private static final String[][] PALETTE = {
            {"$mention", "$mention"},
            {"$args", "$args"},
            {"$arg[1]", "$arg[1]"},
            {"$username", "$username"},
            {"$membercount", "$membercount"},
            {"$message", "$message[Hello!]"},
            {"$if", "$if[$arg[1] == yes]{Yes!}$else{No.}"},
            {"$upper", "$upper[text]"},
            {"$random", "$random[1;100]"},
            {"$calc", "$calc[2*3]"},
            {"$setUserVar", "$setUserVar[points;$calc[$getUserVar[points]+1]]"},
            {"$onlyPerms", "$onlyPerms[manage_messages]{Staff only.}"}
    };

    private EditText nameInput;
    private EditText scriptInput;
    private EditText sampleInput;
    private TextView status;
    private TextView preview;
    private TextView savedCount;
    private TextView variableCount;
    private LinearLayout savedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_builder);
        Store.init(this);

        nameInput = findViewById(R.id.builder_name);
        scriptInput = findViewById(R.id.builder_script);
        sampleInput = findViewById(R.id.builder_sample);
        status = findViewById(R.id.builder_status);
        preview = findViewById(R.id.builder_preview);
        savedCount = findViewById(R.id.builder_saved_count);
        variableCount = findViewById(R.id.builder_var_count);
        savedList = findViewById(R.id.builder_list);

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
        findViewById(R.id.builder_test).setOnClickListener(view -> testRun());
        findViewById(R.id.builder_save).setOnClickListener(view -> save());
        findViewById(R.id.builder_var_clear).setOnClickListener(view -> {
            Variables.clear();
            refresh();
            toast("Script variables cleared.");
        });

        TextView reference = findViewById(R.id.builder_reference);
        reference.setText(Script.reference().replace("**", "").replace("`", ""));

        buildPalette();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildPalette() {
        LinearLayout palette = findViewById(R.id.builder_palette);
        for (final String[] entry : PALETTE) {
            TextView chip = new TextView(this);
            chip.setText(entry[0]);
            chip.setTextColor(getColor(R.color.text));
            chip.setTextSize(12);
            chip.setTypeface(Typeface.MONOSPACE);
            chip.setBackgroundResource(R.drawable.bg_input);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp(8), 0);
            chip.setLayoutParams(params);
            chip.setOnClickListener(view -> insert(entry[1]));
            palette.addView(chip);
        }
    }

    private void insert(String snippet) {
        int start = Math.max(0, scriptInput.getSelectionStart());
        int end = Math.max(start, scriptInput.getSelectionEnd());
        String text = scriptInput.getText().toString();
        String updated = text.substring(0, start) + snippet + text.substring(end);
        scriptInput.setText(updated);
        scriptInput.setSelection(start + snippet.length());
    }

    private void testRun() {
        String source = scriptInput.getText().toString();
        if (source.trim().isEmpty()) {
            setStatus("Write a script first — even plain text works.");
            return;
        }
        final Map<String, String> vars = new LinkedHashMap<>();
        final List<String> messages = new java.util.ArrayList<>();
        final String sample = sampleInput.getText().toString();
        final String command = nameInput.getText().toString().trim();
        Script.Host host = new Script.Host() {
            @Override
            public String var(String key) {
                String value = vars.get(key);
                return value == null ? "" : value;
            }

            @Override
            public void setVar(String key, String value) {
                vars.put(key, value);
            }

            @Override
            public void message(String text) {
                messages.add(text);
            }

            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public String info(String key) {
                switch (key) {
                    case "username":
                    case "displayname":
                        return "Alex";
                    case "userid":
                        return "123456789012345678";
                    case "mention":
                        return "<@123456789012345678>";
                    case "servername":
                        return "Sample server";
                    case "serverid":
                        return "987654321098765432";
                    case "channelname":
                        return "general";
                    case "channelid":
                        return "111222333444555666";
                    case "commandname":
                        return command.isEmpty() ? "test" : command;
                    case "botname":
                        return "BotDroid";
                    case "version":
                        return BotState.VERSION;
                    case "ping":
                        return "42";
                    case "uptime":
                        return "1h 5m";
                    case "guilds":
                        return "2";
                    case "membercount":
                        return "128";
                    default:
                        return "";
                }
            }

            @Override
            public String arg(int index) {
                String[] parts = split();
                int position = index - 1;
                return position >= 0 && position < parts.length ? parts[position] : "";
            }

            @Override
            public String args() {
                return sample.trim();
            }

            @Override
            public int argCount() {
                return split().length;
            }

            private String[] split() {
                return sample.trim().isEmpty() ? new String[0] : sample.trim().split("\\s+");
            }
        };

        Script.Result result = Script.run(source, host);
        StringBuilder output = new StringBuilder();
        if (!result.output.isEmpty()) {
            output.append(result.output);
        }
        for (String message : messages) {
            output.append(output.length() > 0 ? "\n\n" : "")
                    .append("$message would post: ").append(message);
        }
        if (result.failed()) {
            setStatus("Script error: " + result.error);
            output.append(output.length() > 0 ? "\n\n" : "").append("⚠️ ").append(result.error);
        } else {
            setStatus("Ran cleanly on the phone's own interpreter with sample values.");
        }
        if (output.length() == 0) {
            output.append("(no output)");
        }
        preview.setVisibility(TextView.VISIBLE);
        preview.setText(output.toString());
    }

    private void save() {
        String name = nameInput.getText().toString().toLowerCase(Locale.US).trim().replace(' ', '-');
        String source = scriptInput.getText().toString().trim();
        String error = CustomCommands.add(name, source);
        if (error != null) {
            setStatus(error);
            return;
        }
        String problem = Script.validate(source);
        if (problem != null) {
            setStatus("Saved /" + name + ", but its script has a problem: " + problem);
        } else {
            setStatus("Saved /" + name + (BotService.isRunning()
                    ? " — registering it in Discord now."
                    : ". It registers when the bot starts."));
        }
        LogBus.log("Bot builder saved /" + name);
        refresh();
    }

    private void refresh() {
        int count = CustomCommands.count();
        savedCount.setText(count + " of " + CustomCommands.limit() + " allowed with the built-ins you have on");
        variableCount.setText(Variables.count() + " stored variables (max " + Variables.MAX_VARIABLES + ")");

        savedList.removeAllViews();
        List<String> names = CustomCommands.names();
        if (names.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.builder_empty);
            empty.setTextColor(getColor(R.color.muted));
            empty.setTextSize(12);
            savedList.addView(empty);
            return;
        }
        for (final String name : names) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            TextView label = new TextView(this);
            label.setText("/" + name);
            label.setTextColor(getColor(R.color.log));
            label.setTextSize(13);
            label.setTypeface(Typeface.MONOSPACE);
            label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView edit = new TextView(this);
            edit.setText(R.string.builder_load);
            edit.setTextColor(getColor(R.color.accent));
            edit.setTextSize(12);
            edit.setPadding(dp(10), dp(6), dp(10), dp(6));
            edit.setOnClickListener(view -> {
                nameInput.setText(name);
                String source = CustomCommands.response(name);
                scriptInput.setText(source == null ? "" : source);
                setStatus("Loaded /" + name + " into the editor.");
            });

            TextView remove = new TextView(this);
            remove.setText(R.string.custom_remove);
            remove.setTextColor(getColor(R.color.accent));
            remove.setTextSize(12);
            remove.setPadding(dp(10), dp(6), dp(2), dp(6));
            remove.setOnClickListener(view -> {
                CustomCommands.remove(name);
                refresh();
            });

            row.addView(label);
            row.addView(edit);
            row.addView(remove);
            savedList.addView(row);
        }
    }

    private void setStatus(String message) {
        status.setText(message);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
