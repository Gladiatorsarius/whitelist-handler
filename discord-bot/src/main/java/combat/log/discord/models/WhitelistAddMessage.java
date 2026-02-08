package combat.log.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Message sent from Discord bot to Minecraft to add a player to whitelist
 */
public class WhitelistAddMessage extends SocketMessage {
    @SerializedName("requestId")
    private String requestId;
    
    @SerializedName("playerName")
    private String playerName;
    
    @SerializedName("playerUuid")
    private String playerUuid;
    
    @SerializedName("discordId")
    private String discordId;
    
    @SerializedName("requestedBy")
    private String requestedBy;

    public WhitelistAddMessage(String requestId, String playerName, String playerUuid, String discordId, String requestedBy) {
        setType("whitelist_add");
        setTimestamp(System.currentTimeMillis());
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
