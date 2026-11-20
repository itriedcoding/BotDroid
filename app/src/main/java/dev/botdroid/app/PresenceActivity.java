package dev.botdroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Presence page. Discord lets bots set only an activity's name, state, type and
 * url, so those are the fields offered.
 */
public class PresenceActivity extends Activity {

    private Spinner presenceType;
    private Spinner presenceStatus;
    private EditText presenceText;
    private EditText presenceState;
    private EditText presenceUrl;
    private TextView presencePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);
        Store.init(this);

        presenceType = findViewById(R.id.presence_type);
        presenceStatus = findViewById(R.id.presence_status);
        presenceText = findViewById(R.id.presence_text);
        presenceState = findViewById(R.id.presence_state);
        presenceUrl = findViewById(R.id.presence_url);
        presencePreview = findViewById(R.id.presence_preview);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.presence_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presenceType.setAdapter(typeAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.presence_statuses, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presenceStatus.setAdapter(statusAdapter);

        findViewById(R.id.back_button).setOnClickListener(view -> finish());
        findViewById(R.id.save_presence).setOnClickListener(view -> savePresence());

        presenceText.setText(BotPresence.text);
        presenceState.setText(BotPresence.state);
        presenceUrl.setText(BotPresence.url);
        presenceType.setSelection(indexOfType(BotPresence.type));
        presenceStatus.setSelection(indexOfStatus(BotPresence.status));
        presencePreview.setText(BotPresence.describe());
    }

    private void savePresence() {
        int type = BotPresence.TYPE_VALUES[Math.max(0,
                Math.min(presenceType.getSelectedItemPosition(), BotPresence.TYPE_VALUES.length - 1))];
        String status = BotPresence.STATUS_VALUES[Math.max(0,
                Math.min(presenceStatus.getSelectedItemPosition(), BotPresence.STATUS_VALUES.length - 1))];
        String url = presenceUrl.getText().toString().trim();
        if (type == BotPresence.STREAMING
                && !(url.startsWith("https://") || url.startsWith("http://"))) {
            toast("Streaming needs a full http(s) URL, or pick another activity type.");
            return;
        }
        BotPresence.set(type, presenceText.getText().toString(), status,
                presenceState.getText().toString(), url);
        Prefs.savePresence(this, BotPresence.type, BotPresence.text, BotPresence.status,
                BotPresence.state, BotPresence.url);
        BotService.applyPresence(this);
        presencePreview.setText(BotPresence.describe());
        LogBus.log("Presence saved: " + BotPresence.describe());
        toast(BotService.isRunning() ? "Presence updated." : "Saved. It applies when the bot starts.");
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

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
