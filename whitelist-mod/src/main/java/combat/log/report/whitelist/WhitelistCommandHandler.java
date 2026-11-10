package whitelisting.swordsmp.whitelist;

import whitelisting.swordsmp.discord.DiscordBotManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles whitelist commands from Discord bot
 */
public class WhitelistCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistCommandHandler.class);
    private final MinecraftServer server;

    public WhitelistCommandHandler(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Process a whitelist add command
     */
    public void handleWhitelistAdd(String requestId, String playerName, String playerUuid, String discordId, String discordDisplayName, String requestedBy) {
        LOGGER.info("Processing whitelist add for player: {} ({}) - Discord: {}", playerName, playerUuid, discordDisplayName);

        try {
            String command = String.format("whitelist add %s", playerName);
            server.execute(() -> {
                WhitelistCommandGuard.runIgnoringAdd(() ->
                    server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        command
                    )
                );
            });

            LOGGER.info("Added {} to whitelist", playerName);

            DiscordBotManager.sendWhitelistConfirmation(requestId, true, playerName, discordDisplayName, null, requestedBy);

        } catch (Exception e) {
            LOGGER.error("Failed to add player to whitelist: {}", playerName, e);
            DiscordBotManager.sendWhitelistConfirmation(requestId, false, playerName, discordDisplayName, e.getMessage(), requestedBy);
        }
    }

    /**
     * Process a whitelist remove command
     */
    public void handleWhitelistRemove(String playerUuid, String playerName, String cause, String reason) {
        LOGGER.info("Processing whitelist remove for player: {} ({})", playerName, playerUuid);

        String command = String.format("whitelist remove %s", playerName);
        server.execute(() -> {
            try {
                WhitelistCommandGuard.runIgnoringRemove(() ->
                    server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        command
                    )
                );

                LOGGER.info("Removed {} from whitelist", playerName);
            } catch (Exception e) {
                LOGGER.warn("Whitelist remove reported an error for {}: {}", playerName, e.getMessage());
            }

            DiscordBotManager.sendWhitelistRemoveNotification(playerName, true, reason);

            ServerPlayer target = server.getPlayerList().getPlayerByName(playerName);
            if (target != null) {
                String kickMessage = "You unlinked your Discord. Re-link in Discord to rejoin.";
                if ("admin".equalsIgnoreCase(cause)) {
                    kickMessage = "A staff member unlinked your Discord. Go to Discord to re-link.";
                }
                target.connection.disconnect(net.minecraft.network.chat.Component.literal(kickMessage));
            }
        });
    }
}