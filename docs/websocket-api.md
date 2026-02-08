# WebSocket API (bot ↔ server)

Schemas (JSON):

- `link_lookup` (Bot → Server)

  {
    "type": "link_lookup",
    "requestId": "<uuid>",
    "query": "byUuid|byDiscord|byName",
    "value": "<query-value>"
  }

- `link_lookup_response` (Server → Bot)

  {
    "type": "link_lookup_response",
    "requestId": "<uuid>",
    "found": true|false,
    "discordId": "<discord id> (optional)",
    "minecraftUuid": "<uuid> (optional)",
    "minecraftName": "<name> (optional)",
    "whitelisted": true|false
  }

- `link_create_request` (Bot → Server)

  {
    "type": "link_create_request",
    "requestId": "<uuid>",
    "discordId": "<discord id>",
    "playerUuid": "<minecraft uuid>",
    "playerName": "<minecraft name>",
    "requestedBy": "<staff id or AUTO_APPROVED>",
    "whitelisted": true|false
  }

- `link_created` (Server → Bot)

  {
    "type": "link_created",
    "requestId": "<uuid>",
    "discordId": "<discord id>",
    "playerUuid": "<minecraft uuid>",
    "playerName": "<minecraft name>"
  }

- `combat_log_incident` (Server → Bot)

  {
    "type": "combat_log_incident",
    "incidentId": "<uuid>",
    "playerUuid": "<uuid>",
    "playerName": "<name>",
    "combatTimeRemaining": <double>,
    "discordId": "<discord id> (optional)"
  }

Auth:
- Each connection must include an `Authorization: Bearer <token>` header. The token is configured on both sides.

Notes:
- Bots should treat `link_lookup_response.found == false` as "no link".
- Bots must send `link_create_request` for authoritative writes; servers respond with `link_created` once persisted.
- The server persists links to `config/player-links.json` (or a path configured by the server mod).
# WebSocket API — Whitelist Handler

This document describes the minimal WebSocket messages used between the whitelist Discord bot and the whitelist server mod.

1) `link_lookup` (bot → mod)

Request:
```
{ "type": "link_lookup", "requestId": "<uuid>", "query": "byUuid|byDiscord|byName", "value": "..." }
```

Response (`link_lookup_response`):
```
{ "type": "link_lookup_response", "requestId": "<uuid>", "found": true|false,
  "discordId": "...", "minecraftUuid": "...", "minecraftName": "...", "whitelisted": true|false }
```

2) `link_create_request` (bot → mod)

Request:
```
{ "type":"link_create_request", "requestId":"<uuid>", "discordId":"...", "playerUuid":"...", "playerName":"...", "requestedBy":"<user>" }
```

Response (`link_created`):
```
{ "type":"link_created", "requestId":"<uuid>", "discordId":"...", "playerUuid":"...", "playerName":"...", "whitelisted":true }
```

3) `link_removed` (mod → bot)

Emitted when a link is removed on the server (manual or vanilla whitelist removal):
```
{ "type":"link_removed", "discordId":"...", "playerUuid":"...", "playerName":"..." }
```

4) `combat_log_incident` (mod → bot)

The mod should include `discordId` when available to avoid bot-side lookups:
```
{ "type":"combat_log_incident", "incidentId":"...", "playerUuid":"...", "playerName":"...", "combatTimeRemaining":123, "discordId":"optional" }
```

Auth
- Include `socketAuth.token` in the bot and mod configs. Validate on connection/messages.
