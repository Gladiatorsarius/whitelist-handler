package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

/**
 * Base message for socket communication
 */
public abstract class SocketMessage {
    @SerializedName("type")
    private final String type;

    @SerializedName("timestamp")
    private final long timestamp;

    protected SocketMessage(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}