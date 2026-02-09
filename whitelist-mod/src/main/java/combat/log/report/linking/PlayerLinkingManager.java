package combat.log.report.linking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authoritative server-side player link manager (singleton).
 * Stores PlayerLink entries keyed by Minecraft UUID and Discord ID in `player-links.json`.
 */
public class PlayerLinkingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerLinkingManager.class);
    private static PlayerLinkingManager instance;

    private final Map<String, PlayerLink> linksByUuid;  // Minecraft UUID -> PlayerLink
    private final Map<String, PlayerLink> linksByDiscord;  // Discord ID -> PlayerLink
    private final Path linkFile;
    private final Gson gson;

    private PlayerLinkingManager(Path configDir) {
        this.linksByUuid = new HashMap<>();
        this.linksByDiscord = new HashMap<>();
        this.linkFile = configDir.resolve("player-links.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadLinks();
    }

    public static synchronized void initialize(Path configDir) {
        if (instance == null) {
            instance = new PlayerLinkingManager(configDir);
            LOGGER.info("Initialized PlayerLinkingManager ({} links)", instance.linksByUuid.size());
        }
    }

    public static PlayerLinkingManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PlayerLinkingManager not initialized");
        }
        return instance;
    }

    public synchronized void addLink(String discordId, String minecraftUuid, String minecraftName, boolean whitelisted) {
        PlayerLink link = new PlayerLink(discordId, minecraftUuid, minecraftName, whitelisted);
        linksByUuid.put(minecraftUuid, link);
        linksByDiscord.put(discordId, link);
        saveLinks();
        LOGGER.info("Added player link: Discord {} <-> Minecraft {} ({})", discordId, minecraftName, minecraftUuid);
    }

    public synchronized void removeLink(String minecraftUuid) {
        PlayerLink link = linksByUuid.remove(minecraftUuid);
        if (link != null) {
            linksByDiscord.remove(link.getDiscordId());
            saveLinks();
            LOGGER.info("Removed player link for Minecraft UUID: {} ({})", minecraftUuid, link.getMinecraftName());
        }
    }

    public synchronized Optional<String> getDiscordId(String minecraftUuid) {
        PlayerLink link = linksByUuid.get(minecraftUuid);
        if (link != null && link.isWhitelisted()) {
            return Optional.of(link.getDiscordId());
        }
        return Optional.empty();
    }

    public synchronized Optional<String> getMinecraftUuid(String discordId) {
        PlayerLink link = linksByDiscord.get(discordId);
        if (link != null && link.isWhitelisted()) {
            return Optional.of(link.getMinecraftUuid());
        }
        return Optional.empty();
    }

    public synchronized Optional<PlayerLink> getLinkByUuid(String minecraftUuid) {
        return Optional.ofNullable(linksByUuid.get(minecraftUuid));
    }

    public synchronized Optional<PlayerLink> getLinkByName(String minecraftName) {
        if (minecraftName == null) return Optional.empty();
        for (PlayerLink link : linksByUuid.values()) {
            if (minecraftName.equalsIgnoreCase(link.getMinecraftName())) {
                return Optional.of(link);
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<PlayerLink> getLinkByDiscord(String discordId) {
        return Optional.ofNullable(linksByDiscord.get(discordId));
    }

    private void loadLinks() {
        if (!Files.exists(linkFile)) {
            LOGGER.info("No existing link file found at {}, starting fresh", linkFile);
            return;
        }

        try (Reader reader = Files.newBufferedReader(linkFile)) {
            Type type = new TypeToken<Map<String, PlayerLink>>() {}.getType();
            Map<String, PlayerLink> loadedLinks = gson.fromJson(reader, type);
            if (loadedLinks != null) {
                for (PlayerLink link : loadedLinks.values()) {
                    linksByUuid.put(link.getMinecraftUuid(), link);
                    linksByDiscord.put(link.getDiscordId(), link);
                }
                LOGGER.info("Loaded {} player links from file", loadedLinks.size());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load player links from {}", linkFile, e);
        }
    }

    private void saveLinks() {
        try {
            Files.createDirectories(linkFile.getParent());
            try (Writer writer = Files.newBufferedWriter(linkFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                gson.toJson(linksByUuid, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save player links to {}", linkFile, e);
        }
    }
}
