package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Utility commands: bot info, server details, maths, polls, reminders and helpers. */
final class CommandsUtil {

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "botdroid-reminders");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    private CommandsUtil() {
    }

    static void botInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        StringBuilder description = new StringBuilder();
        description.append("**Version:** ").append(BotState.VERSION).append("\n");
        description.append("**Commands:** ").append(Commands.presetCount() + CustomCommands.count())
                .append(" (").append(CustomCommands.count()).append(" custom)\n");
        description.append("**Servers:** ").append(BotState.guildCount).append("\n");
        if (BotState.startedAt > 0) {
            description.append("**Running for:** <t:").append(BotState.startedAt / 1000).append(":R>\n");
        }
        description.append("**Status:** ").append(BotPresence.describe()).append("\n");
        description.append("**Engine:** raw gateway WebSocket, no third-party libraries");
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("BotDroid " + BotState.VERSION, 0x5865F2, description.toString())));
    }

    static void uptime(DiscordRest rest, JSONObject interaction) throws Exception {
        if (BotState.startedAt <= 0) {
            Commands.respond(rest, interaction, "The bot is still starting up.", true, null);
            return;
        }
        long seconds = Math.max(0, (System.currentTimeMillis() - BotState.startedAt) / 1000);
        String description = "**Uptime:** <t:" + (BotState.startedAt / 1000) + ":R>\n"
                + "**Started:** <t:" + (BotState.startedAt / 1000) + ":f>\n"
                + "**Seconds:** " + seconds;
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Uptime", 0x23A55A, description)));
    }

    static void memberCount(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "?with_counts=true");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject guild = new JSONObject(response.body);
        int members = guild.optInt("approximate_member_count", guild.optInt("member_count", 0));
        int online = guild.optInt("approximate_presence_count", -1);
        String description = "**Members:** " + members
                + (online >= 0 ? "\n**Online:** " + online : "");
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed(guild.optString("name", "Server"), 0x5865F2, description)));
    }

    static void roles(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/roles");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONArray roles = new JSONArray(response.body);
        List<JSONObject> sorted = new ArrayList<>();
        for (int i = 0; i < roles.length(); i++) {
            JSONObject role = roles.optJSONObject(i);
            if (role != null) {
                sorted.add(role);
            }
        }
        sorted.sort((left, right) -> Integer.compare(right.optInt("position", 0), left.optInt("position", 0)));
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (JSONObject role : sorted) {
            if (shown >= 25) {
                break;
            }
            String name = role.optString("name", "role");
            if ("@everyone".equals(name)) {
                continue;
            }
            description.append("• <@&").append(role.optString("id")).append("> — `")
                    .append(String.format(Locale.US, "#%06X", role.optInt("color", 0) & 0xFFFFFF)).append("`\n");
            shown++;
        }
        if (shown == 0) {
            description.append("No roles other than @everyone.");
        } else if (sorted.size() > shown) {
            description.append("\n_Showing ").append(shown).append(" of ").append(sorted.size()).append("_");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Roles (" + roles.length() + ")", 0x5865F2, description.toString())));
    }

    static void channelInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
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
        StringBuilder description = new StringBuilder();
        description.append("**ID:** `").append(channelId).append("`\n");
        description.append("**Type:** ").append(channelType(channel.optInt("type", 0))).append("\n");
        description.append("**Created:** <t:").append(Commands.snowflakeTime(channelId) / 1000).append(":R>\n");
        description.append("**Slowmode:** ").append(channel.optInt("rate_limit_per_user", 0)).append("s\n");
        description.append("**NSFW:** ").append(channel.optBoolean("nsfw", false) ? "yes" : "no");
        String topic = channel.optString("topic", "");
        if (!topic.isEmpty()) {
            description.append("\n**Topic:** ").append(topic.length() > 200 ? topic.substring(0, 200) + "…" : topic);
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("#" + channel.optString("name", "channel"), 0x5865F2,
                        description.toString())));
    }

    static void emojis(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "/emojis");
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "View Channels"), true, null);
            return;
        }
        JSONArray emojis = new JSONArray(response.body);
        if (emojis.length() == 0) {
            Commands.respond(rest, interaction, "This server has no custom emojis.", true, null);
            return;
        }
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < emojis.length() && shown < 25; i++) {
            JSONObject emoji = emojis.optJSONObject(i);
            if (emoji == null) {
                continue;
            }
            String name = emoji.optString("name", "emoji");
            String id = emoji.optString("id", "");
            description.append(emoji.optBoolean("animated", false)
                    ? "<a:" + name + ":" + id + "> "
                    : "<:" + name + ":" + id + "> ");
            shown++;
        }
        if (emojis.length() > shown) {
            description.append("\n_Showing ").append(shown).append(" of ").append(emojis.length()).append("_");
        }
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Emojis (" + emojis.length() + ")", 0x5865F2,
                        description.toString())));
    }

    static void invite(DiscordRest rest, JSONObject interaction) throws Exception {
        if (BotState.applicationId == null || BotState.applicationId.isEmpty()) {
            Commands.respond(rest, interaction, "The bot has not finished connecting yet, try again in a moment.",
                    true, null);
            return;
        }
        long permissions = 1024L + 2048L + 16384L + 65536L + 8192L + 16L + 2L + 4L + 64L
                + 134217728L + 268435456L + 1099511627776L;
        String url = "https://discord.com/api/oauth2/authorize?client_id=" + BotState.applicationId
                + "&permissions=" + permissions + "&scope=bot%20applications.commands";
        Commands.respond(rest, interaction, "**Invite me to another server:**\n" + url, true, null);
    }

    static void time(DiscordRest rest, JSONObject interaction) throws Exception {
        long now = System.currentTimeMillis();
        String description = "**Discord time:** <t:" + (now / 1000) + ":F>\n"
                + "**Unix:** `" + (now / 1000) + "`\n"
                + "**UTC:** " + OffsetDateTime.now(ZoneOffset.UTC).format(UTC);
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("Current time", 0x5865F2, description)));
    }

    static void math(DiscordRest rest, JSONObject interaction) throws Exception {
        String expression = Commands.optString(interaction, "expression");
        if (expression == null || expression.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me an expression, for example `(2+3)*4`.", true, null);
            return;
        }
        Double value = evaluate(expression);
        if (value == null) {
            Commands.respond(rest, interaction, "I could not evaluate that. Use numbers, `+ - * / % ^` and brackets.",
                    true, null);
            return;
        }
        Commands.respond(rest, interaction, "`" + expression.trim() + "` = **" + format(value) + "**", true, null);
    }

    static void random(DiscordRest rest, JSONObject interaction) throws Exception {
        int min = Commands.optInt(interaction, "min", 1);
        int max = Commands.optInt(interaction, "max", 100);
        if (min > max) {
            int swap = min;
            min = max;
            max = swap;
        }
        long span = (long) max - (long) min + 1L;
        if (span <= 0 || span > 1000000000L) {
            Commands.respond(rest, interaction, "Pick a smaller range (under a billion numbers).", true, null);
            return;
        }
        long value = min + (long) (RANDOM.nextDouble() * span);
        Commands.respond(rest, interaction, "🎲 **" + value + "** (between " + min + " and " + max + ")", false, null);
    }

    static void choose(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "options");
        if (raw == null || raw.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me options separated by `|`, for example `pizza | sushi`.", true,
                    null);
            return;
        }
        List<String> choices = new ArrayList<>();
        for (String part : raw.split("\\|")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                choices.add(trimmed);
            }
        }
        if (choices.isEmpty()) {
            Commands.respond(rest, interaction, "I need at least one option.", true, null);
            return;
        }
        Commands.respond(rest, interaction, "I choose **" + choices.get(RANDOM.nextInt(choices.size())) + "**", false,
                null);
    }

    static void poll(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = Commands.channelId(interaction);
        if (channelId == null) {
            Commands.respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        String question = Commands.optString(interaction, "question");
        if (question == null || question.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give the poll a question.", true, null);
            return;
        }
        String[] emojis = {"1️⃣", "2️⃣", "3️⃣", "4️⃣"};
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String option = Commands.optString(interaction, "option" + i);
            if (option != null && !option.trim().isEmpty()) {
                options.add(option.trim());
            }
        }
        if (options.size() < 2) {
            Commands.respond(rest, interaction, "A poll needs at least two options.", true, null);
            return;
        }
        StringBuilder content = new StringBuilder("**").append(question.trim()).append("**\n");
        for (int i = 0; i < options.size(); i++) {
            content.append(emojis[i]).append(' ').append(options.get(i)).append('\n');
        }
        JSONObject body = new JSONObject();
        body.put("content", content.toString());
        body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
        Net.Resp response = rest.post("/channels/" + channelId + "/messages", body.toString());
        if (!response.ok()) {
            Commands.respond(rest, interaction, Commands.failure(response, "Send Messages"), true, null);
            return;
        }
        String messageId = new JSONObject(response.body).optString("id", "");
        for (int i = 0; i < options.size() && !messageId.isEmpty(); i++) {
            rest.put("/channels/" + channelId + "/messages/" + messageId + "/reactions/"
                    + Commands.encode(emojis[i]) + "/@me", null);
        }
        String guildId = Commands.guildId(interaction);
        String link = guildId == null ? "" : "https://discord.com/channels/" + guildId + "/" + channelId + "/" + messageId;
        Commands.respond(rest, interaction, link.isEmpty() ? "Poll created." : "Poll created → " + link, true, null);
    }

    static void remind(DiscordRest rest, final JSONObject interaction) throws Exception {
        final String channelId = Commands.channelId(interaction);
        final String userId = Commands.invokerId(interaction);
        if (channelId == null || userId == null) {
            Commands.respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        int minutes = Math.max(1, Math.min(10080, Commands.optInt(interaction, "minutes", 5)));
        String text = Commands.optString(interaction, "text");
        if (text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "What should I remind you about?", true, null);
            return;
        }
        final String message = text.trim();
        SCHEDULER.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject body = new JSONObject();
                    body.put("content", "<@" + userId + "> reminder: " + message);
                    body.put("allowed_mentions", new JSONObject()
                            .put("parse", new JSONArray())
                            .put("users", new JSONArray().put(userId)));
                    rest.post("/channels/" + channelId + "/messages", body.toString());
                } catch (Exception ignored) {
                    // the channel may be gone by the time the reminder fires
                }
            }
        }, minutes, TimeUnit.MINUTES);
        Commands.respond(rest, interaction, "Okay, I will remind you in **" + minutes + " minute"
                + (minutes == 1 ? "" : "s") + "** (keep the bot running).", true, null);
    }

    static void base64(DiscordRest rest, JSONObject interaction) throws Exception {
        String mode = Commands.optString(interaction, "mode");
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text to work with.", true, null);
            return;
        }
        try {
            String result;
            if ("decode".equalsIgnoreCase(mode)) {
                result = new String(Base64.getDecoder().decode(text.trim()), StandardCharsets.UTF_8);
            } else {
                result = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            }
            if (result.length() > 1500) {
                result = result.substring(0, 1500) + "…";
            }
            Commands.respond(rest, interaction, "```\n" + result + "\n```", true, null);
        } catch (IllegalArgumentException e) {
            Commands.respond(rest, interaction, "That is not valid base64.", true, null);
        }
    }

    static void color(DiscordRest rest, JSONObject interaction) throws Exception {
        String raw = Commands.optString(interaction, "hex");
        if (raw == null) {
            Commands.respond(rest, interaction, "Give me a hex colour like `#5865F2`.", true, null);
            return;
        }
        String hex = raw.trim().replace("#", "").replace("0x", "");
        if (!hex.matches("[0-9a-fA-F]{6}")) {
            Commands.respond(rest, interaction, "That is not a 6 digit hex colour (for example `#5865F2`).", true, null);
            return;
        }
        int rgb = Integer.parseInt(hex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        String description = "**HEX:** `#" + hex.toUpperCase(Locale.US) + "`\n"
                + "**RGB:** `" + r + ", " + g + ", " + b + "`\n"
                + "**Decimal:** `" + rgb + "`";
        Commands.respond(rest, interaction, "", true,
                new JSONArray().put(Commands.embed("#" + hex.toUpperCase(Locale.US), rgb, description)));
    }

    // ------------------------------------------------------------------ maths

    /** Small recursive descent evaluator: + - * / % ^ and brackets. */
    static Double evaluate(String expression) {
        if (expression == null) {
            return null;
        }
        String cleaned = expression.replace(" ", "");
        if (cleaned.isEmpty() || cleaned.length() > 120) {
            return null;
        }
        try {
            Parser parser = new Parser(cleaned);
            double result = parser.parseExpression();
            if (parser.pos != cleaned.length() || Double.isNaN(result) || Double.isInfinite(result)) {
                return null;
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9 && Math.abs(value) < 1e15) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format(Locale.US, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        double parseExpression() {
            double value = parseTerm();
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '+') {
                    pos++;
                    value += parseTerm();
                } else if (c == '-') {
                    pos++;
                    value -= parseTerm();
                } else {
                    break;
                }
            }
            return value;
        }

        private double parseTerm() {
            double value = parseFactor();
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '*') {
                    pos++;
                    value *= parseFactor();
                } else if (c == '/') {
                    pos++;
                    double divisor = parseFactor();
                    if (divisor == 0) {
                        throw new ArithmeticException("division by zero");
                    }
                    value /= divisor;
                } else if (c == '%') {
                    pos++;
                    double divisor = parseFactor();
                    if (divisor == 0) {
                        throw new ArithmeticException("division by zero");
                    }
                    value %= divisor;
                } else {
                    break;
                }
            }
            return value;
        }

        private double parseFactor() {
            double base = parseUnary();
            if (pos < text.length() && text.charAt(pos) == '^') {
                pos++;
                return Math.pow(base, parseFactor());
            }
            return base;
        }

        private double parseUnary() {
            if (pos < text.length() && text.charAt(pos) == '-') {
                pos++;
                return -parseUnary();
            }
            if (pos < text.length() && text.charAt(pos) == '+') {
                pos++;
                return parseUnary();
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            if (pos >= text.length()) {
                throw new IllegalArgumentException("unexpected end");
            }
            char c = text.charAt(pos);
            if (c == '(') {
                pos++;
                double value = parseExpression();
                if (pos >= text.length() || text.charAt(pos) != ')') {
                    throw new IllegalArgumentException("missing bracket");
                }
                pos++;
                return value;
            }
            int start = pos;
            while (pos < text.length()) {
                char current = text.charAt(pos);
                if ((current >= '0' && current <= '9') || current == '.') {
                    pos++;
                } else {
                    break;
                }
            }
            if (start == pos) {
                throw new IllegalArgumentException("expected number");
            }
            return Double.parseDouble(text.substring(start, pos));
        }
    }

    private static String channelType(int type) {
        switch (type) {
            case 0:
                return "text";
            case 2:
                return "voice";
            case 4:
                return "category";
            case 5:
                return "announcement";
            case 13:
                return "stage";
            case 15:
                return "forum";
            default:
                return "type " + type;
        }
    }
}
