package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TestNew {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        CustomCommands.load("[]");
        CustomCommands.setSaver(json -> { });
        BotState.robloxEnabled = true;
        BotState.applicationId = "999";
        BotState.startedAt = System.currentTimeMillis() - 120000;
        BotState.guildCount = 3;

        definitions();
        maths();
        info();
        utility();
        moderation();
        fun();
        custom();
        roblox();
        presence();
        commandsPlus();
        commandsExtra();
        toggles();
        System.out.println();
        System.out.println("new: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void definitions() throws Exception {
        JSONArray defs = Commands.definitions();
        check("100 preset commands", defs.length() == 100, "count=" + defs.length());
        StringBuilder problems = new StringBuilder();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < defs.length(); i++) {
            JSONObject command = defs.getJSONObject(i);
            String name = command.optString("name", "");
            if (!names.add(name)) {
                problems.append("duplicate:").append(name).append(';');
            }
            if (command.optString("description", "").isEmpty()) {
                problems.append("desc:").append(name).append(';');
            }
            if (command.has("default_member_permissions")) {
                try {
                    Long.parseLong(command.getString("default_member_permissions"));
                } catch (Exception e) {
                    problems.append("perms:").append(name).append(';');
                }
            }
            JSONArray options = command.optJSONArray("options");
            if (options != null) {
                validateOptions(options, name, problems);
            }
        }
        check("definitions valid", problems.length() == 0, problems.toString());
        check("roblox subcommands", subNames(defs, "roblox").toString()
                .equals("[user, avatar, game, group, games]"), subNames(defs, "roblox").toString());
        check("custom subcommands", subNames(defs, "custom").toString().equals("[add, remove, list, clear]"),
                subNames(defs, "custom").toString());
    }

    private static void validateOptions(JSONArray options, String command, StringBuilder problems) throws Exception {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            int type = option.optInt("type", -1);
            if (type != 1 && type != 3 && type != 4 && type != 6 && type != 7 && type != 8 && type != 10) {
                problems.append("type:").append(command).append(';');
            }
            if (option.optString("name", "").isEmpty()) {
                problems.append("oname:").append(command).append(';');
            }
            if (option.optString("description", "").isEmpty()) {
                problems.append("odesc:").append(command).append(';');
            }
            if (option.has("max_length") && option.optInt("max_length") > 2000) {
                problems.append("maxlen:").append(command).append(';');
            }
            JSONArray choices = option.optJSONArray("choices");
            if (choices != null) {
                for (int k = 0; k < choices.length(); k++) {
                    JSONObject choice = choices.getJSONObject(k);
                    if (choice.optString("name", "").isEmpty() || choice.optString("value", "").isEmpty()) {
                        problems.append("choice:").append(command).append(';');
                    }
                }
            }
            JSONArray nested = option.optJSONArray("options");
            if (nested != null) {
                validateOptions(nested, command + "/" + option.optString("name"), problems);
            }
        }
    }

    private static List<String> subNames(JSONArray defs, String command) throws Exception {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < defs.length(); i++) {
            JSONObject entry = defs.getJSONObject(i);
            if (!command.equals(entry.optString("name"))) {
                continue;
            }
            JSONArray options = entry.optJSONArray("options");
            if (options == null) {
                continue;
            }
            for (int k = 0; k < options.length(); k++) {
                names.add(options.getJSONObject(k).optString("name"));
            }
        }
        return names;
    }

    private static void maths() {
        check("math 2+2*2", "6".equals(eval("2+2*2")), eval("2+2*2"));
        check("math brackets", "25".equals(eval("(2+3)^2")), eval("(2+3)^2"));
        check("math decimals", "2.5".equals(eval("10/4")), eval("10/4"));
        check("math negative", "-3".equals(eval("-5+2")), eval("-5+2"));
        check("math modulo", "1".equals(eval("7%3")), eval("7%3"));
        check("math divide by zero", CommandsUtil.evaluate("1/0") == null, "should reject");
        check("math garbage", CommandsUtil.evaluate("abc") == null, "should reject");
        check("math unbalanced", CommandsUtil.evaluate("(1+2") == null, "should reject");
    }

    private static String eval(String expression) {
        Double value = CommandsUtil.evaluate(expression);
        return value == null ? "error" : CommandsUtil.format(value);
    }

    private static void info() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("botinfo", "G", "C", 0));
        check("botinfo embed", contains(embedDescription(rest), BotState.VERSION), embedDescription(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("uptime", "G", "C", 0));
        check("uptime embed", contains(embedDescription(rest), "Uptime"), embedDescription(rest));
    }

    private static void utility() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && p.startsWith("/guilds/G?with_counts=true")
                ? new Net.Resp(200, "{\"name\":\"Server\",\"approximate_member_count\":42}") : null;
        Commands.handle(rest, interaction("membercount", "G", "C", 0));
        check("membercount", contains(embedDescription(rest), "42"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && p.equals("/guilds/G/roles")
                ? new Net.Resp(200, "[{\"id\":\"55\",\"name\":\"Admin\",\"position\":2,\"color\":5793266}]") : null;
        Commands.handle(rest, interaction("roles", "G", "C", 0));
        check("roles mention", contains(embedDescription(rest), "<@&55>"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && p.equals("/channels/C")
                ? new Net.Resp(200, "{\"name\":\"general\",\"type\":0,\"rate_limit_per_user\":5}") : null;
        Commands.handle(rest, interaction("channelinfo", "G", "C", 0));
        check("channelinfo slowmode", contains(embedDescription(rest), "Slowmode"), embedDescription(rest));
        check("channelinfo title", "#general".equals(embedTitle(rest)), embedTitle(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && p.equals("/guilds/G/emojis")
                ? new Net.Resp(200, "[{\"id\":\"123\",\"name\":\"party\",\"animated\":false}]") : null;
        Commands.handle(rest, interaction("emojis", "G", "C", 0));
        check("emojis mention", contains(embedDescription(rest), ":party:"), embedDescription(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("invite", "G", "C", 0));
        check("invite link", contains(content(rest), "client_id=999"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("time", "G", "C", 0));
        check("time embed", "Current time".equals(embedTitle(rest)), embedTitle(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("random", "G", "C", 0, intOption("min", 5), intOption("max", 5)));
        check("random single value", contains(content(rest), "**5**"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("choose", "G", "C", 0, stringOption("options", "pizza | sushi")));
        check("choose replies", contains(content(rest), "I choose"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "POST".equals(m) && "/channels/C/messages".equals(p)
                ? new Net.Resp(200, "{\"id\":\"777\"}") : null;
        Commands.handle(rest, interaction("poll", "G", "C", 0, stringOption("question", "Lunch?"),
                stringOption("option1", "Pizza"), stringOption("option2", "Sushi")));
        check("poll created", contains(content(rest), "Poll created"), content(rest));
        int reactions = 0;
        boolean pollMessage = false;
        for (DiscordRest.Call call : rest.calls) {
            if (call.path.contains("/reactions/")) {
                reactions++;
            }
            if (call.path.equals("/channels/C/messages") && call.body != null && call.body.contains("Lunch?")) {
                pollMessage = true;
            }
        }
        check("poll reactions", reactions == 2, "reactions=" + reactions);
        check("poll question posted", pollMessage, "missing question");

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("base64", "G", "C", 0, stringOption("mode", "encode"),
                stringOption("text", "hello")));
        check("base64 encode", contains(content(rest), "aGVsbG8="), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("base64", "G", "C", 0, stringOption("mode", "decode"),
                stringOption("text", "aGVsbG8=")));
        check("base64 decode", contains(content(rest), "hello"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("color", "G", "C", 0, stringOption("hex", "#5865F2")));
        JSONArray colors = embeds(rest);
        check("color embed", colors != null && colors.optJSONObject(0).optInt("color") == 0x5865F2,
                colors == null ? "no embed" : String.valueOf(colors.optJSONObject(0).optInt("color")));
    }

    private static void moderation() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> {
            if ("GET".equals(m) && "/channels/C".equals(p)) {
                return new Net.Resp(200, "{\"id\":\"C\",\"permission_overwrites\":[]}");
            }
            if ("PATCH".equals(m) && "/channels/C".equals(p)) {
                return new Net.Resp(200, "{}");
            }
            return null;
        };
        Commands.handle(rest, interaction("lock", "G", "C", 16));
        check("lock replies", contains(content(rest), "Locked"), content(rest));
        DiscordRest.Call patch = lastPatch(rest);
        check("lock denies send", patch != null && patch.body != null
                && patch.body.contains("2048") && patch.body.contains("permission_overwrites"),
                patch == null ? "no patch" : patch.body);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> {
            if ("GET".equals(m) && "/channels/C".equals(p)) {
                return new Net.Resp(200, "{\"id\":\"C\",\"permission_overwrites\":[{\"id\":\"G\",\"type\":0,"
                        + "\"allow\":\"0\",\"deny\":\"2048\"}]}");
            }
            if ("PATCH".equals(m) && "/channels/C".equals(p)) {
                return new Net.Resp(200, "{}");
            }
            return null;
        };
        Commands.handle(rest, interaction("unlock", "G", "C", 16));
        check("unlock replies", contains(content(rest), "Unlocked"), content(rest));
        DiscordRest.Call unlockPatch = lastPatch(rest);
        // Clearing the only bit removes the overwrite entirely, which is also correct.
        check("unlock clears deny", unlockPatch != null && unlockPatch.body != null
                && (unlockPatch.body.contains("\"deny\":\"0\"")
                    || unlockPatch.body.contains("\"permission_overwrites\":[]")),
                unlockPatch == null ? "no patch" : unlockPatch.body);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PATCH".equals(m) && "/channels/C".equals(p)
                ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("topic", "G", "C", 16, stringOption("text", "new topic")));
        check("topic replies", contains(content(rest), "topic"), content(rest));
        DiscordRest.Call topicPatch = lastPatch(rest);
        check("topic body", topicPatch != null && topicPatch.body != null
                && topicPatch.body.contains("new topic"), topicPatch == null ? "no patch" : topicPatch.body);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PATCH".equals(m) ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("nick", "G", "C", (1L << 27), userOption("user", "55", "bob"),
                stringOption("name", "Bobby")));
        check("nick replies", contains(content(rest), "Bobby"), content(rest));
        DiscordRest.Call nickPatch = lastPatch(rest);
        check("nick body", nickPatch != null && nickPatch.body != null
                && nickPatch.body.contains("Bobby"), nickPatch == null ? "no patch" : nickPatch.body);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PUT".equals(m) && "/guilds/G/members/55/roles/77".equals(p)
                ? new Net.Resp(204, "") : null;
        Commands.handle(rest, interaction("addrole", "G", "C", (1L << 28), userOption("user", "55", "bob"),
                new JSONObject().put("name", "role").put("type", 8).put("value", "77")));
        check("addrole replies", contains(content(rest), "<@&77>"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && p.startsWith("/guilds/G/bans")
                ? new Net.Resp(200, "{\"data\":[{\"user\":{\"id\":\"5\",\"username\":\"spammer\"},"
                        + "\"reason\":\"spam\"}]}") : null;
        Commands.handle(rest, interaction("banlist", "G", "C", 4));
        check("banlist users", contains(embedDescription(rest), "spammer"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> {
            if ("PUT".equals(m) && p.startsWith("/guilds/G/bans/55")) {
                return new Net.Resp(204, "");
            }
            if ("DELETE".equals(m) && p.startsWith("/guilds/G/bans/55")) {
                return new Net.Resp(204, "");
            }
            return null;
        };
        Commands.handle(rest, interaction("softban", "G", "C", 4, userOption("user", "55", "bob")));
        check("softban replies", contains(content(rest), "Softbanned"), content(rest));
        int banCalls = 0;
        for (DiscordRest.Call call : rest.calls) {
            if (call.path.startsWith("/guilds/G/bans/55")) {
                banCalls++;
            }
        }
        check("softban bans then unbans", banCalls == 2, "calls=" + banCalls);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PUT".equals(m) && "/channels/C/pins/123".equals(p)
                ? new Net.Resp(204, "") : null;
        Commands.handle(rest, interaction("pin", "G", "C", 8192, stringOption("message_id", "123")));
        check("pin replies", contains(content(rest), "Pinned"), content(rest));
    }

    private static void fun() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("coinflip", "G", "C", 0));
        check("coinflip", contains(content(rest), "🪙"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("rps", "G", "C", 0, stringOption("choice", "rock")));
        check("rps plays", contains(content(rest), "vs"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("joke", "G", "C", 0));
        check("joke", content(rest) != null && content(rest).length() > 10, content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("fact", "G", "C", 0));
        check("fact", content(rest) != null && content(rest).length() > 10, content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("quote", "G", "C", 0));
        check("quote", contains(content(rest), ">"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("rate", "G", "C", 0, stringOption("thing", "pizza")));
        check("rate", contains(content(rest), "/100"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("reverse", "G", "C", 0, stringOption("text", "abc")));
        check("reverse", "cba".equals(content(rest)), content(rest));
    }

    private static void custom() throws Exception {
        CustomCommands.load("[]");
        CommandToggles.loadDefaults();
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, customInteraction("add", "name", "hello", "response", "Hi {user}!"));
        check("custom added", CustomCommands.count() == 1, "count=" + CustomCommands.count());
        check("custom add reply", contains(content(rest), "Added `/hello`"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("hello", "G", "C", 0));
        check("custom executes", contains(content(rest), "<@100>"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, customInteraction("add", "name", "ban", "response", "nope"));
        check("custom rejects built-in", contains(content(rest), "built-in"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, customInteraction("list"));
        check("custom list", contains(content(rest), "/hello"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, customInteraction("remove", "name", "hello"));
        check("custom removed", CustomCommands.count() == 0, "count=" + CustomCommands.count());

        rest = new DiscordRest("t");
        Commands.handle(rest, customInteraction("add", "name", "Bad Name!", "response", "x"));
        check("custom validates names", CustomCommands.count() == 0, "invalid name accepted");

        CustomCommands.load("[]");
        CommandToggles.load("[]");
        check("all enabled leaves no custom room", CustomCommands.limit() == 0,
                "limit=" + CustomCommands.limit());
        check("custom refuses when slots are full", CustomCommands.add("nope", "x") != null,
                "added anyway (count=" + CustomCommands.count() + ")");
        CustomCommands.load("[]");
        CommandToggles.load("[]");
    }

    private static void toggles() throws Exception {
        CustomCommands.load("[]");
        CommandToggles.loadDefaults();
        int defaultLimit = 100 - CommandToggles.enabledCount();
        check("defaults leave room for custom commands", defaultLimit == CommandToggles.DEFAULT_OFF.size(),
                "free=" + defaultLimit);
        check("default-off commands are off", !CommandToggles.isEnabled("dm")
                && CommandToggles.isEnabled("ban"), "unexpected defaults");
        check("toggling works", CommandToggles.setEnabled("ping", false)
                && !CommandToggles.isEnabled("ping"), "still enabled");
        check("disabling frees another custom slot", CustomCommands.limit() == defaultLimit + 1,
                "limit=" + CustomCommands.limit());
        check("custom fits after disabling", CustomCommands.add("mine", "hello") == null,
                "rejected");
        JSONArray registered = Commands.definitions();
        check("disabled command not registered", !names(registered).contains("ping"),
                String.valueOf(registered.length()));
        check("default-off command not registered", !names(registered).contains("dm"),
                String.valueOf(registered.length()));
        check("custom still registered", names(registered).contains("mine"),
                String.valueOf(registered.length()));
        check("registration never passes 100", registered.length() <= Commands.MAX_COMMANDS,
                "count=" + registered.length());

        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("ping", "G", "C", 0));
        check("disabled command refuses to run", contains(content(rest), "turned off"), content(rest));

        CommandToggles.setEnabled("ping", true);
        check("re-enabled command registers", names(Commands.definitions()).contains("ping"), "missing");
        CommandToggles.load("[]");
        CustomCommands.load("[]");
    }

    private static void commandsExtra() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("convert", "G", "C", 0, numberOption("value", 10),
                stringOption("from", "km"), stringOption("to", "mi")));
        check("convert km to mi", contains(content(rest), "6.2137"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("convert", "G", "C", 0, numberOption("value", 100),
                stringOption("from", "c"), stringOption("to", "f")));
        check("convert celsius", contains(content(rest), "212"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("morse", "G", "C", 0, stringOption("mode", "encode"),
                stringOption("text", "SOS")));
        check("morse encodes", contains(content(rest), "... --- ..."), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("morse", "G", "C", 0, stringOption("mode", "decode"),
                stringOption("text", "... --- ...")));
        check("morse decodes", contains(content(rest), "SOS"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("binary", "G", "C", 0, stringOption("mode", "encode"),
                stringOption("text", "A")));
        check("binary encodes", contains(content(rest), "01000001"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("binary", "G", "C", 0, stringOption("mode", "decode"),
                stringOption("text", "01000001")));
        check("binary decodes", contains(content(rest), "A"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("regex", "G", "C", 0, stringOption("pattern", "h(\\w+)o"),
                stringOption("text", "hello")));
        check("regex matches", contains(content(rest), "Match") && contains(content(rest), "ell"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("timezone", "G", "C", 0, stringOption("zone", "Europe/London")));
        check("timezone resolves", contains(content(rest), "Europe/London"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("countdown", "G", "C", 0, stringOption("date", "2030-01-01")));
        check("countdown builds timestamp", contains(content(rest), "<t:1893456000:F>"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("commandcount", "G", "C", 0));
        check("commandcount shows slots", contains(embedDescription(rest), "of 100"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G/members/55".equals(p)
                ? new Net.Resp(200, "{\"joined_at\":\"2024-01-02T03:04:05.000000+00:00\","
                        + "\"user\":{\"id\":\"55\",\"username\":\"bob\"}}") : null;
        Commands.handle(rest, interaction("joined", "G", "C", 0, userOption("user", "55", "bob")));
        check("joined shows join date", contains(embedDescription(rest), "Joined server"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> {
            if ("GET".equals(m) && "/guilds/G/members/55".equals(p)) {
                return new Net.Resp(200, "{\"roles\":[\"77\"],\"user\":{\"id\":\"55\",\"username\":\"bob\"}}");
            }
            if ("GET".equals(m) && "/guilds/G/roles".equals(p)) {
                return new Net.Resp(200, "[{\"id\":\"G\",\"name\":\"@everyone\",\"permissions\":\"0\"},"
                        + "{\"id\":\"77\",\"name\":\"Admin\",\"permissions\":\"268435456\"}]");
            }
            return null;
        };
        Commands.handle(rest, interaction("memberroles", "G", "C", 0, userOption("user", "55", "bob")));
        check("memberroles lists roles", contains(embedDescription(rest), "<@&77>"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> {
            if ("GET".equals(m) && "/guilds/G/members/55".equals(p)) {
                return new Net.Resp(200, "{\"roles\":[\"77\"],\"user\":{\"id\":\"55\",\"username\":\"bob\"}}");
            }
            if ("GET".equals(m) && "/guilds/G/roles".equals(p)) {
                return new Net.Resp(200, "[{\"id\":\"G\",\"name\":\"@everyone\",\"permissions\":\"0\"},"
                        + "{\"id\":\"77\",\"name\":\"Admin\",\"permissions\":\"268435456\"}]");
            }
            return null;
        };
        Commands.handle(rest, interaction("userperms", "G", "C", 0, userOption("user", "55", "bob")));
        check("userperms adds role bits", contains(embedDescription(rest), "Manage Roles"),
                embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "POST".equals(m) && "/channels/C/invites".equals(p)
                ? new Net.Resp(200, "{\"code\":\"abc\"}") : null;
        Commands.handle(rest, interaction("createinvite", "G", "C", Commands.CREATE_INVITE));
        check("createinvite returns link", contains(content(rest), "discord.gg/abc"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G/invites".equals(p)
                ? new Net.Resp(200, "[{\"code\":\"abc\",\"uses\":3,\"channel\":{\"name\":\"general\"},"
                        + "\"inviter\":{\"username\":\"bob\"}}]") : null;
        Commands.handle(rest, interaction("invites", "G", "C", Commands.MANAGE_GUILD));
        check("invites lists codes", contains(embedDescription(rest), "abc"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/channels/C/webhooks".equals(p)
                ? new Net.Resp(200, "[{\"id\":\"1\",\"name\":\"hooky\",\"type\":1}]") : null;
        Commands.handle(rest, interaction("webhooks", "G", "C", Commands.MANAGE_WEBHOOKS));
        check("webhooks lists hooks", contains(embedDescription(rest), "hooky"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/channels/C/threads/active".equals(p)
                ? new Net.Resp(200, "{\"threads\":[{\"name\":\"topic\",\"message_count\":4}]}") : null;
        Commands.handle(rest, interaction("threads", "G", "C", 0));
        check("threads lists threads", contains(embedDescription(rest), "topic"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && p.startsWith("/guilds/G/scheduled-events")
                ? new Net.Resp(200, "[{\"name\":\"Launch\",\"scheduled_start_time\":\"2030-01-01T00:00:00Z\","
                        + "\"user_count\":5}]") : null;
        Commands.handle(rest, interaction("events", "G", "C", 0));
        check("events lists events", contains(embedDescription(rest), "Launch"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G/stickers".equals(p)
                ? new Net.Resp(200, "[{\"name\":\"wave\",\"tags\":\"hi\"}]") : null;
        Commands.handle(rest, interaction("stickers", "G", "C", 0));
        check("stickers lists stickers", contains(embedDescription(rest), "wave"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G/emojis".equals(p)
                ? new Net.Resp(200, "[{\"id\":\"123\",\"name\":\"party\"}]") : null;
        Commands.handle(rest, interaction("emojiinfo", "G", "C", 0, stringOption("emoji", "<:party:123>")));
        check("emojiinfo finds emoji", contains(embedTitle(rest), "party"), embedTitle(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "POST".equals(m) && "/channels/555/messages".equals(p)
                ? new Net.Resp(200, "{\"id\":\"1\"}") : null;
        Commands.handle(rest, interaction("announce", "G", "C", 8192, channelOption("channel", "555"),
                stringOption("text", "Server news")));
        check("announce posts", contains(content(rest), "Announced"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> {
            if ("POST".equals(m) && "/users/@me/channels".equals(p)) {
                return new Net.Resp(200, "{\"id\":\"DM9\"}");
            }
            if ("POST".equals(m) && "/channels/DM9/messages".equals(p)) {
                return new Net.Resp(200, "{\"id\":\"2\"}");
            }
            return null;
        };
        Commands.handle(rest, interaction("dm", "G", "C", 0, userOption("user", "55", "bob"),
                stringOption("text", "hi")));
        check("dm sends", contains(content(rest), "Message sent"), content(rest));
    }

    private static JSONObject numberOption(String name, double value) throws Exception {
        return new JSONObject().put("name", name).put("type", 10).put("value", value);
    }

    private static List<String> names(JSONArray definitions) throws Exception {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < definitions.length(); i++) {
            names.add(definitions.getJSONObject(i).optString("name", ""));
        }
        return names;
    }

    private static void roblox() throws Exception {
        BotState.robloxEnabled = false;
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, robloxInteraction("user", "username", "Roblox"));
        check("roblox disabled message", contains(content(rest), "turned off"), content(rest));
        BotState.robloxEnabled = true;

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("roblox", "G", "C", 0));
        check("roblox usage", contains(content(rest), "/roblox user"), content(rest));
    }

    private static void presence() throws Exception {
        BotPresence.set(BotPresence.PLAYING, "", "online");
        JSONObject payload = BotPresence.gatewayPayload();
        check("presence op is 3", payload.optInt("op") == 3, "op=" + payload.optInt("op"));
        check("presence clears activity when empty",
                payload.getJSONObject("d").getJSONArray("activities").length() == 0, "has activity");
        check("presence default status", "online".equals(payload.getJSONObject("d").optString("status")),
                payload.getJSONObject("d").optString("status"));

        BotPresence.set(BotPresence.WATCHING, "the logs", "idle");
        payload = BotPresence.gatewayPayload();
        JSONObject activity = payload.getJSONObject("d").getJSONArray("activities").getJSONObject(0);
        check("presence watching text", "the logs".equals(activity.optString("name")), activity.toString());
        check("presence watching type", activity.optInt("type") == 3, "type=" + activity.optInt("type"));
        check("presence idle status", "idle".equals(payload.getJSONObject("d").optString("status")),
                payload.getJSONObject("d").optString("status"));
        check("presence describe", BotPresence.describe().contains("Watching"), BotPresence.describe());

        BotPresence.set(BotPresence.CUSTOM, "building bots", "dnd");
        payload = BotPresence.gatewayPayload();
        activity = payload.getJSONObject("d").getJSONArray("activities").getJSONObject(0);
        check("presence custom name", "Custom Status".equals(activity.optString("name")), activity.toString());
        check("presence custom state", "building bots".equals(activity.optString("state")), activity.toString());
        check("presence custom type", activity.optInt("type") == 4, "type=" + activity.optInt("type"));
        check("presence dnd status", "dnd".equals(payload.getJSONObject("d").optString("status")),
                payload.getJSONObject("d").optString("status"));

        BotPresence.set(BotPresence.COMPETING, "the tournament", "online");
        check("presence competing type", BotPresence.gatewayPayload().getJSONObject("d")
                .getJSONArray("activities").getJSONObject(0).optInt("type") == 5, "wrong type");

        BotPresence.set(999, "x", "bogus");
        check("presence invalid type falls back", BotPresence.type == BotPresence.PLAYING,
                "type=" + BotPresence.type);
        check("presence invalid status falls back", "online".equals(BotPresence.status), BotPresence.status);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            builder.append('x');
        }
        BotPresence.set(BotPresence.PLAYING, builder.toString(), "online");
        check("presence text clamped", BotPresence.text.length() == BotPresence.MAX_TEXT,
                "len=" + BotPresence.text.length());

        BotPresence.set(BotPresence.PLAYING, "with the API", "online", "in a server", "");
        payload = BotPresence.gatewayPayload();
        activity = payload.getJSONObject("d").getJSONArray("activities").getJSONObject(0);
        check("presence state line", "in a server".equals(activity.optString("state")), activity.toString());

        BotPresence.set(BotPresence.STREAMING, "live coding", "online", "", "");
        check("streaming without url falls back", BotPresence.type == BotPresence.PLAYING,
                "type=" + BotPresence.type);

        BotPresence.set(BotPresence.STREAMING, "live coding", "online", "building",
                "https://twitch.tv/botdroid");
        payload = BotPresence.gatewayPayload();
        activity = payload.getJSONObject("d").getJSONArray("activities").getJSONObject(0);
        check("streaming type", activity.optInt("type") == 1, activity.toString());
        check("streaming url", "https://twitch.tv/botdroid".equals(activity.optString("url")),
                activity.toString());
        check("presence describes streaming", BotPresence.describe().contains("Streaming"),
                BotPresence.describe());

        BotPresence.set(BotPresence.PLAYING, "", "online");
    }

    private static void commandsPlus() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("about", "G", "C", 0));
        check("about shows version", contains(embedDescription(rest), BotState.VERSION), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G".equals(p)
                ? new Net.Resp(200, "{\"name\":\"Server\",\"icon\":\"abc\"}") : null;
        Commands.handle(rest, interaction("servericon", "G", "C", 0));
        JSONObject image = embeds(rest).optJSONObject(0).optJSONObject("image");
        check("servericon url", image != null && image.optString("url").contains("icons/G/abc.png"),
                image == null ? "no image" : image.toString());

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/users/55".equals(p)
                ? new Net.Resp(200, "{\"username\":\"bob\",\"banner\":\"bee\"}") : null;
        Commands.handle(rest, interaction("banner", "G", "C", 0, userOption("user", "55", "bob")));
        image = embeds(rest).optJSONObject(0).optJSONObject("image");
        check("banner url", image != null && image.optString("url").contains("banners/55/bee.png"),
                image == null ? "no image" : image.toString());

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/users/100".equals(p)
                ? new Net.Resp(200, "{\"username\":\"tester\"}") : null;
        Commands.handle(rest, interaction("banner", "G", "C", 0));
        check("banner missing", contains(content(rest), "no profile banner"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("timestamp", "G", "C", 0, stringOption("when", "1700000000")));
        check("timestamp builds markup", contains(content(rest), "<t:1700000000:F>"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("timestamp", "G", "C", 0, stringOption("when", "tomorrow")));
        check("timestamp rejects junk", contains(content(rest), "not a timestamp"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("snowflake", "G", "C", 0, stringOption("id", "175928847299117063")));
        // 175928847299117063 was created 2016-04-30T11:18:25.796Z
        check("snowflake decodes", contains(content(rest), "<t:1462015105:F>"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("password", "G", "C", 0, intOption("length", 24)));
        String password = content(rest).replace("```", "").trim();
        check("password length", password.length() == 24, "len=" + password.length());
        check("password is ephemeral", reply(rest).optInt("flags") == 64, "flags=" + reply(rest).optInt("flags"));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("uuid", "G", "C", 0));
        check("uuid format", content(rest).matches("`[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}`"),
                content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("hash", "G", "C", 0, stringOption("algorithm", "sha256"),
                stringOption("text", "abc")));
        check("sha256 hashes", contains(content(rest),
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("url", "G", "C", 0, stringOption("mode", "encode"),
                stringOption("text", "a b")));
        check("url encode", contains(content(rest), "a%20b"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("url", "G", "C", 0, stringOption("mode", "decode"),
                stringOption("text", "a%20b")));
        check("url decode", contains(content(rest), "a b"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("case", "G", "C", 0, stringOption("mode", "upper"),
                stringOption("text", "hello")));
        check("case upper", "HELLO".equals(content(rest)), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("wordcount", "G", "C", 0, stringOption("text", "a b\nc")));
        check("wordcount words", contains(content(rest), "**Words:** 3"), content(rest));
        check("wordcount lines", contains(content(rest), "**Lines:** 2"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("myperms", "G", "C", 1L << 3));
        check("myperms administrator", contains(embedDescription(rest), "Administrator"), embedDescription(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("myperms", "G", "C", (1L << 11) | (1L << 14)));
        check("myperms lists bits", contains(embedDescription(rest), "Send Messages")
                && contains(embedDescription(rest), "Embed Links"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/channels/C/pins".equals(p)
                ? new Net.Resp(200, "[{\"id\":\"9\",\"content\":\"hello pin\",\"author\":{\"username\":\"bob\"}}]")
                : null;
        Commands.handle(rest, interaction("pins", "G", "C", 0));
        check("pins lists content", contains(embedDescription(rest), "hello pin"), embedDescription(rest));
        check("pins links", contains(embedDescription(rest), "/channels/G/C/9"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/channels/C/pins".equals(p)
                ? new Net.Resp(200, "[]") : null;
        Commands.handle(rest, interaction("pins", "G", "C", 0));
        check("pins empty", contains(content(rest), "No pinned"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && p.startsWith("/invites/abc")
                ? new Net.Resp(200, "{\"guild\":{\"name\":\"Cool Server\",\"approximate_member_count\":12},"
                        + "\"channel\":{\"name\":\"general\"},\"uses\":3}") : null;
        Commands.handle(rest, interaction("inviteinfo", "G", "C", 0,
                stringOption("code", "https://discord.gg/abc")));
        check("inviteinfo strips link", contains(embedDescription(rest), "Cool Server"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PATCH".equals(m) ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("untimeout", "G", "C", 1L << 40, userOption("user", "55", "bob")));
        DiscordRest.Call untimeout = lastPatch(rest);
        check("untimeout clears timeout", untimeout != null && untimeout.body != null
                && untimeout.body.contains("communication_disabled_until") && untimeout.body.contains("null"),
                untimeout == null ? "no patch" : untimeout.body);

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("untimeout", "G", "C", 0, userOption("user", "55", "bob")));
        check("untimeout needs permission", contains(content(rest), "Moderate Members"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G/bans/55".equals(p)
                ? new Net.Resp(404, "{\"message\":\"Unknown Ban\"}") : null;
        Commands.handle(rest, interaction("baninfo", "G", "C", 4, stringOption("user_id", "55")));
        check("baninfo not banned", contains(content(rest), "not banned"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G/bans/55".equals(p)
                ? new Net.Resp(200, "{\"user\":{\"id\":\"55\",\"username\":\"spammer\"},\"reason\":\"spam\"}")
                : null;
        Commands.handle(rest, interaction("baninfo", "G", "C", 4, stringOption("user_id", "55")));
        check("baninfo reason", contains(embedDescription(rest), "spam"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "GET".equals(m) && "/guilds/G/roles".equals(p)
                ? new Net.Resp(200, "[{\"id\":\"77\",\"name\":\"Admin\",\"color\":5793266,\"position\":2,"
                        + "\"hoist\":true,\"mentionable\":false,\"managed\":false,\"permissions\":\"8\"}]")
                : null;
        Commands.handle(rest, interaction("roleinfo", "G", "C", 0, roleOption("role", "77")));
        check("roleinfo title", "Admin".equals(embedTitle(rest)), embedTitle(rest));
        check("roleinfo permissions", contains(embedDescription(rest), "Administrator"), embedDescription(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PATCH".equals(m) ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("rolecolor", "G", "C", 1L << 28, roleOption("role", "77"),
                stringOption("hex", "#FF0000")));
        DiscordRest.Call colorPatch = lastPatch(rest);
        check("rolecolor body", colorPatch != null && colorPatch.body != null
                && colorPatch.body.contains("16711680"), colorPatch == null ? "no patch" : colorPatch.body);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "POST".equals(m) && "/guilds/G/roles".equals(p)
                ? new Net.Resp(201, "{\"id\":\"88\"}") : null;
        Commands.handle(rest, interaction("createrole", "G", "C", 1L << 28, stringOption("name", "Helpers")));
        DiscordRest.Call rolePost = rest.lastNonCallback();
        check("createrole posts name", rolePost != null && rolePost.body != null
                && rolePost.body.contains("Helpers"), rolePost == null ? "no post" : rolePost.body);
        check("createrole mentions", contains(content(rest), "<@&88>"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "POST".equals(m) && "/guilds/G/channels".equals(p)
                ? new Net.Resp(201, "{\"id\":\"99\"}") : null;
        Commands.handle(rest, interaction("createchannel", "G", "C", 16, stringOption("name", "Voice Room"),
                stringOption("type", "voice")));
        DiscordRest.Call channelPost = rest.lastNonCallback();
        check("createchannel voice type", channelPost != null && channelPost.body != null
                && channelPost.body.contains("\"type\":2") && channelPost.body.contains("voice-room"),
                channelPost == null ? "no post" : channelPost.body);
        check("createchannel mentions", contains(content(rest), "<#99>"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> {
            if ("GET".equals(m) && "/channels/C".equals(p)) {
                return new Net.Resp(200, "{\"name\":\"general\",\"type\":0,\"topic\":\"hi\","
                        + "\"permission_overwrites\":[{\"id\":\"G\",\"type\":0,\"allow\":\"0\",\"deny\":\"0\"}]}");
            }
            if ("POST".equals(m) && "/guilds/G/channels".equals(p)) {
                return new Net.Resp(201, "{\"id\":\"100\"}");
            }
            return null;
        };
        Commands.handle(rest, interaction("clonechannel", "G", "C", 16));
        channelPost = rest.lastNonCallback();
        check("clonechannel copies settings", channelPost != null && channelPost.body != null
                && channelPost.body.contains("general-copy") && channelPost.body.contains("permission_overwrites")
                && channelPost.body.contains("hi"), channelPost == null ? "no post" : channelPost.body);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PATCH".equals(m) ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("move", "G", "C", 1L << 24, userOption("user", "55", "bob"),
                channelOption("channel", "VC")));
        DiscordRest.Call move = lastPatch(rest);
        check("move sets channel", move != null && move.body != null && move.body.contains("\"channel_id\":\"VC\""),
                move == null ? "no patch" : move.body);

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("move", "G", "C", 0, userOption("user", "55", "bob"),
                channelOption("channel", "VC")));
        check("move needs permission", contains(content(rest), "Move Members"), content(rest));

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PATCH".equals(m) ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("mute", "G", "C", 1L << 22, userOption("user", "55", "bob")));
        DiscordRest.Call mute = lastPatch(rest);
        check("mute body", mute != null && mute.body != null && mute.body.contains("\"mute\":true"),
                mute == null ? "no patch" : mute.body);

        rest = new DiscordRest("t");
        rest.responder = (m, p, b) -> "PATCH".equals(m) ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("deafen", "G", "C", 1L << 23, userOption("user", "55", "bob")));
        DiscordRest.Call deafen = lastPatch(rest);
        check("deafen body", deafen != null && deafen.body != null && deafen.body.contains("\"deaf\":true"),
                deafen == null ? "no patch" : deafen.body);

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("fortune", "G", "C", 0));
        check("fortune", content(rest) != null && content(rest).length() > 15, content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("compliment", "G", "C", 0, userOption("user", "55", "bob")));
        check("compliment mentions", contains(content(rest), "<@55>"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("hug", "G", "C", 0, userOption("user", "55", "bob")));
        check("hug action", contains(content(rest), "hugs <@55>"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("pat", "G", "C", 0, userOption("user", "55", "bob")));
        check("pat action", contains(content(rest), "pats <@55>"), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("clap", "G", "C", 0, stringOption("text", "a b")));
        check("clap inserts emoji", "a 👏 b".equals(content(rest)), content(rest));

        rest = new DiscordRest("t");
        Commands.handle(rest, interaction("spoiler", "G", "C", 0, stringOption("text", "secret")));
        check("spoiler wraps text", "||secret||".equals(content(rest)), content(rest));
    }

    private static JSONObject roleOption(String name, String id) throws Exception {
        return new JSONObject().put("name", name).put("type", 8).put("value", id);
    }

    private static JSONObject channelOption(String name, String id) throws Exception {
        return new JSONObject().put("name", name).put("type", 7).put("value", id);
    }

    private static JSONObject interaction(String name, String guild, String channel, long permissions,
                                          JSONObject... options) throws Exception {
        JSONArray optionArray = new JSONArray();
        for (JSONObject option : options) {
            optionArray.put(option);
        }
        JSONObject data = new JSONObject();
        data.put("name", name);
        data.put("options", optionArray);
        return wrap(data, guild, channel, permissions);
    }

    private static JSONObject customInteraction(String sub, String... pairs) throws Exception {
        return wrap(subcommand("custom", sub, pairs), "G", "C", 0);
    }

    private static JSONObject robloxInteraction(String sub, String... pairs) throws Exception {
        return wrap(subcommand("roblox", sub, pairs), "G", "C", 0);
    }

    private static JSONObject subcommand(String command, String sub, String... pairs) throws Exception {
        JSONArray subOptions = new JSONArray();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            subOptions.put(new JSONObject().put("name", pairs[i]).put("type", 3).put("value", pairs[i + 1]));
        }
        JSONObject subOption = new JSONObject().put("name", sub).put("type", 1).put("options", subOptions);
        return new JSONObject().put("name", command).put("options", new JSONArray().put(subOption));
    }

    private static JSONObject wrap(JSONObject data, String guild, String channel, long permissions)
            throws Exception {
        JSONObject member = new JSONObject();
        member.put("permissions", Long.toString(permissions));
        member.put("user", new JSONObject().put("id", "100").put("username", "tester"));
        JSONObject interaction = new JSONObject();
        interaction.put("id", "IID");
        interaction.put("token", "ITOKEN");
        interaction.put("type", 2);
        interaction.put("data", data);
        interaction.put("member", member);
        if (guild != null) {
            interaction.put("guild_id", guild);
        }
        if (channel != null) {
            interaction.put("channel_id", channel);
        }
        return interaction;
    }

    private static JSONObject userOption(String name, String id, String username) throws Exception {
        JSONObject option = new JSONObject();
        option.put("name", name);
        option.put("type", 6);
        option.put("value", id);
        option.put("user", new JSONObject().put("id", id).put("username", username));
        return option;
    }

    private static JSONObject stringOption(String name, String value) throws Exception {
        return new JSONObject().put("name", name).put("type", 3).put("value", value);
    }

    private static JSONObject intOption(String name, int value) throws Exception {
        return new JSONObject().put("name", name).put("type", 4).put("value", value);
    }

    private static JSONObject reply(DiscordRest rest) {
        for (int i = rest.calls.size() - 1; i >= 0; i--) {
            DiscordRest.Call call = rest.calls.get(i);
            if (call.path.endsWith("/callback")) {
                try {
                    return new JSONObject(call.body).optJSONObject("data");
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String content(DiscordRest rest) {
        JSONObject data = reply(rest);
        return data == null ? null : data.optString("content", "");
    }

    private static JSONArray embeds(DiscordRest rest) {
        JSONObject data = reply(rest);
        return data == null ? null : data.optJSONArray("embeds");
    }

    private static String embedDescription(DiscordRest rest) {
        JSONArray embeds = embeds(rest);
        return embeds == null || embeds.length() == 0 ? null
                : embeds.optJSONObject(0).optString("description", "");
    }

    private static String embedTitle(DiscordRest rest) {
        JSONArray embeds = embeds(rest);
        return embeds == null || embeds.length() == 0 ? null
                : embeds.optJSONObject(0).optString("title", "");
    }

    private static DiscordRest.Call lastPatch(DiscordRest rest) {
        for (int i = rest.calls.size() - 1; i >= 0; i--) {
            if ("PATCH".equals(rest.calls.get(i).method)) {
                return rest.calls.get(i);
            }
        }
        return null;
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.contains(needle);
    }

    private static void check(String label, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label + "  -> " + detail);
        }
    }
}
