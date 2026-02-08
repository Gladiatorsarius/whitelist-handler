package combat.log.discord.models;

import com.google.gson.annotations.SerializedName;

public class LinkLookupResponse extends SocketMessage {
    @SerializedName("requestId")
    private final String requestId;

    @SerializedName("found")
    private final boolean found;

    @SerializedName("discordId")
    private final String discordId;

    @SerializedName("minecraftUuid")
    private final String minecraftUuid;

    @SerializedName("minecraftName")
    private final String minecraftName;

    @SerializedName("whitelisted")
    private final boolean whitelisted;

    public LinkLookupResponse(String requestId, boolean found, String discordId, String minecraftUuid, String minecraftName, boolean whitelisted) {
        setType("link_lookup_response");
        setTimestamp(System.currentTimeMillis());
        this.requestId = requestId;
        this.found = found;
        this.discordId = discordId;
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
        this.whitelisted = whitelisted;
    }

    public String getRequestId() { return requestId; }
    public boolean isFound() { return found; }
    public String getDiscordId() { return discordId; }
    public String getMinecraftUuid() { return minecraftUuid; }
    public String getMinecraftName() { return minecraftName; }
    public boolean isWhitelisted() { return whitelisted; }
}
