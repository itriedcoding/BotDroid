package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Preset slash commands and their handlers. */
final class Commands {

    private static final long ADMINISTRATOR = 1L << 3;
    private static final long KICK_MEMBERS = 1L << 1;
    private static final long BAN_MEMBERS = 1L << 2;
    private static final long MANAGE_CHANNELS = 1L << 4;
    private static final long MANAGE_MESSAGES = 1L << 13;
    private static final long MODERATE_MEMBERS = 1L << 40;

    private static final Random RANDOM = new Random();

    private static final String[] EIGHT_BALL = {
            "It is certain.", "Without a doubt.", "Yes, definitely.", "Most likely.",
            "Outlook good.", "Signs point to yes.", "Reply hazy, try again.",
            "Ask again later.", "Better not tell you now.", "Cannot predict now.",
            "Don't count on it.", "My reply is no.", "My sources say no.",
            "Outlook not so good.", "Very doubtful."
    };

    private static final String HELP = "**BotDroid — command list**\n"
            + "__Utility__\n"
            + "`/help` — this list\n"
            + "`/ping` — check latency\n"
            + "`/serverinfo` — server details\n"
            + "`/userinfo [user]` — user details\n"
            + "`/avatar [user]` — user avatar\n"
            + "__Moderation__\n"
            + "`/ban <user> [reason]` — ban a member\n"
            + "`/unban <user_id> [reason]` — lift a ban\n"
            + "`/kick <user> [reason]` — kick a member\n"
            + "`/timeout <user> <minutes> [reason]` — temporarily mute\n"
            + "`/purge <count>` — delete recent messages\n"
            + "`/slowmode <seconds>` — set channel slowmode\n"
            + "`/say <text>` — post a message as the bot\n"
            + "__Fun__\n"
            + "`/8ball <question>` — ask the magic 8-ball\n"
            + "`/roll [2d6]` — roll dice\n"
            + "_Moderation commands require the matching Discord permission._";

    private static final String DEFINITIONS = "["
            + "{\"name\":\"help\",\"description\":\"Show every command BotDroid can run\"},"
            + "{\"name\":\"ping\",\"description\":\"Check the bot latency and connection\"},"
            + "{\"name\":\"serverinfo\",\"description\":\"Show information about this server\"},"
            + "{\"name\":\"userinfo\",\"description\":\"Show information about a user\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"User to look up (defaults to you)\",\"required\":false}]},"
            + "{\"name\":\"avatar\",\"description\":\"Show a user avatar\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"User (defaults to you)\",\"required\":false}]},"
            + "{\"name\":\"ban\",\"description\":\"Ban a member from this server\",\"default_member_permissions\":\"4\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member to ban\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason for the ban\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"unban\",\"description\":\"Remove a ban using the user ID\",\"default_member_permissions\":\"4\",\"options\":["
            + "{\"type\":3,\"name\":\"user_id\",\"description\":\"ID of the banned user\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"kick\",\"description\":\"Kick a member from this server\",\"default_member_permissions\":\"2\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member to kick\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason for the kick\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"timeout\",\"description\":\"Temporarily mute a member\",\"default_member_permissions\":\"1099511627776\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member to time out\",\"required\":true},"
            + "{\"type\":4,\"name\":\"minutes\",\"description\":\"Length in minutes (1-10080)\",\"required\":true,\"min_value\":1,\"max_value\":10080},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"purge\",\"description\":\"Delete recent messages in this channel\",\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":4,\"name\":\"count\",\"description\":\"How many messages (1-100)\",\"required\":true,\"min_value\":1,\"max_value\":100}]},"
            + "{\"name\":\"slowmode\",\"description\":\"Set channel slowmode\",\"default_member_permissions\":\"16\",\"options\":["
            + "{\"type\":4,\"name\":\"seconds\",\"description\":\"Seconds between messages (0-21600)\",\"required\":true,\"min_value\":0,\"max_value\":21600}]},"
            + "{\"name\":\"say\",\"description\":\"Make BotDroid post a message\",\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Message to send\",\"required\":true,\"max_length\":2000}]},"
            + "{\"name\":\"8ball\",\"description\":\"Ask the magic 8-ball a question\",\"options\":["
            + "{\"type\":3,\"name\":\"question\",\"description\":\"Your question\",\"required\":true,\"max_length\":200}]},"
            + "{\"name\":\"roll\",\"description\":\"Roll dice, for example 2d6\",\"options\":["
            + "{\"type\":3,\"name\":\"dice\",\"description\":\"Format NdN (default 1d6)\",\"required\":false}]}"
            + "]";

    private Commands() {
    }

    static JSONArray definitions() {
        try {
            return new JSONArray(DEFINITIONS);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    static void handle(DiscordRest rest, JSONObject interaction) {
        try {
            JSONObject data = interaction.optJSONObject("data");
            String name = data == null ? "" : data.optString("name", "");
            if ("help".equals(name)) {
                help(rest, interaction);
            } else if ("ping".equals(name)) {
                ping(rest, interaction);
            } else if ("serverinfo".equals(name)) {
                serverInfo(rest, interaction);
            } else if ("userinfo".equals(name)) {
                userInfo(rest, interaction);
            } else if ("avatar".equals(name)) {
                avatar(rest, interaction);
            } else if ("ban".equals(name)) {
                ban(rest, interaction);
            } else if ("unban".equals(name)) {
                unban(rest, interaction);
            } else if ("kick".equals(name)) {
                kick(rest, interaction);
            } else if ("timeout".equals(name)) {
                timeout(rest, interaction);
            } else if ("purge".equals(name)) {
                purge(rest, interaction);
            } else if ("slowmode".equals(name)) {
                slowmode(rest, interaction);
            } else if ("say".equals(name)) {
                say(rest, interaction);
            } else if ("8ball".equals(name)) {
                eightBall(rest, interaction);
            } else if ("roll".equals(name)) {
                roll(rest, interaction);
            } else {
                respond(rest, interaction, "That command is not available in this build: `/" + name + "`", true, null);
            }
        } catch (Exception e) {
            try {
                respond(rest, interaction, "Something went wrong: " + e.getMessage(), true, null);
            } catch (Exception ignored) {
                // the interaction token may already be spent
            }
        }
    }

    // ---------------------------------------------------------------- utility

    private static void help(DiscordRest rest, JSONObject interaction) throws Exception {
        respond(rest, interaction, HELP, true, null);
    }

    private static void ping(DiscordRest rest, JSONObject interaction) throws Exception {
        long start = System.currentTimeMillis();
        Net.Resp response = rest.get("/gateway");
        long elapsed = System.currentTimeMillis() - start;
        String gateway = response.ok() ? "reachable" : "HTTP " + response.code;
        respond(rest, interaction, "Pong — API round trip **" + elapsed + " ms**, gateway " + gateway + ".",
                true, null);
    }

    private static void serverInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "?with_counts=true");
        if (!response.ok()) {
            respond(rest, interaction, failure(response, "View Server Insights"), true, null);
            return;
        }
        JSONObject guild = new JSONObject(response.body);
        String name = guild.optString("name", "Server");
        String icon = guild.optString("icon", null);
        String ownerId = guild.optString("owner_id", "");
        long created = snowflakeTime(guildId);
        StringBuilder description = new StringBuilder();
        description.append("**ID:** `").append(guildId).append("`\n");
        description.append("**Owner:** <@").append(ownerId).append(">\n");
        description.append("**Created:** <t:").append(created / 1000).append(":D>\n");
        int members = guild.optInt("approximate_member_count", guild.optInt("member_count", 0));
        if (members > 0) {
            description.append("**Members:** ").append(members).append("\n");
        }
        int online = guild.optInt("approximate_presence_count", -1);
        if (online >= 0) {
            description.append("**Online:** ").append(online).append("\n");
        }
        description.append("**Boost tier:** ").append(guild.optInt("premium_tier", 0)).append("\n");
        description.append("**Verification level:** ").append(guild.optInt("verification_level", 0));

        JSONObject embed = new JSONObject();
        embed.put("title", name);
        embed.put("color", 0x5865F2);
        embed.put("description", description.toString());
        if (icon != null && !icon.isEmpty() && !"null".equals(icon)) {
            embed.put("thumbnail", new JSONObject().put("url",
                    "https://cdn.discordapp.com/icons/" + guildId + "/" + icon + ".png?size=256"));
        }
        respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static void userInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String userId = optString(interaction, "user");
        if (userId == null) {
            userId = invokerId(interaction);
        }
        if (userId == null) {
            respond(rest, interaction, "I could not work out which user to look up.", true, null);
            return;
        }
        Net.Resp response = rest.get("/users/" + userId);
        if (!response.ok()) {
            respond(rest, interaction, failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject user = new JSONObject(response.body);
        String username = user.optString("username", "unknown");
        String globalName = user.optString("global_name", "");
        boolean isBot = user.optBoolean("bot", false);
        long created = snowflakeTime(userId);

        StringBuilder description = new StringBuilder();
        description.append("**Username:** ").append(username).append("\n");
        description.append("**ID:** `").append(userId).append("`\n");
        description.append("**Account created:** <t:").append(created / 1000).append(":R>\n");
        description.append("**Bot:** ").append(isBot ? "yes" : "no").append("\n");

        String guildId = guildId(interaction);
        if (guildId != null) {
            Net.Resp memberResponse = rest.get("/guilds/" + guildId + "/members/" + userId);
            if (memberResponse.ok()) {
                JSONObject member = new JSONObject(memberResponse.body);
                String joined = member.optString("joined_at", "");
                if (!joined.isEmpty()) {
                    try {
                        long joinedAt = OffsetDateTime.parse(joined).toInstant().getEpochSecond();
                        description.append("**Joined server:** <t:").append(joinedAt).append(":R>\n");
                    } catch (Exception ignored) {
                        // Discord changed the timestamp format, keep going
                    }
                }
                JSONArray roles = member.optJSONArray("roles");
                if (roles != null) {
                    description.append("**Roles:** ").append(roles.length()).append("\n");
                }
            }
        }

        JSONObject embed = new JSONObject();
        embed.put("title", globalName.isEmpty() ? username : globalName);
        embed.put("color", isBot ? 0x5865F2 : 0x23A55A);
        embed.put("description", description.toString());
        embed.put("thumbnail", new JSONObject().put("url", avatarUrl(userId, user.optString("avatar", null))));
        respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static void avatar(DiscordRest rest, JSONObject interaction) throws Exception {
        String userId = optString(interaction, "user");
        if (userId == null) {
            userId = invokerId(interaction);
        }
        if (userId == null) {
            respond(rest, interaction, "I could not work out which user to look up.", true, null);
            return;
        }
        Net.Resp response = rest.get("/users/" + userId);
        String hash = null;
        String username = null;
        if (response.ok()) {
            JSONObject user = new JSONObject(response.body);
            hash = user.optString("avatar", null);
            username = user.optString("username", null);
        }
        String url = avatarUrl(userId, hash);
        JSONObject embed = new JSONObject();
        embed.put("title", username == null ? "Avatar" : username + "'s avatar");
        embed.put("color", 0x5865F2);
        embed.put("image", new JSONObject().put("url", url));
        respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    // ------------------------------------------------------------- moderation

    private static void ban(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, BAN_MEMBERS)) {
            respond(rest, interaction, "You need the **Ban Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user");
        if (userId == null) {
            respond(rest, interaction, "Pick a member to ban.", true, null);
            return;
        }
        if (userId.equals(invokerId(interaction))) {
            respond(rest, interaction, "You cannot ban yourself.", true, null);
            return;
        }
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        JSONObject body = new JSONObject();
        body.put("delete_message_seconds", 0);
        Net.Resp response = rest.put("/guilds/" + guildId + "/bans/" + userId + "?reason=" + encode(reason),
                body.toString());
        if (response.ok()) {
            respond(rest, interaction, "Banned " + displayName(interaction, "user", userId)
                    + " (`" + userId + "`)\n**Reason:** " + reason, false, null);
        } else {
            respond(rest, interaction, failure(response, "Ban Members"), true, null);
        }
    }

    private static void unban(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, BAN_MEMBERS)) {
            respond(rest, interaction, "You need the **Ban Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user_id");
        if (userId == null || userId.trim().isEmpty()) {
            respond(rest, interaction, "Give me the numeric user ID to unban.", true, null);
            return;
        }
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        Net.Resp response = rest.delete("/guilds/" + guildId + "/bans/" + userId.trim() + "?reason=" + encode(reason));
        if (response.ok()) {
            respond(rest, interaction, "Unbanned `" + userId.trim() + "`\n**Reason:** " + reason, false, null);
        } else {
            respond(rest, interaction, failure(response, "Ban Members"), true, null);
        }
    }

    private static void kick(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, KICK_MEMBERS)) {
            respond(rest, interaction, "You need the **Kick Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user");
        if (userId == null) {
            respond(rest, interaction, "Pick a member to kick.", true, null);
            return;
        }
        if (userId.equals(invokerId(interaction))) {
            respond(rest, interaction, "You cannot kick yourself.", true, null);
            return;
        }
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        Net.Resp response = rest.delete("/guilds/" + guildId + "/members/" + userId + "?reason=" + encode(reason));
        if (response.ok()) {
            respond(rest, interaction, "Kicked " + displayName(interaction, "user", userId)
                    + " (`" + userId + "`)\n**Reason:** " + reason, false, null);
        } else {
            respond(rest, interaction, failure(response, "Kick Members"), true, null);
        }
    }

    private static void timeout(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, MODERATE_MEMBERS)) {
            respond(rest, interaction, "You need the **Moderate Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user");
        if (userId == null) {
            respond(rest, interaction, "Pick a member to time out.", true, null);
            return;
        }
        int minutes = Math.max(1, Math.min(10080, optInt(interaction, "minutes", 10)));
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        JSONObject body = new JSONObject();
        body.put("communication_disabled_until", Instant.now().plusSeconds(minutes * 60L).toString());
        Net.Resp response = rest.patch("/guilds/" + guildId + "/members/" + userId + "?reason=" + encode(reason),
                body.toString());
        if (response.ok()) {
            respond(rest, interaction, "Timed out " + displayName(interaction, "user", userId)
                    + " for **" + minutes + " minute" + (minutes == 1 ? "" : "s") + "**.\n**Reason:** " + reason,
                    false, null);
        } else {
            respond(rest, interaction, failure(response, "Moderate Members"), true, null);
        }
    }

    private static void purge(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = channelId(interaction);
        if (channelId == null) {
            respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!can(interaction, MANAGE_MESSAGES)) {
            respond(rest, interaction, "You need the **Manage Messages** permission for that.", true, null);
            return;
        }
        int count = Math.max(1, Math.min(100, optInt(interaction, "count", 10)));
        Net.Resp list = rest.get("/channels/" + channelId + "/messages?limit=" + count);
        if (!list.ok()) {
            respond(rest, interaction, failure(list, "Read Message History"), true, null);
            return;
        }
        JSONArray messages = new JSONArray(list.body);
        long cutoff = System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.optJSONObject(i);
            if (message == null) {
                continue;
            }
            String id = message.optString("id", null);
            if (id != null && snowflakeTime(id) >= cutoff) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            respond(rest, interaction, "Nothing recent enough to delete (Discord only bulk deletes messages under 14 days old).",
                    true, null);
            return;
        }
        Net.Resp result;
        if (ids.size() == 1) {
            result = rest.delete("/channels/" + channelId + "/messages/" + ids.get(0));
        } else {
            JSONArray payload = new JSONArray();
            for (String id : ids) {
                payload.put(id);
            }
            result = rest.post("/channels/" + channelId + "/messages/bulk-delete",
                    new JSONObject().put("messages", payload).toString());
        }
        if (!result.ok()) {
            respond(rest, interaction, failure(result, "Manage Messages"), true, null);
            return;
        }
        respond(rest, interaction, "Deleted **" + ids.size() + "** message" + (ids.size() == 1 ? "" : "s") + ".",
                true, null);
    }

    private static void slowmode(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = channelId(interaction);
        if (channelId == null) {
            respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!can(interaction, MANAGE_CHANNELS)) {
            respond(rest, interaction, "You need the **Manage Channels** permission for that.", true, null);
            return;
        }
        int seconds = Math.max(0, Math.min(21600, optInt(interaction, "seconds", 0)));
        Net.Resp response = rest.patch("/channels/" + channelId,
                new JSONObject().put("rate_limit_per_user", seconds).toString());
        if (response.ok()) {
            respond(rest, interaction, seconds == 0
                    ? "Slowmode is now **off** for this channel."
                    : "Slowmode set to **" + seconds + " second" + (seconds == 1 ? "" : "s") + "**.", true, null);
        } else {
            respond(rest, interaction, failure(response, "Manage Channels"), true, null);
        }
    }

    private static void say(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = channelId(interaction);
        if (channelId == null) {
            respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!can(interaction, MANAGE_MESSAGES)) {
            respond(rest, interaction, "You need the **Manage Messages** permission for that.", true, null);
            return;
        }
        String text = optString(interaction, "text");
        if (text == null || text.trim().isEmpty()) {
            respond(rest, interaction, "Give me something to say.", true, null);
            return;
        }
        JSONObject body = new JSONObject();
        body.put("content", text);
        body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
        Net.Resp response = rest.post("/channels/" + channelId + "/messages", body.toString());
        if (!response.ok()) {
            respond(rest, interaction, failure(response, "Send Messages"), true, null);
            return;
        }
        String messageId = new JSONObject(response.body).optString("id", "");
        String guildId = guildId(interaction);
        String link = guildId != null && !messageId.isEmpty()
                ? "https://discord.com/channels/" + guildId + "/" + channelId + "/" + messageId
                : "";
        respond(rest, interaction, link.isEmpty() ? "Posted." : "Posted → " + link, true, null);
    }

    // -------------------------------------------------------------------- fun

    private static void eightBall(DiscordRest rest, JSONObject interaction) throws Exception {
        String question = orDefault(optString(interaction, "question"), "(no question)");
        String answer = EIGHT_BALL[RANDOM.nextInt(EIGHT_BALL.length)];
        respond(rest, interaction, "**Question:** " + question + "\n**Answer:** " + answer, false, null);
    }

    private static void roll(DiscordRest rest, JSONObject interaction) throws Exception {
        String spec = orDefault(optString(interaction, "dice"), "1d6").trim().toLowerCase();
        int count = 1;
        int sides = 6;
        String[] parts = spec.split("d");
        try {
            if (parts.length == 2) {
                count = Integer.parseInt(parts[0].trim());
                sides = Integer.parseInt(parts[1].trim());
            } else if (parts.length == 1) {
                sides = Integer.parseInt(parts[0].trim());
            }
        } catch (NumberFormatException e) {
            respond(rest, interaction, "Use the format `2d6` (dice + sides).", true, null);
            return;
        }
        count = Math.max(1, Math.min(20, count));
        sides = Math.max(2, Math.min(1000, sides));
        StringBuilder results = new StringBuilder();
        int total = 0;
        for (int i = 0; i < count; i++) {
            int value = RANDOM.nextInt(sides) + 1;
            total += value;
            if (i > 0) {
                results.append(", ");
            }
            results.append(value);
        }
        respond(rest, interaction, "**" + count + "d" + sides + ":** " + results
                + (count > 1 ? "\n**Total:** " + total : ""), false, null);
    }

    // --------------------------------------------------------------- helpers

    private static void respond(DiscordRest rest, JSONObject interaction, String content, boolean ephemeral,
                                JSONArray embeds) {
        try {
            JSONObject data = new JSONObject();
            data.put("content", content == null ? "" : content);
            if (ephemeral) {
                data.put("flags", 64);
            }
            data.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
            if (embeds != null) {
                data.put("embeds", embeds);
            }
            JSONObject body = new JSONObject();
            body.put("type", 4);
            body.put("data", data);
            rest.post("/interactions/" + interaction.optString("id") + "/" + interaction.optString("token")
                    + "/callback", body.toString());
        } catch (Exception e) {
            // nothing useful to do if the interaction token is gone
        }
    }

    private static String failure(Net.Resp response, String permission) {
        if (response.code < 0) {
            return "Network problem while talking to Discord: " + response.body;
        }
        if (response.code == 403) {
            return "Discord refused: give me the **" + permission
                    + "** permission and make sure my role is above the target's role.";
        }
        if (response.code == 404) {
            return "Discord could not find that user, channel or message (404).";
        }
        if (response.code == 401) {
            return "Discord rejected the bot token (401). Save a fresh token and restart.";
        }
        String message = "";
        try {
            message = new JSONObject(response.body).optString("message", "");
        } catch (Exception ignored) {
            message = response.body == null ? "" : response.body;
        }
        return "Discord error " + response.code + (message.isEmpty() ? "" : ": " + message);
    }

    private static JSONObject option(JSONObject interaction, String name) {
        JSONObject data = interaction.optJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONArray options = data.optJSONArray("options");
        if (options == null) {
            return null;
        }
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null && name.equals(option.optString("name"))) {
                return option;
            }
        }
        return null;
    }

    private static String optString(JSONObject interaction, String name) {
        JSONObject option = option(interaction, name);
        if (option == null || !option.has("value")) {
            return null;
        }
        return option.optString("value", null);
    }

    private static int optInt(JSONObject interaction, String name, int fallback) {
        JSONObject option = option(interaction, name);
        if (option == null || !option.has("value")) {
            return fallback;
        }
        return option.optInt("value", fallback);
    }

    private static String displayName(JSONObject interaction, String optionName, String fallbackId) {
        JSONObject option = option(interaction, optionName);
        JSONObject user = option == null ? null : option.optJSONObject("user");
        if (user != null) {
            String globalName = user.optString("global_name", "");
            String username = user.optString("username", "");
            String name = globalName.isEmpty() ? username : globalName;
            if (!name.isEmpty()) {
                return "**" + name + "**";
            }
        }
        return "<@" + fallbackId + ">";
    }

    private static String invokerId(JSONObject interaction) {
        JSONObject member = interaction.optJSONObject("member");
        if (member != null) {
            JSONObject user = member.optJSONObject("user");
            if (user != null) {
                return user.optString("id", null);
            }
        }
        JSONObject user = interaction.optJSONObject("user");
        return user == null ? null : user.optString("id", null);
    }

    private static long invokerPermissions(JSONObject interaction) {
        JSONObject member = interaction.optJSONObject("member");
        if (member == null) {
            return 0L;
        }
        String permissions = member.optString("permissions", "0");
        try {
            return Long.parseLong(permissions);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static boolean can(JSONObject interaction, long permission) {
        long permissions = invokerPermissions(interaction);
        return (permissions & ADMINISTRATOR) != 0 || (permissions & permission) != 0;
    }

    private static String guildId(JSONObject interaction) {
        String value = interaction.has("guild_id") && !interaction.isNull("guild_id")
                ? interaction.optString("guild_id", null) : null;
        return value == null || value.isEmpty() ? null : value;
    }

    private static String channelId(JSONObject interaction) {
        String value = interaction.has("channel_id") && !interaction.isNull("channel_id")
                ? interaction.optString("channel_id", null) : null;
        return value == null || value.isEmpty() ? null : value;
    }

    private static long snowflakeTime(String id) {
        try {
            return (Long.parseLong(id) >> 22) + 1420070400000L;
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    private static String avatarUrl(String userId, String hash) {
        if (hash == null || hash.isEmpty() || "null".equals(hash)) {
            int index = 0;
            try {
                index = (int) ((Long.parseLong(userId) >> 22) % 6);
            } catch (NumberFormatException ignored) {
                // keep the default avatar
            }
            return "https://cdn.discordapp.com/embed/avatars/" + index + ".png";
        }
        String extension = hash.startsWith("a_") ? ".gif" : ".png";
        return "https://cdn.discordapp.com/avatars/" + userId + "/" + hash + extension + "?size=1024";
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return "";
        }
    }
}
