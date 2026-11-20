package dev.botdroid.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LogBus.Listener {

    private static final int REQUEST_NOTIFICATIONS = 10;

    private EditText tokenInput;
    private TextView actionButton;
    private TextView statusView;
    private TextView logView;
    private ScrollView logScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Store.init(this);

        tokenInput = findViewById(R.id.token_input);
        actionButton = findViewById(R.id.action_button);
        statusView = findViewById(R.id.status_view);
        logView = findViewById(R.id.log_view);
        logScroll = findViewById(R.id.log_scroll);

        tokenInput.setText(Prefs.token(this));

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        findViewById(R.id.portal_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://discord.com/developers/applications")));
                } catch (Exception e) {
                    toast("No app can open that link");
                }
            }
        });

        for (String line : LogBus.lines()) {
            appendLine(line);
        }
        statusView.setText(LogBus.status());
        playEntrance();
        Updater.autoCheck(this);
    }

    /** Fades the intro wordmark out and brings the screen in piece by piece. */
    private void playEntrance() {
        View overlay = findViewById(R.id.intro_overlay);
        View introLogo = findViewById(R.id.intro_logo);
        if (overlay == null || introLogo == null) {
            return;
        }
        if (animationsDisabled()) {
            overlay.setVisibility(View.GONE);
            return;
        }
        View[] staged = {
                findViewById(R.id.header),
                findViewById(R.id.token_card),
                actionButton,
                statusView,
                logScroll
        };
        for (View view : staged) {
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationY(dp(22));
            }
        }
        introLogo.setAlpha(0f);
        introLogo.setScaleX(0.82f);
        introLogo.setScaleY(0.82f);
        introLogo.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(460).setInterpolator(new DecelerateInterpolator()).start();
        overlay.animate().alpha(0f).setStartDelay(760).setDuration(420)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        overlay.setVisibility(View.GONE);
                    }
                }).start();
        for (int i = 0; i < staged.length; i++) {
            View view = staged[i];
            if (view == null) {
                continue;
            }
            view.animate().alpha(1f).translationY(0f)
                    .setStartDelay(400 + i * 80L).setDuration(480)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private boolean animationsDisabled() {
        try {
            float scale = android.provider.Settings.Global.getFloat(getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return scale == 0f;
        } catch (Exception e) {
            return false;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogBus.addListener(this);
        refreshState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogBus.removeListener(this);
    }

    @Override
    public void onLog(String line) {
        appendLine(line);
    }

    @Override
    public void onStatus(String status) {
        statusView.setText(status);
        refreshState();
    }

    private void toggle() {
        if (BotService.isRunning()) {
            BotService.stop(this);
            appendLine("Stopping…");
            return;
        }
        String token = tokenInput.getText().toString().trim();
        if (!looksLikeToken(token)) {
            toast("That does not look like a bot token (it needs three dot-separated parts)");
            return;
        }
        hideKeyboard();
        Prefs.token(this, token);
        requestNotificationPermission();
        BotService.start(this, token);
        appendLine("Starting…");
        refreshState();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private static boolean looksLikeToken(String token) {
        String[] parts = token.split("\\.");
        return parts.length == 3 && parts[0].length() >= 15 && parts[1].length() >= 5 && parts[2].length() >= 20;
    }

    private void refreshState() {
        boolean running = BotService.isRunning();
        actionButton.setText(running ? getString(R.string.stop) : getString(R.string.start));
        String status = statusView.getText().toString();
        int color;
        if (running && status.startsWith("Online")) {
            color = getColor(R.color.ok);
        } else if (status.startsWith("Stopped") || status.startsWith("Invalid")) {
            color = getColor(R.color.muted);
        } else {
            color = getColor(R.color.warn);
        }
        statusView.setTextColor(color);
    }

    private void appendLine(String line) {
        logView.append(line);
        logView.append("\n");
        if (logView.length() > 20000) {
            logView.setText(logView.getText().subSequence(logView.length() - 15000, logView.length()));
        }
        logScroll.post(new Runnable() {
            @Override
            public void run() {
                logScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
