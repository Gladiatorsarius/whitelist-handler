# Whitelist Server Mod

Fabric Minecraft mod that embeds the Discord bot directly and acts as the authoritative store for Discord↔Minecraft player links.

## Core Responsibilities

- **Persist player links**: Stores Discord↔Minecraft associations in `config/player-links.json`.
- **Embedded Discord bot**: Runs JDA inside the mod process and registers the whitelist slash commands.
- **Whitelist coordination**: Handles `/whitelist add` and `/whitelist remove` commands from the server side.
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

The embedded bot reads `config/whitelisting-via-discord.json` if present and falls back to environment variables for sensitive values.

Supported settings:

- `discord.token`
- `discord.guildId`
- `discord.logChannelId`
- `discord.staffRoleId`

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
- **Permission**: Server operator.

### `/discord unlink`
- **Description**: Unlink your Discord account from Minecraft.
  - Players can unlink themselves.
  - Staff can unlink other players using: `/discord unlink <player-name>`
- **Usage**:
  - Self-unlink: `/discord unlink` (no arguments)
  - Admin unlink: `/discord unlink <player-name>`

## Embedded Bot

The Discord bot now runs inside this mod process, so there is no separate WebSocket service to start or configure.

## Troubleshooting

- **Links not persisting**: Ensure the server has write permission to the `config/` directory.
- **Commands not responding**: Verify the mod loaded successfully by checking server logs for `[Whitelisting via Discord]` messages.

## See Also

- [Top-level project README](../README.md)
