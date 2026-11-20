package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/** Role, channel and voice administration commands. */
final class CommandsAdmin {

    private CommandsAdmin() {
    }

    static void unTimeout(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        if (guildId == null || userId == null) {
            Commands.respond(rest, interaction, "Pick a member in this server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MODERATE_MEMBERS)) {
            Commands.respond(rest, interaction, "You need the **Moderate Members** permission for that.", true, null);
            return;
        }
        String reason = Commands.orDefault(Commands.optString(interaction, "reason"), "Timeout removed");
        JSONObject body = new JSONObject();
        body.put("communication_disabled_until", JSONObject.NULL);
        Net.Resp response = rest.patch("/guilds/" + guildId + "/members/" + userId + "?reason="
                + Commands.encode(reason), body.toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, "Removed the timeout from "
                    + Commands.displayName(interaction, "user", userId) + ".", false, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Moderate Members"), true, null);
        }
    }

    static void banInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user_id");
        if (guildId == null || userId == null || userId.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me the banned user's ID.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.BAN_MEMBERS)) {
            Commands.respond(rest, interaction, "You need the **Ban Members** permission for that.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/bans/" + userId.trim());
        if (response.code == 404) {
            Commands.respond(rest, interaction, "`" + userId.trim() + "` is not banned in this server.", true, null);
            return;
        }
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Ban Members"), true, null);
            return;
        }
        JSONObject entry = new JSONObject(response.body);
        JSONObject user = entry.optJSONObject("user");
        String reason = Commands.orDefault(entry.optString("reason", ""), "No reason recorded");
        String description = "**User:** " + (user == null ? "unknown" : user.optString("username", "unknown"))
                + " (`" + userId.trim() + "`)\n**Reason:** " + reason;
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Ban details", 0xED4245, description)));
    }

    static void roleInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String roleId = Commands.optString(interaction, "role");
        if (guildId == null || roleId == null) {
            Commands.respond(rest, interaction, "Pick a role in this server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/roles");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONArray roles = new JSONArray(response.body);
        JSONObject role = null;
        for (int i = 0; i < roles.length(); i++) {
            JSONObject candidate = roles.optJSONObject(i);
            if (candidate != null && roleId.equals(candidate.optString("id"))) {
                role = candidate;
                break;
            }
        }
        if (role == null) {
            Commands.respond(rest, interaction, "That role does not exist in this server.", true, null);
            return;
        }
        long permissions = 0L;
        try {
            permissions = Long.parseLong(role.optString("permissions", "0"));
        } catch (NumberFormatException ignored) {
            // leave as zero
        }
        StringBuilder description = new StringBuilder();
        description.append("**ID:** `").append(roleId).append("`\n");
        description.append("**Colour:** `").append(String.format(Locale.US, "#%06X",
                role.optInt("color", 0) & 0xFFFFFF)).append("`\n");
        description.append("**Position:** ").append(role.optInt("position", 0)).append("\n");
        description.append("**Hoisted:** ").append(role.optBoolean("hoist", false) ? "yes" : "no").append("\n");
        description.append("**Mentionable:** ").append(role.optBoolean("mentionable", false) ? "yes" : "no").append("\n");
        description.append("**Managed by an integration:** ").append(role.optBoolean("managed", false) ? "yes" : "no");
        List<String> names = Commands.permissionNames(permissions);
        if (!names.isEmpty()) {
            description.append("\n\n**Key permissions**\n");
            int shown = 0;
            for (String name : names) {
                if (shown >= 12) {
                    break;
                }
                description.append("• ").append(name).append('\n');
                shown++;
            }
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed(role.optString("name", "Role"), role.optInt("color", 0),
                        description.toString())));
    }

    static void roleColor(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String roleId = Commands.optString(interaction, "role");
        String hex = Commands.optString(interaction, "hex");
        if (guildId == null || roleId == null || hex == null) {
            Commands.respond(rest, interaction, "Pick a role and give me a hex colour.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_ROLES)) {
            Commands.respond(rest, interaction, "You need the **Manage Roles** permission for that.", true, null);
            return;
        }
        Integer color = parseHex(hex);
        if (color == null) {
            Commands.respond(rest, interaction, "That is not a 6 digit hex colour (for example `#5865F2`).", true, null);
            return;
        }
        Net.Resp response = rest.patch("/guilds/" + guildId + "/roles/" + roleId,
                new JSONObject().put("color", color.intValue()).toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, "Changed <@&" + roleId + "> to `#" + hex.replace("#", "")
                    .toUpperCase(Locale.US) + "`.", false, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Roles"), true, null);
        }
    }

    static void createRole(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String name = Commands.optString(interaction, "name");
        if (guildId == null || name == null || name.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give the role a name.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_ROLES)) {
            Commands.respond(rest, interaction, "You need the **Manage Roles** permission for that.", true, null);
            return;
        }
        JSONObject body = new JSONObject();
        body.put("name", name.trim());
        body.put("mentionable", true);
        String hex = Commands.optString(interaction, "color");
        if (hex != null) {
            Integer color = parseHex(hex);
            if (color != null) {
                body.put("color", color.intValue());
            }
        }
        Net.Resp response = rest.post("/guilds/" + guildId + "/roles", body.toString());
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Roles"), true, null);
            return;
        }
        String roleId = new JSONObject(response.body).optString("id", "");
        Commands.respond(rest, interaction, "Created <@&" + roleId + ">.", false, null);
    }

    static void createChannel(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String name = Commands.optString(interaction, "name");
        if (guildId == null || name == null || name.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give the channel a name.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_CHANNELS)) {
            Commands.respond(rest, interaction, "You need the **Manage Channels** permission for that.", true, null);
            return;
        }
        String type = Commands.optString(interaction, "type");
        int channelType = "voice".equalsIgnoreCase(type) ? 2 : 0;
        JSONObject body = new JSONObject();
        body.put("name", name.trim().toLowerCase(Locale.US).replace(' ', '-'));
        body.put("type", channelType);
        Net.Resp response = rest.post("/guilds/" + guildId + "/channels", body.toString());
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Channels"), true, null);
            return;
        }
        String channelId = new JSONObject(response.body).optString("id", "");
        Commands.respond(rest, interaction, "Created <#" + channelId + ">.", false, null);
    }

    static void cloneChannel(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String channelId = Commands.channelId(interaction);
        if (guildId == null || channelId == null) {
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
        JSONObject source = new JSONObject(current.body);
        JSONObject body = new JSONObject();
        body.put("name", source.optString("name", "channel") + "-copy");
        body.put("type", source.optInt("type", 0));
        if (source.has("topic")) {
            body.put("topic", source.opt("topic"));
        }
        body.put("nsfw", source.optBoolean("nsfw", false));
        if (source.has("rate_limit_per_user")) {
            body.put("rate_limit_per_user", source.optInt("rate_limit_per_user", 0));
        }
        if (source.has("bitrate")) {
            body.put("bitrate", source.optInt("bitrate", 64000));
        }
        if (source.has("user_limit")) {
            body.put("user_limit", source.optInt("user_limit", 0));
        }
        if (source.has("parent_id") && !source.isNull("parent_id")) {
            body.put("parent_id", source.optString("parent_id"));
        }
        JSONArray overwrites = source.optJSONArray("permission_overwrites");
        if (overwrites != null) {
            body.put("permission_overwrites", overwrites);
        }
        Net.Resp response = rest.post("/guilds/" + guildId + "/channels", body.toString());
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Channels"), true, null);
            return;
        }
        String newChannelId = new JSONObject(response.body).optString("id", "");
        Commands.respond(rest, interaction, "Cloned into <#" + newChannelId + ">.", false, null);
    }

    static void move(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        String channelId = Commands.optString(interaction, "channel");
        if (guildId == null || userId == null || channelId == null) {
            Commands.respond(rest, interaction, "Pick a member and a voice channel.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MOVE_MEMBERS)) {
            Commands.respond(rest, interaction, "You need the **Move Members** permission for that.", true, null);
            return;
        }
        JSONObject body = new JSONObject();
        body.put("channel_id", channelId);
        Net.Resp response = rest.patch("/guilds/" + guildId + "/members/" + userId, body.toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, "Moved " + Commands.displayName(interaction, "user", userId)
                    + " to <#" + channelId + ">.", false, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Move Members"), true, null);
        }
    }

    static void mute(DiscordRest rest, JSONObject interaction) throws Exception {
        voiceState(rest, interaction, "mute", Commands.MUTE_MEMBERS, "Server muted", "Mute Members");
    }

    static void deafen(DiscordRest rest, JSONObject interaction) throws Exception {
        voiceState(rest, interaction, "deaf", Commands.DEAFEN_MEMBERS, "Server deafened", "Deafen Members");
    }

    private static void voiceState(DiscordRest rest, JSONObject interaction, String field, long permission,
                                   String verb, String permissionName) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        if (guildId == null || userId == null) {
            Commands.respond(rest, interaction, "Pick a member in this server.", true, null);
            return;
        }
        if (!Commands.can(interaction, permission)) {
            Commands.respond(rest, interaction, "You need the **" + permissionName + "** permission for that.",
                    true, null);
            return;
        }
        JSONObject body = new JSONObject();
        body.put(field, true);
        Net.Resp response = rest.patch("/guilds/" + guildId + "/members/" + userId, body.toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, verb + " " + Commands.displayName(interaction, "user", userId) + ".",
                    false, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, permissionName), true, null);
        }
    }

    private static Integer parseHex(String raw) {
        if (raw == null) {
            return null;
        }
        String hex = raw.trim().replace("#", "").replace("0x", "");
        if (!hex.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        return Integer.valueOf(Integer.parseInt(hex, 16));
    }
}
