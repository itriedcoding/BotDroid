package dev.botdroid.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/** Keeps the bot alive while the app is in the background. */
public class BotService extends Service {

    static final String ACTION_START = "dev.botdroid.app.action.START";
    static final String ACTION_STOP = "dev.botdroid.app.action.STOP";
    static final String EXTRA_TOKEN = "dev.botdroid.app.extra.TOKEN";

    private static final String CHANNEL_ID = "botdroid_status";
    private static final int NOTIFICATION_ID = 1001;

    private static volatile boolean running = false;
    private static volatile String currentStatus = "Stopped";

    private static volatile DiscordBot current;

    private DiscordBot bot;
    private PowerManager.WakeLock wakeLock;

    static boolean isRunning() {
        return running;
    }

    static void start(Context context, String token) {
        Intent intent = new Intent(context, BotService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TOKEN, token);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static void stop(Context context) {
        context.startService(new Intent(context, BotService.class).setAction(ACTION_STOP));
    }

    static void applyPresence(Context context) {
        DiscordBot bot = current;
        if (bot != null) {
            bot.updatePresence();
        }
    }

    static void refreshCommands(Context context) {
        DiscordBot bot = current;
        if (bot != null) {
            bot.refreshCommands();
        } else {
            LogBus.log("Start the bot before refreshing commands");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        Store.init(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            LogBus.log("Stopped by user");
            shutdown();
            return START_NOT_STICKY;
        }

        String token = intent == null ? null : intent.getStringExtra(EXTRA_TOKEN);
        if (token == null || token.trim().isEmpty()) {
            token = Prefs.token(this);
        }
        if (token == null || token.trim().isEmpty()) {
            LogBus.log("No bot token saved, nothing to start");
            shutdown();
            return START_NOT_STICKY;
        }

        if (bot != null) {
            bot.shutdown();
            bot = null;
        }

        running = true;
        currentStatus = "Starting…";
        try {
            startForegroundCompat();
        } catch (Exception e) {
            // Android 12+ can refuse a foreground service start from the
            // background; fail with a message instead of crashing the app.
            running = false;
            LogBus.log("Could not start the background service: " + e.getMessage());
            LogBus.setStatus("Stopped");
            stopSelf();
            return START_NOT_STICKY;
        }
        acquireWakeLock();
        LogBus.log("Starting bot");

        bot = new DiscordBot(token, new DiscordBot.Listener() {
            @Override
            public void log(String line) {
                LogBus.log(line);
            }

            @Override
            public void status(String status) {
                currentStatus = status;
                LogBus.setStatus(status);
                updateNotification(status);
            }

            @Override
            public void stopped(String reason) {
                LogBus.log("Bot stopped: " + reason);
                LogBus.setStatus("Stopped — " + reason);
                shutdown();
            }
        });
        current = bot;
        BotState.startedAt = System.currentTimeMillis();
        bot.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (bot != null) {
            bot.shutdown();
            bot = null;
        }
        releaseWakeLock();
        running = false;
        super.onDestroy();
    }

    private void shutdown() {
        running = false;
        current = null;
        if (bot != null) {
            bot.shutdown();
            bot = null;
        }
        releaseWakeLock();
        LogBus.setStatus("Stopped");
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void createChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Bot status",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows whether BotDroid is connected to Discord");
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private void startForegroundCompat() {
        Notification notification = buildNotification(currentStatus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateNotification(String status) {
        if (!running) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(status));
        }
    }

    private Notification buildNotification(String status) {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(this, BotService.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("BotDroid")
                .setContentText(status)
                .setSmallIcon(R.drawable.ic_notify)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.ic_notify), "Stop", stop).build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }
        return builder.build();
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            return;
        }
        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        if (manager == null) {
            return;
        }
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BotDroid::gateway");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        wakeLock = null;
    }
}
