package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Server inspection, invites, embeds and text tooling commands. */
final class CommandsMore {

    private static final String[][] LENGTH_UNITS = {
            {"mm", "0.001"}, {"cm", "0.01"}, {"m", "1"}, {"km", "1000"},
            {"in", "0.0254"}, {"ft", "0.3048"}, {"yd", "0.9144"}, {"mi", "1609.344"}
    };

    private static final String[][] MASS_UNITS = {
            {"mg", "0.000001"}, {"g", "0.001"}, {"kg", "1"}, {"oz", "0.028349523"}, {"lb", "0.45359237"}
    };

    private static final String[] MORSE_LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
    };

    private static final String[] MORSE_CODES = {
            ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---", "-.-", ".-..", "--", "-.",
            "---", ".--.", "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-", "-.--", "--..",
            "-----", ".----", "..---", "...--", "....-", ".....", "-....", "--...", "---..", "----."
    };

    private CommandsMore() {
    }

    static void joined(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        if (guildId == null || userId == null) {
            Commands.respond(rest, interaction, "Pick a member in this server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/members/" + userId);
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject member = new JSONObject(response.body);
        String joinedAt = member.optString("joined_at", "");
        long accountCreated = Commands.snowflakeTime(userId);
        StringBuilder description = new StringBuilder();
        description.append("**User:** ").append(Commands.displayName(interaction, "user", userId)).append('\n');
        long joinedEpoch = epochSeconds(joinedAt);
        description.append("**Joined server:** ")
                .append(joinedEpoch > 0 ? "<t:" + joinedEpoch + ":F> (<t:" + joinedEpoch + ":R>)" : "unknown")
                .append('\n');
        description.append("**Account created:** <t:").append(accountCreated / 1000L).append(":R>\n");
        if (joinedEpoch > 0) {
            long days = Math.max(0L, (System.currentTimeMillis() / 1000L - joinedEpoch) / 86400L);
            description.append("**Days in server:** ").append(days);
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Member since", 0x5865F2, description.toString())));
    }

    static void memberRoles(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        if (guildId == null || userId == null) {
            Commands.respond(rest, interaction, "Pick a member in this server.", true, null);
            return;
        }
        Net.Resp memberResponse = rest.get("/guilds/" + guildId + "/members/" + userId);
        if (!memberResponse.ok()) {
            Commands.respond(rest, interaction, Commands.failure(memberResponse, "View Channels"), true, null);
            return;
        }
        Net.Resp rolesResponse = rest.get("/guilds/" + guildId + "/roles");
        if (!rolesResponse.ok()) {
            Commands.respond(rest, interaction, Commands.failure(rolesResponse, "View Channels"), true, null);
            return;
        }
        JSONObject member = new JSONObject(memberResponse.body);
        JSONArray memberRoles = member.optJSONArray("roles");
        JSONArray guildRoles = new JSONArray(rolesResponse.body);
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < guildRoles.length() && shown < 30; i++) {
            JSONObject role = guildRoles.optJSONObject(i);
            if (role == null || !contains(memberRoles, role.optString("id"))) {
                continue;
            }
            description.append("• <@&").append(role.optString("id")).append(">\n");
            shown++;
        }
        if (description.length() == 0) {
            description.append("Only the default @everyone role.");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Roles of " + memberDisplay(member), 0x5865F2,
                        description.toString())));
    }

    static void userPermissions(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String userId = Commands.optString(interaction, "user");
        if (guildId == null || userId == null) {
            Commands.respond(rest, interaction, "Pick a member in this server.", true, null);
            return;
        }
        Net.Resp memberResponse = rest.get("/guilds/" + guildId + "/members/" + userId);
        Net.Resp rolesResponse = rest.get("/guilds/" + guildId + "/roles");
        if (!memberResponse.ok() || !rolesResponse.ok()) {
            Commands.respond(rest, interaction, Commands.failure(memberResponse, "View Channels"), true, null);
            return;
        }
        JSONObject member = new JSONObject(memberResponse.body);
        JSONArray memberRoles = member.optJSONArray("roles");
        JSONArray guildRoles = new JSONArray(rolesResponse.body);
        long bits = 0L;
        for (int i = 0; i < guildRoles.length(); i++) {
            JSONObject role = guildRoles.optJSONObject(i);
            if (role == null) {
                continue;
            }
            String roleId = role.optString("id");
            boolean everyone = guildId.equals(roleId);
            if (everyone || contains(memberRoles, roleId)) {
                bits |= parseLong(role.optString("permissions", "0"));
            }
        }
        StringBuilder description = new StringBuilder();
        if ((bits & Commands.ADMINISTRATOR) != 0) {
            description.append("**Administrator** — every permission.\n\n");
        }
        java.util.List<String> names = Commands.permissionNames(bits);
        for (String name : names) {
            description.append("• ").append(name).append('\n');
        }
        if (description.length() == 0) {
            description.append("No notable permissions.");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Permissions of " + memberDisplay(member), 0x5865F2,
                        description.toString())));
    }

    static void createInvite(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a channel.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.CREATE_INVITE)) {
            Commands.respond(rest, interaction, "You need the **Create Invite** permission for that.", true, null);
            return;
        }
        int maxAge = Math.max(0, Math.min(604800, Commands.optInt(interaction, "max_age", 86400)));
        int maxUses = Math.max(0, Math.min(100, Commands.optInt(interaction, "max_uses", 0)));
        JSONObject body = new JSONObject();
        body.put("max_age", maxAge);
        body.put("max_uses", maxUses);
        body.put("unique", true);
        Net.Resp response = rest.post("/channels/" + channelId + "/invites", body.toString());
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Create Invite"), true, null);
            return;
        }
        JSONObject invite = new JSONObject(response.body);
        String code = invite.optString("code", "");
        Commands.respond(rest, interaction, "https://discord.gg/" + code + "\n_Expires: "
                + (maxAge == 0 ? "never" : "in " + maxAge + "s") + " · Uses: "
                + (maxUses == 0 ? "unlimited" : String.valueOf(maxUses)) + "_", true, null);
    }

    static void invites(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_GUILD)) {
            Commands.respond(rest, interaction, "You need the **Manage Server** permission for that.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/invites");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Server"), true, null);
            return;
        }
        JSONArray invites = new JSONArray(response.body);
        if (invites.length() == 0) {
            Commands.respond(rest, interaction, "This server has no active invites.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < invites.length() && shown < 15; i++) {
            JSONObject invite = invites.optJSONObject(i);
            if (invite == null) {
                continue;
            }
            JSONObject inviter = invite.optJSONObject("inviter");
            JSONObject channel = invite.optJSONObject("channel");
            description.append("• `").append(invite.optString("code", "?"))
                    .append("` → #").append(channel == null ? "unknown" : channel.optString("name", "unknown"))
                    .append(" · ").append(invite.optInt("uses", 0)).append(" uses")
                    .append(inviter == null ? "" : " · by " + inviter.optString("username", "unknown"))
                    .append('\n');
            shown++;
        }
        if (invites.length() > shown) {
            description.append("_").append(shown).append(" of ").append(invites.length()).append("_");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Active invites", 0x5865F2, description.toString())));
    }

    static void webhooks(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a channel.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_WEBHOOKS)) {
            Commands.respond(rest, interaction, "You need the **Manage Webhooks** permission for that.", true, null);
            return;
        }
        Net.Resp response = rest.get("/channels/" + channelId + "/webhooks");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Webhooks"), true, null);
            return;
        }
        JSONArray hooks = new JSONArray(response.body);
        if (hooks.length() == 0) {
            Commands.respond(rest, interaction, "This channel has no webhooks.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < hooks.length() && i < 15; i++) {
            JSONObject hook = hooks.optJSONObject(i);
            if (hook == null) {
                continue;
            }
            description.append("• **").append(hook.optString("name", "unnamed")).append("** `")
                    .append(hook.optString("id", "?")).append("` type ").append(hook.optInt("type", 0))
                    .append('\n');
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Webhooks", 0x5865F2, description.toString())));
    }

    static void threads(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a channel.", true, null);
            return;
        }
        Net.Resp response = rest.get("/channels/" + channelId + "/threads/active");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Read Message History"), true, null);
            return;
        }
        JSONArray threads = new JSONObject(response.body).optJSONArray("threads");
        if (threads == null || threads.length() == 0) {
            Commands.respond(rest, interaction, "No active threads here.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < threads.length() && i < 15; i++) {
            JSONObject thread = threads.optJSONObject(i);
            if (thread == null) {
                continue;
            }
            description.append("• **").append(thread.optString("name", "thread")).append("** — ")
                    .append(thread.optInt("message_count", 0)).append(" messages\n");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Active threads", 0x5865F2, description.toString())));
    }

    static void events(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/scheduled-events?with_user_count=true");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONArray events = new JSONArray(response.body);
        if (events.length() == 0) {
            Commands.respond(rest, interaction, "No scheduled events in this server.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < events.length() && i < 15; i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) {
                continue;
            }
            long start = epochSeconds(event.optString("scheduled_start_time", ""));
            description.append("• **").append(event.optString("name", "event")).append("** — ")
                    .append(start > 0 ? "<t:" + start + ":F> (<t:" + start + ":R>)" : "no time")
                    .append(" · ").append(event.optInt("user_count", 0)).append(" interested\n");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Scheduled events", 0x5865F2, description.toString())));
    }

    static void stickers(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/stickers");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONArray stickers = new JSONArray(response.body);
        if (stickers.length() == 0) {
            Commands.respond(rest, interaction, "This server has no custom stickers.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < stickers.length() && i < 20; i++) {
            JSONObject sticker = stickers.optJSONObject(i);
            if (sticker == null) {
                continue;
            }
            description.append("• **").append(sticker.optString("name", "sticker")).append("** — ")
                    .append(sticker.optString("tags", "")).append('\n');
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Stickers", 0x5865F2, description.toString())));
    }

    static void emojiInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String raw = Commands.optString(interaction, "emoji");
        if (guildId == null || raw == null) {
            Commands.respond(rest, interaction, "Run this in a server and give me an emoji.", true, null);
            return;
        }
        String name = raw;
        String id = "";
        Matcher matcher = Pattern.compile("<a?:([a-zA-Z0-9_]+):(\\d+)>").matcher(raw);
        if (matcher.find()) {
            name = matcher.group(1);
            id = matcher.group(2);
        } else if (raw.matches("\\d+")) {
            id = raw;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/emojis");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONArray emojis = new JSONArray(response.body);
        JSONObject found = null;
        for (int i = 0; i < emojis.length(); i++) {
            JSONObject emoji = emojis.optJSONObject(i);
            if (emoji == null) {
                continue;
            }
            if ((!id.isEmpty() && id.equals(emoji.optString("id")))
                    || (id.isEmpty() && name.equalsIgnoreCase(emoji.optString("name")))) {
                found = emoji;
                break;
            }
        }
        if (found == null) {
            Commands.respond(rest, interaction, "No such emoji in this server.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        description.append("**Name:** :").append(found.optString("name", "?")).append(":\n");
        description.append("**ID:** `").append(found.optString("id", "?")).append("`\n");
        description.append("**Animated:** ").append(found.optBoolean("animated", false) ? "yes" : "no").append('\n');
        description.append("**Managed:** ").append(found.optBoolean("managed", false) ? "yes" : "no").append('\n');
        description.append("**Available:** ").append(found.optBoolean("available", true) ? "yes" : "no");
        JSONObject embed = Commands.embed("Emoji " + found.optString("name", ""), 0x5865F2,
                description.toString());
        embed.put("thumbnail", new JSONObject().put("url",
                "https://cdn.discordapp.com/emojis/" + found.optString("id")
                        + (found.optBoolean("animated", false) ? ".gif" : ".png")));
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    static void announce(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.optString(interaction, "channel");
        String text = Commands.optString(interaction, "text");
        if (channelId == null || text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Pick a channel and give me the message.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_MESSAGES)) {
            Commands.respond(rest, interaction, "You need the **Manage Messages** permission for that.", true, null);
            return;
        }
        JSONObject embed = new JSONObject();
        embed.put("description", text.trim().length() > 2000 ? text.trim().substring(0, 2000) : text.trim());
        embed.put("color", 0x5865F2);
        JSONObject body = new JSONObject();
        body.put("embeds", new JSONArray().put(embed));
        body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
        Net.Resp response = rest.post("/channels/" + channelId + "/messages", body.toString());
        if (response.ok()) {
            Commands.respond(rest, interaction, "Announced in <#" + channelId + ">.", true, null);
        } else {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Messages"), true, null);
        }
    }

    static void dm(DiscordRest rest, JSONObject interaction) throws Exception {
        String userId = Commands.optString(interaction, "user");
        String text = Commands.optString(interaction, "text");
        if (userId == null || text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Pick a user and write something to send.", true, null);
            return;
        }
        JSONObject channelBody = new JSONObject();
        channelBody.put("recipient_id", userId);
        Net.Resp channelResponse = rest.post("/users/@me/channels", channelBody.toString());
        if (!channelResponse.ok()) {
            Commands.respond(rest, interaction,
                    "Discord would not open a DM with that user (they may have DMs closed).", true, null);
            return;
        }
        String dmChannel = new JSONObject(channelResponse.body).optString("id", "");
        JSONObject message = new JSONObject();
        message.put("content", text.trim());
        Net.Resp sent = rest.post("/channels/" + dmChannel + "/messages", message.toString());
        Commands.respond(rest, interaction, sent.ok() ? "Message sent to "
                + Commands.displayName(interaction, "user", userId) + "." : "Discord refused to send that DM.",
                true, null);
    }

    static void timezone(DiscordRest rest, JSONObject interaction) throws Exception {
        String zone = Commands.optString(interaction, "zone");
        if (zone == null) {
            Commands.respond(rest, interaction, "Give me a time zone like `Europe/London`.", true, null);
            return;
        }
        try {
            ZoneId zoneId = ZoneId.of(zone.trim());
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            String content = "**" + zoneId.getId() + "**\n"
                    + now.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", Locale.US))
                    + "\nOffset UTC" + now.getOffset().getId().replace("Z", "+00:00")
                    + "\nUnix: `" + now.toEpochSecond() + "`";
            Commands.respond(rest, interaction, content, true, null);
        } catch (Exception e) {
            Commands.respond(rest, interaction, "`" + zone + "` is not a time zone (try `Europe/London`).",
                    true, null);
        }
    }

    static void countdown(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "date");
        if (raw == null) {
            Commands.respond(rest, interaction, "Give me a date like `2030-01-01`.", true, null);
            return;
        }
        try {
            ZonedDateTime target;
            if (raw.trim().length() <= 10) {
                target = LocalDate.parse(raw.trim()).atStartOfDay(ZoneOffset.UTC);
            } else {
                target = LocalDateTime.parse(raw.trim()).atZone(ZoneOffset.UTC);
            }
            long epoch = target.toEpochSecond();
            long seconds = epoch - System.currentTimeMillis() / 1000L;
            String direction = seconds >= 0 ? "in" : "ago";
            long absolute = Math.abs(seconds);
            Commands.respond(rest, interaction, "**" + raw.trim() + "** is <t:" + epoch + ":F> (<t:" + epoch
                    + ":R>)\nThat is " + (absolute / 86400L) + " days " + ((absolute % 86400L) / 3600L)
                    + " hours " + direction + ".", true, null);
        } catch (Exception e) {
            Commands.respond(rest, interaction, "I could not read that date. Use `2030-01-01` or "
                    + "`2030-01-01T18:30`.", true, null);
        }
    }

    static void commandCount(DiscordRest rest, JSONObject interaction) throws Exception {
        int presets = Commands.presetCount();
        int enabled = CommandToggles.enabledCount();
        int custom = CustomCommands.count();
        int used = enabled + custom;
        StringBuilder description = new StringBuilder();
        description.append("**Built-in commands:** ").append(enabled).append(" of ").append(presets)
                .append(" enabled\n");
        description.append("**Custom commands:** ").append(custom).append('\n');
        description.append("**Slots used:** ").append(used).append(" of ").append(Commands.MAX_COMMANDS).append('\n');
        description.append("**Free slots:** ").append(Math.max(0, Commands.MAX_COMMANDS - used)).append("\n\n");
        description.append("Discord allows ").append(Commands.MAX_COMMANDS)
                .append(" commands per server. Turn built-ins on or off in BotDroid → Settings → Commands.");
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Command slots", 0x5865F2, description.toString())));
    }

    static void serverBanner(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId);
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject guild = new JSONObject(response.body);
        String banner = guild.optString("banner", "");
        if (banner.isEmpty() || "null".equals(banner)) {
            Commands.respond(rest, interaction, "This server has no banner.", true, null);
            return;
        }
        String extension = banner.startsWith("a_") ? ".gif" : ".png";
        JSONObject embed = Commands.embed(guild.optString("name", "Server") + " banner", 0x5865F2, null);
        embed.put("image", new JSONObject().put("url",
                "https://cdn.discordapp.com/banners/" + guildId + "/" + banner + extension + "?size=1024"));
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    static void regex(DiscordRest rest, JSONObject interaction) throws Exception {
        String pattern = Commands.optString(interaction, "pattern");
        String text = Commands.optString(interaction, "text");
        if (pattern == null || text == null) {
            Commands.respond(rest, interaction, "Give me a pattern and some text.", true, null);
            return;
        }
        try {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (!matcher.find()) {
                Commands.respond(rest, interaction, "No match.", true, null);
                return;
            }
            StringBuilder result = new StringBuilder("**Match:** `" + matcher.group() + "`\n");
            for (int i = 1; i <= matcher.groupCount() && i <= 5; i++) {
                result.append("**Group ").append(i).append(":** `").append(matcher.group(i)).append("`\n");
            }
            result.append("**Range:** ").append(matcher.start()).append('-').append(matcher.end());
            Commands.respond(rest, interaction, result.toString(), true, null);
        } catch (Exception e) {
            Commands.respond(rest, interaction, "That pattern is not valid: " + e.getMessage(), true, null);
        }
    }

    static void binary(DiscordRest rest, JSONObject interaction) throws Exception {
        String mode = Commands.optString(interaction, "mode");
        String text = Commands.optString(interaction, "text");
        if (mode == null || text == null) {
            Commands.respond(rest, interaction, "Give me a mode and some text.", true, null);
            return;
        }
        if ("decode".equalsIgnoreCase(mode)) {
            try {
                StringBuilder decoded = new StringBuilder();
                for (String chunk : text.trim().split("\\s+")) {
                    int code = Integer.parseInt(chunk, 2);
                    if (code < 0 || code > Character.MAX_CODE_POINT) {
                        throw new NumberFormatException("out of range");
                    }
                    decoded.appendCodePoint(code);
                }
                Commands.respond(rest, interaction, "```\n" + decoded + "\n```", true, null);
            } catch (Exception e) {
                Commands.respond(rest, interaction, "That is not binary text.", true, null);
            }
            return;
        }
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) {
                encoded.append(' ');
            }
            encoded.append(String.format(Locale.US, "%8s", Integer.toBinaryString(text.charAt(i)))
                    .replace(' ', '0'));
        }
        Commands.respond(rest, interaction, "```\n" + encoded + "\n```", true, null);
    }

    static void morse(DiscordRest rest, JSONObject interaction) throws Exception {
        String mode = Commands.optString(interaction, "mode");
        String text = Commands.optString(interaction, "text");
        if (mode == null || text == null) {
            Commands.respond(rest, interaction, "Give me a mode and some text.", true, null);
            return;
        }
        if ("decode".equalsIgnoreCase(mode)) {
            StringBuilder decoded = new StringBuilder();
            String[] words = text.trim().split("\\s*/\\s*");
            for (int w = 0; w < words.length; w++) {
                if (w > 0) {
                    decoded.append(' ');
                }
                for (String code : words[w].trim().split("\\s+")) {
                    int index = indexOfCode(code);
                    decoded.append(index < 0 ? '?' : MORSE_LETTERS[index].charAt(0));
                }
            }
            Commands.respond(rest, interaction, "```\n" + decoded + "\n```", true, null);
            return;
        }
        StringBuilder encoded = new StringBuilder();
        String upper = text.toUpperCase(Locale.US);
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (c == ' ') {
                encoded.append(" / ");
                continue;
            }
            int index = indexOfLetter(c);
            if (index >= 0) {
                encoded.append(MORSE_CODES[index]).append(' ');
            }
        }
        Commands.respond(rest, interaction, "```\n" + encoded.toString().trim() + "\n```", true, null);
    }

    static void convert(DiscordRest rest, JSONObject interaction) throws Exception {
        JSONObject valueOption = Commands.option(interaction, "value");
        String from = Commands.optString(interaction, "from");
        String to = Commands.optString(interaction, "to");
        if (valueOption == null || from == null || to == null) {
            Commands.respond(rest, interaction,
                    "Give me a value, a source unit and a target unit (m, km, mi, ft, in, kg, lb, c, f, k).",
                    true, null);
            return;
        }
        double value = valueOption.optDouble("value", Double.NaN);
        if (Double.isNaN(value)) {
            Commands.respond(rest, interaction, "That value is not a number.", true, null);
            return;
        }
        String source = from.trim().toLowerCase(Locale.US);
        String target = to.trim().toLowerCase(Locale.US);
        Double result = convertValue(value, source, target);
        if (result == null) {
            Commands.respond(rest, interaction, "I cannot convert from `" + source + "` to `" + target + "`.",
                    true, null);
            return;
        }
        Commands.respond(rest, interaction, String.format(Locale.US, "**%s %s** = **%.4f %s**", trim(value),
                source, result, target), true, null);
    }

    private static Double convertValue(double value, String from, String to) {
        Double fromLength = factor(LENGTH_UNITS, from);
        Double toLength = factor(LENGTH_UNITS, to);
        if (fromLength != null && toLength != null) {
            return value * fromLength / toLength;
        }
        Double fromMass = factor(MASS_UNITS, from);
        Double toMass = factor(MASS_UNITS, to);
        if (fromMass != null && toMass != null) {
            return value * fromMass / toMass;
        }
        if (isTemperature(from) && isTemperature(to)) {
            double celsius = toCelsius(value, from);
            return fromCelsius(celsius, to);
        }
        return null;
    }

    private static boolean isTemperature(String unit) {
        return "c".equals(unit) || "f".equals(unit) || "k".equals(unit);
    }

    private static double toCelsius(double value, String unit) {
        if ("f".equals(unit)) {
            return (value - 32d) * 5d / 9d;
        }
        if ("k".equals(unit)) {
            return value - 273.15d;
        }
        return value;
    }

    private static double fromCelsius(double value, String unit) {
        if ("f".equals(unit)) {
            return value * 9d / 5d + 32d;
        }
        if ("k".equals(unit)) {
            return value + 273.15d;
        }
        return value;
    }

    private static Double factor(String[][] table, String unit) {
        for (String[] row : table) {
            if (row[0].equals(unit)) {
                return Double.valueOf(row[1]);
            }
        }
        return null;
    }

    private static int indexOfCode(String code) {
        for (int i = 0; i < MORSE_CODES.length; i++) {
            if (MORSE_CODES[i].equals(code)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfLetter(char letter) {
        for (int i = 0; i < MORSE_LETTERS.length; i++) {
            if (MORSE_LETTERS[i].charAt(0) == letter) {
                return i;
            }
        }
        return -1;
    }

    private static boolean contains(JSONArray array, String value) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String memberDisplay(JSONObject member) {
        JSONObject user = member.optJSONObject("user");
        if (user == null) {
            return "member";
        }
        String global = user.optString("global_name", "");
        return global.isEmpty() ? user.optString("username", "member") : global;
    }

    private static long epochSeconds(String iso) {
        if (iso == null || iso.isEmpty()) {
            return -1L;
        }
        try {
            return java.time.Instant.parse(iso).getEpochSecond();
        } catch (Exception e) {
            return -1L;
        }
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.4f", value);
    }
}
