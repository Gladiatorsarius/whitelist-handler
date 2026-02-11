package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

public class LinkCreatedMessage extends SocketMessage {
    @SerializedName("requestId")
    private final String requestId;

    @SerializedName("discordId")
    private final String discordId;

    @SerializedName("playerUuid")
    private final String playerUuid;

    @SerializedName("playerName")
    private final String playerName;

    public LinkCreatedMessage(String requestId, String discordId, String playerUuid, String playerName) {
        setType("link_created");
        setTimestamp(System.currentTimeMillis());
        this.requestId = requestId;
        this.discordId = discordId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    public String getRequestId() { return requestId; }
    public String getDiscordId() { return discordId; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
}
