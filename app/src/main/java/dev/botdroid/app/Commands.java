package dev.botdroid.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Slash command registry: definitions, dispatch, shared helpers and core handlers. */
final class Commands {

    static final long CREATE_INVITE = 1L << 0;
    static final long KICK_MEMBERS = 1L << 1;
    static final long BAN_MEMBERS = 1L << 2;
    static final long ADMINISTRATOR = 1L << 3;
    static final long MANAGE_CHANNELS = 1L << 4;
    static final long VIEW_AUDIT_LOG = 1L << 7;
    static final long MANAGE_GUILD = 1L << 5;
    static final long MANAGE_WEBHOOKS = 1L << 29;

    /** Discord allows 100 commands per server, built-in and custom combined. */
    static final int MAX_COMMANDS = 100;
    static final long MANAGE_MESSAGES = 1L << 13;
    static final long MANAGE_NICKNAMES = 1L << 27;
    static final long MANAGE_ROLES = 1L << 28;
    static final long MUTE_MEMBERS = 1L << 22;
    static final long DEAFEN_MEMBERS = 1L << 23;
    static final long MOVE_MEMBERS = 1L << 24;
    static final long MODERATE_MEMBERS = 1L << 40;

    private static final long[] PERMISSION_BITS = {
            1L << 3, 1L << 5, 1L << 4, 1L << 28, 1L << 27, 1L << 13, 1L << 1, 1L << 2, 1L << 40,
            1L << 7, 1L << 17, 1L << 24, 1L << 22, 1L << 23, 1L << 29, 1L << 30, 1L << 10, 1L << 11,
            1L << 14, 1L << 15, 1L << 16, 1L << 18, 1L << 6, 1L << 20, 1L << 21, 1L << 26, 1L << 31
    };

    private static final String[] PERMISSION_LABELS = {
            "Administrator", "Manage Server", "Manage Channels", "Manage Roles", "Manage Nicknames",
            "Manage Messages", "Kick Members", "Ban Members", "Moderate Members", "View Audit Log",
            "Mention Everyone", "Move Members", "Mute Members", "Deafen Members", "Manage Webhooks",
            "Manage Emojis", "View Channels", "Send Messages", "Embed Links", "Attach Files",
            "Read Message History", "Use External Emojis", "Add Reactions", "Connect", "Speak",
            "Change Nickname", "Use Application Commands"
    };

    private static final Random RANDOM = new Random();

    private static final String[] EIGHT_BALL = {
            "It is certain.", "Without a doubt.", "Yes, definitely.", "Most likely.",
            "Outlook good.", "Signs point to yes.", "Reply hazy, try again.",
            "Ask again later.", "Better not tell you now.", "Cannot predict now.",
            "Don't count on it.", "My reply is no.", "My sources say no.",
            "Outlook not so good.", "Very doubtful."
    };

    private static final String HELP = "**BotDroid " + BotState.VERSION + " — commands**\n"
            + "__Core__\n"
            + "`/help` `/ping` `/botinfo` `/uptime` `/invite`\n"
            + "__Server and users__\n"
            + "`/serverinfo` `/membercount` `/roles` `/channelinfo` `/emojis` `/userinfo` `/avatar`\n"
            + "__Utility__\n"
            + "`/time` `/math` `/random` `/choose` `/poll` `/remind` `/base64` `/color`\n"
            + "__Moderation__\n"
            + "`/ban` `/unban` `/softban` `/kick` `/timeout` `/purge` `/slowmode` `/lock` `/unlock` "
            + "`/hide` `/unhide` `/topic` `/nick` `/addrole` `/removerole` `/banlist` `/pin` `/unpin` `/say`\n"
            + "__Fun__\n"
            + "`/8ball` `/roll` `/coinflip` `/rps` `/joke` `/fact` `/quote` `/rate` `/reverse`\n"
            + "__Server tools__\n"
            + "`/joined` `/memberroles` `/userperms` `/createinvite` `/invites` `/webhooks` `/threads` "
            + "`/events` `/stickers` `/emojiinfo` `/serverbanner` `/announce` `/dm` `/commandcount`\n"
            + "__Converters__\n"
            + "`/timezone` `/countdown` `/regex` `/binary` `/morse` `/convert`\n"
            + "__Info__\n"
            + "`/about` `/servericon` `/banner` `/timestamp` `/snowflake` `/myperms` `/pins` `/inviteinfo`\n"
            + "__Text tools__\n"
            + "`/password` `/uuid` `/hash` `/url` `/case` `/wordcount`\n"
            + "__Roles, channels, voice__\n"
            + "`/roleinfo` `/rolecolor` `/createrole` `/createchannel` `/clonechannel` `/move` `/mute` `/deafen` "
            + "`/untimeout` `/baninfo`\n"
            + "__More fun__\n"
            + "`/fortune` `/compliment` `/hug` `/pat` `/clap` `/spoiler`\n"
            + "__Device and server__\n"
            + "`/status` `/serverstats` `/channels` `/channelperms` `/auditlog` `/thread` `/topinvites`\n"
            + "__Warnings__\n"
            + "`/warn add|list|clear` `/warnings`\n"
            + "__Text tools 2__\n"
            + "`/mock` `/rot13` `/caesar` `/sortlines` `/dedupe` `/hex` `/charinfo` `/repeat` `/embed` "
            + "`/weekday` `/percentage`\n"
            + "__Roblox__\n"
            + "`/roblox user|avatar|game|group|games`\n"
            + "__Custom and the bot builder__\n"
            + "`/custom add|remove|list|clear|functions`\n"
            + "Responses can be plain text or a script: `$mention`, `$args`, `$if[...]{...}`, "
            + "`$setUserVar[...]` and more. `/custom functions` lists them all.";

    private static volatile Set<String> presetNames;

    private Commands() {
    }

    // ------------------------------------------------------------- definitions

    private static final String DEFINITIONS = "["
            + "{\"name\":\"help\",\"description\":\"Show every command BotDroid can run\"},"
            + "{\"name\":\"ping\",\"description\":\"Check the bot latency and connection\"},"
            + "{\"name\":\"botinfo\",\"description\":\"Bot version, uptime and server count\"},"
            + "{\"name\":\"uptime\",\"description\":\"How long the bot has been running\"},"
            + "{\"name\":\"invite\",\"description\":\"Get the bot invite link\"},"
            + "{\"name\":\"serverinfo\",\"description\":\"Show information about this server\"},"
            + "{\"name\":\"membercount\",\"description\":\"Member and online counts\"},"
            + "{\"name\":\"roles\",\"description\":\"List this server's roles\"},"
            + "{\"name\":\"channelinfo\",\"description\":\"Details about this channel\"},"
            + "{\"name\":\"emojis\",\"description\":\"List this server's custom emojis\"},"
            + "{\"name\":\"time\",\"description\":\"Show the current time\"},"
            + "{\"name\":\"math\",\"description\":\"Evaluate a maths expression\",\"options\":["
            + "{\"type\":3,\"name\":\"expression\",\"description\":\"Example: (2+3)*4\",\"required\":true,\"max_length\":120}]},"
            + "{\"name\":\"random\",\"description\":\"Random number in a range\",\"options\":["
            + "{\"type\":4,\"name\":\"min\",\"description\":\"Minimum\",\"required\":true},"
            + "{\"type\":4,\"name\":\"max\",\"description\":\"Maximum\",\"required\":true}]},"
            + "{\"name\":\"choose\",\"description\":\"Pick one of your options\",\"options\":["
            + "{\"type\":3,\"name\":\"options\",\"description\":\"Separate options with |\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"poll\",\"description\":\"Create a reaction poll\",\"options\":["
            + "{\"type\":3,\"name\":\"question\",\"description\":\"Poll question\",\"required\":true,\"max_length\":200},"
            + "{\"type\":3,\"name\":\"option1\",\"description\":\"First option\",\"required\":true,\"max_length\":80},"
            + "{\"type\":3,\"name\":\"option2\",\"description\":\"Second option\",\"required\":true,\"max_length\":80},"
            + "{\"type\":3,\"name\":\"option3\",\"description\":\"Third option\",\"required\":false,\"max_length\":80},"
            + "{\"type\":3,\"name\":\"option4\",\"description\":\"Fourth option\",\"required\":false,\"max_length\":80}]},"
            + "{\"name\":\"remind\",\"description\":\"Set a reminder in this channel\",\"options\":["
            + "{\"type\":4,\"name\":\"minutes\",\"description\":\"Minutes from now (1-10080)\",\"required\":true,\"min_value\":1,\"max_value\":10080},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"What to remind you about\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"base64\",\"description\":\"Encode or decode base64\",\"options\":["
            + "{\"type\":3,\"name\":\"mode\",\"description\":\"encode or decode\",\"required\":true,\"choices\":["
            + "{\"name\":\"encode\",\"value\":\"encode\"},{\"name\":\"decode\",\"value\":\"decode\"}]},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to process\",\"required\":true,\"max_length\":1500}]},"
            + "{\"name\":\"color\",\"description\":\"Preview a hex colour\",\"options\":["
            + "{\"type\":3,\"name\":\"hex\",\"description\":\"For example #5865F2\",\"required\":true,\"max_length\":12}]},"
            + "{\"name\":\"userinfo\",\"description\":\"Show information about a user\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"User to look up (defaults to you)\",\"required\":false}]},"
            + "{\"name\":\"avatar\",\"description\":\"Show a user avatar\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"User (defaults to you)\",\"required\":false}]},"
            + "{\"name\":\"ban\",\"description\":\"Ban a member from this server\",\"default_member_permissions\":\"4\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member to ban\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason for the ban\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"unban\",\"description\":\"Remove a ban using the user ID\",\"default_member_permissions\":\"4\",\"options\":["
            + "{\"type\":3,\"name\":\"user_id\",\"description\":\"ID of the banned user\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"softban\",\"description\":\"Ban then unban to delete recent messages\",\"default_member_permissions\":\"4\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member to softban\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"kick\",\"description\":\"Kick a member from this server\",\"default_member_permissions\":\"2\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member to kick\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason for the kick\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"timeout\",\"description\":\"Temporarily mute a member\",\"default_member_permissions\":\"1099511627776\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member to time out\",\"required\":true},"
            + "{\"type\":4,\"name\":\"minutes\",\"description\":\"Length in minutes (1-10080)\",\"required\":true,\"min_value\":1,\"max_value\":10080},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"purge\",\"description\":\"Delete recent messages in this channel\",\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":4,\"name\":\"count\",\"description\":\"How many messages (1-100)\",\"required\":true,\"min_value\":1,\"max_value\":100}]},"
            + "{\"name\":\"slowmode\",\"description\":\"Set channel slowmode\",\"default_member_permissions\":\"16\",\"options\":["
            + "{\"type\":4,\"name\":\"seconds\",\"description\":\"Seconds between messages (0-21600)\",\"required\":true,\"min_value\":0,\"max_value\":21600}]},"
            + "{\"name\":\"lock\",\"description\":\"Stop @everyone sending messages here\",\"default_member_permissions\":\"16\"},"
            + "{\"name\":\"unlock\",\"description\":\"Allow @everyone to send messages here\",\"default_member_permissions\":\"16\"},"
            + "{\"name\":\"hide\",\"description\":\"Hide this channel from @everyone\",\"default_member_permissions\":\"16\"},"
            + "{\"name\":\"unhide\",\"description\":\"Make this channel visible to @everyone\",\"default_member_permissions\":\"16\"},"
            + "{\"name\":\"topic\",\"description\":\"Set or clear this channel's topic\",\"default_member_permissions\":\"16\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"New topic (empty clears it)\",\"required\":false,\"max_length\":1024}]},"
            + "{\"name\":\"nick\",\"description\":\"Change a member nickname\",\"default_member_permissions\":\"134217728\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true},"
            + "{\"type\":3,\"name\":\"name\",\"description\":\"New nickname (empty resets it)\",\"required\":false,\"max_length\":32}]},"
            + "{\"name\":\"addrole\",\"description\":\"Give a member a role\",\"default_member_permissions\":\"268435456\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true},"
            + "{\"type\":8,\"name\":\"role\",\"description\":\"Role to add\",\"required\":true}]},"
            + "{\"name\":\"removerole\",\"description\":\"Remove a role from a member\",\"default_member_permissions\":\"268435456\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true},"
            + "{\"type\":8,\"name\":\"role\",\"description\":\"Role to remove\",\"required\":true}]},"
            + "{\"name\":\"banlist\",\"description\":\"Show recent bans\",\"default_member_permissions\":\"4\"},"
            + "{\"name\":\"pin\",\"description\":\"Pin a message by ID\",\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":3,\"name\":\"message_id\",\"description\":\"Message ID\",\"required\":true,\"max_length\":32}]},"
            + "{\"name\":\"unpin\",\"description\":\"Unpin a message by ID\",\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":3,\"name\":\"message_id\",\"description\":\"Message ID\",\"required\":true,\"max_length\":32}]},"
            + "{\"name\":\"say\",\"description\":\"Make BotDroid post a message\",\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Message to send\",\"required\":true,\"max_length\":2000}]},"
            + "{\"name\":\"8ball\",\"description\":\"Ask the magic 8-ball a question\",\"options\":["
            + "{\"type\":3,\"name\":\"question\",\"description\":\"Your question\",\"required\":true,\"max_length\":200}]},"
            + "{\"name\":\"roll\",\"description\":\"Roll dice, for example 2d6\",\"options\":["
            + "{\"type\":3,\"name\":\"dice\",\"description\":\"Format NdN (default 1d6)\",\"required\":false,\"max_length\":12}]},"
            + "{\"name\":\"coinflip\",\"description\":\"Flip a coin\"},"
            + "{\"name\":\"rps\",\"description\":\"Play rock paper scissors\",\"options\":["
            + "{\"type\":3,\"name\":\"choice\",\"description\":\"Your move\",\"required\":true,\"choices\":["
            + "{\"name\":\"rock\",\"value\":\"rock\"},{\"name\":\"paper\",\"value\":\"paper\"},{\"name\":\"scissors\",\"value\":\"scissors\"}]}]},"
            + "{\"name\":\"joke\",\"description\":\"Tell a programming joke\"},"
            + "{\"name\":\"fact\",\"description\":\"Share a random fact\"},"
            + "{\"name\":\"quote\",\"description\":\"Share a random quote\"},"
            + "{\"name\":\"rate\",\"description\":\"Rate something out of 100\",\"options\":["
            + "{\"type\":3,\"name\":\"thing\",\"description\":\"What to rate\",\"required\":true,\"max_length\":100}]},"
            + "{\"name\":\"reverse\",\"description\":\"Reverse some text\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to reverse\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"joined\",\"description\":\"When a member joined and when their account was made\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true}]},"
            + "{\"name\":\"memberroles\",\"description\":\"Roles a member has\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true}]},"
            + "{\"name\":\"userperms\",\"description\":\"Permissions a member gets from their roles\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true}]},"
            + "{\"name\":\"createinvite\",\"description\":\"Create an invite for this channel\",\"options\":["
            + "{\"type\":4,\"name\":\"max_age\",\"description\":\"Seconds until it expires (0 = never)\",\"required\":false,\"min_value\":0,\"max_value\":604800},"
            + "{\"type\":4,\"name\":\"max_uses\",\"description\":\"How many uses (0 = unlimited)\",\"required\":false,\"min_value\":0,\"max_value\":100}]},"
            + "{\"name\":\"invites\",\"description\":\"Active invites in this server\",\"default_member_permissions\":\"32\"},"
            + "{\"name\":\"webhooks\",\"description\":\"Webhooks in this channel\",\"default_member_permissions\":\"536870912\"},"
            + "{\"name\":\"threads\",\"description\":\"Active threads in this channel\"},"
            + "{\"name\":\"events\",\"description\":\"Scheduled events in this server\"},"
            + "{\"name\":\"stickers\",\"description\":\"Custom stickers in this server\"},"
            + "{\"name\":\"emojiinfo\",\"description\":\"Details about one custom emoji\",\"options\":["
            + "{\"type\":3,\"name\":\"emoji\",\"description\":\"The emoji or its name\",\"required\":true,\"max_length\":100}]},"
            + "{\"name\":\"announce\",\"description\":\"Post an announcement embed in a channel\",\"default_member_permissions\":\"8192\",\"options\":["
            + "{\"type\":7,\"name\":\"channel\",\"description\":\"Where to post\",\"required\":true},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"What to announce\",\"required\":true,\"max_length\":2000}]},"
            + "{\"name\":\"dm\",\"description\":\"Send a direct message as the bot\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Who to message\",\"required\":true},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Message text\",\"required\":true,\"max_length\":1500}]},"
            + "{\"name\":\"timezone\",\"description\":\"Current time in a time zone\",\"options\":["
            + "{\"type\":3,\"name\":\"zone\",\"description\":\"IANA zone, e.g. Europe/London\",\"required\":true,\"max_length\":64}]},"
            + "{\"name\":\"countdown\",\"description\":\"Count down to a date (YYYY-MM-DD)\",\"options\":["
            + "{\"type\":3,\"name\":\"date\",\"description\":\"2030-01-01 or 2030-01-01T18:30\",\"required\":true,\"max_length\":32}]},"
            + "{\"name\":\"commandcount\",\"description\":\"How many of Discord's 100 command slots are used\"},"
            + "{\"name\":\"serverbanner\",\"description\":\"Show this server's banner\"},"
            + "{\"name\":\"regex\",\"description\":\"Test a regular expression against text\",\"options\":["
            + "{\"type\":3,\"name\":\"pattern\",\"description\":\"Java regular expression\",\"required\":true,\"max_length\":200},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to search\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"binary\",\"description\":\"Text to binary or binary to text\",\"options\":["
            + "{\"type\":3,\"name\":\"mode\",\"description\":\"encode or decode\",\"required\":true,\"choices\":["
            + "{\"name\":\"encode\",\"value\":\"encode\"},{\"name\":\"decode\",\"value\":\"decode\"}]},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to process\",\"required\":true,\"max_length\":1000}]},"
            + "{\"name\":\"morse\",\"description\":\"Morse code encoder and decoder\",\"options\":["
            + "{\"type\":3,\"name\":\"mode\",\"description\":\"encode or decode\",\"required\":true,\"choices\":["
            + "{\"name\":\"encode\",\"value\":\"encode\"},{\"name\":\"decode\",\"value\":\"decode\"}]},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text or morse to process\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"convert\",\"description\":\"Convert units of length, mass or temperature\",\"options\":["
            + "{\"type\":10,\"name\":\"value\",\"description\":\"Amount to convert\",\"required\":true},"
            + "{\"type\":3,\"name\":\"from\",\"description\":\"Unit, e.g. km or f\",\"required\":true,\"max_length\":8},"
            + "{\"type\":3,\"name\":\"to\",\"description\":\"Target unit, e.g. mi or c\",\"required\":true,\"max_length\":8}]},"
            + "{\"name\":\"about\",\"description\":\"About BotDroid: version, commands and source\"},"
            + "{\"name\":\"servericon\",\"description\":\"Show this server's icon\"},"
            + "{\"name\":\"banner\",\"description\":\"Show a user profile banner\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"User (defaults to you)\",\"required\":false}]},"
            + "{\"name\":\"timestamp\",\"description\":\"Build Discord timestamps from unix time\",\"options\":["
            + "{\"type\":3,\"name\":\"when\",\"description\":\"now, unix seconds or milliseconds\",\"required\":true,\"max_length\":32}]},"
            + "{\"name\":\"snowflake\",\"description\":\"Find when a Discord ID was created\",\"options\":["
            + "{\"type\":3,\"name\":\"id\",\"description\":\"Discord ID\",\"required\":true,\"max_length\":24}]},"
            + "{\"name\":\"myperms\",\"description\":\"Show your permissions in this server\"},"
            + "{\"name\":\"pins\",\"description\":\"List pinned messages in this channel\"},"
            + "{\"name\":\"inviteinfo\",\"description\":\"Inspect a Discord invite\",\"options\":["
            + "{\"type\":3,\"name\":\"code\",\"description\":\"Invite code or link\",\"required\":true,\"max_length\":120}]},"
            + "{\"name\":\"password\",\"description\":\"Generate a random password\",\"options\":["
            + "{\"type\":4,\"name\":\"length\",\"description\":\"Length (8-64, default 16)\",\"required\":false,\"min_value\":8,\"max_value\":64}]},"
            + "{\"name\":\"uuid\",\"description\":\"Generate a random UUID\"},"
            + "{\"name\":\"hash\",\"description\":\"Hash some text\",\"options\":["
            + "{\"type\":3,\"name\":\"algorithm\",\"description\":\"Hash algorithm\",\"required\":true,\"choices\":["
            + "{\"name\":\"md5\",\"value\":\"md5\"},{\"name\":\"sha1\",\"value\":\"sha1\"},{\"name\":\"sha256\",\"value\":\"sha256\"}]},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to hash\",\"required\":true,\"max_length\":1000}]},"
            + "{\"name\":\"url\",\"description\":\"URL encode or decode text\",\"options\":["
            + "{\"type\":3,\"name\":\"mode\",\"description\":\"encode or decode\",\"required\":true,\"choices\":["
            + "{\"name\":\"encode\",\"value\":\"encode\"},{\"name\":\"decode\",\"value\":\"decode\"}]},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to process\",\"required\":true,\"max_length\":1000}]},"
            + "{\"name\":\"case\",\"description\":\"Change text case\",\"options\":["
            + "{\"type\":3,\"name\":\"mode\",\"description\":\"Case style\",\"required\":true,\"choices\":["
            + "{\"name\":\"upper\",\"value\":\"upper\"},{\"name\":\"lower\",\"value\":\"lower\"},"
            + "{\"name\":\"title\",\"value\":\"title\"},{\"name\":\"swap\",\"value\":\"swap\"}]},"
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to change\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"wordcount\",\"description\":\"Count characters, words and lines\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to count\",\"required\":true,\"max_length\":1000}]},"
            + "{\"name\":\"roleinfo\",\"description\":\"Show details about a role\",\"options\":["
            + "{\"type\":8,\"name\":\"role\",\"description\":\"Role\",\"required\":true}]},"
            + "{\"name\":\"rolecolor\",\"description\":\"Change a role colour\",\"default_member_permissions\":\"268435456\",\"options\":["
            + "{\"type\":8,\"name\":\"role\",\"description\":\"Role\",\"required\":true},"
            + "{\"type\":3,\"name\":\"hex\",\"description\":\"Hex colour, for example #5865F2\",\"required\":true,\"max_length\":12}]},"
            + "{\"name\":\"createrole\",\"description\":\"Create a role\",\"default_member_permissions\":\"268435456\",\"options\":["
            + "{\"type\":3,\"name\":\"name\",\"description\":\"Role name\",\"required\":true,\"max_length\":100},"
            + "{\"type\":3,\"name\":\"color\",\"description\":\"Optional hex colour\",\"required\":false,\"max_length\":12}]},"
            + "{\"name\":\"createchannel\",\"description\":\"Create a text or voice channel\",\"default_member_permissions\":\"16\",\"options\":["
            + "{\"type\":3,\"name\":\"name\",\"description\":\"Channel name\",\"required\":true,\"max_length\":100},"
            + "{\"type\":3,\"name\":\"type\",\"description\":\"Channel type\",\"required\":false,\"choices\":["
            + "{\"name\":\"text\",\"value\":\"text\"},{\"name\":\"voice\",\"value\":\"voice\"}]}]},"
            + "{\"name\":\"clonechannel\",\"description\":\"Copy this channel's settings into a new one\",\"default_member_permissions\":\"16\"},"
            + "{\"name\":\"move\",\"description\":\"Move a member to a voice channel\",\"default_member_permissions\":\"16777216\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true},"
            + "{\"type\":7,\"name\":\"channel\",\"description\":\"Voice channel\",\"required\":true}]},"
            + "{\"name\":\"mute\",\"description\":\"Server mute a member\",\"default_member_permissions\":\"4194304\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true}]},"
            + "{\"name\":\"deafen\",\"description\":\"Server deafen a member\",\"default_member_permissions\":\"8388608\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true}]},"
            + "{\"name\":\"untimeout\",\"description\":\"Remove a member timeout\",\"default_member_permissions\":\"1099511627776\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Member\",\"required\":true},"
            + "{\"type\":3,\"name\":\"reason\",\"description\":\"Reason\",\"required\":false,\"max_length\":512}]},"
            + "{\"name\":\"baninfo\",\"description\":\"Check why a user was banned\",\"default_member_permissions\":\"4\",\"options\":["
            + "{\"type\":3,\"name\":\"user_id\",\"description\":\"Banned user ID\",\"required\":true,\"max_length\":24}]},"
            + "{\"name\":\"fortune\",\"description\":\"Open a fortune cookie\"},"
            + "{\"name\":\"compliment\",\"description\":\"Compliment someone\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Who to compliment\",\"required\":false}]},"
            + "{\"name\":\"hug\",\"description\":\"Hug someone\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Who to hug\",\"required\":true}]},"
            + "{\"name\":\"pat\",\"description\":\"Pat someone on the head\",\"options\":["
            + "{\"type\":6,\"name\":\"user\",\"description\":\"Who to pat\",\"required\":true}]},"
            + "{\"name\":\"clap\",\"description\":\"Put claps between words\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to clap\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"spoiler\",\"description\":\"Hide text behind a spoiler\",\"options\":["
            + "{\"type\":3,\"name\":\"text\",\"description\":\"Text to hide\",\"required\":true,\"max_length\":500}]},"
            + "{\"name\":\"roblox\",\"description\":\"Roblox lookups\",\"options\":["
            + "{\"type\":1,\"name\":\"user\",\"description\":\"Look up a Roblox user\",\"options\":["
            + "{\"type\":3,\"name\":\"username\",\"description\":\"Roblox username\",\"required\":true,\"max_length\":32}]},"
            + "{\"type\":1,\"name\":\"avatar\",\"description\":\"Show a Roblox avatar\",\"options\":["
            + "{\"type\":3,\"name\":\"username\",\"description\":\"Roblox username\",\"required\":true,\"max_length\":32}]},"
            + "{\"type\":1,\"name\":\"game\",\"description\":\"Look up a Roblox game\",\"options\":["
            + "{\"type\":3,\"name\":\"universe_id\",\"description\":\"Roblox universe ID\",\"required\":true,\"max_length\":24}]},"
            + "{\"type\":1,\"name\":\"group\",\"description\":\"Look up a Roblox group\",\"options\":["
            + "{\"type\":3,\"name\":\"group_id\",\"description\":\"Roblox group ID\",\"required\":true,\"max_length\":24}]},"
            + "{\"type\":1,\"name\":\"games\",\"description\":\"List a user's Roblox games\",\"options\":["
            + "{\"type\":3,\"name\":\"username\",\"description\":\"Roblox username\",\"required\":true,\"max_length\":32}]}]},"
            + "{\"name\":\"custom\",\"description\":\"Create and manage your own commands\",\"options\":["
            + "{\"type\":1,\"name\":\"add\",\"description\":\"Create or overwrite a command\",\"options\":["
            + "{\"type\":3,\"name\":\"name\",\"description\":\"Command name, e.g. discord\",\"required\":true,\"max_length\":32},"
            + "{\"type\":3,\"name\":\"response\",\"description\":\"What it replies with\",\"required\":true,\"max_length\":1500}]},"
            + "{\"type\":1,\"name\":\"remove\",\"description\":\"Delete a command\",\"options\":["
            + "{\"type\":3,\"name\":\"name\",\"description\":\"Command name\",\"required\":true,\"max_length\":32}]},"
            + "{\"type\":1,\"name\":\"list\",\"description\":\"List custom commands\"},"
            + "{\"type\":1,\"name\":\"functions\",\"description\":\"Show the script functions the bot builder supports\"},"
            + "{\"type\":1,\"name\":\"clear\",\"description\":\"Delete every custom command\"}]}"
            + "]";

    static JSONArray definitions() {
        JSONArray merged = new JSONArray();
        // Custom commands go first so a full server never hides the user's own commands.
        JSONArray custom = CustomCommands.definitions();
        for (int i = 0; i < custom.length() && merged.length() < MAX_COMMANDS; i++) {
            JSONObject entry = custom.optJSONObject(i);
            if (entry != null) {
                merged.put(entry);
            }
        }
        try {
            JSONArray presets = presetDefinitions();
            for (int i = 0; i < presets.length() && merged.length() < MAX_COMMANDS; i++) {
                JSONObject entry = presets.optJSONObject(i);
                if (entry != null && CommandToggles.isEnabled(entry.optString("name", ""))) {
                    merged.put(entry);
                }
            }
        } catch (Exception ignored) {
            // keep the custom commands we already collected
        }
        return merged;
    }

    /** Enabled built-ins that did not fit in Discord's 100 command limit. */
    static int skippedPresetCount() {
        int registeredPresets = Math.max(0, definitions().length() - CustomCommands.count());
        return Math.max(0, CommandToggles.enabledCount() - registeredPresets);
    }

    static int presetCount() {
        return presetDefinitions().length();
    }

    /** The built-ins from this file plus the newer ones in {@link CommandsExtra}. */
    static JSONArray presetDefinitions() {
        JSONArray merged = new JSONArray();
        try {
            JSONArray base = new JSONArray(DEFINITIONS);
            for (int i = 0; i < base.length(); i++) {
                JSONObject entry = base.optJSONObject(i);
                if (entry != null) {
                    merged.put(entry);
                }
            }
        } catch (Exception ignored) {
            // skip the broken half rather than losing everything
        }
        try {
            JSONArray extra = new JSONArray(CommandsExtra.DEFINITIONS);
            for (int i = 0; i < extra.length(); i++) {
                JSONObject entry = extra.optJSONObject(i);
                if (entry != null) {
                    merged.put(entry);
                }
            }
        } catch (Exception ignored) {
            // skip
        }
        return merged;
    }

    static List<String> presetNames() {
        List<String> names = new ArrayList<>();
        JSONArray presets = presetDefinitions();
        for (int i = 0; i < presets.length(); i++) {
            JSONObject entry = presets.optJSONObject(i);
            if (entry != null) {
                names.add(entry.optString("name", ""));
            }
        }
        return names;
    }

    static boolean isPreset(String name) {
        Set<String> names = presetNames;
        if (names == null) {
            names = new HashSet<>();
            try {
                JSONArray array = presetDefinitions();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject command = array.optJSONObject(i);
                    if (command != null) {
                        names.add(command.optString("name", ""));
                    }
                }
            } catch (Exception ignored) {
                // treat as empty, the dispatcher still guards
            }
            presetNames = names;
        }
        return names.contains(name);
    }

    // ---------------------------------------------------------------- dispatch

    static void handle(DiscordRest rest, JSONObject interaction) {
        try {
            JSONObject data = interaction.optJSONObject("data");
            String name = data == null ? "" : data.optString("name", "");
            if (!name.isEmpty() && isPreset(name) && !CommandToggles.isEnabled(name)) {
                respond(rest, interaction, "`/" + name + "` is turned off in BotDroid → Settings → Commands.",
                        true, null);
                return;
            }
            if ("help".equals(name)) {
                help(rest, interaction);
            } else if ("ping".equals(name)) {
                ping(rest, interaction);
            } else if ("botinfo".equals(name)) {
                CommandsUtil.botInfo(rest, interaction);
            } else if ("uptime".equals(name)) {
                CommandsUtil.uptime(rest, interaction);
            } else if ("invite".equals(name)) {
                CommandsUtil.invite(rest, interaction);
            } else if ("serverinfo".equals(name)) {
                serverInfo(rest, interaction);
            } else if ("membercount".equals(name)) {
                CommandsUtil.memberCount(rest, interaction);
            } else if ("roles".equals(name)) {
                CommandsUtil.roles(rest, interaction);
            } else if ("channelinfo".equals(name)) {
                CommandsUtil.channelInfo(rest, interaction);
            } else if ("emojis".equals(name)) {
                CommandsUtil.emojis(rest, interaction);
            } else if ("time".equals(name)) {
                CommandsUtil.time(rest, interaction);
            } else if ("math".equals(name)) {
                CommandsUtil.math(rest, interaction);
            } else if ("random".equals(name)) {
                CommandsUtil.random(rest, interaction);
            } else if ("choose".equals(name)) {
                CommandsUtil.choose(rest, interaction);
            } else if ("poll".equals(name)) {
                CommandsUtil.poll(rest, interaction);
            } else if ("remind".equals(name)) {
                CommandsUtil.remind(rest, interaction);
            } else if ("base64".equals(name)) {
                CommandsUtil.base64(rest, interaction);
            } else if ("color".equals(name)) {
                CommandsUtil.color(rest, interaction);
            } else if ("userinfo".equals(name)) {
                userInfo(rest, interaction);
            } else if ("avatar".equals(name)) {
                avatar(rest, interaction);
            } else if ("ban".equals(name)) {
                ban(rest, interaction);
            } else if ("unban".equals(name)) {
                unban(rest, interaction);
            } else if ("softban".equals(name)) {
                CommandsMod.softban(rest, interaction);
            } else if ("kick".equals(name)) {
                kick(rest, interaction);
            } else if ("timeout".equals(name)) {
                timeout(rest, interaction);
            } else if ("purge".equals(name)) {
                purge(rest, interaction);
            } else if ("slowmode".equals(name)) {
                slowmode(rest, interaction);
            } else if ("lock".equals(name)) {
                CommandsMod.lock(rest, interaction);
            } else if ("unlock".equals(name)) {
                CommandsMod.unlock(rest, interaction);
            } else if ("hide".equals(name)) {
                CommandsMod.hide(rest, interaction);
            } else if ("unhide".equals(name)) {
                CommandsMod.unhide(rest, interaction);
            } else if ("topic".equals(name)) {
                CommandsMod.topic(rest, interaction);
            } else if ("nick".equals(name)) {
                CommandsMod.nick(rest, interaction);
            } else if ("addrole".equals(name)) {
                CommandsMod.addRole(rest, interaction);
            } else if ("removerole".equals(name)) {
                CommandsMod.removeRole(rest, interaction);
            } else if ("banlist".equals(name)) {
                CommandsMod.banList(rest, interaction);
            } else if ("pin".equals(name)) {
                CommandsMod.pin(rest, interaction);
            } else if ("unpin".equals(name)) {
                CommandsMod.unpin(rest, interaction);
            } else if ("say".equals(name)) {
                say(rest, interaction);
            } else if ("8ball".equals(name)) {
                eightBall(rest, interaction);
            } else if ("roll".equals(name)) {
                roll(rest, interaction);
            } else if ("coinflip".equals(name)) {
                CommandsFun.coinFlip(rest, interaction);
            } else if ("rps".equals(name)) {
                CommandsFun.rps(rest, interaction);
            } else if ("joke".equals(name)) {
                CommandsFun.joke(rest, interaction);
            } else if ("fact".equals(name)) {
                CommandsFun.fact(rest, interaction);
            } else if ("quote".equals(name)) {
                CommandsFun.quote(rest, interaction);
            } else if ("rate".equals(name)) {
                CommandsFun.rate(rest, interaction);
            } else if ("reverse".equals(name)) {
                CommandsFun.reverse(rest, interaction);
            } else if ("joined".equals(name)) {
                CommandsMore.joined(rest, interaction);
            } else if ("memberroles".equals(name)) {
                CommandsMore.memberRoles(rest, interaction);
            } else if ("userperms".equals(name)) {
                CommandsMore.userPermissions(rest, interaction);
            } else if ("createinvite".equals(name)) {
                CommandsMore.createInvite(rest, interaction);
            } else if ("invites".equals(name)) {
                CommandsMore.invites(rest, interaction);
            } else if ("webhooks".equals(name)) {
                CommandsMore.webhooks(rest, interaction);
            } else if ("threads".equals(name)) {
                CommandsMore.threads(rest, interaction);
            } else if ("events".equals(name)) {
                CommandsMore.events(rest, interaction);
            } else if ("stickers".equals(name)) {
                CommandsMore.stickers(rest, interaction);
            } else if ("emojiinfo".equals(name)) {
                CommandsMore.emojiInfo(rest, interaction);
            } else if ("announce".equals(name)) {
                CommandsMore.announce(rest, interaction);
            } else if ("dm".equals(name)) {
                CommandsMore.dm(rest, interaction);
            } else if ("timezone".equals(name)) {
                CommandsMore.timezone(rest, interaction);
            } else if ("countdown".equals(name)) {
                CommandsMore.countdown(rest, interaction);
            } else if ("commandcount".equals(name)) {
                CommandsMore.commandCount(rest, interaction);
            } else if ("serverbanner".equals(name)) {
                CommandsMore.serverBanner(rest, interaction);
            } else if ("regex".equals(name)) {
                CommandsMore.regex(rest, interaction);
            } else if ("binary".equals(name)) {
                CommandsMore.binary(rest, interaction);
            } else if ("morse".equals(name)) {
                CommandsMore.morse(rest, interaction);
            } else if ("convert".equals(name)) {
                CommandsMore.convert(rest, interaction);
            } else if ("about".equals(name)) {
                CommandsInfo.about(rest, interaction);
            } else if ("servericon".equals(name)) {
                CommandsInfo.serverIcon(rest, interaction);
            } else if ("banner".equals(name)) {
                CommandsInfo.banner(rest, interaction);
            } else if ("timestamp".equals(name)) {
                CommandsInfo.timestamp(rest, interaction);
            } else if ("snowflake".equals(name)) {
                CommandsInfo.snowflake(rest, interaction);
            } else if ("myperms".equals(name)) {
                CommandsInfo.myPermissions(rest, interaction);
            } else if ("pins".equals(name)) {
                CommandsInfo.pins(rest, interaction);
            } else if ("inviteinfo".equals(name)) {
                CommandsInfo.inviteInfo(rest, interaction);
            } else if ("password".equals(name)) {
                CommandsInfo.password(rest, interaction);
            } else if ("uuid".equals(name)) {
                CommandsInfo.uuid(rest, interaction);
            } else if ("hash".equals(name)) {
                CommandsInfo.hash(rest, interaction);
            } else if ("url".equals(name)) {
                CommandsInfo.url(rest, interaction);
            } else if ("case".equals(name)) {
                CommandsInfo.textCase(rest, interaction);
            } else if ("wordcount".equals(name)) {
                CommandsInfo.wordCount(rest, interaction);
            } else if ("roleinfo".equals(name)) {
                CommandsAdmin.roleInfo(rest, interaction);
            } else if ("rolecolor".equals(name)) {
                CommandsAdmin.roleColor(rest, interaction);
            } else if ("createrole".equals(name)) {
                CommandsAdmin.createRole(rest, interaction);
            } else if ("createchannel".equals(name)) {
                CommandsAdmin.createChannel(rest, interaction);
            } else if ("clonechannel".equals(name)) {
                CommandsAdmin.cloneChannel(rest, interaction);
            } else if ("move".equals(name)) {
                CommandsAdmin.move(rest, interaction);
            } else if ("mute".equals(name)) {
                CommandsAdmin.mute(rest, interaction);
            } else if ("deafen".equals(name)) {
                CommandsAdmin.deafen(rest, interaction);
            } else if ("untimeout".equals(name)) {
                CommandsAdmin.unTimeout(rest, interaction);
            } else if ("baninfo".equals(name)) {
                CommandsAdmin.banInfo(rest, interaction);
            } else if ("fortune".equals(name)) {
                CommandsFun2.fortune(rest, interaction);
            } else if ("compliment".equals(name)) {
                CommandsFun2.compliment(rest, interaction);
            } else if ("hug".equals(name)) {
                CommandsFun2.hug(rest, interaction);
            } else if ("pat".equals(name)) {
                CommandsFun2.pat(rest, interaction);
            } else if ("clap".equals(name)) {
                CommandsFun2.clap(rest, interaction);
            } else if ("spoiler".equals(name)) {
                CommandsFun2.spoiler(rest, interaction);
            } else if ("roblox".equals(name)) {
                CommandsRoblox.handle(rest, interaction);
            } else if ("custom".equals(name)) {
                CommandsCustom.handle(rest, interaction);
            } else if ("status".equals(name)) {
                CommandsExtra.status(rest, interaction);
            } else if ("serverstats".equals(name)) {
                CommandsExtra.serverStats(rest, interaction);
            } else if ("channels".equals(name)) {
                CommandsExtra.channels(rest, interaction);
            } else if ("channelperms".equals(name)) {
                CommandsExtra.channelPerms(rest, interaction);
            } else if ("auditlog".equals(name)) {
                CommandsExtra.auditLog(rest, interaction);
            } else if ("thread".equals(name)) {
                CommandsExtra.thread(rest, interaction);
            } else if ("warn".equals(name)) {
                CommandsExtra.warn(rest, interaction);
            } else if ("warnings".equals(name)) {
                CommandsExtra.warnings(rest, interaction);
            } else if ("topinvites".equals(name)) {
                CommandsExtra.topInvites(rest, interaction);
            } else if ("mock".equals(name)) {
                CommandsExtra.mock(rest, interaction);
            } else if ("rot13".equals(name)) {
                CommandsExtra.rot13(rest, interaction);
            } else if ("caesar".equals(name)) {
                CommandsExtra.caesar(rest, interaction);
            } else if ("sortlines".equals(name)) {
                CommandsExtra.sortLines(rest, interaction);
            } else if ("dedupe".equals(name)) {
                CommandsExtra.dedupe(rest, interaction);
            } else if ("hex".equals(name)) {
                CommandsExtra.hex(rest, interaction);
            } else if ("charinfo".equals(name)) {
                CommandsExtra.charInfo(rest, interaction);
            } else if ("repeat".equals(name)) {
                CommandsExtra.repeat(rest, interaction);
            } else if ("embed".equals(name)) {
                CommandsExtra.embed(rest, interaction);
            } else if ("weekday".equals(name)) {
                CommandsExtra.weekday(rest, interaction);
            } else if ("percentage".equals(name)) {
                CommandsExtra.percentage(rest, interaction);
            } else {
                String custom = CustomCommands.response(name);
                if (custom != null) {
                    CommandsCustom.run(rest, interaction, name, custom);
                } else {
                    respond(rest, interaction, "Unknown command `/" + name + "`. Try `/help`.", true, null);
                }
            }
        } catch (Exception e) {
            try {
                respond(rest, interaction, "Something went wrong: " + e.getMessage(), true, null);
            } catch (Exception ignored) {
                // the interaction token may already be spent
            }
        }
    }

    // ------------------------------------------------------------------- core

    private static void help(DiscordRest rest, JSONObject interaction) throws Exception {
        // An embed holds 4096 characters, so the whole list always fits in one reply.
        respond(rest, interaction, "", true,
                new JSONArray().put(embed("BotDroid " + BotState.VERSION + " — commands", 0x5865F2, HELP)));
    }

    private static void ping(DiscordRest rest, JSONObject interaction) throws Exception {
        long start = System.currentTimeMillis();
        Net.Resp response = rest.get("/gateway");
        long elapsed = System.currentTimeMillis() - start;
        String gateway = response.ok() ? "reachable" : "HTTP " + response.code;
        respond(rest, interaction, "Pong — API round trip **" + elapsed + " ms**, gateway " + gateway + ".",
                true, null);
    }

    private static void serverInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        Net.Resp response = rest.get("/guilds/" + guildId + "?with_counts=true");
        if (!response.ok()) {
            respond(rest, interaction, failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject guild = new JSONObject(response.body);
        String name = guild.optString("name", "Server");
        String icon = guild.optString("icon", null);
        String ownerId = guild.optString("owner_id", "");
        long created = snowflakeTime(guildId);
        StringBuilder description = new StringBuilder();
        description.append("**ID:** `").append(guildId).append("`\n");
        description.append("**Owner:** <@").append(ownerId).append(">\n");
        description.append("**Created:** <t:").append(created / 1000).append(":D>\n");
        int members = guild.optInt("approximate_member_count", guild.optInt("member_count", 0));
        if (members > 0) {
            description.append("**Members:** ").append(members).append("\n");
        }
        int online = guild.optInt("approximate_presence_count", -1);
        if (online >= 0) {
            description.append("**Online:** ").append(online).append("\n");
        }
        description.append("**Boost tier:** ").append(guild.optInt("premium_tier", 0)).append("\n");
        description.append("**Verification level:** ").append(guild.optInt("verification_level", 0));

        JSONObject embed = embed(name, 0x5865F2, description.toString());
        if (icon != null && !icon.isEmpty() && !"null".equals(icon)) {
            embed.put("thumbnail", new JSONObject().put("url",
                    "https://cdn.discordapp.com/icons/" + guildId + "/" + icon + ".png?size=256"));
        }
        respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static void userInfo(DiscordRest rest, JSONObject interaction) throws Exception {
        String userId = optString(interaction, "user");
        if (userId == null) {
            userId = invokerId(interaction);
        }
        if (userId == null) {
            respond(rest, interaction, "I could not work out which user to look up.", true, null);
            return;
        }
        Net.Resp response = rest.get("/users/" + userId);
        if (!response.ok()) {
            respond(rest, interaction, failure(response, "View Channels"), true, null);
            return;
        }
        JSONObject user = new JSONObject(response.body);
        String username = user.optString("username", "unknown");
        String globalName = user.optString("global_name", "");
        boolean isBot = user.optBoolean("bot", false);
        long created = snowflakeTime(userId);

        StringBuilder description = new StringBuilder();
        description.append("**Username:** ").append(username).append("\n");
        description.append("**ID:** `").append(userId).append("`\n");
        description.append("**Account created:** <t:").append(created / 1000).append(":R>\n");
        description.append("**Bot:** ").append(isBot ? "yes" : "no").append("\n");

        String guildId = guildId(interaction);
        if (guildId != null) {
            Net.Resp memberResponse = rest.get("/guilds/" + guildId + "/members/" + userId);
            if (memberResponse.ok()) {
                JSONObject member = new JSONObject(memberResponse.body);
                String joined = member.optString("joined_at", "");
                if (!joined.isEmpty()) {
                    try {
                        long joinedAt = OffsetDateTime.parse(joined).toInstant().getEpochSecond();
                        description.append("**Joined server:** <t:").append(joinedAt).append(":R>\n");
                    } catch (Exception ignored) {
                        // Discord changed the timestamp format, keep going
                    }
                }
                JSONArray roles = member.optJSONArray("roles");
                if (roles != null) {
                    description.append("**Roles:** ").append(roles.length()).append("\n");
                }
            }
        }

        JSONObject embed = embed(globalName.isEmpty() ? username : globalName, isBot ? 0x5865F2 : 0x23A55A,
                description.toString());
        embed.put("thumbnail", new JSONObject().put("url", avatarUrl(userId, user.optString("avatar", null))));
        respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    private static void avatar(DiscordRest rest, JSONObject interaction) throws Exception {
        String userId = optString(interaction, "user");
        if (userId == null) {
            userId = invokerId(interaction);
        }
        if (userId == null) {
            respond(rest, interaction, "I could not work out which user to look up.", true, null);
            return;
        }
        Net.Resp response = rest.get("/users/" + userId);
        String hash = null;
        String username = null;
        if (response.ok()) {
            JSONObject user = new JSONObject(response.body);
            hash = user.optString("avatar", null);
            username = user.optString("username", null);
        }
        String url = avatarUrl(userId, hash);
        JSONObject embed = embed(username == null ? "Avatar" : username + "'s avatar", 0x5865F2, null);
        embed.put("image", new JSONObject().put("url", url));
        respond(rest, interaction, "", true, new JSONArray().put(embed));
    }

    // ------------------------------------------------------------- moderation

    private static void ban(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, BAN_MEMBERS)) {
            respond(rest, interaction, "You need the **Ban Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user");
        if (userId == null) {
            respond(rest, interaction, "Pick a member to ban.", true, null);
            return;
        }
        if (userId.equals(invokerId(interaction))) {
            respond(rest, interaction, "You cannot ban yourself.", true, null);
            return;
        }
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        JSONObject body = new JSONObject();
        body.put("delete_message_seconds", 0);
        Net.Resp response = rest.put("/guilds/" + guildId + "/bans/" + userId + "?reason=" + encode(reason),
                body.toString());
        if (response.ok()) {
            respond(rest, interaction, "Banned " + displayName(interaction, "user", userId)
                    + " (`" + userId + "`)\n**Reason:** " + reason, false, null);
        } else {
            respond(rest, interaction, failure(response, "Ban Members"), true, null);
        }
    }

    private static void unban(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, BAN_MEMBERS)) {
            respond(rest, interaction, "You need the **Ban Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user_id");
        if (userId == null || userId.trim().isEmpty()) {
            respond(rest, interaction, "Give me the numeric user ID to unban.", true, null);
            return;
        }
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        Net.Resp response = rest.delete("/guilds/" + guildId + "/bans/" + userId.trim() + "?reason=" + encode(reason));
        if (response.ok()) {
            respond(rest, interaction, "Unbanned `" + userId.trim() + "`\n**Reason:** " + reason, false, null);
        } else {
            respond(rest, interaction, failure(response, "Ban Members"), true, null);
        }
    }

    private static void kick(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, KICK_MEMBERS)) {
            respond(rest, interaction, "You need the **Kick Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user");
        if (userId == null) {
            respond(rest, interaction, "Pick a member to kick.", true, null);
            return;
        }
        if (userId.equals(invokerId(interaction))) {
            respond(rest, interaction, "You cannot kick yourself.", true, null);
            return;
        }
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        Net.Resp response = rest.delete("/guilds/" + guildId + "/members/" + userId + "?reason=" + encode(reason));
        if (response.ok()) {
            respond(rest, interaction, "Kicked " + displayName(interaction, "user", userId)
                    + " (`" + userId + "`)\n**Reason:** " + reason, false, null);
        } else {
            respond(rest, interaction, failure(response, "Kick Members"), true, null);
        }
    }

    private static void timeout(DiscordRest rest, JSONObject interaction) throws Exception {
        String guildId = guildId(interaction);
        if (guildId == null) {
            respond(rest, interaction, "Run this inside a server.", true, null);
            return;
        }
        if (!can(interaction, MODERATE_MEMBERS)) {
            respond(rest, interaction, "You need the **Moderate Members** permission for that.", true, null);
            return;
        }
        String userId = optString(interaction, "user");
        if (userId == null) {
            respond(rest, interaction, "Pick a member to time out.", true, null);
            return;
        }
        int minutes = Math.max(1, Math.min(10080, optInt(interaction, "minutes", 10)));
        String reason = orDefault(optString(interaction, "reason"), "No reason provided");
        JSONObject body = new JSONObject();
        body.put("communication_disabled_until",
                java.time.Instant.now().plusSeconds(minutes * 60L).toString());
        Net.Resp response = rest.patch("/guilds/" + guildId + "/members/" + userId + "?reason=" + encode(reason),
                body.toString());
        if (response.ok()) {
            respond(rest, interaction, "Timed out " + displayName(interaction, "user", userId)
                    + " for **" + minutes + " minute" + (minutes == 1 ? "" : "s") + "**.\n**Reason:** " + reason,
                    false, null);
        } else {
            respond(rest, interaction, failure(response, "Moderate Members"), true, null);
        }
    }

    private static void purge(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = channelId(interaction);
        if (channelId == null) {
            respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!can(interaction, MANAGE_MESSAGES)) {
            respond(rest, interaction, "You need the **Manage Messages** permission for that.", true, null);
            return;
        }
        int count = Math.max(1, Math.min(100, optInt(interaction, "count", 10)));
        Net.Resp list = rest.get("/channels/" + channelId + "/messages?limit=" + count);
        if (!list.ok()) {
            respond(rest, interaction, failure(list, "Read Message History"), true, null);
            return;
        }
        JSONArray messages = new JSONArray(list.body);
        long cutoff = System.currentTimeMillis() - 14L * 24L * 60L * 60L * 1000L;
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.optJSONObject(i);
            if (message == null) {
                continue;
            }
            String id = message.optString("id", null);
            if (id != null && snowflakeTime(id) >= cutoff) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            respond(rest, interaction,
                    "Nothing recent enough to delete (Discord only bulk deletes messages under 14 days old).",
                    true, null);
            return;
        }
        Net.Resp result;
        if (ids.size() == 1) {
            result = rest.delete("/channels/" + channelId + "/messages/" + ids.get(0));
        } else {
            JSONArray payload = new JSONArray();
            for (String id : ids) {
                payload.put(id);
            }
            result = rest.post("/channels/" + channelId + "/messages/bulk-delete",
                    new JSONObject().put("messages", payload).toString());
        }
        if (!result.ok()) {
            respond(rest, interaction, failure(result, "Manage Messages"), true, null);
            return;
        }
        respond(rest, interaction, "Deleted **" + ids.size() + "** message" + (ids.size() == 1 ? "" : "s") + ".",
                true, null);
    }

    private static void slowmode(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = channelId(interaction);
        if (channelId == null) {
            respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!can(interaction, MANAGE_CHANNELS)) {
            respond(rest, interaction, "You need the **Manage Channels** permission for that.", true, null);
            return;
        }
        int seconds = Math.max(0, Math.min(21600, optInt(interaction, "seconds", 0)));
        Net.Resp response = rest.patch("/channels/" + channelId,
                new JSONObject().put("rate_limit_per_user", seconds).toString());
        if (response.ok()) {
            respond(rest, interaction, seconds == 0
                    ? "Slowmode is now **off** for this channel."
                    : "Slowmode set to **" + seconds + " second" + (seconds == 1 ? "" : "s") + "**.", true, null);
        } else {
            respond(rest, interaction, failure(response, "Manage Channels"), true, null);
        }
    }

    private static void say(DiscordRest rest, JSONObject interaction) throws Exception {
        String channelId = channelId(interaction);
        if (channelId == null) {
            respond(rest, interaction, "Run this in a text channel.", true, null);
            return;
        }
        if (!can(interaction, MANAGE_MESSAGES)) {
            respond(rest, interaction, "You need the **Manage Messages** permission for that.", true, null);
            return;
        }
        String text = optString(interaction, "text");
        if (text == null || text.trim().isEmpty()) {
            respond(rest, interaction, "Give me something to say.", true, null);
            return;
        }
        JSONObject body = new JSONObject();
        body.put("content", text);
        body.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
        Net.Resp response = rest.post("/channels/" + channelId + "/messages", body.toString());
        if (!response.ok()) {
            respond(rest, interaction, failure(response, "Send Messages"), true, null);
            return;
        }
        String messageId = new JSONObject(response.body).optString("id", "");
        String guildId = guildId(interaction);
        String link = guildId != null && !messageId.isEmpty()
                ? "https://discord.com/channels/" + guildId + "/" + channelId + "/" + messageId
                : "";
        respond(rest, interaction, link.isEmpty() ? "Posted." : "Posted → " + link, true, null);
    }

    // -------------------------------------------------------------------- fun

    private static void eightBall(DiscordRest rest, JSONObject interaction) throws Exception {
        String question = orDefault(optString(interaction, "question"), "(no question)");
        String answer = EIGHT_BALL[RANDOM.nextInt(EIGHT_BALL.length)];
        respond(rest, interaction, "**Question:** " + question + "\n**Answer:** " + answer, false, null);
    }

    private static void roll(DiscordRest rest, JSONObject interaction) throws Exception {
        String spec = orDefault(optString(interaction, "dice"), "1d6").trim().toLowerCase();
        int count = 1;
        int sides = 6;
        String[] parts = spec.split("d");
        try {
            if (parts.length == 2) {
                count = Integer.parseInt(parts[0].trim());
                sides = Integer.parseInt(parts[1].trim());
            } else if (parts.length == 1) {
                sides = Integer.parseInt(parts[0].trim());
            }
        } catch (NumberFormatException e) {
            respond(rest, interaction, "Use the format `2d6` (dice + sides).", true, null);
            return;
        }
        count = Math.max(1, Math.min(20, count));
        sides = Math.max(2, Math.min(1000, sides));
        StringBuilder results = new StringBuilder();
        int total = 0;
        for (int i = 0; i < count; i++) {
            int value = RANDOM.nextInt(sides) + 1;
            total += value;
            if (i > 0) {
                results.append(", ");
            }
            results.append(value);
        }
        respond(rest, interaction, "**" + count + "d" + sides + ":** " + results
                + (count > 1 ? "\n**Total:** " + total : ""), false, null);
    }

    // ---------------------------------------------------------------- helpers

    static JSONObject embed(String title, int color, String description) throws Exception {
        JSONObject embed = new JSONObject();
        if (title != null && !title.isEmpty()) {
            embed.put("title", title);
        }
        embed.put("color", color);
        if (description != null && !description.isEmpty()) {
            embed.put("description", description);
        }
        return embed;
    }

    static void respond(DiscordRest rest, JSONObject interaction, String content, boolean ephemeral,
                        JSONArray embeds) {
        try {
            JSONObject data = new JSONObject();
            data.put("content", content == null ? "" : content);
            if (ephemeral) {
                data.put("flags", 64);
            }
            data.put("allowed_mentions", new JSONObject().put("parse", new JSONArray()));
            if (embeds != null) {
                data.put("embeds", embeds);
            }
            JSONObject body = new JSONObject();
            body.put("type", 4);
            body.put("data", data);
            rest.post("/interactions/" + interaction.optString("id") + "/" + interaction.optString("token")
                    + "/callback", body.toString());
        } catch (Exception e) {
            // nothing useful to do if the interaction token is gone
        }
    }

    static String failure(Net.Resp response, String permission) {
        if (response.code < 0) {
            return "Network problem while talking to Discord: " + response.body;
        }
        if (response.code == 403) {
            return "Discord refused: give me the **" + permission
                    + "** permission and make sure my role is above the target's role.";
        }
        if (response.code == 404) {
            return "Discord could not find that user, channel or message (404).";
        }
        if (response.code == 401) {
            return "Discord rejected the bot token (401). Save a fresh token and restart.";
        }
        String message = apiMessage(response.body);
        return "Discord error " + response.code + (message.isEmpty() ? "" : ": " + message);
    }

    static String apiMessage(String body) {
        if (body == null) {
            return "";
        }
        try {
            return new JSONObject(body).optString("message", "");
        } catch (Exception e) {
            return body.length() > 120 ? body.substring(0, 120) : body;
        }
    }

    static JSONObject subcommand(JSONObject interaction) {
        JSONObject data = interaction.optJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONArray options = data.optJSONArray("options");
        if (options == null) {
            return null;
        }
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null && option.optInt("type", 0) == 1) {
                return option;
            }
        }
        return null;
    }

    static String subcommandName(JSONObject interaction) {
        JSONObject sub = subcommand(interaction);
        if (sub == null) {
            return null;
        }
        String name = sub.optString("name", "");
        return name.isEmpty() ? null : name;
    }

    static JSONObject option(JSONObject interaction, String name) {
        JSONObject data = interaction.optJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONObject sub = subcommand(interaction);
        JSONArray options = sub != null ? sub.optJSONArray("options") : data.optJSONArray("options");
        if (options == null) {
            return null;
        }
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option != null && name.equals(option.optString("name"))) {
                return option;
            }
        }
        return null;
    }

    static String optString(JSONObject interaction, String name) {
        JSONObject option = option(interaction, name);
        if (option == null || !option.has("value")) {
            return null;
        }
        return option.optString("value", null);
    }

    static int optInt(JSONObject interaction, String name, int fallback) {
        JSONObject option = option(interaction, name);
        if (option == null || !option.has("value")) {
            return fallback;
        }
        return option.optInt("value", fallback);
    }

    static String displayName(JSONObject interaction, String optionName, String fallbackId) {
        JSONObject option = option(interaction, optionName);
        JSONObject user = option == null ? null : option.optJSONObject("user");
        if (user != null) {
            String globalName = user.optString("global_name", "");
            String username = user.optString("username", "");
            String name = globalName.isEmpty() ? username : globalName;
            if (!name.isEmpty()) {
                return "**" + name + "**";
            }
        }
        return "<@" + fallbackId + ">";
    }

    static String invokerId(JSONObject interaction) {
        JSONObject member = interaction.optJSONObject("member");
        if (member != null) {
            JSONObject user = member.optJSONObject("user");
            if (user != null) {
                return user.optString("id", null);
            }
        }
        JSONObject user = interaction.optJSONObject("user");
        return user == null ? null : user.optString("id", null);
    }

    static String invokerName(JSONObject interaction) {
        JSONObject member = interaction.optJSONObject("member");
        JSONObject user = member == null ? interaction.optJSONObject("user") : member.optJSONObject("user");
        if (user == null) {
            return null;
        }
        String globalName = user.optString("global_name", "");
        return globalName.isEmpty() ? user.optString("username", null) : globalName;
    }

    static long invokerPermissions(JSONObject interaction) {
        JSONObject member = interaction.optJSONObject("member");
        if (member == null) {
            return 0L;
        }
        String permissions = member.optString("permissions", "0");
        try {
            return Long.parseLong(permissions);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    static boolean can(JSONObject interaction, long permission) {
        long permissions = invokerPermissions(interaction);
        return (permissions & ADMINISTRATOR) != 0 || (permissions & permission) != 0;
    }

    static String guildId(JSONObject interaction) {
        String value = interaction.has("guild_id") && !interaction.isNull("guild_id")
                ? interaction.optString("guild_id", null) : null;
        return value == null || value.isEmpty() ? null : value;
    }

    static String channelId(JSONObject interaction) {
        String value = interaction.has("channel_id") && !interaction.isNull("channel_id")
                ? interaction.optString("channel_id", null) : null;
        return value == null || value.isEmpty() ? null : value;
    }

    static long snowflakeTime(String id) {
        try {
            return (Long.parseLong(id) >> 22) + 1420070400000L;
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    static String avatarUrl(String userId, String hash) {
        if (hash == null || hash.isEmpty() || "null".equals(hash)) {
            int index = 0;
            try {
                index = (int) ((Long.parseLong(userId) >> 22) % 6);
            } catch (NumberFormatException ignored) {
                // keep the default avatar
            }
            return "https://cdn.discordapp.com/embed/avatars/" + index + ".png";
        }
        String extension = hash.startsWith("a_") ? ".gif" : ".png";
        return "https://cdn.discordapp.com/avatars/" + userId + "/" + hash + extension + "?size=1024";
    }

    static List<String> permissionNames(long bits) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < PERMISSION_BITS.length; i++) {
            if ((bits & PERMISSION_BITS[i]) != 0) {
                names.add(PERMISSION_LABELS[i]);
            }
        }
        return names;
    }

    static String orDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return "";
        }
    }
}
