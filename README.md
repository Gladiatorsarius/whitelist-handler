# Whitelist Handler

A Fabric Minecraft mod with an embedded Discord bot that manages Discord to Minecraft player links with an authoritative on-server data store.

## Architecture

- **Authoritative store**: Player links live on the Minecraft server (the mod persists them to disk).
- **Communication**: The Discord bot runs inside the mod process and calls the server-side whitelist/linking services directly. There is no separate bot process or WebSocket service.

## Components

- `whitelist-mod/` - Fabric mod containing the embedded Discord bot, the authoritative link store, and the in-game commands.

## Quick Start

1. **Set up the Minecraft mod**. See [whitelist-mod/README.md](whitelist-mod/README.md).
2. **Configure the embedded Discord bot**. Start the server once so the mod creates `config/whitelisting-via-discord.json` (in the server's run directory), then fill in:
   - `token` - your bot token from the Discord Developer Portal
   - `guildId` - optional guild ID for instant command registration (global registration can take up to an hour)
   - `logChannelId` - optional channel ID for whitelist log output (can also be set later with `/whitelist log`)
   - `staffRoleId` - optional role ID used for staff-only actions (falls back to the `DISCORD_STAFF_ROLE_ID` environment variable)
3. **Build the mod**:
   ```powershell
   Set-Location whitelist-handler/whitelist-mod
   .\gradlew.bat build
   ```
4. **Run the mod** by putting the JAR from `whitelist-mod/build/libs/` into your server's `mods/` folder.

## Available Slash Commands

- `/whitelist tickets <channel>` - Staff: post the whitelist request panel in a channel.
- `/whitelist log <channel>` - Staff: set the whitelist log channel (persisted to the config file).
- `/whitelist unlink <user>` - Staff: unlink a Discord user from their Minecraft account.

## In-Game Commands

- `/discord test` - Operator-only: post a test message to the configured whitelist log channel.
- `/discord unlink` - Player: unlink your own Discord account from Minecraft (you are kicked and removed from the whitelist).

## See Also

- [Minecraft Mod Setup](whitelist-mod/README.md)
- [Configuration reference](whitelist-mod/CONFIG_SYSTEM.md)
