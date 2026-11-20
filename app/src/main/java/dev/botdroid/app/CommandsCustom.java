package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/** /custom add|remove|list|clear|functions and running the stored scripts. */
final class CommandsCustom {

    private CommandsCustom() {
    }

    static void handle(DiscordRest rest, JSONObject interaction) throws Exception {
        String sub = Commands.subcommandName(interaction);
        if (sub == null) {
            Commands.respond(rest, interaction, usage(), true, null);
            return;
        }
        if ("add".equals(sub)) {
            add(rest, interaction);
        } else if ("remove".equals(sub)) {
            String name = normalize(Commands.optString(interaction, "name"));
            boolean removed = CustomCommands.remove(name);
            Commands.respond(rest, interaction, removed
                    ? "Removed custom command `/" + name + "`."
                    : "There is no custom command called `/" + name + "`.", true, null);
        } else if ("list".equals(sub)) {
            list(rest, interaction);
        } else if ("functions".equals(sub)) {
            Commands.respond(rest, interaction, "", true,
                    new JSONArray().put(Commands.embed("Bot builder functions", 0x5865F2, Script.reference())));
        } else if ("clear".equals(sub)) {
            int count = CustomCommands.count();
            CustomCommands.clear();
            Commands.respond(rest, interaction, count == 0
                    ? "There were no custom commands." : "Deleted **" + count + "** custom command"
                    + (count == 1 ? "" : "s") + ".", true, null);
        } else {
            Commands.respond(rest, interaction, usage(), true, null);
        }
    }

    private static void add(DiscordRest rest, JSONObject interaction) throws Exception {
        String name = normalize(Commands.optString(interaction, "name"));
        String response = Commands.optString(interaction, "response");
        String error = CustomCommands.add(name, response);
        if (error != null) {
            Commands.respond(rest, interaction, error, true, null);
            return;
        }
        String scriptProblem = response != null && response.contains("$") ? Script.validate(response) : null;
        StringBuilder message = new StringBuilder("Added `/" + name + "`. It is now registered in every server "
                + "the bot is in (give Discord a few seconds).");
        if (scriptProblem != null) {
            message.append("\n⚠️ The script has a problem: ").append(scriptProblem);
        } else if (response != null && response.contains("$")) {
            message.append("\nThe script parsed cleanly.");
        }
        message.append("\nUse `{user}`, `{username}` and `{channel}`, or script functions like `$mention` and "
                + "`$args`. `/custom functions` lists every function.");
        Commands.respond(rest, interaction, message.toString(), true, null);
    }

    private static void list(DiscordRest rest, JSONObject interaction) throws Exception {
        List<String> names = CustomCommands.names();
        if (names.isEmpty()) {
            Commands.respond(rest, interaction, "No custom commands yet.\n" + usage(), true, null);
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            builder.append("`/").append(name).append("`  ");
        }
        builder.append("\n_").append(names.size()).append(" of ").append(CustomCommands.limit())
                .append(" allowed · ").append(CommandToggles.enabledCount()).append(" built-ins on_");
        Commands.respond(rest, interaction, builder.toString(), true, null);
    }

    private static String usage() {
        return "**Custom commands and the bot builder**\n"
                + "`/custom add name:<name> response:<text>` — create or overwrite\n"
                + "`/custom remove name:<name>` — delete\n"
                + "`/custom list` — show them\n"
                + "`/custom functions` — every script function\n"
                + "`/custom clear` — delete all\n"
                + "Placeholders: `{user}`, `{username}`, `{channel}`. "
                + "The response can also be a script, e.g. `Hi $mention, you have $getUserVar[points] points`. "
                + "The app's **Bot builder** page has the editor, palette and a test run.";
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.US).trim().replace(' ', '-');
    }

    /** Fills the legacy {user} style placeholders before the script runs. */
    static String render(String response, JSONObject interaction) {
        String userId = Commands.invokerId(interaction);
        String channelId = Commands.channelId(interaction);
        String username = Commands.invokerName(interaction);
        return response
                .replace("{user}", userId == null ? "you" : "<@" + userId + ">")
                .replace("{username}", username == null ? "there" : username)
                .replace("{channel}", channelId == null ? "this channel" : "<#" + channelId + ">");
    }

    /** Runs a stored script and replies with whatever it produced. */
    static void run(DiscordRest rest, JSONObject interaction, String commandName, String source) throws Exception {
        Host host = new Host(rest, interaction, commandName);
        Script.Result result = Script.run(render(source, interaction), host);

        for (String message : result.messages) {
            host.post(message);
        }

        String output = result.output;
        if (result.failed()) {
            output = (output.isEmpty() ? "" : output + "\n") + "⚠️ **Script error:** " + result.error;
        }
        if (output.isEmpty() && result.messages.isEmpty()) {
            Commands.respond(rest, interaction,
                    "`/" + commandName + "` ran but produced no reply. Add some text or `$message[...]`.",
                    true, null);
            return;
        }
        if (!output.isEmpty()) {
            Commands.respond(rest, interaction, output, false, null);
        }
    }

    /** Bridges the interpreter to Discord and the on-phone stores. */
    private static final class Host implements Script.Host {

        private final DiscordRest rest;
        private final JSONObject interaction;
        private final String commandName;

        private JSONObject guild;
        private JSONObject channel;
        private boolean guildFetched;
        private boolean channelFetched;
        private Long latency;

        Host(DiscordRest rest, JSONObject interaction, String commandName) {
            this.rest = rest;
            this.interaction = interaction;
            this.commandName = commandName;
        }

        @Override
        public String var(String key) {
            return Variables.get(key);
        }

        @Override
        public void setVar(String key, String value) {
            Variables.set(key, value);
        }

        @Override
        public void message(String text) {
            post(text);
        }

        void post(String text) {
            try {
                String channelId = Commands.channelId(interaction);
                if (channelId == null || text.trim().isEmpty()) {
                    return;
                }
                JSONObject body = new JSONObject();
                body.put("content", text.length() > 2000 ? text.substring(0, 2000) : text);
                body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
                rest.post("/channels/" + channelId + "/messages", body.toString());
            } catch (Exception ignored) {
                // the channel may have gone away
            }
        }

        @Override
        public boolean hasPermission(String permission) {
            long bit = permissionBit(permission);
            if (bit == 0L) {
                return false;
            }
            return Commands.can(interaction, bit);
        }

        @Override
        public String info(String key) {
            switch (key) {
                case "username":
                case "displayname":
                    return Commands.orDefault(Commands.invokerName(interaction), "there");
                case "userid":
                    return Commands.orDefault(Commands.invokerId(interaction), "");
                case "mention": {
                    String userId = Commands.invokerId(interaction);
                    return userId == null ? "" : "<@" + userId + ">";
                }
                case "servername":
                    return guild() == null ? "" : guild().optString("name", "");
                case "serverid":
                    return Commands.orDefault(Commands.guildId(interaction), "");
                case "channelname":
                    return channel() == null ? "" : channel().optString("name", "");
                case "channelid":
                    return Commands.orDefault(Commands.channelId(interaction), "");
                case "commandname":
                    return commandName;
                case "botname":
                    return "BotDroid";
                case "version":
                    return BotState.VERSION;
                case "ping":
                    return Long.toString(ping());
                case "uptime":
                    return BotState.startedAt <= 0 ? "0m"
                            : DeviceInfo.duration(Math.max(0L,
                            (System.currentTimeMillis() - BotState.startedAt) / 1000L));
                case "guilds":
                    return Integer.toString(BotState.guildCount);
                case "membercount": {
                    if (guild() == null) {
                        return "0";
                    }
                    return Integer.toString(guild().optInt("approximate_member_count",
                            guild().optInt("member_count", 0)));
                }
                default:
                    return "";
            }
        }

        @Override
        public String arg(int index) {
            String[] parts = splitInput();
            int position = index - 1;
            return position >= 0 && position < parts.length ? parts[position] : "";
        }

        @Override
        public String args() {
            return Commands.orDefault(Commands.optString(interaction, "input"), "");
        }

        @Override
        public int argCount() {
            return splitInput().length;
        }

        private String[] splitInput() {
            String input = Commands.optString(interaction, "input");
            if (input == null || input.trim().isEmpty()) {
                return new String[0];
            }
            return input.trim().split("\\s+");
        }

        private long ping() {
            if (latency == null) {
                long start = System.currentTimeMillis();
                Net.Resp response = rest.get("/gateway");
                latency = response.ok() ? System.currentTimeMillis() - start : -1L;
            }
            return latency;
        }

        private JSONObject guild() {
            if (!guildFetched) {
                guildFetched = true;
                String guildId = Commands.guildId(interaction);
                if (guildId != null) {
                    Net.Resp response = rest.get("/guilds/" + guildId + "?with_counts=true");
                    if (response.ok()) {
                        try {
                            guild = new JSONObject(response.body);
                        } catch (Exception ignored) {
                            // leave null
                        }
                    }
                }
            }
            return guild;
        }

        private JSONObject channel() {
            if (!channelFetched) {
                channelFetched = true;
                String channelId = Commands.channelId(interaction);
                if (channelId != null) {
                    Net.Resp response = rest.get("/channels/" + channelId);
                    if (response.ok()) {
                        try {
                            channel = new JSONObject(response.body);
                        } catch (Exception ignored) {
                            // leave null
                        }
                    }
                }
            }
            return channel;
        }

        private static long permissionBit(String name) {
            String key = name == null ? "" : name.trim().toLowerCase(Locale.US).replace(' ', '_');
            switch (key) {
                case "administrator":
                    return Commands.ADMINISTRATOR;
                case "manage_server":
                case "manage_guild":
                    return Commands.MANAGE_GUILD;
                case "manage_channels":
                    return Commands.MANAGE_CHANNELS;
                case "manage_roles":
                    return Commands.MANAGE_ROLES;
                case "manage_nicknames":
                    return Commands.MANAGE_NICKNAMES;
                case "manage_messages":
                    return Commands.MANAGE_MESSAGES;
                case "manage_webhooks":
                    return Commands.MANAGE_WEBHOOKS;
                case "kick_members":
                    return Commands.KICK_MEMBERS;
                case "ban_members":
                    return Commands.BAN_MEMBERS;
                case "moderate_members":
                    return Commands.MODERATE_MEMBERS;
                case "mute_members":
                    return Commands.MUTE_MEMBERS;
                case "deafen_members":
                    return Commands.DEAFEN_MEMBERS;
                case "move_members":
                    return Commands.MOVE_MEMBERS;
                case "create_invite":
                    return Commands.CREATE_INVITE;
                case "view_audit_log":
                    return Commands.VIEW_AUDIT_LOG;
                default:
                    return 0L;
            }
        }
    }
}
