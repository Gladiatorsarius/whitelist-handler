# Whitelist Server Mod

Fabric Minecraft mod that acts as the authoritative store for Discord↔Minecraft player links and coordinates with a Discord bot over WebSocket.

## Core Responsibilities

- **Persist player links**: Stores Discord↔Minecraft associations in `config/player-links.json`.
- **WebSocket API**: Exposes a listener on the configured port that the Discord bot connects to.
- **Whitelist coordination**: Handles `/whitelist add` and `/whitelist remove` commands, optionally coordinating with the bot.
- **In-game commands**: Provides `/discord test` and `/discord unlink` for admins and players.

## Installation

1. Build the mod with the Gradle wrapper (inside `whitelist-mod/`):
   ```powershell
   Set-Location whitelist-handler/whitelist-mod
   .\gradlew.bat build
   ```

2. Copy the generated JAR from `build/libs/` to your server's `mods/` folder:
   ```powershell
   copy build/libs/whitelist-mod-*.jar /path/to/server/mods/
   ```

3. Restart your Minecraft server. The mod will create `config/player-links.json` on first run.

## Configuration

The mod uses two configuration mechanisms:

### Environment Variable (Recommended)
Set the `DISCORD_SOCKET_URL` environment variable to specify the Discord bot's WebSocket endpoint:

```bash
# Example
export DISCORD_SOCKET_URL=ws://localhost:8080/combat-log
```

If not set, the mod defaults to `ws://localhost:8080/combat-log`.

### Optional: WebSocket Authentication
If the Discord bot has `websocket.authToken` set in its config, ensure the bot and mod use matching tokens. The mod will include `Authorization: Bearer <token>` in the WebSocket handshake.

## Data Storage

- **Player links**: Persisted to `config/player-links.json` on the server. This file is authoritative and contains Discord ID ↔ Minecraft UUID mappings.
- **Link format**:
  ```json
  {
    "links": [
      {
        "discordId": "123456789",
        "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
        "playerName": "PlayerName",
        "whitelisted": true
      }
    ]
  }
  ```

## In-Game Commands

### `/discord test`
- **Description**: Request the Discord bot to run its end-to-end test (create → lookup → unlink). Results appear in the bot's whitelist log channel.
- **Usage**: `/discord test`
- **Permission**: Server operator (OP or creative mode).

### `/discord unlink`
- **Description**: Unlink your Discord account from Minecraft.
  - Players can unlink themselves.
  - Staff can unlink other players using: `/discord unlink <player-name>`
- **Usage**:
  - Self-unlink: `/discord unlink` (no arguments)
  - Admin unlink: `/discord unlink <player-name>`

## WebSocket Protocol

The mod connects to the Discord bot's WebSocket endpoint and exchanges JSON messages. See [../docs/websocket-api.md](../docs/websocket-api.md) for the protocol reference.

## Troubleshooting

- **Mod cannot connect to bot**: Verify `DISCORD_SOCKET_URL` is set correctly and the bot's WebSocket server is running. Check firewall and network settings.
- **Links not persisting**: Ensure the server has write permission to the `config/` directory.
- **Commands not responding**: Verify the mod loaded successfully by checking server logs for `[Whitelisting via Discord]` messages.
- **WebSocket connection errors**: If authentication is enabled, confirm both bot and mod have matching `authToken` values.

## See Also

- [Discord Bot Setup](../discord-bot/README.md)
- [WebSocket Protocol](../docs/websocket-api.md)
