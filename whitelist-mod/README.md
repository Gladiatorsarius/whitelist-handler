# Whitelist Server Mod

Fabric Minecraft mod that embeds the Discord bot directly and acts as the authoritative store for Discord-Minecraft player links.

## Core Responsibilities

- **Persist player links**: Stores Discord-Minecraft associations in `config/player-links.json`.
- **Embedded Discord bot**: Runs JDA inside the mod process and registers the whitelist slash commands.
- **Whitelist coordination**: Executes `/whitelist add` and `/whitelist remove` on the server.
- **In-game commands**: Provides `/discord test` (operators) and `/discord unlink` (players).

## Installation

1. Build the mod with the Gradle wrapper (inside `whitelist-mod/`):
   ```powershell
   Set-Location whitelist-handler/whitelist-mod
   .\gradlew.bat build
   ```

2. Copy the generated JAR from `build/libs/` to your server's `mods/` folder:
   ```powershell
   copy build/libs/whitelisting-via-discord-*.jar /path/to/server/mods/
   ```

3. Restart your Minecraft server. The mod creates `config/whitelisting-via-discord.json` and `config/player-links.json` on first run.

## Configuration

The embedded bot reads `config/whitelisting-via-discord.json` (in the server's run directory) and creates it with defaults if missing:

- `token` - Discord bot token (required for the bot to start)
- `guildId` - optional guild ID for instant slash command registration
- `logChannelId` - optional whitelist log channel (also settable via `/whitelist log`, which saves it back to this file)
- `staffRoleId` - optional staff role ID for staff-only actions (falls back to the `DISCORD_STAFF_ROLE_ID` environment variable)

## Data Storage

- **Player links**: Persisted to `config/player-links.json` on the server. This file is authoritative and contains Discord ID to Minecraft UUID mappings, stored as a JSON object keyed by Minecraft UUID.
- **Entry format**:
  ```json
  {
    "550e8400-e29b-41d4-a716-446655440000": {
      "discordId": "123456789",
      "minecraftUuid": "550e8400-e29b-41d4-a716-446655440000",
      "minecraftName": "PlayerName",
      "whitelisted": true
    }
  }
  ```
  Links created by in-game `/whitelist add` use the placeholder discord id `none`.

## In-Game Commands

### `/discord test`
- **Description**: Post a test message to the configured whitelist log channel.
- **Usage**: `/discord test`
- **Permission**: Server operator (permission level 2).

### `/discord unlink`
- **Description**: Unlink your own Discord account from Minecraft. You are removed from the whitelist and kicked.
- **Usage**: `/discord unlink` (no arguments)
- Staff can unlink other players from Discord with the `/whitelist unlink <user>` slash command.

## Embedded Bot

The Discord bot runs inside this mod process; there is no separate WebSocket service to start or configure.

## Troubleshooting

- **Links not persisting**: Ensure the server has write permission to the `config/` directory.
- **Commands not responding**: Check the server log for the `Embedded Discord bot connected as ...` line. Without a valid `token`, the bot stays offline.

## See Also

- [Top-level project README](../README.md)
