package whitelisting.swordsmp.whitelist;

import whitelisting.swordsmp.discord.DiscordBotManager;
import whitelisting.swordsmp.linking.LinkingService;
import whitelisting.swordsmp.linking.PlayerLinkingManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

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

        String command = String.format("whitelist add %s", playerName);
        server.execute(() -> {
            try {
                WhitelistCommandGuard.runIgnoringAdd(() ->
                    server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        command
                    )
                );

                // performPrefixedCommand returns void and logs vanilla failures instead of
                // throwing, so verify the resulting whitelist state explicitly.
                if (addSucceeded(isWhitelistedStored(playerName, playerUuid))) {
                    // Write the link only now that the whitelist state is confirmed,
                    // so a failed add never blocks a retry with a stale link.
                    var linkResult = LinkingService.getInstance().createLinkChecked(discordId, playerUuid, playerName, true);
                    if (linkResult == PlayerLinkingManager.AddLinkResult.CONFLICT_UUID_OTHER_OWNER) {
                        LOGGER.warn("Whitelisted {} but the account is already linked to a different Discord user", playerName);
                        DiscordBotManager.sendWhitelistConfirmation(requestId, false, playerName, discordDisplayName, "Minecraft account is already linked to a different Discord user", requestedBy);
                    } else {
                        LOGGER.info("Added {} to whitelist", playerName);
                        DiscordBotManager.sendWhitelistConfirmation(requestId, true, playerName, discordDisplayName, null, requestedBy);
                    }
                } else {
                    LOGGER.warn("Whitelist add did not take effect for {}", playerName);
                    DiscordBotManager.sendWhitelistConfirmation(requestId, false, playerName, discordDisplayName, "Player is not whitelisted after the command ran", requestedBy);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to add player to whitelist: {}", playerName, e);
                DiscordBotManager.sendWhitelistConfirmation(requestId, false, playerName, discordDisplayName, e.getMessage(), requestedBy);
            }
        });
    }

    /**
     * Check the STORED whitelist membership after running a vanilla whitelist command.
     * Uses UserWhiteList.isWhiteListed (stored-list contains), NOT PlayerList.isWhiteListed,
     * which is login eligibility (also true when the whitelist is disabled or the player is op).
     * Returns null when verification itself failed - unknown must not be treated as absent.
     */
    private Boolean isWhitelistedStored(String playerName, String playerUuid) {
        try {
            return server.getPlayerList().getWhiteList().isWhiteListed(new NameAndId(UUID.fromString(playerUuid), playerName));
        } catch (Exception e) {
            LOGGER.warn("Could not verify whitelist state for {}: {}", playerName, e.getMessage());
            return null;
        }
    }

    /** Add succeeded only when membership is confirmed present. */
    static boolean addSucceeded(Boolean whitelistedAfter) {
        return Boolean.TRUE.equals(whitelistedAfter);
    }

    /** Remove succeeded only when absence is confirmed; unknown counts as failure. */
    static boolean removeSucceeded(Boolean whitelistedAfter) {
        return Boolean.FALSE.equals(whitelistedAfter);
    }

    /**
     * Process a whitelist remove command
     */
    public void handleWhitelistRemove(String playerUuid, String playerName, String cause, String reason) {
        LOGGER.info("Processing whitelist remove for player: {} ({})", playerName, playerUuid);

        String command = String.format("whitelist remove %s", playerName);
        server.execute(() -> {
            boolean success = true;
            try {
                WhitelistCommandGuard.runIgnoringRemove(() ->
                    server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack(),
                        command
                    )
                );

                LOGGER.info("Removed {} from whitelist", playerName);
                // Verify the resulting stored membership: the command does not signal
                // failure, and unknown verification counts as failure, not absence.
                success = removeSucceeded(isWhitelistedStored(playerName, playerUuid));
            } catch (Exception e) {
                success = false;
                LOGGER.warn("Whitelist remove reported an error for {}: {}", playerName, e.getMessage());
            }

            DiscordBotManager.sendWhitelistRemoveNotification(playerName, success, reason);

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
