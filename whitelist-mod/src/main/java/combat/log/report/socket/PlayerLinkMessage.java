package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

/**
 * Message from Discord to Minecraft to store player link
 */
public class PlayerLinkMessage extends SocketMessage {
    @SerializedName("discordId")
    private final String discordId;

    @SerializedName("playerUuid")
    private final String playerUuid;

    @SerializedName("playerName")
    private final String playerName;

    @SerializedName("whitelisted")
    private final boolean whitelisted;

    // Constructor for GSON deserialization
    public PlayerLinkMessage(String discordId, String playerUuid, String playerName, boolean whitelisted) {
        super("link_player");
        this.discordId = discordId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.whitelisted = whitelisted;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isWhitelisted() {
        return whitelisted;
    }
}