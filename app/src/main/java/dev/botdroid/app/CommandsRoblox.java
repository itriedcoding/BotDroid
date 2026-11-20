package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;

/** /roblox user|avatar|game|group backed by the public Roblox API. */
final class CommandsRoblox {

    private CommandsRoblox() {
    }

    static void handle(DiscordRest rest, JSONObject interaction) throws Exception {
        if (!BotState.robloxEnabled) {
            Commands.respond(rest, interaction,
                    "Roblox commands are turned off. Enable them in BotDroid → Settings.", true, null);
            return;
        }
        String sub = Commands.subcommandName(interaction);
        if (sub == null) {
            Commands.respond(rest, interaction,
                    "Use `/roblox user`, `/roblox avatar`, `/roblox game` or `/roblox group`.", true, null);
            return;
        }
        try {
            if ("user".equals(sub)) {
                user(rest, interaction);
            } else if ("avatar".equals(sub)) {
                avatar(rest, interaction);
            } else if ("game".equals(sub)) {
                game(rest, interaction);
            } else if ("group".equals(sub)) {
                group(rest, interaction);
            } else {
                Commands.respond(rest, interaction, "Unknown Roblox lookup.", true, null);
            }
        } catch (RobloxApi.ApiException e) {
            Commands.respond(rest, interaction, "Roblox lookup failed: " + e.getMessage(), true, null);
        }
    }

    private static void user(DiscordRest rest, JSONObject interaction) throws Exception {
        String username = Commands.optString(interaction, "username");
        if (username == null || username.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me a Roblox username.", true, null);
            return;
        }
        long id = RobloxApi.findUserId(username.trim());
        if (id <= 0) {
            Commands.respond(rest, interaction, "No Roblox user named **" + username.trim() + "**.", true, null);
            return;
        }
        JSONObject user = RobloxApi.user(id);
        int friends = RobloxApi.friendsCount(id);
        int followers = RobloxApi.followersCount(id);

        StringBuilder description = new StringBuilder();
        description.append("**Display name:** ").append(user.optString("displayName", "unknown")).append("\n");
        description.append("**Username:** ").append(user.optString("name", username.trim())).append("\n");
        description.append("**User ID:** `").append(id).append("`\n");
        String created = user.optString("created", "");
        long epoch = epochSeconds(created);
        if (epoch > 0) {
            description.append("**Created:** <t:").append(epoch).append(":R>\n");
        }
        if (friends >= 0) {
            description.append("**Friends:** ").append(friends).append("\n");
        }
        if (followers >= 0) {
            description.append("**Followers:** ").append(followers).append("\n");
        }
        if (user.optBoolean("isBanned", false)) {
            description.append("**Banned:** yes\n");
        }
        String about = user.optString("description", "");
        if (!about.isEmpty()) {
            description.append("\n").append(about.length() > 400 ? about.substring(0, 400) + "…" : about);
        }

        JSONObject embed = Commands.embed(user.optString("displayName", username.trim()), 0xE2231A,
                description.toString());
        embed.put("url", "https://www.roblox.com/users/" + id + "/profile");
        String headshot = RobloxApi.headshot(id);
        if (headshot != null) {
            embed.put("thumbnail", new JSONObject().put("url", headshot));
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static void avatar(DiscordRest rest, JSONObject interaction) throws Exception {
        String username = Commands.optString(interaction, "username");
        if (username == null || username.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me a Roblox username.", true, null);
            return;
        }
        long id = RobloxApi.findUserId(username.trim());
        if (id <= 0) {
            Commands.respond(rest, interaction, "No Roblox user named **" + username.trim() + "**.", true, null);
            return;
        }
        String image = RobloxApi.avatar(id);
        if (image == null) {
            image = RobloxApi.headshot(id);
        }
        if (image == null) {
            Commands.respond(rest, interaction, "Roblox has no rendered avatar for that user yet, try again shortly.",
                    true, null);
            return;
        }
        JSONObject embed = Commands.embed(username.trim() + "'s avatar", 0xE2231A,
                "[Open on Roblox](https://www.roblox.com/users/" + id + "/profile)");
        embed.put("image", new JSONObject().put("url", image));
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static void game(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "universe_id");
        long universeId = parseId(raw);
        if (universeId <= 0) {
            Commands.respond(rest, interaction,
                    "Give me a numeric Roblox universe ID (the number in the game's URL parameters).", true, null);
            return;
        }
        JSONObject game = RobloxApi.game(universeId);
        if (game == null) {
            Commands.respond(rest, interaction, "No Roblox game found with universe ID `" + universeId + "`.", true,
                    null);
            return;
        }
        JSONObject creator = game.optJSONObject("creator");
        StringBuilder description = new StringBuilder();
        description.append("**Universe ID:** `").append(universeId).append("`\n");
        if (creator != null) {
            description.append("**Creator:** ").append(creator.optString("name", "unknown")).append("\n");
        }
        description.append("**Playing now:** ").append(game.optInt("playing", 0)).append("\n");
        description.append("**Visits:** ").append(game.optLong("visits", 0)).append("\n");
        description.append("**Favourites:** ").append(game.optLong("favoritedCount", 0)).append("\n");
        description.append("**Max players:** ").append(game.optInt("maxPlayers", 0)).append("\n");
        long epoch = epochSeconds(game.optString("created", ""));
        if (epoch > 0) {
            description.append("**Created:** <t:").append(epoch).append(":R>");
        }
        JSONObject embed = Commands.embed(game.optString("name", "Roblox game"), 0xE2231A, description.toString());
        String icon = RobloxApi.gameIcon(universeId);
        if (icon != null) {
            embed.put("thumbnail", new JSONObject().put("url", icon));
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static void group(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "group_id");
        long groupId = parseId(raw);
        if (groupId <= 0) {
            Commands.respond(rest, interaction, "Give me a numeric Roblox group ID.", true, null);
            return;
        }
        JSONObject group = RobloxApi.group(groupId);
        JSONObject owner = group.optJSONObject("owner");
        StringBuilder description = new StringBuilder();
        description.append("**Group ID:** `").append(groupId).append("`\n");
        description.append("**Members:** ").append(group.optInt("memberCount", 0)).append("\n");
        if (owner != null) {
            description.append("**Owner:** ").append(owner.optString("name", "unknown")).append("\n");
        }
        String about = group.optString("description", "");
        if (!about.isEmpty()) {
            description.append("\n").append(about.length() > 400 ? about.substring(0, 400) + "…" : about);
        }
        JSONObject embed = Commands.embed(group.optString("name", "Roblox group"), 0xE2231A, description.toString());
        String icon = RobloxApi.groupIcon(groupId);
        if (icon != null) {
            embed.put("thumbnail", new JSONObject().put("url", icon));
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static long parseId(String raw) {
        if (raw == null) {
            return -1L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static long epochSeconds(String iso) {
        if (iso == null || iso.isEmpty()) {
            return -1L;
        }
        try {
            return Instant.parse(iso).getEpochSecond();
        } catch (Exception e) {
            return -1L;
        }
    }
}
