package combat.log.discord;

import combat.log.discord.config.BotConfig;
import combat.log.discord.whitelist.WhitelistManager;
import combat.log.discord.websocket.WhitelistWebSocketServer;

import java.io.File;

public class WhitelistBotMain {
    public static void main(String[] args) throws Exception {
        File cfgFile = new File("whitelist-handler/discord-bot/config.json");
        BotConfig config = BotConfig.load(cfgFile);

        // Initialize websocket server for Minecraft connections
        WhitelistWebSocketServer ws = new WhitelistWebSocketServer(config);
        ws.start();

        // For a full bot, JDA initialization would go here; omitted for build-only purposes.
        System.out.println("Whitelist bot started (compile-time run). WebSocket listening on " + config.websocket.port);
    }
}
