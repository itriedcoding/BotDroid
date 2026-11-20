# BotDroid

A Discord bot that runs **on your phone**. Paste a bot token, press Start, and
100 built-in slash commands appear in every server the bot is in (90 on by
default, the rest one switch away) — plus Roblox lookups, a command builder
inside the app, a rich presence page and a settings screen.
No server, no hosting, no third-party libraries: the APK is around 100 KB.

## What it does

- Connects to the Discord gateway and stays online inside a foreground service.
- Registers up to 100 slash commands in every server the bot joins, and
  re-registers automatically when you add a custom command or flip a switch.
- **Roblox lookups** through the public Roblox API: users, avatars, games, groups.
- **Command builder**: type a name and a response in Settings, press **Add
  command**, and `/name` exists in every server. Remove commands from the same
  list. `/custom add name response` in Discord still works too.
- **Per-command switches**: Settings → Commands lists every built-in with a
  toggle. Off commands are not registered and free a slot for a custom command.
- **Rich presence page**: activity type (including Streaming with a URL), main
  line, second line and online/idle/DND/invisible, applied live.
- **Start on boot**: the bot comes back by itself after a phone restart.
- **Bot status**: show the bot as playing, listening, watching, competing or
  with a custom status, and choose online / idle / do not disturb / invisible.
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
| `/custom clear` | Delete every custom command |

Example: `/custom add name:hello response:Hi {user}, welcome to {channel}!`
then anybody can run `/hello`. Names are lowercase letters, numbers, `-` and `_`,
up to 32 characters; built-in names are reserved; responses are capped at 1500
characters. They are stored on the phone and re-registered in every server
automatically (Discord can take a few seconds to show them).

The same commands can be built without leaving the app: **Settings → Custom
commands** has a name field, a response field and **Add command**, and every
saved command has a **Remove** button.

### How the 100 command slots are shared

Discord allows **100 commands per server**, counting built-ins and your own
together, so the custom limit is `100 − enabled built-ins` rather than a fixed
number. Ten niche built-ins (`/dm`, `/announce`, `/webhooks`, `/invites`,
`/stickers`, `/events`, `/threads`, `/emojiinfo`, `/serverbanner`,
`/memberroles`) start off, which leaves room for ten custom commands on a fresh
install. Turn built-ins off in **Settings → Commands** to free more, or on if
you need them. `/commandcount` shows the current split, and if a server ever
exceeds 100 the app keeps your custom commands and logs which built-ins were
skipped.

## Settings

The **Settings** button on the main screen opens:

- **Commands** — every built-in command with an on/off switch, plus **Turn all
  on** / **Turn all off** and a live count of free slots.
- **Rich presence** — its own page: activity type (Playing, Streaming,
  Listening to, Watching, Competing in, Custom status), the main line, an
  optional second line, a stream URL for Streaming, and the presence (Online,
  Idle, Do Not Disturb, Invisible). **Save** applies it to the running bot
  immediately through a gateway presence update; leaving the text empty shows no
  activity. `/botinfo` also reports the active status.
- **Roblox integration** — turn `/roblox` on or off.
- **Custom commands** — the command builder (name + response + **Add command**),
  the saved list with per-command **Remove**, and **Clear custom commands**.
- **Start on boot** — restart the bot by itself when the phone reboots
  (Android 8–14; newer Android versions restrict foreground services started at
  boot).
- **Re-register commands in Discord** — useful if Discord is showing a stale list.
- **Forget token and stop** — clears the stored token and stops the bot.
- Link to this repository.

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
SDK**, a native library distributed for C++, Unreal and Unity (Android 7+ is
supported, but as a native `.so` with its own OAuth/account-linking flow and a
Discord app configured for the SDK). This project is a plain-Java APK built
with `aapt2`/`javac` and no native dependencies, so the SDK is not bundled and
the presence here uses the official gateway and REST API instead. Bundling it
would need the SDK binary from the Discord Developer Portal and a JNI wrapper —
a real, separate piece of work, not something to fake with a mock.

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

Pushing to `main`, pushing a `v*` tag, or running the **Build and release APK**
workflow publishes `BotDroid.apk` to GitHub Releases.
`keystore/botdroid.jks` is a development signing key (password `botdroid`) so
updated builds install over old ones; replace it for a store release.

## Tests

The command layer runs on the JVM against fake Discord and network clients, so
handler payloads can be checked without a token:

```bash
./tools/devtest/run-tests.sh
```

It covers all handlers (permission gates, REST paths, request bodies, embeds),
the 100 command definitions and their subcommands, the maths parser, the custom
command store, the command-slot arithmetic (defaults, toggles, the 100 cap),
the Roblox toggle and every presence payload shape (playing, streaming with and
without a URL, listening, watching, competing, custom status, state lines,
empty activity, invalid values). 181 checks in total.
`tools/` is not compiled into the APK.

## How it works

```
app/src/main/java/dev/botdroid/app/
  MainActivity.java     token entry, start/stop, live log, entrance animation
  SettingsActivity.java command builder, toggles entry, autostart, token reset
  CommandsActivity.java per-command on/off switches
  PresenceActivity.java rich presence page
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
```

Everything network-facing is built on plain sockets, so the app needs no
third-party dependencies: Android SDK classes plus `org.json` only.

## Security

The token lives in app-private `SharedPreferences` (not world readable, excluded
from backups) and is only sent to `discord.com` and `gateway.discord.gg` over
TLS, with certificate hostname verification. Roblox requests carry no secrets.
If a token leaks, reset it in the Developer Portal.
