package dev.botdroid.app;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/** /custom add|remove|list|clear plus execution of stored custom commands. */
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
        Commands.respond(rest, interaction, "Added `/" + name + "`. It is now registered in every server "
                + "the bot is in (give Discord a few seconds).\nUse `{user}`, `{username}` and `{channel}` "
                + "inside the response to fill in details at run time.", true, null);
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
        return "**Custom commands**\n"
                + "`/custom add name:<name> response:<text>` — create or overwrite\n"
                + "`/custom remove name:<name>` — delete\n"
                + "`/custom list` — show them\n"
                + "`/custom clear` — delete all\n"
                + "Placeholders: `{user}`, `{username}`, `{channel}`";
    }

    private static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.US).trim().replace(' ', '-');
    }

    /** Fills placeholders in a stored custom command response. */
    static String render(String response, JSONObject interaction) {
        String userId = Commands.invokerId(interaction);
        String channelId = Commands.channelId(interaction);
        String username = Commands.invokerName(interaction);
        return response
                .replace("{user}", userId == null ? "you" : "<@" + userId + ">")
                .replace("{username}", username == null ? "there" : username)
                .replace("{channel}", channelId == null ? "this channel" : "<#" + channelId + ">");
    }
}
