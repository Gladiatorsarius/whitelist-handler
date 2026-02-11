package whitelist.handler.discord.api;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a Mojang player profile
 */
public class MojangProfile {
    @SerializedName("id")
    private String uuid;
    
    @SerializedName("name")
    private String name;

    public MojangProfile(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    /**
     * Convert raw UUID (without dashes) to standard UUID format
     */
    public String getFormattedUuid() {
        if (uuid == null || uuid.length() != 32) {
            return uuid;
        }
        return String.format("%s-%s-%s-%s-%s",
            uuid.substring(0, 8),
            uuid.substring(8, 12),
            uuid.substring(12, 16),
            uuid.substring(16, 20),
            uuid.substring(20, 32)
        );
    }
}
