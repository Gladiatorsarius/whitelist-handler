package combat.log.report.linking;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal server-side player link manager used by the whitelist-mod bundle.
 * Stores links in a JSON file `player-links.json` and exposes simple add/remove/lookup.
 */
public class PlayerLinkingManager {
    private final Path dbFile;
    private final Gson gson = new Gson();
    private Map<String, String> discordToUuid = new HashMap<>();

    public PlayerLinkingManager(Path dbFile) {
        this.dbFile = dbFile;
        load();
    }

    private void load() {
        try {
            if (Files.exists(dbFile)) {
                String json = Files.readString(dbFile);
                Type type = new TypeToken<Map<String,String>>(){}.getType();
                discordToUuid = gson.fromJson(json, type);
                if (discordToUuid == null) discordToUuid = new HashMap<>();
            }
        } catch (IOException e) {
            discordToUuid = new HashMap<>();
        }
    }

    private void save() {
        try {
            String json = gson.toJson(discordToUuid);
            Files.createDirectories(dbFile.getParent());
            Files.writeString(dbFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    public synchronized void addLink(String discordId, String playerUuid) {
        discordToUuid.put(discordId, playerUuid);
        save();
    }

    public synchronized void removeLink(String discordId) {
        discordToUuid.remove(discordId);
        save();
    }

    public synchronized Optional<String> getMinecraftUuid(String discordId) {
        return Optional.ofNullable(discordToUuid.get(discordId));
    }
}
