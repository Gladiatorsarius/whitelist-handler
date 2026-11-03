# File-Based Configuration System

## Overview

The Whitelisting via Discord mod now uses a **file-based configuration system** instead of environment variables. When the server starts, the mod automatically:

1. Creates the `config/` directory if it doesn't exist
2. Creates `config/whitelisting-via-discord.json` with default values if it's missing
3. Loads the configuration from the file on every server start

## Configuration File Location

The config file is located at: `config/whitelisting-via-discord.json` (relative to server working directory)

## Configuration Structure

The config file is a JSON file with the following structure:

```json
{
  "token": "",
  "guildId": "",
  "logChannelId": "",
  "staffRoleId": ""
}
```

### Configuration Options

- **token** (string): Your Discord bot token. Get this from [Discord Developer Portal](https://discord.com/developers/applications)
- **guildId** (string): The Discord server ID where the bot will operate. Optional - if empty, commands are registered globally
- **logChannelId** (string): The ID of the Discord channel where whitelist confirmations will be posted
- **staffRoleId** (string): Reserved for future use (staff verification)

## Setup Instructions

### Step 1: Start the Server

Run the server once. The `config/` directory and default config file will be created automatically.

### Step 2: Configure the Bot

Edit `config/whitelisting-via-discord.json` and fill in your Discord bot details:

```json
{
  "token": "YOUR_DISCORD_BOT_TOKEN_HERE",
  "guildId": "YOUR_DISCORD_SERVER_ID",
  "logChannelId": "YOUR_LOG_CHANNEL_ID",
  "staffRoleId": ""
}
```

### Step 3: Restart the Server

Restart the server to load the new configuration. The Discord bot will connect if a valid token is provided.

## Getting Discord IDs

### Bot Token
1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application or select existing one
3. Go to "Bot" section and copy the token

### Guild (Server) ID
Right-click your Discord server name → "Copy Server ID"

### Channel ID
Right-click a channel name → "Copy Channel ID"

## Logging

Configuration loading is logged to the server console:
- `[INFO] Created config directory: ...` - When config directory is created
- `[INFO] Created default config file: ...` - When default config is created
- `[INFO] Discord config loaded from config/whitelisting-via-discord.json` - When config is successfully loaded
- `[WARN] Discord bot token not configured in config/whitelisting-via-discord.json; embedded Discord bot will not start` - When token is missing

## Migration from Environment Variables

The system has **fully migrated from environment variables** to file-based config:

- `DISCORD_BOT_TOKEN` → `config/whitelisting-via-discord.json` → `token` field
- `DISCORD_GUILD_ID` → `config/whitelisting-via-discord.json` → `guildId` field
- `WHITELIST_LOG_CHANNEL_ID` → `config/whitelisting-via-discord.json` → `logChannelId` field

Environment variables are **no longer used**.

## Default Configuration

If the `config/whitelisting-via-discord.json` file is deleted, a new one with default values (all empty strings) will be created on the next server start.

## Error Handling

- If the config file cannot be read, the bot uses default empty values and logs a warning
- If the config directory cannot be created, a warning is logged but the server continues to start
- If the bot token is empty or invalid, a warning is logged and the Discord bot doesn't start (but the server continues normally)
