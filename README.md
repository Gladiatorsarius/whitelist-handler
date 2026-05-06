# Whitelist Handler

A Fabric Minecraft mod with an embedded Discord bot that manages Discord↔Minecraft player links with an authoritative on-server data store.

## Architecture

- **Authoritative store**: Player links live on the Minecraft server (mod persists to disk).
- **Communication**: The Discord bot runs inside the mod process and calls the server-side whitelist/linking services directly.
- **Persistence**: Player links live on the Minecraft server and are written to disk by the mod.

## Components

- `whitelist-mod/` — Fabric mod that contains the embedded Discord bot, authoritative link store, and in-game admin commands.

## Quick Start

1. **Set up the Minecraft mod**. See [whitelist-mod/README.md](whitelist-mod/README.md).
2. **Configure the embedded Discord bot** by copying `whitelist-mod/config.example.json` to `whitelist-mod/config.json` and setting:
   - `discord.token` — your bot token from the Discord Developer Portal
   - `discord.guildId` — optional guild ID for instant command registration
   - `discord.logChannelId` — channel ID for whitelist log output
   - `discord.staffRoleId` — optional role ID used for staff-only actions
3. **Build the mod**:
   ```powershell
   Set-Location whitelist-handler/whitelist-mod
   .\gradlew.bat build
   ```
4. **Run the mod** from your Minecraft server or client workspace as usual.

## Available Slash Commands

- `/whitelist tickets <channel>` — Setup a channel with the whitelist request button.
- `/whitelist log <channel>` — Set the whitelist log channel.
- `/whitelist unlink <user>` — Admin: unlink a Discord user from their Minecraft account.

## In-Game Commands

- `/discord test` — Request the embedded bot to post a test message to the configured log channel.
- `/discord unlink` — Unlink your Discord account from Minecraft (or staff can unlink others).

## Notes

- Global slash command registration can take up to a minute; for development, set `discord.guildId` in the config for instant registration.
- The mod reads `whitelist-mod/config.json` if present and falls back to environment variables for the bot token and other Discord settings.

## See Also

- [Minecraft Mod Setup](whitelist-mod/README.md)
