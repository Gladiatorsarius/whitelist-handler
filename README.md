# whitelist-handler

This bundle contains the separated whitelist functionality for the Swordsmp mods.

Contents:
- `discord-bot/` — Discord-side UI and request workflow for whitelisting (requests, approvals, UI). The bot sends `link_lookup` and `link_create_request` messages to the authoritative server and listens for `link_created` / `link_lookup_response` confirmations.
- `whitelist-mod/` — Server-side authoritative link storage (`player-links.json`) and WebSocket client that accepts create/lookup requests from bots.

Design decisions:
- The Minecraft server is authoritative for player links (persisted to `config/player-links.json`).
- The Discord bot no longer performs authoritative writes; it requests the server to create links via `link_create_request` and treats local DB as cache/archive.
- No migration tool is provided; the existing `whitelist.db` (Discord bot SQLite) is retained as an archive/cache.

Auth:
- The WebSocket connection is protected by a shared token. Configure `socketAuth.token` / `websocket.authToken` in each component's config to the same value.

See `docs/websocket-api.md` for message schemas and integration notes.
# Whitelist Handler

This bundle contains the whitelist handler split from the combat-log bundle.

Structure:
- `discord-bot/` — Discord bot project for whitelist UI and request workflow (Java).
- `whitelist-mod/` — Minecraft server-side mod for authoritative link storage and whitelist execution (Java).
- `docs/` — protocol and configuration documentation.

Follow `whitelist-handler/docs/websocket-api.md` for the message formats.
