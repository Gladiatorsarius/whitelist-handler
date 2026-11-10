package whitelisting.swordsmp.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Looks up Minecraft profiles via the Mojang API.
 * Results (including misses) are cached briefly to avoid rate limiting.
 */
public class MojangAPIService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MojangAPIService.class);
    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final int CACHE_MAX = 256;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > CACHE_MAX;
        }
    };

    public boolean isValidUsernameFormat(String name) {
        if (name == null) return false;
        return name.matches("[A-Za-z0-9_]{3,16}");
    }

    /**
     * Resolve a Minecraft username to its Mojang profile.
     * Returns empty when the name does not exist or the lookup fails (log a warning on failure).
     */
    public synchronized Optional<MojangProfile> getProfile(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String key = name.toLowerCase();
        CacheEntry cached = cache.get(key);
        if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_TTL_MS) {
            return cached.profile;
        }

        Optional<MojangProfile> result = fetchProfile(name);
        cache.put(key, new CacheEntry(result, System.currentTimeMillis()));
        return result;
    }

    private Optional<MojangProfile> fetchProfile(String name) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL + name))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404 || response.statusCode() == 204) {
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                LOGGER.warn("Mojang API returned status {} looking up {}", response.statusCode(), name);
                return Optional.empty();
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String id = json.get("id").getAsString();
            String resolvedName = json.get("name").getAsString();
            return Optional.of(new MojangProfile(id, resolvedName));
        } catch (Exception e) {
            LOGGER.warn("Failed to look up Minecraft profile for {}: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private static class CacheEntry {
        final Optional<MojangProfile> profile;
        final long fetchedAt;

        CacheEntry(Optional<MojangProfile> profile, long fetchedAt) {
            this.profile = profile;
            this.fetchedAt = fetchedAt;
        }
    }
}

class MojangProfile {
    private final String id;
    private final String name;

    MojangProfile(String id, String name) { this.id = id; this.name = name; }

    /** UUID with dashes, formatted from the Mojang-provided undashed id. */
    public String getFormattedUuid() {
        if (id != null && id.length() == 32 && !id.contains("-")) {
            return id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                    + "-" + id.substring(16, 20) + "-" + id.substring(20);
        }
        return id;
    }

    public String getName() { return name; }
}
