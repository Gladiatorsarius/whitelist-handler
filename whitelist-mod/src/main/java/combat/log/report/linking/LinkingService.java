package whitelisting.swordsmp.linking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
     * Create a link between a Discord ID and a Minecraft account.
     */
    public void createLink(String discordId, String minecraftUuid, String minecraftName, boolean whitelisted) {
        try {
            PlayerLinkingManager.getInstance().addLink(discordId, minecraftUuid, minecraftName, whitelisted);
            LOGGER.info("Link created: Discord {} -> Minecraft {} ({})", discordId, minecraftName, minecraftUuid);
        } catch (Exception e) {
            LOGGER.error("Failed to create link for {} -> {}", discordId, minecraftName, e);
        }
    }

    /** Create a link after an atomic ownership check on the Minecraft UUID. */
    public PlayerLinkingManager.AddLinkResult createLinkChecked(String discordId, String minecraftUuid, String minecraftName, boolean whitelisted) {
        return PlayerLinkingManager.getInstance().addLinkChecked(discordId, minecraftUuid, minecraftName, whitelisted);
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
}
