package whitelisting.swordsmp.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Discord integration settings loaded from the mod config directory.
 * Automatically creates default config file if it doesn't exist.
 */
public class DiscordConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String token;
    public String guildId;

    /**
     * Load or create Discord config from file.
     * @param configDir Directory where config files are stored
     * @return Loaded config or default config with created file
     * @throws IOException If directory creation fails
     */
    public static DiscordConfig loadOrCreate(Path configDir) throws IOException {
        // Ensure config directory exists
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        Path file = configDir.resolve("whitelisting-via-discord.json");

        // If file doesn't exist, create it with defaults
        if (!Files.exists(file)) {
            DiscordConfig defaults = new DiscordConfig();
            defaults.token = "";
            defaults.guildId = "";

            String json = GSON.toJson(defaults);
            Files.write(file, json.getBytes());
            return defaults;
        }

        // Load existing file
        try (Reader reader = Files.newBufferedReader(file)) {
            DiscordConfig config = GSON.fromJson(reader, DiscordConfig.class);
            return config == null ? new DiscordConfig() : config;
        } catch (IOException e) {
            // On read error, return defaults
            return new DiscordConfig();
        }
    }

    public String getToken() {
        return valueOrBlank(token);
    }

    public String getGuildId() {
        return valueOrBlank(guildId);
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value.trim();
    }
}
