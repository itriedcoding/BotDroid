package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

public final class TestCore {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        help();
        ping();
        banGate();
        banSuccess();
        banForbidden();
        kick();
        unban();
        timeout();
        purge();
        purgeOld();
        slowmode();
        say();
        userInfo();
        serverInfo();
        avatar();
        roll();
        eightBall();
        malformed();
        System.out.println();
        System.out.println("core: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void help() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("help", "G", "C", 0));
        JSONObject data = reply(rest);
        check("help replies", data != null, "no reply");
        check("help is ephemeral", data != null && data.optInt("flags") == 64, "flags wrong");
        check("help lists ban", contains(content(rest), "/ban"), String.valueOf(content(rest)));
    }

    private static void ping() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "GET".equals(method) && "/gateway".equals(path)
                ? new Net.Resp(200, "{\"url\":\"wss://gateway.discord.gg\"}") : null;
        Commands.handle(rest, interaction("ping", "G", "C", 0));
        check("ping replies", contains(content(rest), "Pong"), String.valueOf(content(rest)));
    }

    private static void banGate() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("ban", "G", "C", 0, userOption("user", "55", "badguy")));
        check("ban blocked without permission", contains(content(rest), "Ban Members"), String.valueOf(content(rest)));
        check("ban makes no API call when blocked", rest.lastNonCallback() == null, "unexpected call");
    }

    private static void banSuccess() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "PUT".equals(method) && path.startsWith("/guilds/G/bans/55")
                ? new Net.Resp(204, "") : null;
        Commands.handle(rest, interaction("ban", "G", "C", 4, userOption("user", "55", "badguy"),
                stringOption("reason", "spam")));
        check("ban succeeds", contains(content(rest), "Banned"), String.valueOf(content(rest)));
        DiscordRest.Call call = rest.lastNonCallback();
        check("ban endpoint", call != null && call.path.startsWith("/guilds/G/bans/55?reason=spam"),
                call == null ? "no call" : call.path);
    }

    private static void banForbidden() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> new Net.Resp(403, "{\"message\":\"Missing Permissions\"}");
        Commands.handle(rest, interaction("ban", "G", "C", 4, userOption("user", "55", "badguy")));
        check("ban explains missing permission", contains(content(rest), "role"), String.valueOf(content(rest)));
    }

    private static void kick() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "DELETE".equals(method) && path.startsWith("/guilds/G/members/55")
                ? new Net.Resp(204, "") : null;
        Commands.handle(rest, interaction("kick", "G", "C", 2, userOption("user", "55", "badguy")));
        check("kick succeeds", contains(content(rest), "Kicked"), String.valueOf(content(rest)));
    }

    private static void unban() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "DELETE".equals(method) && path.startsWith("/guilds/G/bans/77")
                ? new Net.Resp(204, "") : null;
        Commands.handle(rest, interaction("unban", "G", "C", 4, stringOption("user_id", "77")));
        check("unban succeeds", contains(content(rest), "Unbanned"), String.valueOf(content(rest)));
    }

    private static void timeout() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "PATCH".equals(method) && path.startsWith("/guilds/G/members/55")
                ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("timeout", "G", "C", (1L << 40), userOption("user", "55", "badguy"),
                intOption("minutes", 30)));
        check("timeout succeeds", contains(content(rest), "Timed out"), String.valueOf(content(rest)));
        DiscordRest.Call call = rest.lastNonCallback();
        check("timeout body", call != null && call.body != null
                && call.body.contains("communication_disabled_until"), call == null ? "no call" : call.body);
    }

    private static void purge() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        String fresh1 = snowflake(System.currentTimeMillis() - 60000);
        String fresh2 = snowflake(System.currentTimeMillis() - 120000);
        rest.responder = (method, path, body) -> {
            if ("GET".equals(method) && path.startsWith("/channels/C/messages?limit=5")) {
                return new Net.Resp(200, "[{\"id\":\"" + fresh1 + "\"},{\"id\":\"" + fresh2 + "\"}]");
            }
            if ("POST".equals(method) && "/channels/C/messages/bulk-delete".equals(path)) {
                return new Net.Resp(204, "");
            }
            return null;
        };
        Commands.handle(rest, interaction("purge", "G", "C", 8192, intOption("count", 5)));
        check("purge reports count", contains(content(rest), "Deleted **2**"), String.valueOf(content(rest)));
    }

    private static void purgeOld() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        String old = snowflake(1420070400000L + 86400000L);
        rest.responder = (method, path, body) -> "GET".equals(method)
                ? new Net.Resp(200, "[{\"id\":\"" + old + "\"}]") : null;
        Commands.handle(rest, interaction("purge", "G", "C", 8192, intOption("count", 5)));
        check("purge skips old messages", contains(content(rest), "Nothing recent"), String.valueOf(content(rest)));
    }

    private static void slowmode() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "PATCH".equals(method) && "/channels/C".equals(path)
                ? new Net.Resp(200, "{}") : null;
        Commands.handle(rest, interaction("slowmode", "G", "C", 16, intOption("seconds", 10)));
        check("slowmode succeeds", contains(content(rest), "Slowmode set"), String.valueOf(content(rest)));
    }

    private static void say() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "POST".equals(method) && "/channels/C/messages".equals(path)
                ? new Net.Resp(200, "{\"id\":\"999\"}") : null;
        Commands.handle(rest, interaction("say", "G", "C", 8192, stringOption("text", "hello")));
        check("say returns link", contains(content(rest), "discord.com/channels/G/C/999"), String.valueOf(content(rest)));
    }

    private static void userInfo() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> {
            if ("GET".equals(method) && "/users/77".equals(path)) {
                return new Net.Resp(200, "{\"id\":\"77\",\"username\":\"alice\",\"global_name\":\"Alice\",\"avatar\":\"abc\"}");
            }
            if ("GET".equals(method) && "/guilds/G/members/77".equals(path)) {
                return new Net.Resp(200, "{\"joined_at\":\"2024-01-02T03:04:05.000000+00:00\",\"roles\":[]}");
            }
            return null;
        };
        Commands.handle(rest, interaction("userinfo", "G", "C", 0, userOption("user", "77", "alice")));
        JSONArray embeds = embeds(rest);
        check("userinfo embed", embeds != null && embeds.length() == 1, "no embed");
        check("userinfo joined_at", contains(embeds == null ? null
                : embeds.optJSONObject(0).optString("description"), "Joined server"), "joined_at missing");
    }

    private static void serverInfo() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "GET".equals(method) && path.startsWith("/guilds/G?with_counts=true")
                ? new Net.Resp(200, "{\"name\":\"Testers\",\"icon\":null,\"owner_id\":\"1\"}") : null;
        Commands.handle(rest, interaction("serverinfo", "G", "C", 0));
        JSONArray embeds = embeds(rest);
        check("serverinfo title", embeds != null && "Testers".equals(embeds.optJSONObject(0).optString("title")),
                "wrong title");
    }

    private static void avatar() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        rest.responder = (method, path, body) -> "GET".equals(method) && "/users/77".equals(path)
                ? new Net.Resp(200, "{\"id\":\"77\",\"username\":\"alice\",\"avatar\":\"hashvalue\"}") : null;
        Commands.handle(rest, interaction("avatar", "G", "C", 0, userOption("user", "77", "alice")));
        JSONArray embeds = embeds(rest);
        String url = embeds == null ? "" : embeds.optJSONObject(0).optJSONObject("image").optString("url");
        check("avatar cdn url", url.startsWith("https://cdn.discordapp.com/avatars/77/hashvalue"), url);
    }

    private static void roll() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("roll", "G", "C", 0));
        check("roll defaults to 1d6", contains(content(rest), "1d6"), String.valueOf(content(rest)));
    }

    private static void eightBall() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        Commands.handle(rest, interaction("8ball", "G", "C", 0, stringOption("question", "will it work?")));
        check("8ball answers", contains(content(rest), "Answer"), String.valueOf(content(rest)));
    }

    private static void malformed() throws Exception {
        DiscordRest rest = new DiscordRest("t");
        boolean threw = false;
        try {
            Commands.handle(rest, new JSONObject().put("id", "x").put("token", "y"));
        } catch (Exception e) {
            threw = true;
        }
        check("malformed interaction tolerated", !threw, "exception escaped");
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

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.contains(needle);
    }

    private static String snowflake(long millis) {
        return Long.toString((millis - 1420070400000L) << 22);
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
