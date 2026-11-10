# File-Based Configuration System

## Overview

The mod uses a file-based configuration. When the server starts, the mod automatically:

1. Creates the `config/` directory if it doesn't exist
2. Creates `config/whitelisting-via-discord.json` with default values if it's missing
3. Loads the configuration from the file on every server start

## Configuration File Location

`config/whitelisting-via-discord.json` (relative to the server's working directory)

## Configuration Structure

```json
{
  "token": "",
  "guildId": "",
  "logChannelId": "",
  "staffRoleId": ""
}
```

### Configuration Options

- **token** (string): Your Discord bot token from the [Discord Developer Portal](https://discord.com/developers/applications). Required; without it the bot stays offline.
- **guildId** (string): The Discord server ID where the bot operates. Optional - if empty, slash commands are registered globally (can take up to an hour to appear).
- **logChannelId** (string): The channel ID where whitelist confirmations and the whitelist list are posted. Optional - can also be set at runtime with `/whitelist log <channel>`, which persists the value back to this file.
- **staffRoleId** (string): Discord role allowed to use staff-only actions (`/whitelist tickets`, `/whitelist log`, `/whitelist unlink`, approve/deny buttons). Optional - falls back to the `DISCORD_STAFF_ROLE_ID` environment variable. Members with Manage Server permission always count as staff.

## Setup Instructions

### Step 1: Start the Server

Run the server once. The `config/` directory and default config file are created automatically.

### Step 2: Configure the Bot

Edit `config/whitelisting-via-discord.json` and fill in at least your bot token:

```json
{
  "token": "YOUR_DISCORD_BOT_TOKEN_HERE",
  "guildId": "YOUR_DISCORD_SERVER_ID",
  "logChannelId": "",
  "staffRoleId": ""
}
```

### Step 3: Restart the Server

Restart the server to load the new configuration. The bot connects when a valid token is present.

## Required Bot Intents

Enable the **Server Members** and **Message Content** privileged intents in the Developer Portal; the mod requests them when starting JDA.
