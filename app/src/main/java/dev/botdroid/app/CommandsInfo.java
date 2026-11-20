package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/** Extra information and utility commands. */
final class CommandsInfo {

    private static final Random RANDOM = new Random();
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*-_=+";

    private CommandsInfo() {
    }

    static void about(DiscordRest rest, JSONObject interaction) throws Exception {
        StringBuilder description = new StringBuilder();
        description.append("A Discord bot that runs entirely on an Android phone.\n\n");
        description.append("**Version:** ").append(BotState.VERSION).append("\n");
        description.append("**Commands:** ").append(Commands.presetCount() + CustomCommands.count()).append("\n");
        description.append("**Servers:** ").append(BotState.guildCount).append("\n");
        description.append("**Source:** https://github.com/itriedcoding/BotDroid");
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("About BotDroid", 0x5865F2, description.toString())));
    }

    static void serverIcon(DiscordRest rest, JSONObject interaction) throws Exception {
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
        String icon = guild.optString("icon", "");
        if (icon.isEmpty() || "null".equals(icon)) {
            Commands.respond(rest, interaction, "This server has no icon.", true, null);
            return;
        }
        String extension = icon.startsWith("a_") ? ".gif" : ".png";
        JSONObject embed = Commands.embed(guild.optString("name", "Server icon"), 0x5865F2, null);
        embed.put("image", new JSONObject().put("url",
                "https://cdn.discordapp.com/icons/" + guildId + "/" + icon + extension + "?size=1024"));
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    static void banner(DiscordRest rest, JSONObject interaction) throws Exception {
        String userId = Commands.optString(interaction, "user");
        if (userId == null) {
            userId = Commands.invokerId(interaction);
        }
        if (userId == null) {
            Commands.respond(rest, interaction, "I could not work out which user to look up.", true, null);
            return;
        }
        Net.Resp response = rest.get("/users/" + userId);
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject user = new JSONObject(response.body);
        String hash = user.optString("banner", "");
        if (hash.isEmpty() || "null".equals(hash)) {
            Commands.respond(rest, interaction, "That user has no profile banner.", true, null);
            return;
        }
        String extension = hash.startsWith("a_") ? ".gif" : ".png";
        JSONObject embed = Commands.embed(user.optString("username", "User") + "'s banner", 0x5865F2, null);
        embed.put("image", new JSONObject().put("url",
                "https://cdn.discordapp.com/banners/" + userId + "/" + hash + extension + "?size=1024"));
        Commands.respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    static void timestamp(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "when");
        if (raw == null) {
            Commands.respond(rest, interaction, "Give me `now`, a unix timestamp or a number of seconds.", true, null);
            return;
        }
        long seconds;
        try {
            String value = raw.trim().toLowerCase(Locale.US);
            if ("now".equals(value)) {
                seconds = System.currentTimeMillis() / 1000L;
            } else {
                long parsed = Long.parseLong(value);
                seconds = parsed > 100000000000L ? parsed / 1000L : parsed;
            }
        } catch (NumberFormatException e) {
            Commands.respond(rest, interaction, "That is not a timestamp. Use `now` or unix seconds.", true, null);
            return;
        }
        String content = "**Full:** <t:" + seconds + ":F>\n"
                + "**Relative:** <t:" + seconds + ":R>\n"
                + "**Time:** <t:" + seconds + ":t>\n"
                + "**Short:** <t:" + seconds + ":d>\n"
                + "**Unix:** `" + seconds + "`";
        Commands.respond(rest, interaction, content, true, null);
    }

    static void password(DiscordRest rest, JSONObject interaction) throws Exception {
        int length = Math.max(8, Math.min(64, Commands.optInt(interaction, "length", 16)));
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        Commands.respond(rest, interaction, "```\n" + password + "\n```", true, null);
    }

    static void uuid(DiscordRest rest, JSONObject interaction) throws Exception {
        Commands.respond(rest, interaction, "`" + UUID.randomUUID() + "`", true, null);
    }

    static void hash(DiscordRest rest, JSONObject interaction) throws Exception {
        String algorithm = Commands.orDefault(Commands.optString(interaction, "algorithm"), "sha256");
        String text = Commands.optString(interaction, "text");
        if (text == null) {
            Commands.respond(rest, interaction, "Give me text to hash.", true, null);
            return;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm.toUpperCase(Locale.US));
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format(Locale.US, "%02x", value));
            }
            Commands.respond(rest, interaction, "**" + algorithm.toLowerCase(Locale.US) + "**\n```\n" + hex + "\n```",
                    true, null);
        } catch (Exception e) {
            Commands.respond(rest, interaction, "Unsupported hash algorithm.", true, null);
        }
    }

    static void url(DiscordRest rest, JSONObject interaction) throws Exception {
        String mode = Commands.optString(interaction, "mode");
        String text = Commands.optString(interaction, "text");
        if (text == null) {
            Commands.respond(rest, interaction, "Give me some text.", true, null);
            return;
        }
        try {
            String result;
            if ("decode".equalsIgnoreCase(mode)) {
                result = URLDecoder.decode(text, "UTF-8");
            } else {
                result = URLEncoder.encode(text, "UTF-8").replace("+", "%20");
            }
            if (result.length() > 1500) {
                result = result.substring(0, 1500) + "…";
            }
            Commands.respond(rest, interaction, "```\n" + result + "\n```", true, null);
        } catch (Exception e) {
            Commands.respond(rest, interaction, "That is not valid percent-encoding.", true, null);
        }
    }

    static void textCase(DiscordRest rest, JSONObject interaction) throws Exception {
        String mode = Commands.optString(interaction, "mode");
        String text = Commands.optString(interaction, "text");
        if (text == null) {
            Commands.respond(rest, interaction, "Give me some text.", true, null);
            return;
        }
        String result;
        if ("upper".equalsIgnoreCase(mode)) {
            result = text.toUpperCase(Locale.US);
        } else if ("lower".equalsIgnoreCase(mode)) {
            result = text.toLowerCase(Locale.US);
        } else if ("swap".equalsIgnoreCase(mode)) {
            StringBuilder swapped = new StringBuilder(text.length());
            for (char c : text.toCharArray()) {
                swapped.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
            }
            result = swapped.toString();
        } else {
            StringBuilder title = new StringBuilder(text.length());
            boolean start = true;
            for (char c : text.toCharArray()) {
                title.append(start ? Character.toUpperCase(c) : Character.toLowerCase(c));
                start = Character.isWhitespace(c);
            }
            result = title.toString();
        }
        Commands.respond(rest, interaction, result, true, null);
    }

    static void wordCount(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null) {
            Commands.respond(rest, interaction, "Give me some text.", true, null);
            return;
        }
        int characters = text.length();
        int withoutSpaces = text.replaceAll("\\s", "").length();
        String trimmed = text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int lines = text.split("\n", -1).length;
        Commands.respond(rest, interaction, "**Characters:** " + characters + "\n**Without spaces:** " + withoutSpaces
                + "\n**Words:** " + words + "\n**Lines:** " + lines, true, null);
    }

    static void snowflake(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "id");
        if (raw == null) {
            Commands.respond(rest, interaction, "Give me a Discord ID.", true, null);
            return;
        }
        long id;
        try {
            id = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            Commands.respond(rest, interaction, "That is not a Discord snowflake ID.", true, null);
            return;
        }
        long millis = (id >> 22) + 1420070400000L;
        long seconds = millis / 1000L;
        Commands.respond(rest, interaction, "**Created:** <t:" + seconds + ":F> (<t:" + seconds + ":R>)\n"
                + "**Milliseconds:** `" + millis + "`", true, null);
    }

    static void myPermissions(DiscordRest rest, JSONObject interaction) throws Exception {
        long bits = Commands.invokerPermissions(interaction);
        if (bits == 0) {
            Commands.respond(rest, interaction, "No permissions were included in this interaction.", true, null);
            return;
        }
        List<String> names = Commands.permissionNames(bits);
        StringBuilder description = new StringBuilder();
        for (String name : names) {
            description.append("• ").append(name).append('\n');
        }
        if (description.length() == 0) {
            description.append("No notable permissions.");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Your permissions", 0x5865F2, description.toString())));
    }

    static void pins(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        Net.Resp response = rest.get("/channels/" + channelId + "/pins");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Read Message History"), true, null);
            return;
        }
        JSONArray pins = new JSONArray(response.body);
        if (pins.length() == 0) {
            Commands.respond(rest, interaction, "No pinned messages in this channel.", true, null);
            return;
        }
        String guildId = Commands.guildId(interaction);
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < pins.length() && shown < 10; i++) {
            JSONObject message = pins.optJSONObject(i);
            if (message == null) {
                continue;
            }
            JSONObject author = message.optJSONObject("author");
            String content = message.optString("content", "");
            if (content.length() > 80) {
                content = content.substring(0, 80) + "…";
            }
            if (content.isEmpty()) {
                content = "[embed or attachment]";
            }
            String link = guildId == null ? ""
                    : "https://discord.com/channels/" + guildId + "/" + channelId + "/" + message.optString("id");
            description.append("• ").append(author == null ? "unknown" : author.optString("username", "unknown"))
                    .append(": ").append(content);
            if (!link.isEmpty()) {
                description.append(" [jump](").append(link).append(')');
            }
            description.append('\n');
            shown++;
        }
        if (pins.length() > shown) {
            description.append("_").append(shown).append(" of ").append(pins.length()).append("_");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Pinned messages", 0x5865F2, description.toString())));
    }

    static void inviteInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "code");
        if (raw == null || raw.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me an invite code or link.", true, null);
            return;
        }
        String code = raw.trim();
        if (code.contains("/")) {
            code = code.substring(code.lastIndexOf('/') + 1);
        }
        int query = code.indexOf('?');
        if (query >= 0) {
            code = code.substring(0, query);
        }
        Net.Resp response = rest.get("/invites/" + Commands.encode(code) + "?with_counts=true&with_expiration=true");
        if (!response.ok()) {
            Commands.respond(rest, interaction, "That invite is invalid or expired.", true, null);
            return;
        }
        JSONObject invite = new JSONObject(response.body);
        JSONObject guild = invite.optJSONObject("guild");
        JSONObject channel = invite.optJSONObject("channel");
        JSONObject inviter = invite.optJSONObject("inviter");
        StringBuilder description = new StringBuilder();
        description.append("**Code:** `").append(code).append("`\n");
        if (guild != null) {
            description.append("**Server:** ").append(guild.optString("name", "unknown")).append("\n");
        }
        if (channel != null) {
            description.append("**Channel:** #").append(channel.optString("name", "unknown")).append("\n");
        }
        if (inviter != null) {
            description.append("**Created by:** ").append(inviter.optString("username", "unknown")).append("\n");
        }
        if (guild != null) {
            description.append("**Members:** ").append(guild.optInt("approximate_member_count", 0)).append("\n");
            description.append("**Online:** ").append(guild.optInt("approximate_presence_count", 0)).append("\n");
        }
        description.append("**Uses:** ").append(invite.optInt("uses", 0)).append("\n");
        long expires = invite.optLong("expires_at", 0L);
        description.append("**Expires:** ").append(expires > 0
                ? "<t:" + (expires / 1000L) + ":R>" : "never");
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Invite information", 0x5865F2, description.toString())));
    }
}
