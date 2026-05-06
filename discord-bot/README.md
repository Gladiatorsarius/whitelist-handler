# Whitelist Discord Bot

Java-based Discord bot that manages whitelist requests and coordinates with the authoritative Minecraft mod.

## Key Features

- **Whitelist requests**: Users submit Minecraft username via a modal; staff approve/deny with Discord commands.
- **End-to-end testing**: `/test` command triggers a full create → lookup → unlink cycle; results post to the whitelist log channel.
- **Persistent message queue**: Outgoing messages survive bot restarts via a local file queue.
- **WebSocket coordination**: Reliable two-way messaging with the Minecraft server over JSON-based protocol.

## Prerequisites

- Java 17+
- Discord bot token with intents: `GUILD_MESSAGES`, `MESSAGE_CONTENT`, `GUILD_MEMBERS`
- Bot invited to your Discord server

## Quick Configuration

1. Copy `config.example.json` to `config.json`:
   ```powershell
   copy config.example.json config.json
   ```
2. Set the required fields:
   - `discord.token` — your bot token from the Discord Developer Portal
   - `websocket.host` and `websocket.port` — where the bot's WebSocket server will listen (e.g., `0.0.0.0` and `8080`)
   - `channels.whitelistLogChannelId` — channel ID where logs and test results are posted

## Configuration Details

Edit `discord-bot/config.json` and set the following fields:

### Discord Settings
- `discord.token` (string, required): Bot token from Discord Developer Portal.
- `discord.guildId` (string, optional): If set, commands register to this guild immediately (for development). If omitted, global registration takes up to a minute.
- `discord.staffRoleId` (string, optional): Staff role ID for permission checks (if omitted, "Manage Server" permission is used).

### WebSocket Settings
- `websocket.host` (string): Network interface for the WebSocket server (e.g., `0.0.0.0` for all interfaces).
- `websocket.port` (number): Port for the WebSocket server. The Minecraft mod connects to `ws://<host>:<port>/combat-log` by default.
- `websocket.authToken` (string, optional): If set, enables WebSocket authentication via `Authorization: Bearer <token>` header. Both bot and mod must have matching tokens.

### Channels
- `channels.whitelistLogChannelId` (string, recommended): Channel ID where whitelist events and `/test` results are posted.
- `channels.whitelistChannelId` (string, optional): Channel ID for general whitelist notifications.
- `channels.reviewChannelId` (string, optional): Channel ID for staff review of pending requests.
- `channels.ticketChannelId` (string, optional): Channel ID for ticket operations (used by ticket system).

### Features
- `features.whitelistEnabled` (boolean): Enable/disable the whitelist feature (default: `true`).
- `features.mojangApiEnabled` (boolean): Enable/disable Mojang API lookups for username validation (default: `true`).
- `features.useForumChannel` (boolean): Use forum channels for organized threads (default: `true`).
- `features.autoDenyEnabled` (boolean): Auto-deny requests under certain conditions (default: `true`).
- `features.privateThreads` (boolean): Create private threads for staff channels (default: `true`).

### Timeouts & API
- `timeouts.ticketTimeoutMinutes` (number): How long before a request expires (default: 60).
- `timeouts.mojangCacheDurationMinutes` (number): How long to cache Mojang API results (default: 5).
- `timeouts.mojangApiTimeoutSeconds` (number): Timeout for Mojang API requests (default: 5).

### Override File
The bot also supports `config.local.json` as a local override file. Any values in `config.local.json` override the base `config.json`. This is useful for environment-specific settings or sensitive values that should not be committed to version control.

Example `config.local.json`:
```json
{
  "discord": {
    "token": "YOUR_ACTUAL_TOKEN_HERE"
  },
  "websocket": {
    "authToken": "your-secret-auth-token"
  }
}
```

## Discord Developer Portal Setup

1. **Create the application**
   - Go to https://discord.com/developers/applications
   - Click **New Application**, give it a name, open the application.

2. **Add a bot**
   - In the sidebar select **Bot** → **Add Bot**.
   - Copy the **Token** and set it into `config.json` as `discord.token` (keep this secret).

3. **Intents** (configure these under **Privileged Gateway Intents**)
   - ✅ **Server Members Intent** — recommended for member lookups and role checks.
   - ℹ️ **Message Content Intent** — optional, only needed if reading raw message content (not required for slash commands).

4. **OAuth2 & Invite to Your Server**
   - In the sidebar choose **OAuth2 → URL Generator**.
   - Scopes: check `bot` and `applications.commands` (required for slash commands).
   - Bot Permissions: Grant either **Administrator** for testing, or the minimal set:
     - View Channels
     - Send Messages
     - Embed Links
     - Create Public Threads
     - Create Private Threads
     - Manage Threads (optional, for cleanup)
   - Copy the generated invite URL and open it in your server.

5. **Command Registration Strategy**
   - **Development**: Set `discord.guildId` in `config.json` and invite the bot to that guild. Commands register instantly.
   - **Production**: Omit `discord.guildId`. Commands register globally but may take up to a minute to appear.

## Build

From the `discord-bot/` folder:

```powershell
Set-Location whitelist-handler/discord-bot
.\gradlew.bat build
```

The build creates a runnable fat JAR under `build/libs/`.

## Run

After building, start the bot:

```powershell
java -jar build/libs/discord-bot-<version>-all.jar
```

The bot will:
- Load or create `config.json`
- Start the JDA Discord client
- Start the WebSocket server on the configured `websocket.host:websocket.port`
- Register slash commands (guild-scoped if `discord.guildId` is set, otherwise global)

## Available Slash Commands

### `/test`
- **Description**: Run an end-to-end whitelist test (create → lookup → unlink). Results are posted to the whitelist log channel.
- **Permission**: Any user can invoke.
- **Usage**: `/test`

### `/whitelist tickets <channel>`
- **Description**: Setup a channel with the whitelist request button and initial message.
- **Permission**: Staff (requires Manage Server or configured staff role).
- **Usage**: `/whitelist tickets channel:#whitelist-requests`

### `/whitelist log <channel>`
- **Description**: Set the channel where whitelist events and test results are posted.
- **Permission**: Staff (requires Manage Server or configured staff role).
- **Usage**: `/whitelist log channel:#whitelist-log`

### `/whitelist unlink <user>`
- **Description**: Admin command to unlink a Discord user from their Minecraft account.
- **Permission**: Staff (requires Manage Server or configured staff role).
- **Usage**: `/whitelist unlink user:@SomeUser`

## WebSocket Server

The bot exposes a WebSocket server on `ws://websocket.host:websocket.port/combat-log` that the Minecraft mod connects to. The mod sends and receives whitelist-related messages (see [../docs/websocket-api.md](../docs/websocket-api.md) for protocol details).

### Authentication (Optional)

By default, WebSocket connections do not require authentication. To enable authentication:

1. Set `websocket.authToken` in `config.json` (or `config.local.json`).
2. Ensure the Minecraft mod has the same token configured.
3. The bot will then validate all incoming WebSocket connections with `Authorization: Bearer <token>` header.

## Troubleshooting

- **Bot cannot send messages**: Ensure the bot has the required channel permissions in Discord.
- **Commands not appearing**: For guild-scoped commands, verify `discord.guildId` is set correctly. For global commands, wait up to a minute and refresh Discord.
- **WebSocket connection refused**: Check that `websocket.host` and `websocket.port` are correct and accessible from the Minecraft server. Verify firewall settings.
- **Outgoing messages are queued**: Check `discord-bot/data/pending-whitelist.log` for the queue. Restart the bot if the mod is not responding.
- **Check logs**: Look in `discord-bot/logs/` (if present) for detailed trace messages.
