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
        System.out.println();
        System.out.println("new: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void definitions() throws Exception {
        JSONArray defs = Commands.definitions();
        check("50 preset commands", defs.length() == 50, "count=" + defs.length());
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
        check("roblox subcommands", subNames(defs, "roblox").toString().equals("[user, avatar, game, group]"),
                subNames(defs, "roblox").toString());
        check("custom subcommands", subNames(defs, "custom").toString().equals("[add, remove, list, clear]"),
                subNames(defs, "custom").toString());
    }

    private static void validateOptions(JSONArray options, String command, StringBuilder problems) throws Exception {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            int type = option.optInt("type", -1);
            if (type != 1 && type != 3 && type != 4 && type != 6 && type != 8) {
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

        BotPresence.set(BotPresence.PLAYING, "", "online");
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
