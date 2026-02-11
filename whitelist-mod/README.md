# Whitelist Server Mod

Minecraft server-side Fabric mod that stores authoritative player-to-discord links and exposes a WebSocket API for bots to query/create links and receive events.

What it does
- Persists player links (default: `config/player-links.json`).
- Responds to `link_lookup` requests and handles `link_create_request` from bots.
- Executes server whitelist operations (`whitelist add` / `whitelist remove`) when configured and emits `link_created`/`link_removed` messages.

Installation
1. Build the mod (see `whitelist-mod/` Gradle project).
2. Drop the generated mod JAR into your server's `mods/` folder.
3. Configure the mod via `config.json` (see `config.example.json` shipped with the mod).

Configuration
- `socketAuth.token` — shared secret required by connecting bots.
- `websocket.port` / `websocket.path` — where the mod listens for WebSocket clients (or integrated into an existing web endpoint).
- `playerLinks.path` — path to persisted links (defaults to `config/player-links.json`).

Operation
- When a `link_create_request` arrives and is authenticated, the mod writes the link to `player-links.json` and replies with `link_created`.
- The mod can optionally run the in-server `whitelist` command to add/remove players from the vanilla whitelist when links are created/removed.

Security
- The WebSocket endpoint requires the shared token in the `Authorization: Bearer <token>` header. Keep the token secret.

Troubleshooting
- If bots cannot connect, confirm firewall/port and that the mod is listening on the configured path.
- If links are not persisted, ensure the server process has write permission to the configured `playerLinks.path`.

See also
- WebSocket message formats: [../docs/websocket-api.md](../docs/websocket-api.md)
- Bot client: [../discord-bot](../discord-bot/)
