package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Message components: action rows, buttons and string select menus. Every
 * command builds the same JSON shape Discord expects, so a new interactive
 * message does not have to hand-write component payloads.
 */
final class Components {

    static final int BUTTON_SECONDARY = 2;
    static final int SELECT_STRING = 3;
    static final int MAX_OPTIONS = 25;
    static final int MAX_LABEL = 100;

    private Components() {
    }

    static JSONObject button(String customId, String label, int style, String emoji) {
        JSONObject button = new JSONObject();
        try {
            button.put("type", 2);
            button.put("style", style);
            button.put("custom_id", customId);
            button.put("label", clip(label, 80));
            if (emoji != null && !emoji.isEmpty()) {
                button.put("emoji", new JSONObject().put("name", emoji));
            }
        } catch (Exception ignored) {
            // every value above is settable
        }
        return button;
    }

    static JSONObject option(String value, String label, String description) {
        JSONObject option = new JSONObject();
        try {
            option.put("value", value);
            option.put("label", clip(label, MAX_LABEL));
            if (description != null && !description.isEmpty()) {
                option.put("description", clip(description, MAX_LABEL));
            }
        } catch (Exception ignored) {
            // every value above is settable
        }
        return option;
    }

    /** A string select menu. Discord sends no remembered state, so callers
     * decide whether one pick replaces or toggles. */
    static JSONObject select(String customId, String placeholder, JSONArray options,
                             int minValues, int maxValues) {
        JSONObject select = new JSONObject();
        try {
            select.put("type", SELECT_STRING);
            select.put("custom_id", customId);
            select.put("placeholder", clip(placeholder, 150));
            select.put("min_values", Math.max(0, Math.min(MAX_OPTIONS, minValues)));
            select.put("max_values", Math.max(1, Math.min(MAX_OPTIONS, maxValues)));
            select.put("options", options == null ? new JSONArray() : options);
        } catch (Exception ignored) {
            // every value above is settable
        }
        return select;
    }

    /** One action row; Discord shows at most five components per row. */
    static JSONObject row(JSONObject... components) {
        JSONArray children = new JSONArray();
        for (int i = 0; i < components.length && i < 5; i++) {
            if (components[i] != null) {
                children.put(components[i]);
            }
        }
        JSONObject row = new JSONObject();
        try {
            row.put("type", 1);
            row.put("components", children);
        } catch (Exception ignored) {
            // every value above is settable
        }
        return row;
    }

    static JSONArray rows(JSONObject... rows) {
        JSONArray array = new JSONArray();
        for (JSONObject row : rows) {
            if (row != null) {
                array.put(row);
            }
        }
        return array;
    }

    static String clip(String value, int limit) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() <= limit ? text : text.substring(0, limit);
    }
}
