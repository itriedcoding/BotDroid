# BotDroid

A Discord bot that runs **on your phone**. Paste a bot token, press Start, and
120 built-in slash commands are ready for every server the bot is in (91 on by
default, the other 29 one switch away) — plus Roblox lookups, a Bot
Designer–style **bot builder** with its own script language, `/status` for the
phone the bot runs on, a rich presence page and a settings screen.
No server, no hosting, no third-party libraries: the APK stays tiny.

## What it does

- Connects to the Discord gateway and stays online inside a foreground service.
- Registers up to 100 slash commands in every server the bot joins, and
  re-registers automatically when you add a custom command or flip a switch.
- **`/status`**: the phone the bot is running on — model, Android version,
  battery, network, RAM, storage, uptime — plus latency, server count and command
  slots. Everything is measured on the device, nothing is faked.
- **Bot builder**: a full builder page (name, script editor, function palette,
  test run, saved list) with a BDScript-style language: `$mention`, `$args`,
  `$upper[x]`, `$if[...]{...}`, `$setUserVar[name;value]` and about 60 more
  functions. `/custom functions` documents them in Discord too.
- **Roblox lookups** through the public Roblox API: users, avatars, games, groups.
- **Per-command switches**: Settings → Commands lists every built-in with a
  toggle. Off commands are not registered and free a slot for a custom command.
- **Warnings** kept on the phone: `/warn add|list|clear` and `/warnings`.
- **Rich presence page**: activity type (including Streaming with a URL), main
  line, second line and online/idle/DND/invisible, applied live.
- **Start on boot**: the bot comes back by itself after a phone restart.
- **Bot status**: show the bot as playing, listening, watching, competing or
  with a custom status, and choose online / idle / do not disturb / invisible.
- **Updates**: reads this project's GitHub releases, downloads a newer APK,
  checks it is signed with the same key and hands it to Android's installer.
- **Settings screen**: bot status, Roblox toggle, custom command management,
  re-register commands, forget the token.
- Live activity log and status inside the app, with an animated entrance.

## Install

1. Download `BotDroid.apk` from the [Releases](../../releases) page.
2. Open it on the phone and allow installing from your browser or file manager.
3. Requires Android 8.0 (API 26) or newer.

## Set up your bot

1. Open the [Discord Developer Portal](https://discord.com/developers/applications)
   and create an application.
2. **Bot → Reset Token**, copy the token. Treat it like a password.
3. Invite the bot: **OAuth2 → URL Generator** with scopes `bot` and
   `applications.commands`, and these permissions: View Channels, Send Messages,
   Embed Links, Read Message History, Manage Messages, Manage Channels,
   Manage Roles, Manage Nicknames, Kick Members, Ban Members, Moderate Members,
   Add Reactions.
4. Paste the token in BotDroid and press **Start bot**.
5. Type `/help` in your server. Commands are registered per server, so they show
   up immediately.

BotDroid needs only the `GUILDS` intent, so no privileged intents are required.

## Commands

### Core and utility
| Command | What it does |
| --- | --- |
| `/help` | Every command, in one list |
| `/ping` | API round-trip latency |
| `/botinfo` | Version, uptime, server and command counts |
| `/uptime` | How long the bot has been running |
| `/invite` | Invite link with the right permissions |
| `/time` | Current time in Discord, Unix and UTC |
| `/math` | Safe expression evaluator: `(2+3)*4`, `2^10`, `7%3` |
| `/random` | Random number in a range |
| `/choose` | Pick one of `pizza \| sushi \| tacos` |
| `/poll` | Reaction poll with up to four options |
| `/remind` | Reminder in this channel after N minutes |
| `/base64` | Encode or decode base64 |
| `/color` | Preview a hex colour |

### Server and users
| Command | What it does |
| --- | --- |
| `/serverinfo` | Owner, creation date, members, boosts |
| `/membercount` | Member and online counts |
| `/roles` | Roles by position with colours |
| `/channelinfo` | Type, topic, slowmode, creation date |
| `/emojis` | Custom emojis in the server |
| `/userinfo` | Account, ID, join date, role count |
| `/avatar` | Full-size avatar |

### Moderation
| Command | What it does | Permission |
| --- | --- | --- |
| `/ban` | Ban a member with an audit reason | Ban Members |
| `/unban` | Lift a ban by user ID | Ban Members |
| `/softban` | Ban and unban to delete a day of messages | Ban Members |
| `/kick` | Kick a member | Kick Members |
| `/timeout` | Time out a member up to 7 days | Moderate Members |
| `/purge` | Delete 1-100 recent messages | Manage Messages |
| `/slowmode` | Set channel slowmode | Manage Channels |
| `/lock` `/unlock` | Stop or allow @everyone sending messages | Manage Channels |
| `/hide` `/unhide` | Hide or show the channel for @everyone | Manage Channels |
| `/topic` | Set or clear the channel topic | Manage Channels |
| `/nick` | Change or reset a nickname | Manage Nicknames |
| `/addrole` `/removerole` | Manage a member's roles | Manage Roles |
| `/banlist` | Recent bans and reasons | Ban Members |
| `/pin` `/unpin` | Pin or unpin a message by ID | Manage Messages |
| `/say` | Post a message as the bot | Manage Messages |
| `/untimeout` | Remove a member timeout | Moderate Members |
| `/baninfo` | Why a user was banned | Ban Members |
| `/roleinfo` | Role colour, position, flags and key permissions | — |
| `/rolecolor` | Change a role colour | Manage Roles |
| `/createrole` | Create a role, optionally coloured | Manage Roles |
| `/createchannel` | Create a text or voice channel | Manage Channels |
| `/clonechannel` | Copy this channel's topic, slowmode and overwrites | Manage Channels |
| `/move` | Move a member to a voice channel | Move Members |
| `/mute` `/deafen` | Server mute or deafen a member | Mute/Deafen Members |

### Info and text tools
| Command | What it does |
| --- | --- |
| `/about` | Version, command count and source link |
| `/servericon` `/banner` | Full-size server icon or a user's profile banner |
| `/timestamp` | Discord timestamp from `now` or unix time, in four formats |
| `/snowflake` | When a Discord ID was created |
| `/myperms` | Your permissions in this server, spelled out |
| `/pins` | Pinned messages with jump links |
| `/inviteinfo` | Server, channel, uses and expiry of an invite |
| `/password` | Random password (8-64 chars), ephemeral |
| `/uuid` | Random UUID v4 |
| `/hash` | MD5, SHA-1 or SHA-256 of some text |
| `/url` | URL encode or decode |
| `/case` | upper, lower, title or swap |
| `/wordcount` | Characters, words, lines |

### Server tools and converters
| Command | What it does |
| --- | --- |
| `/joined` | Join date, days in server and account age for a member |
| `/memberroles` | The roles a member holds |
| `/userperms` | Permissions a member gets from @everyone plus their roles |
| `/createinvite` | Invite link with optional expiry and use limit |
| `/invites` | Every active invite with its use count |
| `/webhooks` | Webhooks in the channel |
| `/threads` | Active threads in the channel |
| `/events` | Scheduled events and interest counts |
| `/stickers` | The server's custom stickers |
| `/emojiinfo` | One emoji's id, animation and availability |
| `/serverbanner` | The server banner |
| `/announce` | Post an embed in another channel |
| `/dm` | DM a user as the bot |
| `/commandcount` | How many of the 100 command slots are used |
| `/timezone` | Current time in any IANA zone |
| `/countdown` | Discord timestamp and time left until a date |
| `/regex` | Test a Java regex and see groups and offsets |
| `/binary` | Text ↔ binary |
| `/morse` | Text ↔ morse code |
| `/convert` | Length, mass and temperature units |

### Device and server
| Command | What it does |
| --- | --- |
| `/status` | Where the bot runs: phone model, Android build, battery, network, RAM, storage, uptime, latency, servers and free command slots |
| `/serverstats` | Members, online, roles, emojis, stickers, boosts, verification, channels |
| `/channels` | Every channel grouped by text, voice, categories and other |
| `/channelperms` | Permission overwrites in this channel, resolved to role names |
| `/auditlog` | Recent audit log entries, with the moderator and reason |
| `/thread` | Start a public thread in this channel |
| `/topinvites` | Invites ranked by how many people joined through them |

### Warnings
| Command | What it does | Permission |
| --- | --- | --- |
| `/warn add` | Warn a member with a reason | Moderate Members |
| `/warn list` `/warn clear` | Show or delete a member's warnings | Moderate Members |
| `/warnings` | Check your own (or someone's) warnings | — |

Warnings live on the phone, in the app's private storage, up to 2000 entries —
the bot has no database server of its own, and the command says so.

### Text tools 2
| Command | What it does |
| --- | --- |
| `/mock` | sPoNgEbOb case |
| `/rot13` `/caesar` | Letter rotation by 13 or any shift |
| `/sortlines` `/dedupe` | Sort lines, or drop duplicates |
| `/hex` | Text ↔ hex |
| `/charinfo` | Code point, decimal, category and UTF-8 size per character |
| `/repeat` | Repeat text up to 10 times |
| `/embed` | Post a custom embed with a title, body and colour |
| `/weekday` | Which day of the week a date falls on |
| `/percentage` | Part ÷ total as a percentage |

Every moderation command checks the invoker's permissions first and explains
Discord's refusal (403 role hierarchy, missing permission, unknown user).

### Fun
| Command | What it does |
| --- | --- |
| `/8ball` | Magic 8-ball answer |
| `/roll` | Dice, e.g. `2d6` |
| `/coinflip` | Heads or tails |
| `/rps` | Rock, paper, scissors |
| `/joke` `/fact` `/quote` | Random output from built-in lists |
| `/rate` | Rate something out of 100 |
| `/reverse` | Reverse text |
| `/fortune` | Fortune cookie |
| `/compliment` | Compliment yourself or someone else |
| `/hug` `/pat` | Social actions with mentions |
| `/clap` | 👏 between 👏 every 👏 word |
| `/spoiler` | Hide text behind ||spoiler tags|| |

### Roblox
| Command | What it does |
| --- | --- |
| `/roblox user <username>` | Display name, ID, creation date, friends, followers, about, headshot |
| `/roblox avatar <username>` | Full-body avatar render |
| `/roblox game <universe_id>` | Name, creator, playing, visits, favourites, icon |
| `/roblox group <group_id>` | Name, owner, member count, description, icon |
| `/roblox games <username>` | A user's public games with visit counts and links |

These use the public Roblox endpoints (`users.roblox.com`, `games.roblox.com`,
`groups.roblox.com`, `friends.roblox.com`, `thumbnails.roblox.com`) with no key.
A game's universe ID is not the same as its place ID; it appears in the game's
web URL parameters. Roblox lookups can be switched off in Settings.

### Custom commands
| Command | What it does |
| --- | --- |
| `/custom add <name> <response>` | Create or overwrite a command |
| `/custom remove <name>` | Delete one |
| `/custom list` | Show all of them |
| `/custom functions` | Every bot builder function, in Discord |
| `/custom clear` | Delete every custom command |

Example: `/custom add name:hello response:Hi {user}, welcome to {channel}!`
then anybody can run `/hello`. Names are lowercase letters, numbers, `-` and `_`,
up to 32 characters; built-in names are reserved; responses are capped at 1500
characters. They are stored on the phone and re-registered in every server
automatically (Discord can take a few seconds to show them).

Every custom command takes one optional `input` option, which is what `$args`
and `$arg[n]` read: `/hello input:good morning`.

## Bot builder

**Settings → Bot builder** is the app's answer to Bot Designer for Discord's
command creator: a name field, a monospace script editor, a row of function
buttons that insert snippets at the cursor, a **Test run** button and the list of
saved commands with **Edit** and **Remove**.

The response of a custom command is a script. Plain text works as before; a
`$` starts a function call, arguments are separated by `;`, and calls can be
nested:

```
Hi $mention, welcome to $servername!
You are level $getUserVar[level] with $argCount argument(s): $args
$if[$arg[1] == yes]{Nice.}$else{Try again with yes.}
```

| Group | Functions |
| --- | --- |
| Text | `$nomention` `$message[text]` `$upper[x]` `$lower[x]` `$capitalize[x]` `$trim[x]` `$reverse[x]` `$length[x]` `$replace[x;from;to]` `$substring[x;start;end]` `$split[x;sep;index]` `$contains[x;y]` `$startsWith[x;y]` `$endsWith[x;y]` `$repeat[x;times]` |
| Numbers | `$calc[expr]` `$sum[a;b]` `$mod[a;b]` `$round[x]` `$floor[x]` `$ceil[x]` `$abs[x]` `$max[a;b]` `$min[a;b]` `$random[min;max]` `$choose[a;b;c]` |
| Context | `$username` `$displayname` `$userid` `$mention` `$args` `$argCount` `$arg[n]` `$argsFrom[n]` `$servername` `$serverid` `$channelname` `$channelid` `$commandname` `$botname` `$version` `$ping` `$uptime` `$guilds` `$membercount` `$date` `$time` `$timestamp` `$weekday[date]` |
| Variables | `$getVar[name]` `$setVar[name;value]` (global) · `$getUserVar` / `$setUserVar` (per member) · `$getServerVar` / `$setServerVar` (per server) |
| Logic | `$if[condition]{yes}$else{no}` with `==`, `!=`, `>`, `<`, `>=`, `<=` · `$onlyIf[condition]{...}` · `$onlyPerms[manage_messages]{...}` · `$not[x]` `$equals[x;y]` |

Escape `$`, `;`, `{`, `}` and `[` with a backslash: `\$5` prints `$5`.
Variables persist on the phone (500 max per install, 900 characters each) and
are cleared from the builder page. `$onlyPerms` checks the caller's real
permissions in the server, so a script can keep a reply for staff.

The builder's **Test run** executes the same interpreter on the device with
sample values — it is a preview of the real evaluation, not a separate mock
mode, and it never posts anything to Discord.

### How the 100 command slots are shared

BotDroid ships **120 built-in commands**, but Discord allows **100 per server**,
counting built-ins and your own together, so the custom limit is
`100 − enabled built-ins` rather than a fixed number. **91** are on by default;
the other **29** are niche or specialised (`/dm`, `/announce`, `/auditlog`,
`/warn`, `/mock`, `/hex`, …) and start switched off, which leaves room for nine
custom commands on a fresh install. Turn built-ins off in **Settings → Commands**
to free more, or on if you need them — that page lists every command with a
switch plus **Turn all on** / **Turn all off**. `/commandcount` shows the current
split, and if a server ever exceeds 100 the app keeps your custom commands and
logs which built-ins were skipped.

## Settings

The **Settings** button on the main screen opens:

- **Commands** — every built-in command with an on/off switch, plus **Turn all
  on** / **Turn all off** and a live count of free slots.
- **Bot builder** — the script editor, function palette, test run, saved
  commands with **Edit** / **Remove**, the variable store and the full function
  reference.
- **Rich presence** — its own page: activity type (Playing, Streaming,
  Listening to, Watching, Competing in, Custom status), the main line, an
  optional second line, a stream URL for Streaming, and the presence (Online,
  Idle, Do Not Disturb, Invisible). **Save** applies it to the running bot
  immediately through a gateway presence update; leaving the text empty shows no
  activity. `/botinfo` also reports the active status.
- **Roblox integration** — turn `/roblox` on or off.
- **Custom commands** — a quick-add field (name + response + **Add command**),
  the saved list with per-command **Remove**, and **Clear custom commands**. For
  scripts use the **Bot builder** button in the same card.
- **Updates** — installed version and build number, **Check for updates** and
  **Download and install**. It reads `releases/latest` from the GitHub API,
  shows the tag, and only offers the install when that tag is newer.
- **Start on boot** — restart the bot by itself when the phone reboots
  (Android 8–14; newer Android versions restrict foreground services started at
  boot).
- **Re-register commands in Discord** — useful if Discord is showing a stale list.
- **Forget token and stop** — clears the stored token and stops the bot.
- Link to this repository.

## Updates

The app updates itself from the [Releases](../../releases) page:

1. On launch it asks `https://api.github.com/repos/itriedcoding/BotDroid/releases/latest`
   at most once a day (and **Check for updates** in Settings does it on demand).
2. If `tag_name` is newer than the installed build, **Download and install**
   fetches the release asset `BotDroid.apk` over TLS, following GitHub's
   redirect to the asset host.
3. The APK is rejected unless its signing certificate is identical to the
   installed app's (SHA-256 of the certificate), and unless its size matches the
   release metadata.
4. The bytes are written into a `PackageInstaller` session and committed, so
   Android shows its own update dialog and applies the install. There is no
   silent install: on Android 8+ the app also has to be allowed to install
   unknown apps, and the app opens that settings page when it is not.

The update only works when the release APK is signed with the same key as the
installed one, which is why `keystore/botdroid.jks` is committed and used by the
release workflow. If the signature check fails, the download is deleted and the
install is refused.

## Staying online

The bot is a process on your phone, so it is online exactly while the phone is
powered on and the foreground service is running: a partial wake lock and a
permanent notification keep it alive with the screen off, and **Start on boot**
brings it back after a restart. If the device is switched off or the service is
force-stopped, the bot goes offline — no Android app can stay connected after
the hardware is powered down. Keeping it online through a phone shutdown needs
a hosted machine (a small VPS or a free-tier container running the same
token), which is outside what this app can do by itself.

## About Discord Rich Presence and the Social SDK

The presence page sets everything Discord's API accepts **for bots**: activity
name, state, type and URL. Discord documents that bot users can only set those
fields, so image assets, party data and buttons are intentionally not offered —
they would be ignored.

The full Discord Rich Presence with artwork belongs to the **Discord Social
SDK**, a native C++20 library (Android 7+ ships it as an `.aar`/`.so`) that
authenticates **users** through OAuth account linking — not bots — and needs a
Discord application with the Social SDK enabled, plus a mobile redirect URI of
the form `discord-YOUR_APP_ID:/authorize/callback`.

That SDK is not bundled here, and it is not stubbed out with a mock:

- Discord only publishes the binaries under **Downloads** in the Developer
  Portal, after the Social SDK is enabled for that specific application.
- Its terms allow shipping the SDK *inside* your own application but not
  redistributing the SDK itself, so its binaries do not belong in this public
  repository or in the APK published from it.
- BotDroid is a plain-Java APK built with `aapt2`/`javac` and no native
  dependencies, and it runs a **bot**; the Social SDK would add a user OAuth
  login, an account-linking flow, C++20 JNI and the NDK to the build.

The presence page therefore uses the official gateway and REST API, which is
everything a bot is allowed to set. Adding the Social SDK is a separate feature
that starts with the SDK package and an app ID from the Discord portal, not with
a JNI shim written against headers that are not present.

## Battery and reliability

- While running, BotDroid holds a partial wake lock and shows a permanent
  notification with a **Stop** button.
- If Android kills the bot, allow unrestricted background usage under
  Settings → Apps → BotDroid → Battery.
- Mobile networks drop connections; the bot reconnects with exponential backoff.

## Build from source

Requirements: JDK 17+ and the Android SDK with `build-tools;34.0.0` and
`platforms;android-34`. No Gradle, no external libraries.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./scripts/build-apk.sh
# -> dist/BotDroid.apk
adb install -r dist/BotDroid.apk
```

`dist/` is build output and is not committed; the release workflow builds the
APK from source and uploads it. Pushing to `main`, pushing a `v*` tag, or running
the **Build and release APK** workflow publishes `BotDroid.apk` to GitHub Releases.
`keystore/botdroid.jks` is a development signing key (password `botdroid`) so
updated builds install over old ones; replace it for a store release.

## Tests

The command layer runs on the JVM against fake Discord and network clients, so
handler payloads can be checked without a token:

```bash
./tools/devtest/run-tests.sh
```

It covers all handlers (permission gates, REST paths, request bodies, embeds),
the 120 command definitions and their subcommands, the maths parser, the custom
command store, the command-slot arithmetic (defaults, toggles, the 100 cap),
the Roblox toggle, every presence payload shape (playing, streaming with and
without a URL, listening, watching, competing, custom status, state lines,
empty activity, invalid values), `/status` with a canned device, the warnings
store and the script language (nesting, arguments, `$if`/`$else`, variables,
`$onlyPerms`, escapes, error reporting). 235 checks in total.
`tools/` is not compiled into the APK.

## How it works

```
app/src/main/java/dev/botdroid/app/
  MainActivity.java     token entry, start/stop, live log, entrance animation
  SettingsActivity.java settings, toggles entry, autostart, token reset
  CommandsActivity.java per-command on/off switches
  BuilderActivity.java  bot builder: editor, palette, test run, saved scripts
  PresenceActivity.java rich presence page
  Script.java           the $function script language (parser and evaluator)
  Variables.java        global / per-user / per-server script variables
  Warnings.java         warnings issued with /warn, stored on the phone
  DeviceInfo.java       device snapshot model and formatting for /status
  DeviceProbe.java      reads Build, battery, network, memory and storage
  BootReceiver.java     restarts the bot after a reboot
  Store.java            loads prefs into memory, persists toggles and commands
  CommandToggles.java   which built-ins are enabled and the slot arithmetic
  BotPresence.java      activity/status model and gateway presence payload
  BotService.java       foreground service, notification, wake lock
  DiscordBot.java       gateway loop, heartbeats, command registration
  Commands.java         100 command definitions, dispatch, core handlers
  CommandsUtil.java     utility commands and the maths parser
  CommandsInfo.java     info, text and invite commands
  CommandsAdmin.java    role, channel and voice commands
  CommandsMore.java     server tools, invites, converters
  CommandsExtra.java    /status, server inspection, warnings, text tools
  CommandsMod.java      extra moderation commands
  CommandsFun.java      fun commands
  CommandsFun2.java     social and text fun commands
  CommandsRoblox.java   /roblox handlers
  CommandsCustom.java   /custom handlers and placeholder rendering
  CustomCommands.java    custom command store (persisted to prefs)
  RobloxApi.java        Roblox public API client
  BotState.java         shared runtime state
  DiscordRest.java      Discord REST v10 client with rate-limit retry
  GatewaySocket.java    minimal RFC 6455 WebSocket client
  Net.java              minimal HTTPS client (supports PATCH)
  LogBus.java           in-memory log shared with the UI
  Prefs.java            app-private storage
  Updater.java          GitHub release check, APK check and installer session
  UpdateReceiver.java   install result from Android's package installer
```

Everything network-facing is built on plain sockets, so the app needs no
third-party dependencies: Android SDK classes plus `org.json` only.

## Security

The token lives in app-private `SharedPreferences` (not world readable, excluded
from backups) and is only sent to `discord.com` and `gateway.discord.gg` over
TLS, with certificate hostname verification. Roblox requests carry no secrets.
If a token leaks, reset it in the Developer Portal.
