package dev.botdroid.app;

import org.json.JSONObject;

/** Thin wrapper around the Discord REST API (v10) with basic rate-limit retry. */
final class DiscordRest {

    private static final String HOST = "discord.com";
    private static final String PREFIX = "/api/v10";

    private final String token;

    DiscordRest(String token) {
        this.token = token;
    }

    Net.Resp get(String path) {
        return call("GET", path, null);
    }

    Net.Resp post(String path, String json) {
        return call("POST", path, json);
    }

    Net.Resp put(String path, String json) {
        return call("PUT", path, json);
    }

    Net.Resp patch(String path, String json) {
        return call("PATCH", path, json);
    }

    Net.Resp delete(String path) {
        return call("DELETE", path, null);
    }

    private Net.Resp call(String method, String path, String json) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Net.Resp response = Net.request(method, HOST, PREFIX + path, token, json);
                if (response.code == 429 && attempt < 2) {
                    long waitMs = 1000L;
                    try {
                        waitMs = (long) (new JSONObject(response.body).optDouble("retry_after", 1.0) * 1000.0) + 100L;
                    } catch (Exception ignored) {
                        // keep the default wait
                    }
                    Thread.sleep(Math.min(waitMs, 5000L));
                    continue;
                }
                return response;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Net.Resp(-1, "interrupted");
            } catch (Exception e) {
                if (attempt == 2) {
                    String message = e.getMessage();
                    return new Net.Resp(-1, message == null ? e.getClass().getSimpleName() : message);
                }
                try {
                    Thread.sleep(400L * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new Net.Resp(-1, "interrupted");
                }
            }
        }
        return new Net.Resp(-1, "request failed");
    }
}
