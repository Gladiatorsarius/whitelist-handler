package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

public class LinkLookupMessage extends SocketMessage {
    @SerializedName("requestId")
    private final String requestId;

    @SerializedName("query")
    private final String query; // byUuid | byDiscord | byName

    @SerializedName("value")
    private final String value;

    public LinkLookupMessage(String requestId, String query, String value) {
        super("link_lookup");
        this.requestId = requestId;
        this.query = query;
        this.value = value;
    }

    public String getRequestId() { return requestId; }
    public String getQuery() { return query; }
    public String getValue() { return value; }
}