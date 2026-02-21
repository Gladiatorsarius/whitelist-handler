# whitelist-handler
# whitelist-handler

Repository overview

This folder contains the separated whitelist functionality used by Swordsmp: a Discord bot and a Fabric Minecraft mod that together manage authoritative Discord↔Minecraft links.

Contents
- `discord-bot/` — Java Discord bot: UI, slash commands and staff flows for whitelist requests; runs a WebSocket server to talk to the Minecraft mod.
- `whitelist-mod/` — Fabric mod: authoritative store for player links, a WebSocket client/server endpoint for bot integration, and in-game commands for admins.
- `docs/` — Protocol documentation (message schemas and examples).

Principles
- The authoritative store for links is the Minecraft server (the mod persists links).
- The bot and mod communicate over a small JSON WebSocket protocol; the bot requests lookups and creation, the mod responds with authoritative events.

New/important features
- `/test` (Discord slash): triggers the bot-side end-to-end test flow (create → lookup → unlink). Results are posted to the configured whitelist log channel.
- Mod in-game commands:
  - `/discord test` — ask the bot to run its remote test (server-initiated). Results appear in the bot's whitelist log channel.
  - `/discord unlink` — unlink a player from Discord (player or admin usage depending on arguments).
- Persistent outbound queue: outgoing messages use a file-backed pending queue at `discord-bot/data/pending-whitelist.log` to survive restarts.

Quick start
1. Install and configure the `whitelist-mod` on your Minecraft server first — it is authoritative. See [whitelist-mod/README.md](whitelist-mod/README.md).
2. Configure the Discord bot at `discord-bot/config.json` (copy `config.example.json` and fill values).
   - At minimum set `discord.token` (bot token), `websocket.port/host`, and `channels.whitelistLogChannelId`.
3. Build the bot from the `discord-bot/` folder using the Gradle wrapper:

```powershell
Set-Location whitelist-handler/discord-bot
.\gradlew.bat build
```

4. Run the bot from the produced JAR (recommended for production):

```powershell
java -jar build/libs/discord-bot-<version>-all.jar
```

Notes
- Slash command registration can take up to a minute (global) or be immediate for guild-scoped registration.
- This deployment does not use a shared secret by default — the WebSocket endpoint accepts connections without an `Authorization` header.

Troubleshooting & tips

See also
- Protocol: [docs/websocket-api.md](docs/websocket-api.md)
- Bot README: [discord-bot/README.md](discord-bot/README.md)
- Mod README: [whitelist-mod/README.md](whitelist-mod/README.md)

If you want, I can also run the bot locally and perform an end-to-end test with the mod.
