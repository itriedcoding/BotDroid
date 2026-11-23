package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Per-server automation kept on the phone: reaction roles, repeating messages
 * and giveaways.
 *
 * Nothing here is simulated. The gateway tells us when somebody reacts, the
 * timer decides when a schedule or a giveaway is due, and everything else is a
 * normal Discord REST call made by {@link DiscordRest}.
 */
final class Automation {

    interface Saver {
        void save(String json);
    }

    interface Logger {
        void log(String line);
    }

    static final int MAX_REACTION_ROLES = 50;
    static final int MAX_SCHEDULES = 20;
    static final int MAX_GIVEAWAYS = 20;
    static final int MAX_MESSAGE = 1500;
    static final int MAX_INTERVAL = 10080;
    static final int MAX_ROLE_MENUS = 10;
    static final int MAX_MENU_ROLES = 25;

    /** Component ids the role menus post; the rest of the id is the menu key. */
    static final String MENU_SELECT = "bd:rr:";
    static final String MENU_CLEAR = "bd:rrx:";

    /**
     * Permissions a role must not carry if it is offered on a self-service
     * menu: handing these out to anyone who clicks would be an escalation.
     */
    private static final long SELF_SERVICE_BLOCKED = (1L << 1) | (1L << 2) | (1L << 3) | (1L << 4)
            | (1L << 5) | (1L << 13) | (1L << 17) | (1L << 27) | (1L << 28) | (1L << 29)
            | (1L << 30) | (1L << 33) | (1L << 34) | (1L << 40);

    /** The emoji a giveaway entry uses. */
    static final String ENTRY_EMOJI = "🎉";

    private static final Random RANDOM = new Random();
    private static final long MINUTE = 60000L;

    static final class ReactionRole {
        final String messageId;
        final String emoji;
        final String roleId;
        final String channelId;

        ReactionRole(String messageId, String emoji, String roleId, String channelId) {
            this.messageId = messageId;
            this.emoji = emoji;
            this.roleId = roleId;
            this.channelId = channelId;
        }
    }

    static final class Schedule {
        final int id;
        final String guildId;
        final String channelId;
        final String message;
        final int minutes;
        long next;

        Schedule(int id, String guildId, String channelId, String message, int minutes, long next) {
            this.id = id;
            this.guildId = guildId;
            this.channelId = channelId;
            this.message = message;
            this.minutes = minutes;
            this.next = next;
        }
    }

    static final class Giveaway {
        final int id;
        final String guildId;
        final String channelId;
        final String messageId;
        final String prize;
        final int winners;
        long endsAt;
        boolean done;
        List<String> won = new ArrayList<>();

        Giveaway(int id, String guildId, String channelId, String messageId, String prize,
                 long endsAt, int winners) {
            this.id = id;
            this.guildId = guildId;
            this.channelId = channelId;
            this.messageId = messageId;
            this.prize = prize;
            this.endsAt = endsAt;
            this.winners = winners;
        }
    }

    /** A posted role picker: which roles its dropdown and button control. */
    static final class RoleMenu {
        final String id;
        final String channelId;
        final String messageId;
        final List<String> roleIds;

        RoleMenu(String id, String channelId, String messageId, List<String> roleIds) {
            this.id = id;
            this.channelId = channelId;
            this.messageId = messageId;
            this.roleIds = new ArrayList<>(roleIds);
        }
    }

    private static final class Guild {
        final String id;
        final List<ReactionRole> reactionRoles = new ArrayList<>();
        final List<Schedule> schedules = new ArrayList<>();
        final List<Giveaway> giveaways = new ArrayList<>();
        final List<RoleMenu> roleMenus = new ArrayList<>();
        int nextId = 1;

        Guild(String id) {
            this.id = id;
        }
    }

    private static final Map<String, Guild> GUILDS = new LinkedHashMap<>();
    private static volatile Saver saver;

    private Automation() {
    }

    static void setSaver(Saver value) {
        saver = value;
    }

    // ------------------------------------------------------------ persistence

    static synchronized void load(String json) {
        GUILDS.clear();
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject entry = array.optJSONObject(i);
                if (entry == null) {
                    continue;
                }
                String id = entry.optString("g", "");
                if (id.isEmpty()) {
                    continue;
                }
                Guild guild = guild(id);
                guild.nextId = Math.max(1, entry.optInt("i", 1));
                JSONArray roles = entry.optJSONArray("rr");
                if (roles != null) {
                    for (int k = 0; k < roles.length(); k++) {
                        JSONObject row = roles.optJSONObject(k);
                        if (row == null) {
                            continue;
                        }
                        String messageId = row.optString("m", "");
                        String emoji = row.optString("e", "");
                        String roleId = row.optString("r", "");
                        if (messageId.isEmpty() || emoji.isEmpty() || roleId.isEmpty()) {
                            continue;
                        }
                        guild.reactionRoles.add(new ReactionRole(messageId, emoji, roleId,
                                row.optString("c", "")));
                    }
                }
                JSONArray schedules = entry.optJSONArray("s");
                if (schedules != null) {
                    for (int k = 0; k < schedules.length(); k++) {
                        JSONObject row = schedules.optJSONObject(k);
                        if (row == null) {
                            continue;
                        }
                        String channelId = row.optString("c", "");
                        String message = row.optString("t", "");
                        if (channelId.isEmpty() || message.isEmpty()) {
                            continue;
                        }
                        guild.schedules.add(new Schedule(row.optInt("i", 0), id, channelId, message,
                                Math.max(1, row.optInt("e", 60)), row.optLong("n", 0L)));
                    }
                }
                JSONArray giveaways = entry.optJSONArray("gv");
                if (giveaways != null) {
                    for (int k = 0; k < giveaways.length(); k++) {
                        JSONObject row = giveaways.optJSONObject(k);
                        if (row == null) {
                            continue;
                        }
                        String messageId = row.optString("m", "");
                        if (messageId.isEmpty()) {
                            continue;
                        }
                        Giveaway giveaway = new Giveaway(row.optInt("i", 0), id, row.optString("c", ""),
                                messageId, row.optString("p", "Giveaway"), row.optLong("u", 0L),
                                Math.max(1, row.optInt("w", 1)));
                        giveaway.done = row.optBoolean("d", false);
                        JSONArray won = row.optJSONArray("x");
                        if (won != null) {
                            for (int w = 0; w < won.length(); w++) {
                                String winner = won.optString(w, "");
                                if (!winner.isEmpty()) {
                                    giveaway.won.add(winner);
                                }
                            }
                        }
                        guild.giveaways.add(giveaway);
                    }
                }
                JSONArray menus = entry.optJSONArray("rm");
                if (menus != null) {
                    for (int k = 0; k < menus.length(); k++) {
                        JSONObject row = menus.optJSONObject(k);
                        JSONArray ids = row == null ? null : row.optJSONArray("r");
                        String menuId = row == null ? "" : row.optString("i", "");
                        if (ids == null || menuId.isEmpty()) {
                            continue;
                        }
                        List<String> roleIds = new ArrayList<>();
                        for (int r = 0; r < ids.length() && roleIds.size() < MAX_MENU_ROLES; r++) {
                            String roleId = ids.optString(r, "");
                            if (!roleId.isEmpty() && !roleIds.contains(roleId)) {
                                roleIds.add(roleId);
                            }
                        }
                        if (!roleIds.isEmpty()) {
                            guild.roleMenus.add(new RoleMenu(menuId, row.optString("c", ""),
                                    row.optString("m", ""), roleIds));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // a corrupt store must never stop the bot
        }
    }

    static synchronized String json() {
        JSONArray array = new JSONArray();
        for (Guild guild : GUILDS.values()) {
            try {
                JSONObject entry = new JSONObject().put("g", guild.id).put("i", guild.nextId);
                JSONArray roles = new JSONArray();
                for (ReactionRole role : guild.reactionRoles) {
                    roles.put(new JSONObject().put("m", role.messageId).put("e", role.emoji)
                            .put("r", role.roleId).put("c", role.channelId));
                }
                entry.put("rr", roles);
                JSONArray schedules = new JSONArray();
                for (Schedule schedule : guild.schedules) {
                    schedules.put(new JSONObject().put("i", schedule.id).put("c", schedule.channelId)
                            .put("t", schedule.message).put("e", schedule.minutes).put("n", schedule.next));
                }
                entry.put("s", schedules);
                JSONArray giveaways = new JSONArray();
                for (Giveaway giveaway : guild.giveaways) {
                    JSONArray won = new JSONArray();
                    for (String winner : giveaway.won) {
                        won.put(winner);
                    }
                    giveaways.put(new JSONObject().put("i", giveaway.id).put("c", giveaway.channelId)
                            .put("m", giveaway.messageId).put("p", giveaway.prize)
                            .put("u", giveaway.endsAt).put("w", giveaway.winners)
                            .put("d", giveaway.done).put("x", won));
                }
                entry.put("gv", giveaways);
                JSONArray menus = new JSONArray();
                for (RoleMenu menu : guild.roleMenus) {
                    JSONArray roleIds = new JSONArray();
                    for (String roleId : menu.roleIds) {
                        roleIds.put(roleId);
                    }
                    menus.put(new JSONObject().put("i", menu.id).put("c", menu.channelId)
                            .put("m", menu.messageId).put("r", roleIds));
                }
                entry.put("rm", menus);
                array.put(entry);
            } catch (Exception ignored) {
                // skip a row that cannot be written rather than losing the rest
            }
        }
        return array.toString();
    }

    private static Guild guild(String guildId) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            guild = new Guild(guildId);
            GUILDS.put(guildId, guild);
        }
        return guild;
    }

    private static void persist() {
        Saver current = saver;
        if (current != null) {
            current.save(json());
        }
    }

    // -------------------------------------------------------- reaction roles

    static synchronized List<ReactionRole> reactionRoles(String guildId) {
        Guild guild = GUILDS.get(guildId);
        return guild == null ? new ArrayList<>() : new ArrayList<>(guild.reactionRoles);
    }

    static synchronized ReactionRole reactionRole(String guildId, String messageId, String emoji) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            return null;
        }
        for (ReactionRole role : guild.reactionRoles) {
            if (role.messageId.equals(messageId) && role.emoji.equals(emoji)) {
                return role;
            }
        }
        return null;
    }

    /** Returns false when the list is full or that message already has the emoji. */
    static synchronized boolean addReactionRole(String guildId, String messageId, String emoji,
                                                String roleId, String channelId) {
        Guild guild = guild(guildId);
        if (guild.reactionRoles.size() >= MAX_REACTION_ROLES) {
            return false;
        }
        if (reactionRole(guildId, messageId, emoji) != null) {
            return false;
        }
        guild.reactionRoles.add(new ReactionRole(messageId, emoji, roleId, channelId));
        persist();
        return true;
    }

    static synchronized boolean removeReactionRole(String guildId, String messageId, String emoji) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            return false;
        }
        for (int i = 0; i < guild.reactionRoles.size(); i++) {
            ReactionRole role = guild.reactionRoles.get(i);
            if (role.messageId.equals(messageId) && role.emoji.equals(emoji)) {
                guild.reactionRoles.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    static synchronized int clearReactionRoles(String guildId) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null || guild.reactionRoles.isEmpty()) {
            return 0;
        }
        int removed = guild.reactionRoles.size();
        guild.reactionRoles.clear();
        persist();
        return removed;
    }

    // ------------------------------------------------------------- role menus

    static synchronized List<RoleMenu> roleMenus(String guildId) {
        Guild guild = GUILDS.get(guildId);
        return guild == null ? new ArrayList<>() : new ArrayList<>(guild.roleMenus);
    }

    static synchronized RoleMenu findRoleMenu(String guildId, String id) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null || id == null || id.isEmpty()) {
            return null;
        }
        for (RoleMenu menu : guild.roleMenus) {
            if (menu.id.equals(id)) {
                return menu;
            }
        }
        return null;
    }

    /** Saves a posted menu, dropping the oldest one when the list is full. */
    static synchronized void addRoleMenu(String guildId, RoleMenu menu) {
        Guild guild = guild(guildId);
        while (guild.roleMenus.size() >= MAX_ROLE_MENUS) {
            guild.roleMenus.remove(0);
        }
        guild.roleMenus.add(menu);
        persist();
    }

    static synchronized int clearRoleMenus(String guildId) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null || guild.roleMenus.isEmpty()) {
            return 0;
        }
        int removed = guild.roleMenus.size();
        guild.roleMenus.clear();
        persist();
        return removed;
    }

    /** A component id that stays inside Discord's 100 character custom_id limit. */
    static String newMenuKey() {
        return "k" + Long.toString(System.nanoTime(), 36) + Integer.toString(RANDOM.nextInt(1296), 36);
    }

    /** A role that can be offered on a self-service menu without granting power. */
    static boolean isPickableRole(String guildId, JSONObject role) {
        if (role == null) {
            return false;
        }
        String id = role.optString("id", "");
        if (id.isEmpty() || id.equals(guildId) || role.optBoolean("managed", false)) {
            return false;
        }
        if (role.optInt("position", 0) <= 0) {
            return false;
        }
        long permissions;
        try {
            permissions = Long.parseLong(role.optString("permissions", "0"));
        } catch (NumberFormatException e) {
            return false;
        }
        return (permissions & SELF_SERVICE_BLOCKED) == 0;
    }

    /** The safe roles on a server, highest first, capped at one menu. */
    static List<String> pickableRoleIds(JSONArray roles, String guildId) {
        List<JSONObject> sorted = new ArrayList<>();
        if (roles != null) {
            for (int i = 0; i < roles.length(); i++) {
                JSONObject role = roles.optJSONObject(i);
                if (isPickableRole(guildId, role)) {
                    sorted.add(role);
                }
            }
        }
        Collections.sort(sorted, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                return b.optInt("position", 0) - a.optInt("position", 0);
            }
        });
        List<String> ids = new ArrayList<>();
        for (JSONObject role : sorted) {
            if (ids.size() >= MAX_MENU_ROLES) {
                break;
            }
            ids.add(role.optString("id", ""));
        }
        return ids;
    }

    /** Role IDs from "<@&123>", "123" or "@name" tokens; unknown roles are skipped. */
    static List<String> parseRoleIds(String raw, JSONArray roles, String guildId) {
        List<String> ids = new ArrayList<>();
        if (raw == null) {
            return ids;
        }
        String[] parts = raw.split("[,\\s]+");
        for (String part : parts) {
            String token = part.trim();
            if (token.startsWith("<@&") && token.endsWith(">")) {
                token = token.substring(3, token.length() - 1);
            } else if (token.startsWith("<@") && token.endsWith(">")) {
                token = token.substring(2, token.length() - 1);
            }
            if (!isDigits(token) || ids.contains(token)) {
                continue;
            }
            JSONObject role = findRole(roles, token);
            if (role == null || token.equals(guildId) || role.optBoolean("managed", false)) {
                continue;
            }
            ids.add(token);
            if (ids.size() >= MAX_MENU_ROLES) {
                break;
            }
        }
        return ids;
    }

    static JSONObject findRole(JSONArray roles, String id) {
        if (roles == null || id == null) {
            return null;
        }
        for (int i = 0; i < roles.length(); i++) {
            JSONObject role = roles.optJSONObject(i);
            if (role != null && id.equals(role.optString("id", ""))) {
                return role;
            }
        }
        return null;
    }

    static String roleName(JSONArray roles, String roleId) {
        JSONObject role = findRole(roles, roleId);
        String name = role == null ? "" : role.optString("name", "");
        return name.isEmpty() ? roleId : name;
    }

    /** What one click on a menu would change. */
    static final class RoleChange {
        final List<String> add = new ArrayList<>();
        final List<String> remove = new ArrayList<>();

        boolean isEmpty() {
            return add.isEmpty() && remove.isEmpty();
        }
    }

    /** One dropdown pick: add the role, or take it away when it is already held. */
    static RoleChange planToggle(List<String> memberRoles, List<String> menuRoles,
                                 List<String> selected) {
        RoleChange change = new RoleChange();
        String pick = selected == null || selected.isEmpty() ? "" : selected.get(0);
        if (pick.isEmpty() || !menuRoles.contains(pick)) {
            return change;
        }
        if (memberRoles != null && memberRoles.contains(pick)) {
            change.remove.add(pick);
        } else {
            change.add.add(pick);
        }
        return change;
    }

    static RoleChange planRoleChange(List<String> memberRoles, List<String> menuRoles,
                                     List<String> selected) {
        RoleChange change = new RoleChange();
        List<String> have = memberRoles == null ? new ArrayList<>() : memberRoles;
        List<String> picked = selected == null ? new ArrayList<>() : selected;
        for (String role : menuRoles) {
            boolean wanted = picked.contains(role);
            boolean has = have.contains(role);
            if (wanted && !has) {
                change.add.add(role);
            } else if (!wanted && has) {
                change.remove.add(role);
            }
        }
        return change;
    }

    // -------------------------------------------------------------- schedules

    static synchronized List<Schedule> schedules(String guildId) {
        Guild guild = GUILDS.get(guildId);
        return guild == null ? new ArrayList<>() : new ArrayList<>(guild.schedules);
    }

    static synchronized Schedule addSchedule(String guildId, String channelId, String message, int minutes,
                                             long now) {
        Guild guild = guild(guildId);
        if (guild.schedules.size() >= MAX_SCHEDULES) {
            return null;
        }
        Schedule schedule = new Schedule(guild.nextId++, guildId, channelId, message,
                Math.max(1, Math.min(MAX_INTERVAL, minutes)), now + Math.max(1, minutes) * MINUTE);
        guild.schedules.add(schedule);
        persist();
        return schedule;
    }

    static synchronized boolean removeSchedule(String guildId, int id) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            return false;
        }
        for (int i = 0; i < guild.schedules.size(); i++) {
            if (guild.schedules.get(i).id == id) {
                guild.schedules.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    static synchronized Schedule findSchedule(String guildId, int id) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            return null;
        }
        for (Schedule schedule : guild.schedules) {
            if (schedule.id == id) {
                return schedule;
            }
        }
        return null;
    }

    static synchronized List<Schedule> dueSchedules(long now) {
        List<Schedule> due = new ArrayList<>();
        for (Guild guild : GUILDS.values()) {
            for (Schedule schedule : guild.schedules) {
                if (schedule.next > 0 && schedule.next <= now) {
                    due.add(schedule);
                }
            }
        }
        return due;
    }

    static synchronized void markSent(String guildId, int id, long now) {
        Schedule schedule = findSchedule(guildId, id);
        if (schedule == null) {
            return;
        }
        schedule.next = advance(schedule.next, schedule.minutes, now);
        persist();
    }

    /**
     * The next time a schedule should run. Missed runs collapse into one, so a
     * phone that was asleep for a day posts once instead of ninety times.
     */
    static long advance(long current, int minutes, long now) {
        long step = Math.max(1, minutes) * MINUTE;
        long next = (current <= 0 ? now : current) + step;
        if (next <= now) {
            long missed = (now - next) / step + 1;
            next += missed * step;
        }
        return next;
    }

    // -------------------------------------------------------------- giveaways

    static synchronized List<Giveaway> giveaways(String guildId) {
        Guild guild = GUILDS.get(guildId);
        return guild == null ? new ArrayList<>() : new ArrayList<>(guild.giveaways);
    }

    static synchronized Giveaway addGiveaway(String guildId, String channelId, String messageId,
                                             String prize, long endsAt, int winners) {
        Guild guild = guild(guildId);
        if (guild.giveaways.size() >= MAX_GIVEAWAYS) {
            guild.giveaways.remove(0);
        }
        Giveaway giveaway = new Giveaway(guild.nextId++, guildId, channelId, messageId, prize, endsAt,
                Math.max(1, Math.min(10, winners)));
        guild.giveaways.add(giveaway);
        persist();
        return giveaway;
    }

    static synchronized Giveaway findGiveaway(String guildId, int id) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            return null;
        }
        for (Giveaway giveaway : guild.giveaways) {
            if (giveaway.id == id) {
                return giveaway;
            }
        }
        return null;
    }

    static synchronized boolean removeGiveaway(String guildId, int id) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            return false;
        }
        for (int i = 0; i < guild.giveaways.size(); i++) {
            if (guild.giveaways.get(i).id == id) {
                guild.giveaways.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    static synchronized List<Giveaway> dueGiveaways(long now) {
        List<Giveaway> due = new ArrayList<>();
        for (Guild guild : GUILDS.values()) {
            for (Giveaway giveaway : guild.giveaways) {
                if (!giveaway.done && giveaway.endsAt > 0 && giveaway.endsAt <= now) {
                    due.add(giveaway);
                }
            }
        }
        return due;
    }

    static synchronized boolean hasActiveGiveaway(String guildId) {
        Guild guild = GUILDS.get(guildId);
        if (guild == null) {
            return false;
        }
        for (Giveaway giveaway : guild.giveaways) {
            if (!giveaway.done) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------- events

    /**
     * Handles MESSAGE_REACTION_ADD / MESSAGE_REACTION_REMOVE: gives or takes a
     * role when the reaction belongs to a registered reaction-role message.
     */
    static void onReaction(DiscordRest rest, JSONObject event, boolean added, String botId, Logger log) {
        if (event == null) {
            return;
        }
        String userId = event.optString("user_id", "");
        String guildId = event.optString("guild_id", "");
        String messageId = event.optString("message_id", "");
        if (userId.isEmpty() || guildId.isEmpty() || messageId.isEmpty() || userId.equals(botId)) {
            return;
        }
        String emoji = emojiKey(event.optJSONObject("emoji"));
        ReactionRole role = reactionRole(guildId, messageId, emoji);
        if (role == null) {
            return;
        }
        String path = "/guilds/" + guildId + "/members/" + userId + "/roles/" + role.roleId;
        Net.Resp response = added ? rest.put(path, null) : rest.delete(path);
        if (response.ok()) {
            log.log((added ? "Gave " : "Removed ") + "role " + role.roleId + (added ? " to " : " from ")
                    + "member " + userId + " (reaction " + emoji + ")");
        } else {
            log.log("Could not " + (added ? "assign" : "remove") + " role " + role.roleId
                    + " (HTTP " + response.code + "): " + Commands.apiMessage(response.body)
                    + " — I need Manage Roles and a role above mine.");
        }
    }

    /**
     * The MESSAGE_COMPONENT side of a role menu: one dropdown pick toggles the
     * role (Discord keeps no menu state between clicks), the button takes every
     * role on the menu away.
     */
    static void onComponent(DiscordRest rest, JSONObject interaction, Logger log) {
        JSONObject data = interaction.optJSONObject("data");
        String customId = data == null ? "" : data.optString("custom_id", "");
        boolean clear = customId.startsWith(MENU_CLEAR);
        if (!clear && !customId.startsWith(MENU_SELECT)) {
            Commands.respond(rest, interaction, "That control is from an older BotDroid build — post the menu again.",
                    true, null);
            return;
        }
        String menuId = customId.substring((clear ? MENU_CLEAR : MENU_SELECT).length());
        String guildId = Commands.guildId(interaction);
        if (guildId == null) {
            Commands.respond(rest, interaction, "Role menus only work inside a server.", true, null);
            return;
        }
        RoleMenu menu = findRoleMenu(guildId, menuId);
        if (menu == null) {
            Commands.respond(rest, interaction,
                    "This role menu is no longer active. Ask an admin to post a new one.", true, null);
            return;
        }
        String userId = Commands.invokerId(interaction);
        if (userId == null) {
            Commands.respond(rest, interaction, "I could not tell who clicked that.", true, null);
            return;
        }
        List<String> selected = new ArrayList<>();
        if (!clear) {
            JSONArray values = data.optJSONArray("values");
            if (values != null && values.length() > 0) {
                String value = values.optString(0, "");
                if (menu.roleIds.contains(value)) {
                    selected.add(value);
                }
            }
        }
        Net.Resp member = rest.get("/guilds/" + guildId + "/members/" + userId);
        if (!member.ok()) {
            Commands.respond(rest, interaction, "I could not read your roles: "
                    + Commands.failure(member, "Manage Roles"), true, null);
            return;
        }
        List<String> current = new ArrayList<>();
        try {
            JSONArray roles = new JSONObject(member.body).optJSONArray("roles");
            if (roles != null) {
                for (int i = 0; i < roles.length(); i++) {
                    String role = roles.optString(i, "");
                    if (!role.isEmpty()) {
                        current.add(role);
                    }
                }
            }
        } catch (Exception ignored) {
            // an unreadable member body means there is no safe change to plan
        }
        RoleChange change = clear
                ? planRoleChange(current, menu.roleIds, new ArrayList<>())
                : planToggle(current, menu.roleIds, selected);
        if (change.isEmpty()) {
            Commands.respond(rest, interaction, clear
                    ? "You did not have any of the roles on this menu."
                    : "That role is not on this menu any more. Ask an admin to post it again.", true, null);
            return;
        }
        int failed = 0;
        for (String roleId : change.add) {
            Net.Resp response = rest.put("/guilds/" + guildId + "/members/" + userId + "/roles/" + roleId, null);
            if (!response.ok()) {
                failed++;
                log.log("Role menu " + menu.id + ": could not give role " + roleId + " to member " + userId
                        + " (HTTP " + response.code + "): " + Commands.apiMessage(response.body));
            }
        }
        for (String roleId : change.remove) {
            Net.Resp response = rest.delete("/guilds/" + guildId + "/members/" + userId + "/roles/" + roleId);
            if (!response.ok()) {
                failed++;
                log.log("Role menu " + menu.id + ": could not remove role " + roleId + " from member " + userId
                        + " (HTTP " + response.code + "): " + Commands.apiMessage(response.body));
            }
        }
        StringBuilder message = new StringBuilder();
        if (!change.add.isEmpty()) {
            message.append("Added ").append(mentions(change.add, 8)).append('.');
        }
        if (!change.remove.isEmpty()) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append("Removed ").append(mentions(change.remove, 8)).append('.');
        }
        if (failed > 0) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append(failed).append(failed == 1 ? " change was" : " changes were")
                    .append(" refused — my role has to sit above the roles on this menu.");
        }
        Commands.respond(rest, interaction, message.toString(), true, null);
        log.log("Role menu " + menu.id + ": member " + userId + " changed "
                + (change.add.size() + change.remove.size()) + " role(s)");
    }

    /**
     * Runs every 30 seconds from the gateway loop: posts due scheduled messages
     * and closes giveaways whose timer has run out.
     */
    static int tick(DiscordRest rest, long now, Logger log) {
        int actions = 0;
        for (Schedule schedule : dueSchedules(now)) {
            Net.Resp response = postMessage(rest, schedule.channelId, schedule.message);
            if (response.ok()) {
                actions++;
                log.log("Posted scheduled message in channel " + schedule.channelId);
            } else {
                log.log("Scheduled message failed in channel " + schedule.channelId + " (HTTP "
                        + response.code + "): " + Commands.apiMessage(response.body));
            }
            markSent(schedule.guildId, schedule.id, now);
        }
        for (Giveaway giveaway : dueGiveaways(now)) {
            if (finish(rest, giveaway, log)) {
                actions++;
            }
        }
        return actions;
    }

    /** Ends a giveaway now (the /giveaway end command uses this too). */
    static boolean end(DiscordRest rest, String guildId, int id, Logger log) {
        Giveaway giveaway = findGiveaway(guildId, id);
        if (giveaway == null) {
            return false;
        }
        return finish(rest, giveaway, log);
    }

    static boolean reroll(DiscordRest rest, String guildId, int id, Logger log) {
        Giveaway giveaway = findGiveaway(guildId, id);
        if (giveaway == null) {
            return false;
        }
        List<String> pool = entrants(rest, giveaway, log);
        if (pool == null) {
            return false;
        }
        List<String> picked = pick(pool, giveaway.winners);
        if (picked.isEmpty()) {
            postMessage(rest, giveaway.channelId, "No valid entries left to reroll **"
                    + giveaway.prize + "**.");
        } else {
            postMessage(rest, giveaway.channelId, "🎲 Rerolled **" + giveaway.prize + "** — new winner"
                    + (picked.size() == 1 ? ": " : "s: ") + mentions(picked));
            remember(guildId, id, picked);
        }
        log.log("Rerolled giveaway " + id + " in server " + guildId);
        return true;
    }

    private static boolean finish(DiscordRest rest, Giveaway giveaway, Logger log) {
        List<String> pool = entrants(rest, giveaway, log);
        if (pool == null) {
            markDone(giveaway.guildId, giveaway.id, new ArrayList<>());
            return false;
        }
        // Draw once: the announced winner has to be the one that was recorded.
        List<String> picked = pick(pool, giveaway.winners);
        markDone(giveaway.guildId, giveaway.id, picked);
        if (picked.isEmpty()) {
            postMessage(rest, giveaway.channelId, "🎉 **" + giveaway.prize
                    + "** ended with no valid entries.");
        } else {
            postMessage(rest, giveaway.channelId, "🎉 **" + giveaway.prize + "** — winner"
                    + (picked.size() == 1 ? ": " : "s: ") + mentions(picked)
                    + "\nCongratulations!");
        }
        log.log("Giveaway " + giveaway.id + " ended in server " + giveaway.guildId);
        return true;
    }

    /** Everyone who reacted with the entry emoji and is not a bot, or null on error. */
    private static List<String> entrants(DiscordRest rest, Giveaway giveaway, Logger log) {
        if (giveaway.channelId.isEmpty() || giveaway.messageId.isEmpty()) {
            return new ArrayList<>();
        }
        Net.Resp response = rest.get("/channels/" + giveaway.channelId + "/messages/"
                + giveaway.messageId + "/reactions/" + reactionPath(ENTRY_EMOJI) + "?limit=100");
        if (!response.ok()) {
            log.log("Could not read giveaway entries (HTTP " + response.code + "): "
                    + Commands.apiMessage(response.body));
            return null;
        }
        List<String> ids = new ArrayList<>();
        try {
            JSONArray users = new JSONArray(response.body);
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.optJSONObject(i);
                if (user == null || user.optBoolean("bot", false)) {
                    continue;
                }
                String id = user.optString("id", "");
                if (!id.isEmpty() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        } catch (Exception ignored) {
            // a malformed body means no entries we can trust
        }
        return ids;
    }

    private static List<String> pick(List<String> pool, int winners) {
        List<String> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, RANDOM);
        int count = Math.max(1, Math.min(winners, copy.size()));
        return copy.isEmpty() ? new ArrayList<>() : new ArrayList<>(copy.subList(0, count));
    }

    /** Posts a schedule immediately and restarts its timer from now. */
    static boolean runNow(DiscordRest rest, Schedule schedule, long now) {
        Net.Resp response = postMessage(rest, schedule.channelId, schedule.message);
        markSent(schedule.guildId, schedule.id, now);
        return response.ok();
    }

    private static synchronized void markDone(String guildId, int id, List<String> winners) {
        Giveaway giveaway = findGiveaway(guildId, id);
        if (giveaway == null) {
            return;
        }
        giveaway.done = true;
        giveaway.won = new ArrayList<>(winners);
        persist();
    }

    private static synchronized void remember(String guildId, int id, List<String> winners) {
        Giveaway giveaway = findGiveaway(guildId, id);
        if (giveaway == null) {
            return;
        }
        giveaway.won = new ArrayList<>(winners);
        persist();
    }

    private static String mentions(List<String> ids) {
        return mentions(ids, ids.size());
    }

    /** Mentions at most `limit` ids, then says how many were left out. */
    private static String mentions(List<String> ids, int limit) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i == limit) {
                text.append(", …and ").append(ids.size() - limit).append(" more");
                break;
            }
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append("<@").append(ids.get(i)).append('>');
        }
        return text.toString();
    }

    private static Net.Resp postMessage(DiscordRest rest, String channelId, String content) {
        JSONObject body = new JSONObject();
        try {
            body.put("content", content.length() > 2000 ? content.substring(0, 2000) : content);
            body.put("allowed_mentions", new JSONObject().put("parse",
                    new JSONArray().put("users").put("everyone")));
        } catch (Exception ignored) {
            // the content is always settable; this never happens
        }
        return rest.post("/channels/" + channelId + "/messages", body.toString());
    }

    // ---------------------------------------------------------------- helpers

    /**
     * A stable key for an emoji: "name:id" for a custom emoji and the bare
     * character for a unicode one, without the variation selector Discord often
     * drops.
     */
    static String emojiKey(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.length() > 2 && text.startsWith("<") && text.endsWith(">")) {
            String inner = text.substring(1, text.length() - 1);
            if (inner.startsWith("a:")) {
                inner = inner.substring(2);
            } else if (inner.startsWith(":")) {
                inner = inner.substring(1);
            }
            int colon = inner.lastIndexOf(':');
            if (colon > 0) {
                String name = inner.substring(0, colon);
                String id = inner.substring(colon + 1);
                if (!name.isEmpty() && isDigits(id)) {
                    return name + ":" + id;
                }
            }
            return text;
        }
        return text.replace("\uFE0F", "").replace("\uFE0E", "");
    }

    static String emojiKey(JSONObject emoji) {
        if (emoji == null) {
            return "";
        }
        String id = emoji.optString("id", "");
        String name = emoji.optString("name", "");
        if (!id.isEmpty() && !"null".equals(id) && !name.isEmpty()) {
            return name + ":" + id;
        }
        return emojiKey(name);
    }

    /** The percent-encoded path segment Discord expects for a reaction. */
    static String reactionPath(String key) {
        return Commands.encode(key);
    }

    static boolean isDigits(String value) {
        if (value == null || value.isEmpty() || value.length() > 24) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /** A short single-line preview for lists. */
    static String preview(String text, int limit) {
        String line = text.replace('\n', ' ').trim();
        return line.length() <= limit ? line : line.substring(0, limit) + "…";
    }
}
