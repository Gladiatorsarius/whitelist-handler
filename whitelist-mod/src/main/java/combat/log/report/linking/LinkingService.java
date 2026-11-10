package whitelisting.swordsmp.linking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Small service facade around PlayerLinkingManager to simplify calls from Discord listeners.
 */
public class LinkingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkingService.class);
    private static LinkingService instance;

    private LinkingService() {}

    public static synchronized LinkingService getInstance() {
        if (instance == null) instance = new LinkingService();
        return instance;
    }

    /**
     * Create a link between a Discord ID and a Minecraft name.
     * Generates a stable UUID from the player name if a real UUID is not provided.
     */
    public void createLink(String discordId, String minecraftName, boolean whitelisted) {
        try {
            String uuid = resolveUuidForName(minecraftName);
            PlayerLinkingManager.getInstance().addLink(discordId, uuid, minecraftName, whitelisted);
            LOGGER.info("Link created: Discord {} -> Minecraft {} ({})", discordId, minecraftName, uuid);
        } catch (Exception e) {
            LOGGER.error("Failed to create link for {} -> {}", discordId, minecraftName, e);
        }
    }

    public void removeLinkByUuid(String minecraftUuid) {
        PlayerLinkingManager.getInstance().removeLink(minecraftUuid);
    }

    public Optional<String> getMinecraftUuid(String discordId) {
        return PlayerLinkingManager.getInstance().getMinecraftUuid(discordId);
    }

    public Optional<PlayerLink> getLinkByName(String name) {
        return PlayerLinkingManager.getInstance().getLinkByName(name);
    }

    private String resolveUuidForName(String name) {
        if (name == null) return "";
        // Stable name-based UUID (not Mojang UUID). Good enough until Mojang validation is added.
        UUID uuid = UUID.nameUUIDFromBytes(name.toLowerCase().getBytes(StandardCharsets.UTF_8));
        return uuid.toString();
    }
}
