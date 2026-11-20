package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Device status, server inspection, warnings and the newer text tools. */
final class CommandsExtra {

    /** Definitions merged after the built-ins in Commands.DEFINITIONS. */
    static final String DEFINITIONS = "["
            + "{\"name\":\"status\",\"description\":\"Where the bot runs: the phone, battery, network and latency\"},"
            + "{\"name\":\"serverstats\",\"description\":\"Channels, roles, emojis, boosts and features here\"},"
            + "{\"name\":\"channels\",\"description\":\"Every channel in this server, grouped by type\"},"
            + "{\"name\":\"channelperms\",\"description\":\"Permission overwrites in this channel\"},"
            + "{\"name\":\"auditlog\",\"description\":\"Recent entries from the server audit log\","
            + "\"default_member_permissions\":\"128\",\"options\":["
            + "{\"type\":4,\"name\":\"limit\",\"description\":\"How many entries (1-25)\",\"required\":false,"
            + "\"min_value\":1,\"max_value\":25}]},"
            + "{\"name\":\"thread\",\"description\":\"Start a public thread in this channel\","
            + "\"default_member_permissions\":\"17179869184\",\"options\":["
            + "{\"type\":3,\"name\":\"name\",\"description\":\"Thread name\",\"required\":true,\"max_length\":100}]},"
            + "{\"name\":\"warn\",\"description\":\"Warnings kept on the phone\","
            + "\"default_member_permissions\":\"1099511627776\",\"options\":["
            + "{\"type\":1,\"name\":\"add\",\"description\":\"Warn a member\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason\",\"required\":true,\"max_length\":400}]},"
            + "{\"type\":1,\"name\":\"list\",\"description\":\"Warnings for a member\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true}]},"
            + "{\"type\":1,\"name\":\"clear\",\"description\":\"Delete a member's warnings\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true}]}]},"
            + "{\"name\":\"warnings\",\"description\":\"Check warnings for you or someone else\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member (defaults to you)\",\"required\":false}]},"
            + "{\"name\":\"topinvites\",\"description\":\"Invites ranked by how many people used them\","
            + "\"default_member_permissions\":\"32\"},"
            + "{\"name\":\"mock\",\"description\":\"sPoNgEbOb case some text\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to mock\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"rot13\",\"description\":\"Rotate letters by 13\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to rotate\",\"required\":true,\"max_length\":1000}]},"
            + "{\"name\":\"caesar\",\"description\":\"Caesar cipher with any shift\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to shift\",\"required\":true,\"max_length\":1000},"
            + "{\"type\":4,\"name\":\"shift\",\"description\":\"Letters to shift (-25 to 25)\",\"required\":true,"
            + "\"min_value\":-25,\"max_value\":25}]},"
            + "{\"name\":\"sortlines\",\"description\":\"Sort lines alphabetically\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Lines to sort\",\"required\":true,\"max_length\":1500}]},"
            + "{\"name\":\"dedupe\",\"description\":\"Remove duplicate lines\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Lines to clean\",\"required\":true,\"max_length\":1500}]},"
            + "{\"name\":\"hex\",\"description\":\"Text to hex or hex to text\",\"options\":["
            + "{\"type\":3,\"name\":\"mode\",\"description\":\"encode or decode\",\"required\":true,\"choices\":["
            + "{\"name\":\"encode\",\"value\":\"encode\"},{\"name\":\"decode\",\"value\":\"decode\"}]},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to process\",\"required\":true,\"max_length\":1000}]},"
            + "{\"name\":\"charinfo\",\"description\":\"Code points and categories of some text\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to inspect\",\"required\":true,\"max_length\":100}]},"
            + "{\"name\":\"repeat\",\"description\":\"Repeat text a few times\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to repeat\",\"required\":true,\"max_length\":300},"
            + "{\"type\":4,\"name\":\"count\",\"description\":\"How many times (1-10)\",\"required\":true,"
            + "\"min_value\":1,\"max_value\":10}]},"
            + "{\"name\":\"embed\",\"description\":\"Post a custom embed as the bot\","
            + "\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":3,\"name\":\"title\",\"description\":\"Embed title\",\"required\":true,\"max_length\":200},"
            + "{\"type\":3,\"name\":\"description\",\"description\":\"Embed body\",\"required\":true,\"max_length\":1800},"
            + "{\"type\":3,\"name\":\"color\",\"description\":\"Hex colour, for example #5865F2\",\"required\":false,"
            + "\"max_length\":12}]},"
            + "{\"name\":\"weekday\",\"description\":\"Which day of the week a date falls on\",\"options\":["
            + "{\"type\":3,\"name\":\"date\",\"description\":\"YYYY-MM-DD\",\"required\":true,\"max_length\":32}]},"
            + "{\"name\":\"percentage\",\"description\":\"Work out a percentage\",\"options\":["
            + "{\"type\":10,\"name\":\"part\",\"description\":\"The part\",\"required\":true},"
            + "{\"type\":10,\"name\":\"total\",\"description\":\"The whole\",\"required\":true}]}"
            + "]";

    private CommandsExtra() {
    }

    // ------------------------------------------------------------------ status

    static void status(DiscordRest rest, JSONObject interaction) throws Exception {
        DeviceInfo device = DeviceProbe.capture();

        long start = System.currentTimeMillis();
        Net.Resp pingResponse = rest.get("/gateway");
        long latency = System.currentTimeMillis() - start;

        StringBuilder description = new StringBuilder(device.describe());
        description.append("\n\n**Bot**\n");
        description.append("**BotDroid:** ").append(BotState.VERSION).append(" · ")
                .append(BotPresence.describe()).append('\n');
        if (BotState.startedAt > 0) {
            long seconds = Math.max(0L, (System.currentTimeMillis() - BotState.startedAt) / 1000L);
            description.append("**Online:** ").append(DeviceInfo.duration(seconds))
                    .append(" (since <t:").append(BotState.startedAt / 1000L).append(":t>)\n");
        }
        if (pingResponse.ok()) {
            description.append("**API latency:** ").append(latency).append(" ms\n");
        } else {
            description.append("**API latency:** unavailable (HTTP ").append(pingResponse.code).append(")\n");
        }
        description.append("**Servers:** ").append(BotState.guildCount).append('\n');
        int enabled = CommandToggles.enabledCount();
        int custom = CustomCommands.count();
        description.append("**Commands:** ").append(enabled).append(" built-ins on · ").append(custom)
                .append(" custom · ").append(Math.max(0, Commands.MAX_COMMANDS - enabled - custom))
                .append(" of ").append(Commands.MAX_COMMANDS).append(" slots free\n");
        description.append("**Runs on:** this phone, no server or hosting");
        if (device.capturedAt > 0) {
            description.append("\n**Measured:** <t:").append(device.capturedAt / 1000L).append(":R>");
        }

        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Running on " + device.shortName(), 0x23A55A, description.toString())));
    }

    // ------------------------------------------------------------ server tools

    static void serverStats(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp guildResponse = rest.get("/guilds/" + guildId + "?with_counts=true");
        if (!guildResponse.ok()) {
            Commands.respond(rest, interaction, Commands.failure(guildResponse, "View Channels"), true, null);
            return;
        }
        JSONObject guild = new JSONObject(guildResponse.body);
        StringBuilder description = new StringBuilder();
        int members = guild.optInt("approximate_member_count", guild.optInt("member_count", 0));
        int online = guild.optInt("approximate_presence_count", -1);
        description.append("**Members:** ").append(members);
        if (online >= 0) {
            description.append(" · ").append(online).append(" online");
        }
        description.append('\n');

        JSONArray roles = guild.optJSONArray("roles");
        JSONArray emojis = guild.optJSONArray("emojis");
        JSONArray stickers = guild.optJSONArray("stickers");
        description.append("**Roles:** ").append(roles == null ? 0 : roles.length()).append('\n');
        description.append("**Emojis:** ").append(emojis == null ? 0 : emojis.length()).append('\n');
        if (stickers != null && stickers.length() > 0) {
            description.append("**Stickers:** ").append(stickers.length()).append('\n');
        }
        description.append("**Boost tier:** ").append(guild.optInt("premium_tier", 0))
                .append(" · boosts: ").append(guild.optInt("premium_subscription_count", 0)).append('\n');
        description.append("**Verification:** ").append(verificationLevel(guild.optInt("verification_level", 0)))
                .append('\n');
        description.append("**Content filter:** ").append(explicitFilter(guild.optInt("explicit_content_filter", 0)))
                .append('\n');
        description.append("**2FA for moderation:** ")
                .append(guild.optInt("mfa_level", 0) > 0 ? "required" : "not required").append('\n');
        description.append("**Created:** <t:").append(Commands.snowflakeTime(guildId) / 1000L).append(":D>");

        Net.Resp channelsResponse = rest.get("/guilds/" + guildId + "/channels");
        if (channelsResponse.ok()) {
            try {
                JSONArray channels = new JSONArray(channelsResponse.body);
                int text = 0;
                int voice = 0;
                int categories = 0;
                int threads = 0;
                for (int i = 0; i < channels.length(); i++) {
                    JSONObject channel = channels.optJSONObject(i);
                    if (channel == null) {
                        continue;
                    }
                    int type = channel.optInt("type", 0);
                    if (type == 0 || type == 5) {
                        text++;
                    } else if (type == 2 || type == 13) {
                        voice++;
                    } else if (type == 4) {
                        categories++;
                    } else if (type == 15 || type == 16) {
                        threads++;
                    }
                }
                description.append("\n**Channels:** ").append(text).append(" text · ").append(voice)
                        .append(" voice · ").append(categories).append(" categories");
                if (threads > 0) {
                    description.append(" · ").append(threads).append(" forums");
                }
            } catch (Exception ignored) {
                // the guild payload alone is still useful
            }
        }

        JSONArray features = guild.optJSONArray("features");
        if (features != null && features.length() > 0) {
            StringBuilder list = new StringBuilder();
            for (int i = 0; i < features.length() && i < 6; i++) {
                if (list.length() > 0) {
                    list.append(", ");
                }
                list.append(features.optString(i).toLowerCase(Locale.US).replace('_', ' '));
            }
            description.append("\n**Features:** ").append(list);
        }

        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed(guild.optString("name", "Server") + " — stats", 0x5865F2, description.toString())));
    }

    static void channels(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/channels");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONArray channels = new JSONArray(response.body);
        List<String> text = new ArrayList<>();
        List<String> voice = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<String> other = new ArrayList<>();
        for (int i = 0; i < channels.length(); i++) {
            JSONObject channel = channels.optJSONObject(i);
            if (channel == null) {
                continue;
            }
            String name = channel.optString("name", "channel");
            int type = channel.optInt("type", 0);
            if (type == 0 || type == 5) {
                text.add("#" + name);
            } else if (type == 2) {
                voice.add("🔊 " + name);
            } else if (type == 4) {
                categories.add(name);
            } else if (type == 15) {
                other.add("🗂 " + name + " (forum)");
            } else if (type == 13) {
                other.add("🎙 " + name + " (stage)");
            } else {
                other.add(name + " (type " + type + ")");
            }
        }
        StringBuilder description = new StringBuilder();
        appendSection(description, "Text", text);
        appendSection(description, "Voice", voice);
        appendSection(description, "Categories", categories);
        appendSection(description, "Other", other);
        if (description.length() == 0) {
            description.append("No channels I can see.");
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Channels (" + channels.length() + ")", 0x5865F2, description.toString())));
    }

    static void channelPerms(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        String guildId = Commands.guildId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a channel.", true, null);
            return;
        }
        Net.Resp response = rest.get("/channels/" + channelId);
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject channel = new JSONObject(response.body);
        JSONArray overwrites = channel.optJSONArray("permission_overwrites");
        if (overwrites == null || overwrites.length() == 0) {
            Commands.respond(rest, interaction, "This channel has no permission overwrites — it follows "
                    + "the server's roles.", true, null);
            return;
        }
        Map<String, String> roleNames = new LinkedHashMap<>();
        if (guildId != null) {
            Net.Resp rolesResponse = rest.get("/guilds/" + guildId + "/roles");
            if (rolesResponse.ok()) {
                try {
                    JSONArray roles = new JSONArray(rolesResponse.body);
                    for (int i = 0; i < roles.length(); i++) {
                        JSONObject role = roles.optJSONObject(i);
                        if (role != null) {
                            roleNames.put(role.optString("id"), role.optString("name", "role"));
                        }
                    }
                } catch (Exception ignored) {
                    // fall back to ids
                }
            }
        }
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < overwrites.length() && shown < 15; i++) {
            JSONObject overwrite = overwrites.optJSONObject(i);
            if (overwrite == null) {
                continue;
            }
            String id = overwrite.optString("id");
            boolean role = overwrite.optInt("type", 0) == 0;
            String label = role ? "@" + roleNames.getOrDefault(id, id) : "<@" + id + ">";
            description.append("• **").append(label).append("**\n");
            description.append("   allow: ").append(describeBits(overwrite.optString("allow", "0"))).append('\n');
            description.append("   deny: ").append(describeBits(overwrite.optString("deny", "0"))).append('\n');
            shown++;
        }
        if (overwrites.length() > shown) {
            description.append("\n_").append(shown).append(" of ").append(overwrites.length()).append(" shown_");
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("#" + channel.optString("name", "channel"), 0x5865F2, description.toString())));
    }

    static void auditLog(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.VIEW_AUDIT_LOG)) {
            Commands.respond(rest, interaction, "You need the **View Audit Log** permission for that.",
                    true, null);
            return;
        }
        int limit = Math.max(1, Math.min(25, Commands.optInt(interaction, "limit", 10)));
        Net.Resp response = rest.get("/guilds/" + guildId + "/audit-logs?limit=" + limit);
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Audit Log"), true, null);
            return;
        }
        JSONArray entries = new JSONObject(response.body).optJSONArray("audit_log_entries");
        if (entries == null || entries.length() == 0) {
            Commands.respond(rest, interaction, "The audit log is empty.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry == null) {
                continue;
            }
            String action = actionName(entry.optInt("action_type", 0));
            String target = entry.optString("target_id", "");
            String actor = entry.optString("user_id", "");
            String reason = entry.optString("reason", "");
            description.append("• **").append(action).append("**");
            if (!actor.isEmpty()) {
                description.append(" by <@").append(actor).append('>');
            }
            if (!target.isEmpty()) {
                description.append(" · `").append(target).append('`');
            }
            description.append(" · <t:").append(Commands.snowflakeTime(entry.optString("id", "0")) / 1000L)
                    .append(":R>");
            if (!reason.isEmpty()) {
                description.append("\n   _").append(reason.length() > 120 ? reason.substring(0, 120) + "…" : reason)
                        .append('_');
            }
            description.append('\n');
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Audit log", 0x5865F2, description.toString())));
    }

    static void thread(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        String name = Commands.optString(interaction, "name");
        if (name == null || name.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give the thread a name.", true, null);
            return;
        }
        JSONObject body = new JSONObject();
        body.put("name", name.trim());
        body.put("auto_archive_duration", 1440);
        Net.Resp response = rest.post("/channels/" + channelId + "/threads", body.toString());
        if (!response.ok()) {
            Commands.respond(rest, interaction, "Could not create the thread: "
                    + Commands.apiMessage(response.body) + " (HTTP " + response.code + "). "
                    + "I need **Create Public Threads**.", true, null);
            return;
        }
        JSONObject thread = new JSONObject(response.body);
        String guildId = Commands.guildId(interaction);
        String link = guildId == null ? "" : "https://discord.com/channels/" + guildId + "/"
                + thread.optString("id");
        Commands.respond(rest, interaction, "Thread <#" + thread.optString("id") + "> created"
                + (link.isEmpty() ? "." : " → " + link), true, null);
    }

    static void warn(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        String sub = Commands.subcommandName(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MODERATE_MEMBERS)) {
            Commands.respond(rest, interaction, "You need the **Moderate Members** permission for that.",
                    true, null);
            return;
        }
        String userId = Commands.optString(interaction, "user");
        if (userId == null) {
            Commands.respond(rest, interaction, "Pick a member.", true, null);
            return;
        }
        if ("add".equals(sub)) {
            String reason = Commands.orDefault(Commands.optString(interaction, "reason"), "No reason provided");
            if (!Warnings.add(guildId, userId, Commands.invokerId(interaction), reason)) {
                Commands.respond(rest, interaction, "The warning list on the phone is full (" 
                        + Warnings.MAX_ENTRIES + " entries). Clear some first.", true, null);
                return;
            }
            int total = Warnings.forMember(guildId, userId).size();
            Commands.respond(rest, interaction, "Warned " + Commands.displayName(interaction, "user", userId)
                    + " — they now have **" + total + "** warning" + (total == 1 ? "" : "s") + " here.\n"
                    + "**Reason:** " + reason + "\n_Warnings are stored on the phone, not on Discord._",
                    false, null);
            return;
        }
        if ("list".equals(sub)) {
            sendWarnings(rest, interaction, guildId, userId);
            return;
        }
        if ("clear".equals(sub)) {
            int removed = Warnings.clear(guildId, userId);
            Commands.respond(rest, interaction, removed == 0
                    ? "That member has no warnings."
                    : "Deleted **" + removed + "** warning" + (removed == 1 ? "" : "s") + " for "
                    + Commands.displayName(interaction, "user", userId) + ".", true, null);
            return;
        }
        Commands.respond(rest, interaction, "Use `/warn add`, `/warn list` or `/warn clear`.", true, null);
    }

    static void warnings(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        String userId = Commands.optString(interaction, "user");
        if (userId == null) {
            userId = Commands.invokerId(interaction);
        }
        if (userId == null) {
            Commands.respond(rest, interaction, "I could not work out which member to check.", true, null);
            return;
        }
        sendWarnings(rest, interaction, guildId, userId);
    }

    private static void sendWarnings(DiscordRest rest, JSONObject interaction, String guildId, String userId)
            throws Exception {
        List<Warnings.Entry> entries = Warnings.forMember(guildId, userId);
        if (entries.isEmpty()) {
            Commands.respond(rest, interaction, "<@" + userId + "> has no warnings in this server.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        description.append("**Member:** <@").append(userId).append(">\n");
        description.append("**Total:** ").append(entries.size()).append('\n');
        int shown = 0;
        for (Warnings.Entry entry : entries) {
            if (shown >= 10) {
                break;
            }
            description.append("\n**").append(shown + 1).append(".** ").append(entry.reason)
                    .append("\n   by <@").append(entry.byId).append("> · <t:")
                    .append(entry.at / 1000L).append(":R>");
            shown++;
        }
        if (entries.size() > shown) {
            description.append("\n\n_Showing the first ").append(shown).append("._");
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Warnings", 0xE67E22, description.toString())));
    }

    static void topInvites(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_GUILD)) {
            Commands.respond(rest, interaction, "You need the **Manage Server** permission for that.",
                    true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/invites");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Manage Server"), true, null);
            return;
        }
        JSONArray invites = new JSONArray(response.body);
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < invites.length(); i++) {
            JSONObject invite = invites.optJSONObject(i);
            if (invite != null) {
                list.add(invite);
            }
        }
        if (list.isEmpty()) {
            Commands.respond(rest, interaction, "There are no active invites in this server.", true, null);
            return;
        }
        Collections.sort(list, (left, right) -> Integer.compare(right.optInt("uses", 0), left.optInt("uses", 0)));
        StringBuilder description = new StringBuilder();
        int total = 0;
        for (JSONObject invite : list) {
            total += invite.optInt("uses", 0);
        }
        description.append("**Total joins through invites:** ").append(total).append('\n');
        int shown = 0;
        for (JSONObject invite : list) {
            if (shown >= 12) {
                break;
            }
            JSONObject inviter = invite.optJSONObject("inviter");
            description.append("\n**").append(invite.optInt("uses", 0)).append("** · `")
                    .append(invite.optString("code", "")).append('`');
            if (inviter != null) {
                description.append(" by ").append(inviter.optString("username", "someone"));
            }
            String expires = invite.optString("expires_at", "");
            if (!expires.isEmpty() && !"null".equals(expires)) {
                description.append(" · expires ").append(expires);
            }
            shown++;
        }
        if (list.size() > shown) {
            description.append("\n\n_").append(shown).append(" of ").append(list.size()).append(" invites_");
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Invite leaderboard", 0x5865F2, description.toString())));
    }

    // -------------------------------------------------------------- text tools

    static void mock(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text to mock.", true, null);
            return;
        }
        StringBuilder out = new StringBuilder();
        int index = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                out.append(index % 2 == 0 ? Character.toLowerCase(c) : Character.toUpperCase(c));
                index++;
            } else {
                out.append(c);
            }
        }
        Commands.respond(rest, interaction, out.toString(), false, null);
    }

    static void rot13(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text.", true, null);
            return;
        }
        Commands.respond(rest, interaction, shift(text, 13), false, null);
    }

    static void caesar(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text.", true, null);
            return;
        }
        int shift = Math.max(-25, Math.min(25, Commands.optInt(interaction, "shift", 3)));
        Commands.respond(rest, interaction, "**Shift " + shift + "**\n" + shift(text, shift), false, null);
    }

    static void sortLines(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me some lines to sort.", true, null);
            return;
        }
        String[] lines = text.split("\n", -1);
        List<String> sorted = new ArrayList<>();
        Collections.addAll(sorted, lines);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        StringBuilder out = new StringBuilder();
        for (String line : sorted) {
            out.append(line).append('\n');
        }
        Commands.respond(rest, interaction, "```\n" + trimBlock(out.toString()) + "\n```", true, null);
    }

    static void dedupe(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me some lines to clean.", true, null);
            return;
        }
        List<String> seen = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (!seen.contains(line)) {
                seen.add(line);
                out.append(line).append('\n');
            }
        }
        Commands.respond(rest, interaction, "```\n" + trimBlock(out.toString()) + "\n```", true, null);
    }

    static void hex(DiscordRest rest, JSONObject interaction) throws Exception {
        String mode = Commands.optString(interaction, "mode");
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text to work with.", true, null);
            return;
        }
        if ("decode".equalsIgnoreCase(mode)) {
            String cleaned = text.replaceAll("\\s+", "");
            if (!cleaned.matches("[0-9a-fA-F]*") || cleaned.length() % 2 != 0) {
                Commands.respond(rest, interaction, "That is not valid hex (pairs of 0-9a-f).", true, null);
                return;
            }
            byte[] bytes = new byte[cleaned.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(cleaned.substring(i * 2, i * 2 + 2), 16);
            }
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            Commands.respond(rest, interaction, "```\n" + clip(decoded, 1500) + "\n```", true, null);
            return;
        }
        StringBuilder out = new StringBuilder();
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            out.append(String.format(Locale.US, "%02x", b & 0xFF));
        }
        Commands.respond(rest, interaction, "```\n" + clip(out.toString(), 1500) + "\n```", true, null);
    }

    static void charInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text to inspect.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (int offset = 0; offset < text.length() && shown < 20; ) {
            int codePoint = text.codePointAt(offset);
            int size = Character.charCount(codePoint);
            description.append("`").append(new String(Character.toChars(codePoint))).append("` — ")
                    .append("U+").append(String.format(Locale.US, "%04X", codePoint))
                    .append(" · decimal ").append(codePoint)
                    .append(" · ").append(categoryName(Character.getType(codePoint)))
                    .append(" · UTF-8 ").append(new String(Character.toChars(codePoint))
                            .getBytes(StandardCharsets.UTF_8).length).append(" bytes\n");
            offset += size;
            shown++;
        }
        if (text.codePointCount(0, text.length()) > shown) {
            description.append("\n_Showing the first ").append(shown).append(" characters._");
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Characters", 0x5865F2, description.toString())));
    }

    static void repeat(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text to repeat.", true, null);
            return;
        }
        int count = Math.max(1, Math.min(10, Commands.optInt(interaction, "count", 2)));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(text);
        }
        Commands.respond(rest, interaction, clip(out.toString(), 1900), false, null);
    }

    static void embed(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        String title = Commands.optString(interaction, "title");
        String text = Commands.optString(interaction, "description");
        String color = Commands.optString(interaction, "color");
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_MESSAGES)) {
            Commands.respond(rest, interaction, "You need the **Manage Messages** permission for that.",
                    true, null);
            return;
        }
        if (title == null || title.trim().isEmpty() || text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "A title and a body are both needed.", true, null);
            return;
        }
        int rgb = 0x5865F2;
        if (color != null && !color.trim().isEmpty()) {
            String cleaned = color.trim().replace("#", "").replace("0x", "");
            if (!cleaned.matches("[0-9a-fA-F]{6}")) {
                Commands.respond(rest, interaction, "That colour is not a 6 digit hex value.", true, null);
                return;
            }
            rgb = Integer.parseInt(cleaned, 16);
        }
        JSONObject embed = new JSONObject();
        embed.put("title", title.trim());
        embed.put("description", text.trim());
        embed.put("color", rgb);
        JSONObject body = new JSONObject();
        body.put("embeds", new JSONArray().put(embed));
        body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
        Net.Resp response = rest.post("/channels/" + channelId + "/messages", body.toString());
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Send Messages"), true, null);
            return;
        }
        Commands.respond(rest, interaction, "Embed posted in <#" + channelId + ">.", true, null);
    }

    static void weekday(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "date");
        if (raw == null || raw.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me a date like `2030-01-01`.", true, null);
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw.trim());
        } catch (Exception e) {
            Commands.respond(rest, interaction, "`" + raw.trim() + "` is not a date. Use YYYY-MM-DD.", true, null);
            return;
        }
        long epoch = date.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        String day = date.getDayOfWeek().name();
        day = day.charAt(0) + day.substring(1).toLowerCase(Locale.US);
        String description = "**" + date + "** is a **" + day + "**\n"
                + "Discord time: <t:" + epoch + ":D> · <t:" + epoch + ":R>\n"
                + "Days from today: " + (date.toEpochDay() - LocalDate.now(ZoneOffset.UTC).toEpochDay());
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Weekday", 0x5865F2, description)));
    }

    static void percentage(DiscordRest rest, JSONObject interaction) throws Exception {
        double part = numberOption(interaction, "part");
        double total = numberOption(interaction, "total");
        if (total == 0) {
            Commands.respond(rest, interaction, "The total cannot be zero.", true, null);
            return;
        }
        double percent = part / total * 100.0;
        Commands.respond(rest, interaction, "**" + CommandsUtil.format(part) + "** of **"
                + CommandsUtil.format(total) + "** is **" + String.format(Locale.US, "%.2f%%", percent) + "**",
                true, null);
    }

    // ----------------------------------------------------------------- helpers

    private static double numberOption(JSONObject interaction, String name) {
        JSONObject option = Commands.option(interaction, name);
        if (option == null) {
            return 0;
        }
        return option.optDouble("value", 0);
    }

    private static void appendSection(StringBuilder builder, String title, List<String> items) {
        if (items.isEmpty()) {
            return;
        }
        builder.append("**").append(title).append("** (").append(items.size()).append(")\n");
        int shown = 0;
        for (String item : items) {
            if (shown >= 14) {
                builder.append("…\n");
                break;
            }
            builder.append(item).append(' ');
            shown++;
        }
        builder.append("\n\n");
    }

    private static String describeBits(String raw) {
        long bits;
        try {
            bits = Long.parseLong(raw);
        } catch (Exception e) {
            return "none";
        }
        List<String> names = Commands.permissionNames(bits);
        if (names.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size() && i < 6; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(names.get(i));
        }
        if (names.size() > 6) {
            builder.append(" +").append(names.size() - 6);
        }
        return builder.toString();
    }

    static String shift(String text, int amount) {
        StringBuilder out = new StringBuilder();
        int shift = ((amount % 26) + 26) % 26;
        for (char c : text.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                out.append((char) ('a' + (c - 'a' + shift) % 26));
            } else if (c >= 'A' && c <= 'Z') {
                out.append((char) ('A' + (c - 'A' + shift) % 26));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String categoryName(int type) {
        switch (type) {
            case Character.UPPERCASE_LETTER:
                return "uppercase letter";
            case Character.LOWERCASE_LETTER:
                return "lowercase letter";
            case Character.TITLECASE_LETTER:
                return "titlecase letter";
            case Character.MODIFIER_LETTER:
                return "modifier letter";
            case Character.OTHER_LETTER:
                return "letter";
            case Character.DECIMAL_DIGIT_NUMBER:
                return "digit";
            case Character.LETTER_NUMBER:
            case Character.OTHER_NUMBER:
                return "number";
            case Character.SPACE_SEPARATOR:
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR:
                return "separator";
            case Character.CONTROL:
                return "control";
            case Character.FORMAT:
                return "format";
            case Character.UNASSIGNED:
                return "unassigned";
            case Character.PRIVATE_USE:
                return "private use";
            case Character.SURROGATE:
                return "surrogate";
            case Character.DASH_PUNCTUATION:
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.CONNECTOR_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
                return "punctuation";
            case Character.MATH_SYMBOL:
            case Character.CURRENCY_SYMBOL:
            case Character.MODIFIER_SYMBOL:
            case Character.OTHER_SYMBOL:
                return "symbol";
            case Character.NON_SPACING_MARK:
            case Character.ENCLOSING_MARK:
            case Character.COMBINING_SPACING_MARK:
                return "mark";
            default:
                return "type " + type;
        }
    }

    private static String verificationLevel(int level) {
        switch (level) {
            case 0:
                return "none";
            case 1:
                return "low (verified email)";
            case 2:
                return "medium (5 minutes on Discord)";
            case 3:
                return "high (10 minutes in server)";
            case 4:
                return "highest (verified phone)";
            default:
                return "level " + level;
        }
    }

    private static String explicitFilter(int level) {
        switch (level) {
            case 0:
                return "disabled";
            case 1:
                return "members without roles";
            case 2:
                return "all members";
            default:
                return "level " + level;
        }
    }

    private static String actionName(int type) {
        switch (type) {
            case 1:
                return "Server updated";
            case 10:
                return "Channel created";
            case 11:
                return "Channel updated";
            case 12:
                return "Channel deleted";
            case 13:
                return "Channel permissions changed";
            case 20:
                return "Member kicked";
            case 22:
                return "Member banned";
            case 23:
                return "Member unbanned";
            case 24:
                return "Member updated";
            case 25:
                return "Member roles updated";
            case 26:
                return "Member moved";
            case 27:
                return "Member disconnected";
            case 28:
                return "Bot added";
            case 30:
                return "Role created";
            case 31:
                return "Role updated";
            case 32:
                return "Role deleted";
            case 50:
                return "Invite created";
            case 51:
                return "Invite deleted";
            case 60:
                return "Webhook created";
            case 61:
                return "Webhook updated";
            case 62:
                return "Webhook deleted";
            case 72:
                return "Message deleted";
            case 73:
                return "Messages bulk deleted";
            case 74:
                return "Message pinned";
            case 75:
                return "Message unpinned";
            case 80:
                return "Emoji created";
            case 81:
                return "Emoji updated";
            case 82:
                return "Emoji deleted";
            case 90:
                return "Integration created";
            case 91:
                return "Integration updated";
            case 92:
                return "Integration deleted";
            case 110:
                return "Stage created";
            case 112:
                return "Stage deleted";
            case 130:
                return "Sticker created";
            case 131:
                return "Sticker updated";
            case 132:
                return "Sticker deleted";
            default:
                return "Action " + type;
        }
    }

    static String clip(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit) + "…";
    }

    private static String trimBlock(String value) {
        String trimmed = value;
        while (trimmed.endsWith("\n")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return clip(trimmed, 1500);
    }
}
