# BotDroid

A very small Discord bot that runs **on your phone**. Paste a bot token, press
Start, and the preset slash commands appear in every server the bot is in.
No server, no hosting, no dependencies — the APK is under 100 KB.

## What it does

- Connects to the Discord gateway and keeps the connection alive with a
  foreground service (so it survives you switching apps).
- Registers a full set of preset slash commands in every server the bot joins.
- Stores the token only in the app's private storage on the device.
- Shows a live log and a status line inside the app.

## Install

1. Download `BotDroid.apk` from the [Releases](../../releases) page.
2. Open it on the phone and allow installing from your browser/file manager when
   Android asks.
3. Requires Android 8.0 (API 26) or newer.

## Set up your bot

1. Open the [Discord Developer Portal](https://discord.com/developers/applications)
   and create an application.
2. Go to **Bot -> Reset Token**, copy the token. Treat it like a password.
3. Invite the bot to your server: **OAuth2 -> URL Generator** with the scopes
   `bot` and `applications.commands`, and these permissions:
   View Channels, Send Messages, Embed Links, Read Message History,
   Manage Messages, Manage Channels, Kick Members, Ban Members,
   Moderate Members.
4. In BotDroid, paste the token and press **Start bot**.
5. Type `/help` in your server. Commands are registered per server, so they
   show up immediately (global commands can take an hour).

BotDroid only uses the `GUILDS` intent, so you do not need to enable any
privileged intents in the Developer Portal.

## Commands

| Command | What it does | Needs permission |
| --- | --- | --- |
| `/help` | List every command | — |
| `/ping` | API round-trip latency | — |
| `/serverinfo` | Server details (members, owner, boosts) | — |
| `/userinfo [user]` | Account, ID and join info | — |
| `/avatar [user]` | Show a user's avatar | — |
| `/ban <user> [reason]` | Ban a member | Ban Members |
| `/unban <user_id> [reason]` | Lift a ban by user ID | Ban Members |
| `/kick <user> [reason]` | Kick a member | Kick Members |
| `/timeout <user> <minutes> [reason]` | Temporarily mute (up to 7 days) | Moderate Members |
| `/purge <count>` | Delete 1-100 recent messages | Manage Messages |
| `/slowmode <seconds>` | Set channel slowmode | Manage Channels |
| `/say <text>` | Post a message as the bot | Manage Messages |
| `/8ball <question>` | Magic 8-ball answer | — |
| `/roll [2d6]` | Roll dice | — |

Moderation commands also check the invoker's permissions before calling Discord,
and reply there with a readable message when Discord refuses (403 role
hierarchy, missing permission, unknown user).

## Battery and reliability notes

- While the bot runs, BotDroid holds a partial wake lock and shows a permanent
  notification with a **Stop** button. Battery use is small (one WebSocket),
  but a phone will always use more power than a VPS.
- Android may kill background apps aggressively. If the bot goes offline, open
  Android settings -> Apps -> BotDroid -> Battery and allow unrestricted
  background usage.
- Mobile networks drop connections; the bot reconnects automatically with
  backoff.

## Build from source

Requirements: JDK 17+ and the Android SDK with `build-tools;34.0.0` and
`platforms;android-34`. No Gradle, no external libraries.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./scripts/build-apk.sh
# -> dist/BotDroid.apk
adb install -r dist/BotDroid.apk
```

The script compiles resources with `aapt2`, sources with `javac`, dexes with
`d8`, aligns and signs with `apksigner`.
`keystore/botdroid.jks` is a development signing key (password `botdroid`) so
that updated builds install over old ones; replace it for a store release.

Pushing to `main`, pushing a `v*` tag, or running the **Build and release APK**
workflow publishes `BotDroid.apk` to GitHub Releases.

## How it works

```
app/src/main/java/dev/botdroid/app/
  MainActivity.java   token entry, start/stop, live log
  BotService.java     foreground service + notification + wake lock
  DiscordBot.java     gateway loop, heartbeats, slash command registration
  Commands.java       preset commands and their handlers
  DiscordRest.java    Discord REST v10 client with rate-limit retry
  GatewaySocket.java  minimal RFC 6455 WebSocket client
  Net.java            minimal HTTPS client (supports PATCH)
  LogBus.java         in-memory log shared with the UI
  Prefs.java          app-private token storage
```

Everything network-facing is implemented on plain sockets, so the app has zero
third-party dependencies: Android SDK classes plus `org.json` only.

## Security

The token is stored in the app's private `SharedPreferences` (not world
readable, excluded from backups) and is only sent to `discord.com` and
`gateway.discord.gg` over TLS. If you share the APK, do not share a token; if a
token leaks, reset it in the Developer Portal.
