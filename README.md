# BotDroid

A Discord bot that runs **on your phone**. Paste a bot token, press Start, and
50 preset slash commands appear in every server the bot is in — plus Roblox
lookups, custom commands you create yourself, and a settings screen.
No server, no hosting, no third-party libraries: the APK is around 70 KB.

## What it does

- Connects to the Discord gateway and stays online inside a foreground service.
- Registers 50 slash commands in every server the bot joins, and re-registers
  them automatically when you add a custom command.
- **Roblox lookups** through the public Roblox API: users, avatars, games, groups.
- **Custom commands**: create your own with `/custom add name response` and
  invoke them like `/hello`.
- **Settings screen**: toggle Roblox commands, review and clear custom commands,
  re-register commands, forget the token.
- Live activity log and status inside the app.

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

### Roblox
| Command | What it does |
| --- | --- |
| `/roblox user <username>` | Display name, ID, creation date, friends, followers, about, headshot |
| `/roblox avatar <username>` | Full-body avatar render |
| `/roblox game <universe_id>` | Name, creator, playing, visits, favourites, icon |
| `/roblox group <group_id>` | Name, owner, member count, description, icon |

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
characters; up to 40 custom commands. They are stored on the phone and
re-registered in every server automatically (Discord can take a few seconds to
show them).

## Settings

The **Settings** button on the main screen opens:

- **Roblox integration** — turn `/roblox` on or off.
- **Custom commands** — count, list, and clear all of them.
- **Re-register commands in Discord** — useful if Discord is showing a stale list.
- **Forget token and stop** — clears the stored token and stops the bot.
- Link to this repository.

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
the 50 command definitions and their subcommands, the maths parser, the custom
command store and the Roblox toggle. `tools/` is not compiled into the APK.

## How it works

```
app/src/main/java/dev/botdroid/app/
  MainActivity.java     token entry, start/stop, live log
  SettingsActivity.java Roblox toggle, custom commands, token reset
  BotService.java       foreground service, notification, wake lock
  DiscordBot.java       gateway loop, heartbeats, command registration
  Commands.java         50 command definitions, dispatch, core handlers
  CommandsUtil.java     utility commands and the maths parser
  CommandsMod.java      extra moderation commands
  CommandsFun.java      fun commands
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
