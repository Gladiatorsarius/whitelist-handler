package combat.log.discord.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BotConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public WebSocketSettings websocket = new WebSocketSettings();
    public DiscordSettings discord = new DiscordSettings();

    public static class WebSocketSettings {
        public int port = 8080;
        public String host = "0.0.0.0";
        public String authToken = "";
    }

    public static class DiscordSettings {
        public String token = "";
        public String guildId = "";
        public String staffRoleId = "";
    }

    public static BotConfig load(File configFile) {
        if (!configFile.exists()) {
            BotConfig c = new BotConfig();
            save(configFile, c);
            return c;
        }
        try (FileReader r = new FileReader(configFile)) {
            return GSON.fromJson(r, BotConfig.class);
        } catch (IOException e) {
            return new BotConfig();
        }
    }

    public static void save(File configFile, BotConfig config) {
        try {
            if (configFile.getParentFile() != null) configFile.getParentFile().mkdirs();
            try (FileWriter w = new FileWriter(configFile)) { GSON.toJson(config, w); }
        } catch (IOException ignored) {}
    }
}
