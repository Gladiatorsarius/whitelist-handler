package whitelisting.swordsmp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central manager for mod configuration.
 * Ensures config directory exists and logs configuration loading status.
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Paths.get("config");

    /**
     * Initialize configuration system: create config directory if needed.
     * Call this early on server start.
     */
    public static void initialize() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
                LOGGER.info("Created config directory: {}", CONFIG_DIR.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create config directory", e);
        }
    }

    public static Path getConfigDir() {
        return CONFIG_DIR;
    }

    /**
     * Helper: create or load a JSON config file with defaults.
     */
    public static <T> T loadOrCreateConfig(Path file, Class<T> configClass, T defaults) throws IOException {
        if (!Files.exists(file)) {
            String json = GSON.toJson(defaults);
            Files.write(file, json.getBytes());
            LOGGER.info("Created default config file: {}", file.toAbsolutePath());
            return defaults;
        }

        try {
            String json = Files.readString(file);
            T config = GSON.fromJson(json, configClass);
            return config != null ? config : defaults;
        } catch (IOException e) {
            LOGGER.warn("Failed to read config file {}, using defaults", file.toAbsolutePath(), e);
            return defaults;
        }
    }
}
