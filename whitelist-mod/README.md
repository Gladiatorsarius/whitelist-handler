
# Whitelist Server Mod

Fabric Minecraft mod that acts as the authoritative store for Discord↔Minecraft links and exposes a WebSocket API for integration with Discord bots.

Core responsibilities
- Persist player links (default: `config/player-links.json`).
- Handle `link_lookup` and `link_create_request` messages from external bots and emit `link_created` / `whitelist_confirmation` / `link_removed` events.
- Optionally execute in-server `whitelist add` / `whitelist remove` when configured.

Installation
1. Build the mod with the Gradle wrapper (inside `whitelist-mod/`).
2. Copy the generated JAR into the server `mods/` folder and restart the server.
-3. Edit `config.json` (or `config.example.json`) to set the WebSocket options and other settings.

Important configuration
- `playerLinks.path` — where links are persisted (defaults to `config/player-links.json`). Ensure the server has write permissions.
  
Note: This deployment does not use a shared secret by default; the WebSocket endpoint accepts unauthenticated connections unless you modify the server and bot to require an `Authorization` header.
- `playerLinks.path` — where links are persisted (defaults to `config/player-links.json`). Ensure the server has write permissions.

In-game commands
- `/discord test` — request the bot to run its remote test (server-initiated). Use as a server admin; the bot will post results to its whitelist log channel.
- `/discord unlink` — unlink a player from Discord. This can be used by players (self-unlink) or by staff to unlink others (depending on permissions).

Notes on end-to-end testing
- The bot and mod exchange short JSON messages over WebSocket. A typical test flow:
	1. Bot or mod sends `link_create_request` / `test_request`.
	2. Server writes the link and responds with `link_created`.
	3. Bot performs a `link_lookup` and optionally issues `unlink` to clean up.
	4. Test results are posted to the bot's configured whitelist log channel.

Security & troubleshooting
- Confirm the `Authorization` header matches `Bearer <token>` on both sides.
- If the mod is rejecting connections, check the server logs for handshake errors and ensure the port is open.

See also
- Protocol: [../docs/websocket-api.md](../docs/websocket-api.md)
- Bot: [../discord-bot/README.md](../discord-bot/README.md)
