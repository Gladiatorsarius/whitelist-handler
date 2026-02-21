# Whitelist Discord Bot

Java-based Discord bot that collects whitelist requests and coordinates with the authoritative Minecraft mod.

Key features
- Handles user whitelist requests and queues server operations reliably (file-backed pending queue).
- Slash command `/test` — run bot-side end-to-end test (create → lookup → unlink). Results post to the configured whitelist log channel.

Prerequisites
- Java 17+.
- A Discord bot token with required intents (GUILD_MEMBERS, MESSAGE_CONTENT) and the bot invited to your server.

Configuration
- Copy `config.example.json` → `config.json` and set:
  - `discord.token` — your bot token.
  - `channels.whitelistLogChannelId` — channel ID where whitelist logs and test results will be posted.
  - `websocket.host` / `websocket.port` — host/port for WebSocket server that the mod will connect to.

Configuration details

Edit `discord-bot/config.json` (or `config.example.json`) and set the following fields:

- `discord.token` (string, required): The bot token from the Discord Developer Portal.
- `discord.guildId` (string, optional): If set, command registration can be restricted to a single guild for instant registration during development.
- `channels.whitelistLogChannelId` (string, recommended): Channel ID where whitelist events and `/test` results will be posted.
- `websocket.host` (string) and `websocket.port` (number): Network interface and port for the bot's WebSocket server. The Minecraft mod should connect to `ws://<host>:<port>/combat-log` by default.
- `database.path` (string): Local SQLite path used by the bot for caching/linking history (optional).
- `logging.dir` and `logging.level`: Where to write logs and the desired log level.
- `features.whitelistRequests` (boolean): Enable/disable the whitelist feature.

Minimal example (important keys only):

```json
{
  "discord": { "token": "YOUR_BOT_TOKEN", "guildId": "YOUR_GUILD_ID" },
  "websocket": { "host": "0.0.0.0", "port": 8080 },
  "channels": { "whitelistLogChannelId": "YOUR_LOG_CHANNEL_ID" }
}
```

Discord Developer Portal setup
------------------------------

1. Create the application
  - Go to the Discord Developer Portal: https://discord.com/developers/applications
  - Click **New Application**, give it a name, then open the application.

2. Add a bot
  - In the application sidebar select **Bot** → **Add Bot**.
  - Copy the **Token** and set it into `discord-bot/config.json` as `discord.token` (keep this secret).

3. Intents (recommended)
  - Under **Privileged Gateway Intents** enable **Server Members Intent** if you need member lookups or role checks.
  - `MESSAGE_CONTENT` intent is optional and only needed if you plan to read raw message content (not required for slash commands).

4. OAuth2 / Invite the bot to your server
  - In the app sidebar choose **OAuth2 → URL Generator**.
  - Scopes: check `bot` and `applications.commands` (the latter is required for slash commands).
  - Bot Permissions: either give **Administrator** (easiest for testing) or the minimal set below:
    - View Channels (Read Messages / View Channels)
    - Send Messages
    - Embed Links
    - Create Public Threads
    - Create Private Threads
    - Manage Threads (optional, for thread cleanup)
    - Manage Messages (optional, if the bot moderates messages)
  - If you enabled Server Members Intent, make sure the bot has `View Channels` and `Send Messages` so it can respond in channels.
  - Copy the generated invite URL and open it in your server to invite the bot.

5. Guild vs Global command registration
  - During development set `discord.guildId` in `config.json` and invite the bot to that guild; guild-scoped slash commands register instantly.
  - Global registration (without `guildId`) can take up to an hour to propagate.

6. Example invite URL template
  - Replace `CLIENT_ID` and `PERMISSIONS` (use Discord's permission calculator) in the URL below:

```
https://discord.com/oauth2/authorize?client_id=CLIENT_ID&permissions=PERMISSIONS&scope=bot%20applications.commands
```

Notes
-----
- Keep your bot token secret. Do not commit `config.json` with a real token to public repositories.
- If you prefer a single-click shortcut, you can temporarily grant `Administrator` while testing and later narrow permissions for production.
- If your server uses private threads heavily, ensure the bot has the thread creation permissions listed above.

Build
From the `discord-bot/` folder use the Gradle wrapper:

```powershell
Set-Location whitelist-handler/discord-bot
.\gradlew.bat build
```

The build creates runnable JAR(s) under `build/libs/`.

Run (production)
After building, start the fat JAR:

```powershell
java -jar build/libs/discord-bot-<version>-all.jar
```

Notes
- Slash commands may take a short while to register globally — for quick testing prefer guild-scoped registration.
- The bot exposes a WebSocket server (configured in `config.json`) that the Minecraft mod connects to. By default no `Authorization` header is required.
- Outgoing messages to the server are persisted to `discord-bot/data/pending-whitelist.log` to survive restarts.

Commands
- `/test` — Request the bot to run an end-to-end create→lookup→unlink test. The bot will post success/failure to the whitelist log channel.

Available Slash Commands

- `/test`
  - Description: Run a bot-side end-to-end whitelist test: create → lookup → unlink.
  - Permission: Any user can invoke; results are posted to the configured whitelist log channel.

- `/whitelist-setup channel_id:<channelId>`
  - Description: Setup a channel with the whitelist request button and initial message. Posts a button that users can click to request whitelist access.
  - Permission: Staff (requires Manage Server or configured staff role).
  - Example: `/whitelist-setup channel_id:123456789012345678`

- `/unlink user:<@user|id>`
  - Description: Admin command to unlink a Discord user from their Minecraft account (sends unlink to server and logs the action).
  - Permission: Staff (requires Manage Server or configured staff role).
  - Example: `/unlink user:@SomeUser` or `/unlink user:123456789012345678`

Troubleshooting
- If the bot cannot accept connections from the mod: confirm host/port and firewall settings.
- Check `discord-bot/logs/` (if present) for detailed trace messages.

See also
- Protocol: [../docs/websocket-api.md](../docs/websocket-api.md)
