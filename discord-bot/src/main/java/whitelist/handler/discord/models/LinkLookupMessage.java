package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

public class LinkLookupMessage extends SocketMessage {
    @SerializedName("requestId")
    private final String requestId;

    @SerializedName("query")
    private final String query;

    @SerializedName("value")
    private final String value;

    public LinkLookupMessage(String requestId, String query, String value) {
        setType("link_lookup");
        setTimestamp(System.currentTimeMillis());
        this.requestId = requestId;
        this.query = query;
        this.value = value;
    }

    public String getRequestId() { return requestId; }
    public String getQuery() { return query; }
    public String getValue() { return value; }
}
