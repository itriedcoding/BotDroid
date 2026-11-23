package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * The server automation commands: reaction roles, repeating messages and
 * giveaways. All three read and write {@link Automation}, which stores the
 * settings on the phone; the gateway loop fires the events and timers.
 */
final class CommandsAutomation {

    /** Definitions merged after the built-ins in {@link Commands}. */
    static final String DEFINITIONS = "["
            + "{\"name\":\"reactionrole\",\"description\":\"Let members pick roles by reacting to a message\","
            + "\"default_member_permissions\":\"268435456\",\"options\":["
            + "{\"type\":1,\"name\":\"add\",\"description\":\"Make a reaction on a message give a role\",\"options\":["
            + "{\"type\":3,\"name\":\"message_id\",\"description\":\"Message ID (Copy Message ID in Discord)\","
            + "\"required\":true,\"max_length\":24},"
            + "{\"type\":3,\"name\":\"emoji\",\"description\":\"One emoji, e.g. 🎉 or a custom one\","
            + "\"required\":true,\"max_length\":64},"
            + "{\"type\":8,\"name\":\"role\",\"description\":\"Role to give\",\"required\":true},"
            + "{\"type\":7,\"name\":\"channel\",\"description\":\"Channel with the message (default: this one)\","
            + "\"required\":false}]},"
            + "{\"type\":1,\"name\":\"menu\",\"description\":\"Post a clickable role picker\",\"options\":["
            + "{\"type\":7,\"name\":\"channel\",\"description\":\"Where to post it (default: this channel)\","
            + "\"required\":false},"
            + "{\"type\":3,\"name\":\"title\",\"description\":\"Heading (default: Pick your roles)\","
            + "\"required\":false,\"max_length\":256},"
            + "{\"type\":3,\"name\":\"description\",\"description\":\"Text under the heading\","
            + "\"required\":false,\"max_length\":1000},"
            + "{\"type\":3,\"name\":\"roles\",\"description\":\"Roles to offer: @mentions or IDs, comma separated (default: every safe role)\","
            + "\"required\":false,\"max_length\":1000}]},"
            + "{\"type\":1,\"name\":\"remove\",\"description\":\"Stop a reaction from giving a role\",\"options\":["
            + "{\"type\":3,\"name\":\"message_id\",\"description\":\"Message ID\",\"required\":true,\"max_length\":24},"
            + "{\"type\":3,\"name\":\"emoji\",\"description\":\"The emoji to unpair\",\"required\":true,"
            + "\"max_length\":64}]},"
            + "{\"type\":1,\"name\":\"list\",\"description\":\"Reaction roles in this server\"},"
            + "{\"type\":1,\"name\":\"clear\",\"description\":\"Delete every reaction role here\"}]},"
            + "{\"name\":\"schedule\",\"description\":\"Post a message in a channel on a repeating timer\","
            + "\"default_member_permissions\":\"32\",\"options\":["
            + "{\"type\":1,\"name\":\"add\",\"description\":\"Start a repeating message\",\"options\":["
            + "{\"type\":7,\"name\":\"channel\",\"description\":\"Where to post it\",\"required\":true},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"What to post\",\"required\":true,\"max_length\":1500},"
            + "{\"type\":4,\"name\":\"minutes\",\"description\":\"How often, in minutes (5-10080)\","
            + "\"required\":true,\"min_value\":5,\"max_value\":10080}]},"
            + "{\"type\":1,\"name\":\"list\",\"description\":\"Repeating messages stored here\"},"
            + "{\"type\":1,\"name\":\"remove\",\"description\":\"Delete a repeating message\",\"options\":["
            + "{\"type\":4,\"name\":\"id\",\"description\":\"Number from /schedule list\",\"required\":true,"
            + "\"min_value\":1}]},"
            + "{\"type\":1,\"name\":\"run\",\"description\":\"Post it now and restart its timer\",\"options\":["
            + "{\"type\":4,\"name\":\"id\",\"description\":\"Number from /schedule list\",\"required\":true,"
            + "\"min_value\":1}]}]},"
            + "{\"name\":\"giveaway\",\"description\":\"Run a reaction giveaway and pick a winner\","
            + "\"default_member_permissions\":\"32\",\"options\":["
            + "{\"type\":1,\"name\":\"start\",\"description\":\"Start a giveaway\",\"options\":["
            + "{\"type\":7,\"name\":\"channel\",\"description\":\"Where to post it\",\"required\":true},"
            + "{\"type\":3,\"name\":\"prize\",\"description\":\"What is being given away\",\"required\":true,"
            + "\"max_length\":200},"
            + "{\"type\":4,\"name\":\"minutes\",\"description\":\"How long it runs (1-10080)\",\"required\":true,"
            + "\"min_value\":1,\"max_value\":10080},"
            + "{\"type\":4,\"name\":\"winners\",\"description\":\"How many winners (default 1)\","
            + "\"required\":false,\"min_value\":1,\"max_value\":10}]},"
            + "{\"type\":1,\"name\":\"list\",\"description\":\"Giveaways in this server\"},"
            + "{\"type\":1,\"name\":\"end\",\"description\":\"End a giveaway now and pick the winner\",\"options\":["
            + "{\"type\":4,\"name\":\"id\",\"description\":\"Number from /giveaway list\",\"required\":true,"
            + "\"min_value\":1}]},"
            + "{\"type\":1,\"name\":\"reroll\",\"description\":\"Pick a new winner for a finished giveaway\","
            + "\"options\":["
            + "{\"type\":4,\"name\":\"id\",\"description\":\"Number from /giveaway list\",\"required\":true,"
            + "\"min_value\":1}]}]}"
            + "]";

    /** Command handlers report to the user, so nothing is logged twice here. */
    private static final Automation.Logger QUIET = line -> {
    };

    private CommandsAutomation() {
    }

    // ---------------------------------------------------------- reaction roles

    static void reactionRole(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            reply(rest, interaction, "Run this inside a server.");
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_ROLES)) {
            reply(rest, interaction, "You need the **Manage Roles** permission for that.");
            return;
        }
        String sub = Commands.subcommandName(interaction);
        if ("add".equals(sub)) {
            addReactionRole(rest, interaction, guildId);
            return;
        }
        if ("menu".equals(sub)) {
            roleMenu(rest, interaction, guildId);
            return;
        }
        if ("remove".equals(sub)) {
            String messageId = trim(Commands.optString(interaction, "message_id"));
            String emoji = Automation.emojiKey(Commands.optString(interaction, "emoji"));
            if (!Automation.isDigits(messageId) || emoji.isEmpty()) {
                reply(rest, interaction, "Give me the message ID and the emoji you want to remove.");
                return;
            }
            reply(rest, interaction, Automation.removeReactionRole(guildId, messageId, emoji)
                    ? "Reacting with " + emoji + " no longer gives a role."
                    : "That message and emoji are not set up as a reaction role.");
            return;
        }
        if ("list".equals(sub)) {
            listReactionRoles(rest, interaction, guildId);
            return;
        }
        if ("clear".equals(sub)) {
            int removed = Automation.clearReactionRoles(guildId);
            int menus = Automation.clearRoleMenus(guildId);
            if (removed == 0 && menus == 0) {
                reply(rest, interaction, "There were no reaction roles or role menus to delete.");
                return;
            }
            StringBuilder message = new StringBuilder("Deleted ");
            if (removed > 0) {
                message.append("**").append(removed).append("** reaction role")
                        .append(removed == 1 ? "" : "s");
                if (menus > 0) {
                    message.append(" and ");
                }
            }
            if (menus > 0) {
                message.append("**").append(menus).append("** role menu").append(menus == 1 ? "" : "s");
            }
            message.append(". The reactions and menus already on messages are left alone; they just stop working.");
            reply(rest, interaction, message.toString());
            return;
        }
        reply(rest, interaction, "Use `/reactionrole add`, `menu`, `remove`, `list` or `clear`.");
    }

    private static void addReactionRole(DiscordRest rest, JSONObject interaction, String guildId)
            throws Exception {
        String messageId = trim(Commands.optString(interaction, "message_id"));
        if (!Automation.isDigits(messageId)) {
            reply(rest, interaction, "That is not a message ID. In Discord, turn on "
                    + "**Settings → Advanced → Developer Mode**, then right-click the message → "
                    + "**Copy Message ID**.");
            return;
        }
        String emoji = Automation.emojiKey(Commands.optString(interaction, "emoji"));
        if (emoji.isEmpty() || emoji.length() > 60) {
            reply(rest, interaction, "Give me exactly one emoji, for example `🎉` or a custom emoji.");
            return;
        }
        String roleId = trim(Commands.optString(interaction, "role"));
        if (!Automation.isDigits(roleId)) {
            reply(rest, interaction, "Pick the role that reaction should give.");
            return;
        }
        String channelId = trim(Commands.optString(interaction, "channel"));
        if (channelId.isEmpty()) {
            channelId = Commands.channelId(interaction);
        }
        if (channelId == null || channelId.isEmpty()) {
            reply(rest, interaction, "Tell me which channel has that message.");
            return;
        }
        if (Automation.reactionRole(guildId, messageId, emoji) != null) {
            reply(rest, interaction, "That message already gives a role for " + emoji
                    + ". Remove it first with `/reactionrole remove`.");
            return;
        }

        // The bot has to react itself, otherwise members have nothing to click.
        Net.Resp reaction = rest.put("/channels/" + channelId + "/messages/" + messageId
                + "/reactions/" + Automation.reactionPath(emoji) + "/@me", null);
        if (!reaction.ok()) {
            reply(rest, interaction, "I could not react to that message: "
                    + Commands.failure(reaction, "Add Reactions and Read Message History")
                    + "\nCheck the message ID and that I can see <#" + channelId + ">.");
            return;
        }
        if (!Automation.addReactionRole(guildId, messageId, emoji, roleId, channelId)) {
            reply(rest, interaction, "I already track **" + Automation.MAX_REACTION_ROLES
                    + "** reaction roles here. Remove one first.");
            return;
        }
        reply(rest, interaction, "Reacting with " + emoji + " on `" + messageId + "` in <#" + channelId
                + "> now gives <@&" + roleId + ">.\nUn-reacting takes it away again, and my role has to sit "
                + "above it.");
    }

    private static void listReactionRoles(DiscordRest rest, JSONObject interaction, String guildId)
            throws Exception {
        List<Automation.ReactionRole> roles = Automation.reactionRoles(guildId);
        List<Automation.RoleMenu> menus = Automation.roleMenus(guildId);
        if (roles.isEmpty() && menus.isEmpty()) {
            reply(rest, interaction, "No reaction roles yet. Use `/reactionrole add` for a reaction, "
                    + "or `/reactionrole menu` for a clickable picker.");
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < roles.size() && i < 25; i++) {
            Automation.ReactionRole role = roles.get(i);
            description.append(role.emoji).append(" → <@&").append(role.roleId).append("> · message `")
                    .append(role.messageId).append("` in <#").append(role.channelId).append(">\n");
        }
        if (roles.size() > 25) {
            description.append("_and ").append(roles.size() - 25).append(" more._\n");
        }
        if (!menus.isEmpty()) {
            description.append("**Role menus**\n");
            for (Automation.RoleMenu menu : menus) {
                description.append("· ").append(menu.roleIds.size()).append(" role")
                        .append(menu.roleIds.size() == 1 ? "" : "s").append(" in <#")
                        .append(menu.channelId).append(">\n");
            }
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Reaction roles · " + roles.size()
                        + (menus.isEmpty() ? "" : " · " + menus.size() + " menu" + (menus.size() == 1 ? "" : "s")),
                        0x5865F2, description.toString().trim())));
    }

    /**
     * `/reactionrole menu`: posts one message with a dropdown of roles and a
     * button that clears them. Members pick roles by clicking; no message IDs.
     */
    private static void roleMenu(DiscordRest rest, JSONObject interaction, String guildId)
            throws Exception {
        String channelId = trim(Commands.optString(interaction, "channel"));
        if (channelId.isEmpty()) {
            channelId = Commands.channelId(interaction);
        }
        if (channelId == null || channelId.isEmpty()) {
            reply(rest, interaction, "Pick the channel to post the menu in.");
            return;
        }
        Net.Resp roleResponse = rest.get("/guilds/" + guildId + "/roles");
        if (!roleResponse.ok()) {
            reply(rest, interaction, "I could not read the roles here: "
                    + Commands.failure(roleResponse, "Manage Roles"));
            return;
        }
        JSONArray roles;
        try {
            roles = new JSONArray(roleResponse.body);
        } catch (Exception e) {
            reply(rest, interaction, "Discord sent a role list I could not read. Try again.");
            return;
        }
        String raw = Commands.optString(interaction, "roles");
        boolean automatic = raw == null || raw.trim().isEmpty();
        List<String> roleIds = automatic
                ? Automation.pickableRoleIds(roles, guildId)
                : Automation.parseRoleIds(raw, roles, guildId);
        if (roleIds.isEmpty()) {
            reply(rest, interaction, automatic
                    ? "I found no roles I can hand out — they are all managed or carry moderator permissions. "
                    + "Name the roles instead, e.g. `roles:@Blue @Red`."
                    : "None of those roles exist here, or they are managed by an integration. "
                    + "Use @mentions or role IDs.");
            return;
        }
        Net.Resp self = rest.get("/guilds/" + guildId + "/members/@me");
        if (self.ok()) {
            long permissions = 0;
            try {
                permissions = Long.parseLong(new JSONObject(self.body).optString("permissions", "0"));
            } catch (Exception ignored) {
                // an unreadable member leaves the post to fail with its own reason
            }
            if ((permissions & Commands.MANAGE_ROLES) == 0) {
                reply(rest, interaction, "I cannot hand out roles yet: give me **Manage Roles** "
                        + "and put my role above the roles you want on the menu.");
                return;
            }
        }

        String title = trim(Commands.optString(interaction, "title"));
        if (title.isEmpty()) {
            title = "Pick your roles";
        }
        String description = trim(Commands.optString(interaction, "description"));
        if (description.isEmpty()) {
            description = "Pick a role to add it, pick the same role again to remove it. "
                    + "The button clears every role on this menu.";
        }
        String key = Automation.newMenuKey();
        JSONArray options = new JSONArray();
        for (String roleId : roleIds) {
            options.put(Components.option(roleId, Automation.roleName(roles, roleId), null));
        }
        JSONArray components = Components.rows(
                Components.row(Components.select(Automation.MENU_SELECT + key, "Pick a role",
                        options, 1, 1)),
                Components.row(Components.button(Automation.MENU_CLEAR + key, "Remove all",
                        Components.BUTTON_SECONDARY, "🧹")));
        JSONObject body = new JSONObject();
        body.put("embeds", new JSONArray().put(Commands.embed(title, 0x5865F2, description)));
        body.put("components", components);
        body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
        Net.Resp posted = rest.post("/channels/" + channelId + "/messages", body.toString());
        if (!posted.ok()) {
            reply(rest, interaction, "I could not post the menu: "
                    + Commands.failure(posted, "Send Messages and Embed Links"));
            return;
        }
        String messageId = new JSONObject(posted.body).optString("id", "");
        if (messageId.isEmpty()) {
            reply(rest, interaction,
                    "Discord accepted the menu but returned no ID, so it would not work. Nothing was saved.");
            return;
        }
        Automation.addRoleMenu(guildId, new Automation.RoleMenu(key, channelId, messageId, roleIds));
        reply(rest, interaction, "Posted a role menu with **" + roleIds.size() + "** role"
                + (roleIds.size() == 1 ? "" : "s") + " in <#" + channelId + ">.\n"
                + "Members pick a role to add or remove it; **Remove all** clears them. "
                + "My role has to sit above every role on the menu.");
    }

    // -------------------------------------------------------------- schedules

    static void schedule(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            reply(rest, interaction, "Run this inside a server.");
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_GUILD)) {
            reply(rest, interaction,
                    "You need the **Manage Server** permission for that.");
            return;
        }
        String sub = Commands.subcommandName(interaction);
        if ("add".equals(sub)) {
            String channelId = trim(Commands.optString(interaction, "channel"));
            String text = Commands.optString(interaction, "text");
            int minutes = Commands.optInt(interaction, "minutes", 60);
            if (channelId.isEmpty()) {
                reply(rest, interaction, "Pick the channel to post in.");
                return;
            }
            if (text == null || text.trim().isEmpty()) {
                reply(rest, interaction, "Give me the message to post.");
                return;
            }
            if (minutes < 1 || minutes > Automation.MAX_INTERVAL) {
                reply(rest, interaction, "The interval has to be between 1 and "
                        + Automation.MAX_INTERVAL + " minutes.");
                return;
            }
            Automation.Schedule added = Automation.addSchedule(guildId, channelId,
                    text.length() > Automation.MAX_MESSAGE ? text.substring(0, Automation.MAX_MESSAGE) : text,
                    minutes, System.currentTimeMillis());
            if (added == null) {
                reply(rest, interaction, "This server already has **" + Automation.MAX_SCHEDULES
                        + "** repeating messages. Remove one first.");
                return;
            }
            reply(rest, interaction, "Scheduled **#" + added.id + "**: this will be posted in <#" + channelId
                    + "> every **" + minutes + " minutes**, first <t:" + (added.next / 1000) + ":R>.\n"
                    + "_The timer runs inside BotDroid, so the bot has to be running at that moment._");
            return;
        }
        if ("list".equals(sub)) {
            listSchedules(rest, interaction, guildId);
            return;
        }
        if ("remove".equals(sub)) {
            int id = Commands.optInt(interaction, "id", 0);
            reply(rest, interaction, Automation.removeSchedule(guildId, id)
                    ? "Deleted repeating message **#" + id + "**."
                    : "There is no repeating message **#" + id + "** here.");
            return;
        }
        if ("run".equals(sub)) {
            Automation.Schedule target = Automation.findSchedule(guildId,
                    Commands.optInt(interaction, "id", 0));
            if (target == null) {
                reply(rest, interaction, "There is no repeating message with that number.");
                return;
            }
            boolean posted = Automation.runNow(rest, target, System.currentTimeMillis());
            reply(rest, interaction, posted
                    ? "Posted **#" + target.id + "** in <#" + target.channelId + ">. Its timer restarted."
                    : "I could not post **#" + target.id + "** — give me **Send Messages** in <#"
                    + target.channelId + ">.");
            return;
        }
        reply(rest, interaction, "Use `/schedule add`, `list`, `remove` or `run`.");
    }

    private static void listSchedules(DiscordRest rest, JSONObject interaction, String guildId)
            throws Exception {
        List<Automation.Schedule> schedules = Automation.schedules(guildId);
        if (schedules.isEmpty()) {
            reply(rest, interaction, "No repeating messages yet. Start one with `/schedule add`.");
            return;
        }
        StringBuilder description = new StringBuilder();
        for (Automation.Schedule schedule : schedules) {
            description.append("**#").append(schedule.id).append("** <#").append(schedule.channelId)
                    .append("> · every ").append(schedule.minutes).append(" min · next <t:")
                    .append(schedule.next / 1000).append(":R>\n> ")
                    .append(Automation.preview(schedule.message, 120)).append("\n\n");
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Repeating messages · " + schedules.size(), 0x5865F2,
                        description.toString().trim())));
    }

    // -------------------------------------------------------------- giveaways

    static void giveaway(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            reply(rest, interaction, "Run this inside a server.");
            return;
        }
        if (!Commands.can(interaction, Commands.MANAGE_GUILD)) {
            reply(rest, interaction, "You need the **Manage Server** permission for that.");
            return;
        }
        String sub = Commands.subcommandName(interaction);
        if ("start".equals(sub)) {
            startGiveaway(rest, interaction, guildId);
            return;
        }
        if ("list".equals(sub)) {
            listGiveaways(rest, interaction, guildId);
            return;
        }
        if ("end".equals(sub)) {
            int id = Commands.optInt(interaction, "id", 0);
            Automation.Giveaway giveaway = Automation.findGiveaway(guildId, id);
            if (giveaway == null) {
                reply(rest, interaction, "There is no giveaway **#" + id + "** here.");
                return;
            }
            if (giveaway.done) {
                reply(rest, interaction, "Giveaway **#" + id + "** already ended. "
                        + "Use `/giveaway reroll` for a new winner.");
                return;
            }
            reply(rest, interaction, Automation.end(rest, guildId, id, QUIET)
                    ? "Giveaway **#" + id + "** ended — the winner is in <#" + giveaway.channelId + ">."
                    : "I could not read the entries for **#" + id + "** — the message may be deleted.");
            return;
        }
        if ("reroll".equals(sub)) {
            int id = Commands.optInt(interaction, "id", 0);
            Automation.Giveaway giveaway = Automation.findGiveaway(guildId, id);
            if (giveaway == null) {
                reply(rest, interaction, "There is no giveaway **#" + id + "** here.");
                return;
            }
            reply(rest, interaction, Automation.reroll(rest, guildId, id, QUIET)
                    ? "Rerolled giveaway **#" + id + "** from the same reactions."
                    : "I could not read the entries for **#" + id + "** — the message may be deleted.");
            return;
        }
        reply(rest, interaction, "Use `/giveaway start`, `list`, `end` or `reroll`.");
    }

    private static void startGiveaway(DiscordRest rest, JSONObject interaction, String guildId)
            throws Exception {
        String channelId = trim(Commands.optString(interaction, "channel"));
        String prize = Commands.optString(interaction, "prize");
        int minutes = Commands.optInt(interaction, "minutes", 60);
        int winners = Commands.optInt(interaction, "winners", 1);
        if (channelId.isEmpty()) {
            reply(rest, interaction, "Pick the channel to run the giveaway in.");
            return;
        }
        if (prize == null || prize.trim().isEmpty()) {
            reply(rest, interaction, "Give me the prize.");
            return;
        }
        if (minutes < 1 || minutes > Automation.MAX_INTERVAL) {
            reply(rest, interaction, "The length has to be between 1 and " + Automation.MAX_INTERVAL
                    + " minutes.");
            return;
        }
        winners = Math.max(1, Math.min(10, winners));
        long endsAt = System.currentTimeMillis() + minutes * 60000L;
        String host = Commands.invokerId(interaction);
        String description = "React with " + Automation.ENTRY_EMOJI + " to enter.\nEnds <t:"
                + (endsAt / 1000) + ":R> · **" + winners + "** winner" + (winners == 1 ? "" : "s")
                + (host == null ? "" : "\nHosted by <@" + host + ">");
        JSONObject body = new JSONObject();
        body.put("embeds", new JSONArray().put(Commands.embed("🎉 " + prize, 0x5793266, description)));
        body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
        Net.Resp posted = rest.post("/channels/" + channelId + "/messages", body.toString());
        if (!posted.ok()) {
            reply(rest, interaction, "I could not post the giveaway: "
                    + Commands.failure(posted, "Send Messages and Embed Links"));
            return;
        }
        String messageId = new JSONObject(posted.body).optString("id", "");
        if (messageId.isEmpty()) {
            reply(rest, interaction, "Discord accepted the message but returned no ID, so I cannot track "
                    + "the entries. Nothing was saved.");
            return;
        }
        Net.Resp reaction = rest.put("/channels/" + channelId + "/messages/" + messageId
                + "/reactions/" + Automation.reactionPath(Automation.ENTRY_EMOJI) + "/@me", null);
        if (!reaction.ok()) {
            reply(rest, interaction, "I posted the giveaway but could not add the entry reaction, so "
                    + "there is nothing to enter on. Give me **Add Reactions** and run it again.");
            return;
        }
        Automation.Giveaway giveaway = Automation.addGiveaway(guildId, channelId, messageId,
                prize.length() > 200 ? prize.substring(0, 200) : prize, endsAt, winners);
        reply(rest, interaction, "Giveaway **#" + giveaway.id + "** started in <#" + channelId
                + "> — ends <t:" + (endsAt / 1000) + ":R>.\n_It is closed by BotDroid's timer, so the bot "
                + "has to be running when it ends._");
    }

    private static void listGiveaways(DiscordRest rest, JSONObject interaction, String guildId)
            throws Exception {
        List<Automation.Giveaway> giveaways = Automation.giveaways(guildId);
        if (giveaways.isEmpty()) {
            reply(rest, interaction, "No giveaways here yet. Start one with `/giveaway start`.");
            return;
        }
        StringBuilder description = new StringBuilder();
        for (int i = giveaways.size() - 1; i >= 0 && description.length() < 3500; i--) {
            Automation.Giveaway giveaway = giveaways.get(i);
            description.append("**#").append(giveaway.id).append("** 🎉 ").append(giveaway.prize)
                    .append(" · <#").append(giveaway.channelId).append("> · ");
            if (giveaway.done) {
                description.append("ended");
                if (!giveaway.won.isEmpty()) {
                    description.append(" · winner ");
                    for (int w = 0; w < giveaway.won.size(); w++) {
                        if (w > 0) {
                            description.append(", ");
                        }
                        description.append("<@").append(giveaway.won.get(w)).append('>');
                    }
                }
            } else {
                description.append("ends <t:").append(giveaway.endsAt / 1000).append(":R>");
            }
            description.append("\n");
        }
        Commands.respond(rest, interaction, "", true, new JSONArray().put(
                Commands.embed("Giveaways · " + giveaways.size(), 0x5793266, description.toString().trim())));
    }

    // ---------------------------------------------------------------- helpers

    private static void reply(DiscordRest rest, JSONObject interaction, String text) {
        Commands.respond(rest, interaction, text, true, null);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
