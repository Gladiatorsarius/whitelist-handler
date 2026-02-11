# whitelist-handler

This repository contains the separated whitelist functionality used by Swordsmp mods. It provides a Discord bot that collects whitelist requests and a server-side Minecraft mod that stores and enforces player links.

Contents
- `discord-bot/` — Java-based Discord bot that provides the UI and workflow for whitelist requests and approval.
- `whitelist-mod/` — Minecraft Fabric mod that stores authoritative player links and exposes a WebSocket endpoint for bots to request link creation and lookups.
- `docs/` — Protocol documentation, including message schemas (`websocket-api.md`).

Quick overview
- The authoritative data store for player links is on the Minecraft server (persisted by `whitelist-mod`).
- The Discord bot sends `link_create_request` and `link_lookup` messages to the server over WebSocket. The server responds with `link_created` and `link_lookup_response`.
- The bot's local DB (if present) is used as a cache/archive only — the server is authoritative.

Getting started
1. Configure the Minecraft server mod first. See [whitelist-mod](whitelist-mod/README.md) for build and install steps.
2. Configure the Discord bot by editing `discord-bot/config.json` (see the example below).
3. Build and run the bot from the `discord-bot/` folder using Gradle (there are tasks and scripts included).

Discord bot configuration (summary)
- `discord-bot/config.json` — Primary runtime configuration for the bot. See `discord-bot/config.example.json` for a minimal example.
- Required values: `discord.botToken`, `websocket.serverUrl`, `socketAuth.token` (shared secret between bot and server).

Example `discord-bot/config.json`
The repository contains `discord-bot/config.example.json`. A fuller example is provided in the repo and included with the bot distribution. Replace the placeholder values before starting the bot.

Build & run
- To build the bot (from repository root):

	- On Windows (PowerShell):

		Set-Location whitelist-handler/discord-bot; .\gradlew.bat shadowJar

	- Or use the provided wrapper scripts inside `discord-bot/`.

- The resulting artifact is placed under `discord-bot/build/libs/` (a fat/shadow jar). Run it with:

	java -jar build/libs/discord-bot-1.0.0-all.jar

WebSocket API
- See [docs/websocket-api.md](docs/websocket-api.md) for message schemas, request/response flow and required auth headers.

Configuration notes
- Keep the `socketAuth.token` secret — it authenticates bot requests to the server.
- Use `websocket.serverUrl` to point to your running Minecraft server WebSocket endpoint (for example `wss://example.com/ws` or `ws://localhost:8080/ws`).

Troubleshooting
- If the bot fails to connect to the server, verify the WebSocket URL and shared token. Check the server logs for handshake/auth failures.
- Use the `logs/` folder in `discord-bot/` (if present) to inspect runtime logs.

Further reading
- Protocol details: [docs/websocket-api.md](docs/websocket-api.md)
- Server implementation: [whitelist-mod/](whitelist-mod/)

License
- See the top-level LICENSE files in the subprojects for licensing details.

If you want, I can also validate the bot startup locally or update `discord-bot/README.md` with build instructions.
