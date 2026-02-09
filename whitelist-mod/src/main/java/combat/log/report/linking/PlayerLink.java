package combat.log.report.linking;

/**
 * Represents a link between Discord ID and Minecraft UUID
 */
public class PlayerLink {
    private final String discordId;
    private final String minecraftUuid;
    private final String minecraftName;
    private final boolean whitelisted;

    public PlayerLink(String discordId, String minecraftUuid, String minecraftName, boolean whitelisted) {
        this.discordId = discordId;
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
        this.whitelisted = whitelisted;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public boolean isWhitelisted() {
        return whitelisted;
    }
}