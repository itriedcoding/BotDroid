package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Minimal Discord bot core: gateway connection, heartbeats, slash command
 * registration and interaction dispatch. Runs without any Discord library.
 */
final class DiscordBot {

    interface Listener {
        void log(String line);

        void status(String status);

        void stopped(String reason);
    }

    private static final String GATEWAY_HOST = "gateway.discord.gg";
    private static final String GATEWAY_PATH = "/?v=10&encoding=json";
    // GUILDS is enough: slash commands and guild metadata, no privileged intents.
    private static final int INTENTS = 1;

    private final String token;
    private final Listener listener;
    private final DiscordRest rest;
    private final ExecutorService workers;
    private final ExecutorService registrar;

    private volatile boolean running = true;
    private volatile boolean fatal = false;
    private volatile GatewaySocket socket;
    private volatile Thread heartbeatThread;
    private volatile long heartbeatInterval = 41250L;
    private volatile long lastAck = 0L;
    private volatile int sequence = -1;
    private volatile String applicationId;
    private volatile boolean identified = false;
    private final Set<String> knownGuilds = ConcurrentHashMap.newKeySet();

    DiscordBot(String token, Listener listener) {
        this.token = token;
        this.listener = listener;
        this.rest = new DiscordRest(token);
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "botdroid-worker");
                thread.setDaemon(true);
                return thread;
            }
        };
        this.workers = Executors.newFixedThreadPool(2, factory);
        this.registrar = Executors.newSingleThreadExecutor(factory);
    }

    void start() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "botdroid-gateway");
        thread.setDaemon(true);
        thread.start();
    }

    void shutdown() {
        running = false;
        stopHeartbeat();
        GatewaySocket current = socket;
        if (current != null) {
            current.close();
        }
        workers.shutdownNow();
        registrar.shutdownNow();
    }

    private void log(String line) {
        listener.log(line);
    }

    private void status(String value) {
        listener.status(value);
    }

    private void loop() {
        int attempt = 0;
        while (running && !fatal) {
            GatewaySocket current = null;
            try {
                identified = false;
                status("Connecting…");
                log("Connecting to Discord gateway…");
                current = new GatewaySocket(GATEWAY_HOST, GATEWAY_PATH);
                current.connect();
                socket = current;
                sequence = -1;
                lastAck = 0L;

                String hello = current.readMessage();
                JSONObject helloPayload = new JSONObject(hello);
                JSONObject helloData = helloPayload.optJSONObject("d");
                if (helloData != null) {
                    heartbeatInterval = helloData.optLong("heartbeat_interval", 41250L);
                }

                JSONObject identify = new JSONObject();
                identify.put("op", 2);
                JSONObject data = new JSONObject();
                data.put("token", token);
                data.put("intents", INTENTS);
                data.put("compress", false);
                JSONObject properties = new JSONObject();
                properties.put("os", "android");
                properties.put("browser", "BotDroid");
                properties.put("device", "BotDroid");
                data.put("properties", properties);
                identify.put("d", data);
                current.send(identify.toString());

                startHeartbeat();
                log("Gateway handshake sent");
                attempt = 0;

                while (running) {
                    String message = current.readMessage();
                    if (message == null) {
                        continue;
                    }
                    handle(message);
                }
            } catch (Exception e) {
                int closeCode = e instanceof GatewaySocket.GatewayException
                        ? ((GatewaySocket.GatewayException) e).closeCode
                        : -1;
                if (closeCode == 4004 || closeCode == 4003 || closeCode == 4010 || closeCode == 4011 || closeCode == 4012) {
                    fatal = true;
                    status("Invalid token");
                    log("Discord rejected the token (gateway close " + closeCode + ").");
                    stopSafely(current);
                    listener.stopped("Invalid bot token");
                    return;
                }
                if (closeCode == 4014) {
                    fatal = true;
                    status("Intents rejected");
                    log("Discord rejected the requested gateway intents (4014).");
                    stopSafely(current);
                    listener.stopped("Gateway intents rejected");
                    return;
                }
                if (running) {
                    String message = e.getMessage();
                    log("Gateway closed: " + (message == null ? e.getClass().getSimpleName() : message));
                }
            } finally {
                stopHeartbeat();
                socket = null;
                stopSafely(current);
            }

            if (!running || fatal) {
                break;
            }
            attempt = Math.min(attempt + 1, 6);
            long delay = Math.min(60000L, 1500L << (attempt - 1));
            status("Reconnecting in " + (delay / 1000) + "s…");
            log("Reconnecting in " + (delay / 1000) + "s");
            sleep(delay);
        }
        if (!fatal) {
            log("Gateway loop finished");
        }
    }

    private void stopSafely(GatewaySocket target) {
        if (target != null) {
            target.close();
        }
    }

    private void handle(String raw) {
        try {
            JSONObject payload = new JSONObject(raw);
            int op = payload.optInt("op", -1);
            if (payload.has("s") && !payload.isNull("s")) {
                sequence = payload.optInt("s", sequence);
            }
            switch (op) {
                case 0: {
                    String type = payload.optString("t", "");
                    JSONObject data = payload.optJSONObject("d");
                    if (data == null) {
                        return;
                    }
                    if ("READY".equals(type)) {
                        onReady(data);
                    } else if ("GUILD_CREATE".equals(type)) {
                        String guildId = data.optString("id", null);
                        if (guildId != null) {
                            knownGuilds.add(guildId);
                            BotState.guildCount = knownGuilds.size();
                        }
                        registerCommands(guildId);
                    } else if ("INTERACTION_CREATE".equals(type)) {
                        onInteraction(data);
                    }
                    break;
                }
                case 1:
                    sendHeartbeat();
                    break;
                case 7:
                    log("Discord asked for a reconnect");
                    requestReconnect();
                    break;
                case 9:
                    log("Session invalidated, starting a fresh one");
                    requestReconnect();
                    break;
                case 11:
                    lastAck = System.currentTimeMillis();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log("Could not read gateway payload: " + e.getMessage());
        }
    }

    private void requestReconnect() {
        GatewaySocket current = socket;
        if (current != null) {
            current.close();
        }
    }

    private void onReady(JSONObject data) {
        JSONObject user = data.optJSONObject("user");
        if (user != null) {
            applicationId = user.optString("id", null);
            BotState.applicationId = applicationId == null ? "" : applicationId;
            String name = user.optString("username", "bot");
            log("Logged in as " + name + " (" + applicationId + ")");
            status("Online as " + name);
        }
        JSONArray guilds = data.optJSONArray("guilds");
        List<String> ids = new ArrayList<>();
        if (guilds != null) {
            for (int i = 0; i < guilds.length(); i++) {
                JSONObject guild = guilds.optJSONObject(i);
                if (guild != null) {
                    String id = guild.optString("id", null);
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
        }
        for (String id : ids) {
            knownGuilds.add(id);
        }
        BotState.guildCount = knownGuilds.size();
        log("Connected to " + ids.size() + " server(s)");
        identified = true;
        updatePresence();
        for (final String id : ids) {
            registerCommands(id);
        }
    }

    /** Pushes the configured activity/status to Discord (op 3). */
    void updatePresence() {
        GatewaySocket current = socket;
        if (!identified || current == null) {
            return;
        }
        try {
            current.send(BotPresence.gatewayPayload().toString());
            log("Bot status set: " + BotPresence.describe());
        } catch (Exception e) {
            log("Could not update bot status: " + e.getMessage());
        }
    }

    private void registerCommands(final String guildId) {
        final String appId = applicationId;
        if (guildId == null || appId == null || registrar.isShutdown()) {
            return;
        }
        // Registrations run on their own thread so a bot in many servers does
        // not delay command responses with a backlog of REST calls.
        registrar.submit(new Runnable() {
            @Override
            public void run() {
                Net.Resp response = rest.put("/applications/" + appId + "/guilds/" + guildId + "/commands",
                        Commands.definitions().toString());
                if (response.ok()) {
                    log("Slash commands ready in server " + guildId);
                } else {
                    log("Could not register commands in server " + guildId + " (HTTP " + response.code + "): "
                            + shortBody(response.body));
                }
            }
        });
    }

    /** Re-registers the command set (preset plus custom) in every known server. */
    void refreshCommands() {
        if (registrar.isShutdown()) {
            return;
        }
        registrar.submit(new Runnable() {
            @Override
            public void run() {
                String appId = applicationId;
                if (appId == null) {
                    return;
                }
                String body = Commands.definitions().toString();
                for (String guildId : knownGuilds) {
                    Net.Resp response = rest.put("/applications/" + appId + "/guilds/" + guildId + "/commands", body);
                    if (response.ok()) {
                        log("Commands refreshed in server " + guildId);
                    } else {
                        log("Command refresh failed in server " + guildId + " (HTTP " + response.code + ")");
                    }
                }
            }
        });
    }

    private void onInteraction(final JSONObject interaction) {
        if (workers.isShutdown()) {
            return;
        }
        workers.submit(new Runnable() {
            @Override
            public void run() {
                JSONObject data = interaction.optJSONObject("data");
                String name = data == null ? "" : data.optString("name", "");
                log("Handling /" + name);
                Commands.handle(rest, interaction);
            }
        });
    }

    private void startHeartbeat() {
        stopHeartbeat();
        final long interval = heartbeatInterval;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep((long) (interval * Math.random()));
                    while (running) {
                        sendHeartbeat();
                        if (lastAck > 0 && System.currentTimeMillis() - lastAck > interval * 3) {
                            log("Heartbeat was not acknowledged, reconnecting");
                            requestReconnect();
                            return;
                        }
                        Thread.sleep(interval);
                    }
                } catch (InterruptedException ignored) {
                    // service is stopping
                }
            }
        }, "botdroid-heartbeat");
        thread.setDaemon(true);
        heartbeatThread = thread;
        thread.start();
    }

    private void stopHeartbeat() {
        Thread thread = heartbeatThread;
        if (thread != null) {
            thread.interrupt();
            heartbeatThread = null;
        }
    }

    private void sendHeartbeat() {
        GatewaySocket current = socket;
        if (current == null) {
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("op", 1);
            Object sequenceValue = sequence >= 0 ? (Object) Integer.valueOf(sequence) : JSONObject.NULL;
            payload.put("d", sequenceValue);
            current.send(payload.toString());
        } catch (Exception e) {
            log("Heartbeat failed: " + e.getMessage());
        }
    }

    private static String shortBody(String body) {
        if (body == null) {
            return "";
        }
        String cleaned = body.replace('\n', ' ').trim();
        return cleaned.length() > 160 ? cleaned.substring(0, 160) + "…" : cleaned;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
