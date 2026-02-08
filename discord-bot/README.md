# Whitelist Discord Bot

This project contains the Discord bot that handles whitelist requests, modals, and staff review.

Notes:
- Keep the bot implementation in Java (existing codebase).
- The bot will no longer act as the authoritative store for player links. It will send `link_lookup` and `link_create_request` messages to the whitelist server mod and wait for confirmations.
- Configuration example is provided in `config.example.json`.

See `../docs/websocket-api.md` for the WebSocket message formats.
