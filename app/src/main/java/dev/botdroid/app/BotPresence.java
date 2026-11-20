package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Bot activity/status shown in Discord, and the gateway presence payload for it.
 *
 * <p>Discord only lets bots set an activity's name, state, type and url, so
 * images, buttons and party data are deliberately not offered here: they would
 * be silently ignored by the API.</p>
 */
final class BotPresence {

    static final int PLAYING = 0;
    static final int STREAMING = 1;
    static final int LISTENING = 2;
    static final int WATCHING = 3;
    static final int CUSTOM = 4;
    static final int COMPETING = 5;

    static final int[] TYPE_VALUES = {PLAYING, STREAMING, LISTENING, WATCHING, COMPETING, CUSTOM};
    static final String[] TYPE_LABELS = {
            "Playing", "Streaming", "Listening to", "Watching", "Competing in", "Custom status"
    };

    static final String[] STATUS_VALUES = {"online", "idle", "dnd", "invisible"};
    static final String[] STATUS_LABELS = {"Online", "Idle", "Do Not Disturb", "Invisible"};

    static final int MAX_TEXT = 128;
    static final int MAX_STATE = 128;

    static volatile int type = PLAYING;
    static volatile String text = "";
    static volatile String status = "online";
    static volatile String state = "";
    static volatile String url = "";

    private BotPresence() {
    }

    static void set(int activityType, String activityText, String presenceStatus) {
        set(activityType, activityText, presenceStatus, "", "");
    }

    static void set(int activityType, String activityText, String presenceStatus, String stateText,
                    String streamUrl) {
        String stream = clamp(streamUrl, 512);
        int normalizedType = validType(activityType);
        // Discord requires a url for the streaming activity; without one it is invalid.
        if (normalizedType == STREAMING && stream.isEmpty()) {
            normalizedType = PLAYING;
        }
        type = normalizedType;
        text = clamp(activityText, MAX_TEXT);
        status = normalizeStatus(presenceStatus);
        state = clamp(stateText, MAX_STATE);
        url = stream;
    }

    private static String clamp(String value, int max) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    static boolean hasText() {
        return !text.isEmpty();
    }

    static int validType(int value) {
        for (int candidate : TYPE_VALUES) {
            if (candidate == value) {
                return value;
            }
        }
        return PLAYING;
    }

    static String normalizeStatus(String value) {
        if (value != null) {
            for (String candidate : STATUS_VALUES) {
                if (candidate.equals(value)) {
                    return candidate;
                }
            }
        }
        return "online";
    }

    static String typeLabel(int value) {
        for (int i = 0; i < TYPE_VALUES.length; i++) {
            if (TYPE_VALUES[i] == value) {
                return TYPE_LABELS[i];
            }
        }
        return TYPE_LABELS[0];
    }

    static String statusLabel(String value) {
        for (int i = 0; i < STATUS_VALUES.length; i++) {
            if (STATUS_VALUES[i].equals(value)) {
                return STATUS_LABELS[i];
            }
        }
        return STATUS_LABELS[0];
    }

    static String describe() {
        if (!hasText()) {
            return "No activity — " + statusLabel(status);
        }
        if (type == CUSTOM) {
            return "Custom: " + text + " — " + statusLabel(status);
        }
        StringBuilder description = new StringBuilder(typeLabel(type)).append(' ').append(text);
        if (!state.isEmpty()) {
            description.append(" (").append(state).append(')');
        }
        if (type == STREAMING) {
            description.append(" → ").append(url);
        }
        return description.append(" — ").append(statusLabel(status)).toString();
    }

    /** Builds the op 3 PRESENCE_UPDATE payload. */
    static JSONObject gatewayPayload() throws Exception {
        JSONObject data = new JSONObject();
        JSONArray activities = new JSONArray();
        if (hasText()) {
            JSONObject activity = new JSONObject();
            if (type == CUSTOM) {
                activity.put("name", "Custom Status");
                activity.put("type", CUSTOM);
                activity.put("state", text);
            } else {
                activity.put("name", text);
                activity.put("type", type);
                if (!state.isEmpty()) {
                    activity.put("state", state);
                }
                if (type == STREAMING && !url.isEmpty()) {
                    activity.put("url", url);
                }
            }
            activities.put(activity);
        }
        data.put("since", JSONObject.NULL);
        data.put("activities", activities);
        data.put("status", status);
        data.put("afk", false);
        JSONObject payload = new JSONObject();
        payload.put("op", 3);
        payload.put("d", data);
        return payload;
    }
}
