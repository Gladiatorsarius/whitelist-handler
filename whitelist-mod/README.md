# Whitelist Server Mod

This mod runs on the Minecraft server and holds authoritative player link state in `config/player-links.json`.

Responsibilities:
- Persist `player-links.json` and answer `link_lookup` requests.
- Execute `whitelist add` / `whitelist remove` when requested by the Discord bot.
- Emit `link_created` and `link_removed` events to connected bots.

Configuration example is provided in `config.example.json`.
