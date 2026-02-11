# Whitelist Discord Bot

Java-based Discord bot that collects whitelist requests, sends them to the authoritative Minecraft server mod, and presents staff UI for approvals.

What it does
- Presents modals/commands to request whitelist links for Discord users.
- Sends `link_lookup` and `link_create_request` messages to the server mod over WebSocket.
- Displays confirmations and admin controls in Discord.

Prerequisites
- Java 17+ (match the project's Gradle JVM setup).
- A Discord bot token with required intents (guild members, messages) and the bot invited to your server.

Configuration
- Edit `config.json` (or copy `config.example.json`) and set the following at minimum:
	- `discord.botToken` — your bot token.
	- `websocket.serverUrl` — WebSocket URL of the server mod (wss or ws).
	- `socketAuth.token` — shared secret matching the server mod's config.

Build
From the `discord-bot/` folder you can use the included Gradle wrapper:

```powershell
Set-Location whitelist-handler/discord-bot
.\gradlew.bat shadowJar
```

The fat (shadow) JAR will be placed in `build/libs/` (name may include version and `all` suffix).

Run
Start the bot with the generated jar and the config file present in the working directory:

```powershell
java -jar build/libs/discord-bot-1.0.0-all.jar
```

Notes
- The bot is a client only — authoritative writes must go through the server mod.
- Use `logs/` (if present) to inspect runtime output. Adjust `logging.level` in `config.json` for verbosity.

Troubleshooting
- If the bot fails to connect: verify `websocket.serverUrl` and `socketAuth.token`.
- If Discord features fail: ensure the bot has correct intents and that `discord.botToken` is valid.

See also
- WebSocket message formats: [../docs/websocket-api.md](../docs/websocket-api.md)
