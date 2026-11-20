package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

/** Extra moderation commands beyond ban/kick/timeout/purge. */
final class CommandsMod {

    private static final long VIEW_CHANNEL = 1L << 10;
    private static final long SEND_MESSAGES = 1L << 11;

    private CommandsMod() {
    }

    static void lock(DiscordRest rest, JSONObject interaction) throws Exception {
        overwrite(rest, interaction, SEND_MESSAGES, true, "🔒 Locked");
    }

    static void unlock(DiscordRest rest, JSONObject interaction) throws Exception {
        overwrite(rest, interaction, SEND_MESSAGES, false, "🔓 Unlocked");
    }

    static void hide(DiscordRest rest, JSONObject interaction) throws Exception {
        overwrite(rest, interaction, VIEW_CHANNEL, true, "🙈 Hidden");
    }

    static void unhide(DiscordRest rest, JSONObject interaction) throws Exception {
        overwrite(rest, interaction, VIEW_CHANNEL, false, "👁️ Visible again");
    }

    static void topic(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_CHANNELS)) {
            Commands.respond(rest, interaction, "You need the **Manage Channels** permission for that.", true, null);
            return;
        }
        String text = Commands.optString(interaction, "text");
        JSONObject patch = new JSONObject();
        patch.put("topic", text == null || text.trim().isEmpty() ? JSONObject.NULL : text.trim());
        Net.Resp response = rest.patch("/channels/" + channelId, patch.toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, text == null || text.trim().isEmpty()
                    ? "Cleared the channel topic." : "Set the channel topic.", true, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Channels"), true, null);
        }
    }

    static void nick(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        if (guildId == null || userId == null) {
            Commands.respond(rest, interaction, "Pick a member in this server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_NICKNAMES)) {
            Commands.respond(rest, interaction, "You need the **Manage Nicknames** permission for that.", true, null);
            return;
        }
        String name = Commands.optString(interaction, "name");
        JSONObject patch = new JSONObject();
        patch.put("nick", name == null || name.trim().isEmpty() ? JSONObject.NULL : name.trim());
        Net.Resp response = rest.patch("/guilds/" + guildId + "/members/" + userId, patch.toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, name == null || name.trim().isEmpty()
                    ? "Reset " + Commands.displayName(interaction, "user", userId) + "'s nickname."
                    : "Renamed " + Commands.displayName(interaction, "user", userId) + " to **" + name.trim() + "**.",
                    false, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Nicknames"), true, null);
        }
    }

    static void addRole(DiscordRest rest, JSONObject interaction) throws Exception {
        role(rest, interaction, true);
    }

    static void removeRole(DiscordRest rest, JSONObject interaction) throws Exception {
        role(rest, interaction, false);
    }

    private static void role(DiscordRest rest, JSONObject interaction, boolean add) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        String roleId = Commands.optString(interaction, "role");
        if (guildId == null || userId == null || roleId == null) {
            Commands.respond(rest, interaction, "Pick a member and a role in this server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_ROLES)) {
            Commands.respond(rest, interaction, "You need the **Manage Roles** permission for that.", true, null);
            return;
        }
        String path = "/guilds/" + guildId + "/members/" + userId + "/roles/" + roleId;
        Net.Resp response = add ? rest.put(path, null) : rest.delete(path);
        if (response.ok()) {
            Commands.respond(rest, interaction, (add ? "Added <@&" : "Removed <@&") + roleId + "> "
                    + (add ? "to " : "from ") + Commands.displayName(interaction, "user", userId) + ".", false, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Roles"), true, null);
        }
    }

    static void banList(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.BAN_MEMBERS)) {
            Commands.respond(rest, interaction, "You need the **Ban Members** permission for that.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/bans?limit=15");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Ban Members"), true, null);
            return;
        }
        JSONArray data = new JSONObject(response.body).optJSONArray("data");
        if (data == null || data.length() == 0) {
            Commands.respond(rest, interaction, "There are no bans in this server.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            JSONObject entry = data.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            JSONObject user = entry.optJSONObject("user");
            String username = user == null ? "unknown" : user.optString("username", "unknown");
            String id = user == null ? "" : user.optString("id", "");
            String reason = entry.optString("reason", "");
            if (reason != null && reason.length() > 60) {
                reason = reason.substring(0, 60) + "…";
            }
            description.append("• **").append(username).append("** `").append(id).append("`")
                    .append(reason == null || reason.isEmpty() ? "" : " — " + reason).append("\n");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Bans", 0xED4245, description.toString())));
    }

    static void softban(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        if (guildId == null || userId == null) {
            Commands.respond(rest, interaction, "Pick a member in this server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.BAN_MEMBERS)) {
            Commands.respond(rest, interaction, "You need the **Ban Members** permission for that.", true, null);
            return;
        }
        String reason = Commands.orDefault(Commands.optString(interaction, "reason"), "Softban");
        JSONObject body = new JSONObject();
        body.put("delete_message_seconds", 86400);
        Net.Resp ban = rest.put("/guilds/" + guildId + "/bans/" + userId + "?reason=" + Commands.encode(reason),
                body.toString());
        if (!ban.ok()) {
            Commands.respond(rest, interaction, Commands.failure(ban, "Ban Members"), true, null);
            return;
        }
        Net.Resp unban = rest.delete("/guilds/" + guildId + "/bans/" + userId + "?reason="
                + Commands.encode("Softban: " + reason));
        Commands.respond(rest, interaction, "Softbanned " + Commands.displayName(interaction, "user", userId)
                + " — messages from the last day were removed"
                + (unban.ok() ? "." : " (but I could not lift the ban: " + Commands.apiMessage(unban.body) + ")"),
                false, null);
    }

    static void pin(DiscordRest rest, JSONObject interaction) throws Exception {
        pinInternal(rest, interaction, true);
    }

    static void unpin(DiscordRest rest, JSONObject interaction) throws Exception {
        pinInternal(rest, interaction, false);
    }

    private static void pinInternal(DiscordRest rest, JSONObject interaction, boolean add) throws Exception {
        String channelId = Commands.channelId(interaction);
        String messageId = Commands.optString(interaction, "message_id");
        if (channelId == null || messageId == null || messageId.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me a message ID from this channel.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_MESSAGES)) {
            Commands.respond(rest, interaction, "You need the **Manage Messages** permission for that.", true, null);
            return;
        }
        String path = "/channels/" + channelId + "/pins/" + messageId.trim();
        Net.Resp response = add ? rest.put(path, null) : rest.delete(path);
        if (response.ok()) {
            Commands.respond(rest, interaction, (add ? "Pinned" : "Unpinned") + " message `" + messageId.trim() + "`.",
                    true, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Messages"), true, null);
        }
    }

    private static void overwrite(DiscordRest rest, JSONObject interaction, long bit, boolean deny, String verb)
            throws Exception {
        String channelId = Commands.channelId(interaction);
        String guildId = Commands.guildId(interaction);
        if (channelId == null || guildId == null) {
            Commands.respond(rest, interaction, "Run this in a server channel.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_CHANNELS)) {
            Commands.respond(rest, interaction, "You need the **Manage Channels** permission for that.", true, null);
            return;
        }
        Net.Resp current = rest.get("/channels/" + channelId);
        if (!current.ok()) {
            Commands.respond(rest, interaction, Commands.failure(current, "Manage Channels"), true, null);
            return;
        }
        JSONObject channel = new JSONObject(current.body);
        JSONArray existing = channel.optJSONArray("permission_overwrites");
        JSONArray rebuilt = new JSONArray();
        JSONObject everyone = null;
        if (existing != null) {
            for (int i = 0; i < existing.length(); i++) {
                JSONObject override = existing.optJSONObject(i);
                if (override == null) {
                    continue;
                }
                if (override.optInt("type", -1) == 0 && guildId.equals(override.optString("id"))) {
                    everyone = override;
                } else {
                    rebuilt.put(override);
                }
            }
        }
        if (everyone == null) {
            everyone = new JSONObject();
            everyone.put("id", guildId);
            everyone.put("type", 0);
            everyone.put("allow", "0");
            everyone.put("deny", "0");
        }
        long allow = parseBits(everyone.optString("allow", "0"));
        long denied = parseBits(everyone.optString("deny", "0"));
        if (deny) {
            denied |= bit;
            allow &= ~bit;
        } else {
            denied &= ~bit;
            allow &= ~bit;
        }
        everyone.put("allow", Long.toString(allow));
        everyone.put("deny", Long.toString(denied));
        if (allow != 0 || denied != 0) {
            rebuilt.put(everyone);
        }
        JSONObject patch = new JSONObject();
        patch.put("permission_overwrites", rebuilt);
        Net.Resp response = rest.patch("/channels/" + channelId, patch.toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, verb + " <#" + channelId + ">.", true, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Channels"), true, null);
        }
    }

    private static long parseBits(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }
}
