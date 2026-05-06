# Whitelist Handler

A Discord bot and Fabric Minecraft mod that work together to manage Discord↔Minecraft player links with an authoritative on-server data store.

## Architecture

- **Authoritative store**: Player links live on the Minecraft server (mod persists to disk).
- **Communication**: The bot and mod exchange JSON messages over WebSocket. The bot requests link lookups and creation; the mod responds with authoritative events and handles vanilla whitelist operations.
- **Persistent queue**: Outgoing messages are backed by a file queue to survive restarts.

## Components

- `discord-bot/` — Discord bot that collects whitelist requests, manages staff workflows, and coordinates with the Minecraft server over WebSocket.
- `whitelist-mod/` — Fabric mod that acts as the authoritative link store, exposes a WebSocket listener, and provides in-game admin commands.
- `docs/websocket-api.md` — Protocol reference for bot↔mod messages.

## Quick Start

1. **Set up the Minecraft mod first** (it is authoritative). See [whitelist-mod/README.md](whitelist-mod/README.md).
2. **Configure the Discord bot** by copying `discord-bot/config.example.json` to `discord-bot/config.json` and setting:
   - `discord.token` — your bot token from the Discord Developer Portal
   - `websocket.host` and `websocket.port` — network interface and port (the mod will connect to this)
   - `channels.whitelistLogChannelId` — channel ID for whitelist log output
3. **Build the bot**:
   ```powershell
   Set-Location whitelist-handler/discord-bot
   .\gradlew.bat build
   ```
4. **Run the bot**:
   ```powershell
   java -jar build/libs/discord-bot-<version>-all.jar
   ```

## Available Slash Commands

- `/test` — Run an end-to-end test (create → lookup → unlink). Results post to the whitelist log channel.
- `/whitelist tickets <channel>` — Setup a channel with the whitelist request button.
- `/whitelist log <channel>` — Set the whitelist log channel.
- `/whitelist unlink <user>` — Admin: unlink a Discord user from their Minecraft account.

## In-Game Commands

- `/discord test` — Request the bot to run its remote test. Results appear in the bot's whitelist log channel.
- `/discord unlink` — Unlink your Discord account from Minecraft (or staff can unlink others).

## Notes

- Global slash command registration can take up to a minute; for development, set `discord.guildId` in the config for instant registration.
- WebSocket auth is optional by default. To require authentication, set `websocket.authToken` in both bot and mod configs.
- The bot also supports `config.local.json` as an override file for sensitive or environment-specific values.

## See Also

- [Discord Bot Setup](discord-bot/README.md)
- [Minecraft Mod Setup](whitelist-mod/README.md)
- [WebSocket Protocol](docs/websocket-api.md)
