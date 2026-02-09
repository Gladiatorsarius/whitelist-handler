package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

/**
 * Message from Discord to Minecraft to add player to whitelist
 */
public class WhitelistAddMessage extends SocketMessage {
    @SerializedName("requestId")
    private final String requestId;

    @SerializedName("playerName")
    private final String playerName;

    @SerializedName("playerUuid")
    private final String playerUuid;

    @SerializedName("discordId")
    private final String discordId;

    @SerializedName("requestedBy")
    private final String requestedBy;

    // Constructor for GSON deserialization
    public WhitelistAddMessage(String requestId, String playerName, String playerUuid, String discordId, String requestedBy) {
        super("whitelist_add");
        this.requestId = requestId;
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.discordId = discordId;
        this.requestedBy = requestedBy;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getRequestedBy() {
        return requestedBy;
    }
}