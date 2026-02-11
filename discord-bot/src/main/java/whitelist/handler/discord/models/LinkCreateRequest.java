package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

public class LinkCreateRequest extends SocketMessage {
    @SerializedName("requestId")
    private final String requestId;

    @SerializedName("discordId")
    private final String discordId;

    @SerializedName("playerUuid")
    private final String playerUuid;

    @SerializedName("playerName")
    private final String playerName;

    @SerializedName("requestedBy")
    private final String requestedBy;

    @SerializedName("whitelisted")
    private final boolean whitelisted;

    public LinkCreateRequest(String requestId, String discordId, String playerUuid, String playerName, String requestedBy, boolean whitelisted) {
        setType("link_create_request");
        setTimestamp(System.currentTimeMillis());
        this.requestId = requestId;
        this.discordId = discordId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.requestedBy = requestedBy;
        this.whitelisted = whitelisted;
    }

    public String getRequestId() { return requestId; }
    public String getDiscordId() { return discordId; }
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getRequestedBy() { return requestedBy; }
    public boolean isWhitelisted() { return whitelisted; }
}
