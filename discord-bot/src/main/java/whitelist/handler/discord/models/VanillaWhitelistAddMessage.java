package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Message from Minecraft when vanilla /whitelist add is used.
 */
public class VanillaWhitelistAddMessage extends SocketMessage {
    @SerializedName("playerName")
    private String playerName;

    @SerializedName("playerUuid")
    private String playerUuid;

    @SerializedName("sourceName")
    private String sourceName;

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getSourceName() {
        return sourceName;
    }
}
