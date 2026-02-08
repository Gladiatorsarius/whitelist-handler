package combat.log.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Message sent from Discord bot to Minecraft to store player link
 */
public class PlayerLinkMessage extends SocketMessage {
    @SerializedName("discordId")
    private String discordId;
    
    @SerializedName("playerUuid")
    private String playerUuid;
    
    @SerializedName("playerName")
    private String playerName;
    
    @SerializedName("whitelisted")
    private boolean whitelisted;

    public PlayerLinkMessage(String discordId, String playerUuid, String playerName, boolean whitelisted) {
        setType("link_player");
        setTimestamp(System.currentTimeMillis());
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
