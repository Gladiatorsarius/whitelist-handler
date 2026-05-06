# WebSocket Protocol: Discord Bot ↔ Minecraft Mod

This document describes the JSON-based messages exchanged between the Discord bot and the Minecraft whitelist mod over WebSocket.

## Connection

- **URL**: `ws://bot-host:bot-port/combat-log`
- **Default URL on mod**: `ws://localhost:8080/combat-log` (configurable via `DISCORD_SOCKET_URL` environment variable)
- **Authentication**: Optional. If `websocket.authToken` is set on the bot, the mod must include `Authorization: Bearer <token>` header.

## Message Format

All messages are JSON objects with a required `type` field and message-specific payload fields.

---

## Bot → Mod Messages

### `link_lookup`

Bot requests information about a specific player link.

**Request (Bot → Mod)**:
```json
{
  "type": "link_lookup",
  "requestId": "<uuid>",
  "query": "byUuid|byDiscord|byName",
  "value": "<query-value>"
}
```

**Response (Mod → Bot)**:
```json
{
  "type": "link_lookup_response",
  "requestId": "<uuid>",
  "found": true,
  "discordId": "123456789",
  "minecraftUuid": "550e8400-e29b-41d4-a716-446655440000",
  "minecraftName": "PlayerName",
  "whitelisted": true
}
```

### `link_create_request`

Bot requests the mod to create a new Discord↔Minecraft link and add the player to the server whitelist.

**Request (Bot → Mod)**:
```json
{
  "type": "link_create_request",
  "requestId": "<uuid>",
  "discordId": "123456789",
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "PlayerName",
  "requestedBy": "staff-user-id or AUTO_APPROVED"
}
```

**Response (Mod → Bot)**:
```json
{
  "type": "link_created",
  "requestId": "<uuid>",
  "discordId": "123456789",
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "PlayerName"
}
```

### `unlink_player`

Bot requests the mod to unlink a player and remove them from the server whitelist.

**Request (Bot → Mod)**:
```json
{
  "type": "unlink_player",
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "PlayerName",
  "reason": "admin_unlink|player_request|link_removal"
}
```

**Mod acknowledges** by logging the unlink event internally.

---

## Mod → Bot Messages

### `whitelist_confirmation`

Mod confirms a successful whitelist add.

**Message (Mod → Bot)**:
```json
{
  "type": "whitelist_confirmation",
  "discordId": "123456789",
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "PlayerName",
  "success": true
}
```

### `whitelist_remove_confirmation`

Mod confirms a successful whitelist remove.

**Message (Mod → Bot)**:
```json
{
  "type": "whitelist_remove_confirmation",
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "PlayerName",
  "success": true
}
```

### `vanilla_whitelist_add`

Mod notifies bot when a player is added to the vanilla server whitelist (e.g., via `/whitelist add` command).

**Message (Mod → Bot)**:
```json
{
  "type": "vanilla_whitelist_add",
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "PlayerName"
}
```

### `test_request`

Mod (admin or `/discord test` command) requests the bot to run an end-to-end test.

**Message (Mod → Bot)**:
```json
{
  "type": "test_request"
}
```

**Bot responds with** a series of link operations, then sends:
```json
{
  "type": "test_result",
  "success": true,
  "message": "Test completed successfully"
}
```

The bot posts results to the configured whitelist log channel.

---

## Notes

- All `requestId` values should be unique UUIDs to correlate request/response pairs.
- The mod is the authoritative store: if a link exists in the mod's `player-links.json`, it is the source of truth.
- If authentication is enabled (`websocket.authToken` on bot), all connections and messages are subject to token validation.
- The bot maintains a file-backed pending queue at `discord-bot/data/pending-whitelist.log` to ensure messages survive restarts.
- If the mod is disconnected, the bot will queue messages and resend them when the connection is re-established.
