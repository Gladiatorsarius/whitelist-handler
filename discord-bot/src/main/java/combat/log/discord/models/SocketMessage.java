package combat.log.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Base message for WebSocket communication
 */
public class SocketMessage {
    @SerializedName("type")
    private String type;
    
    @SerializedName("timestamp")
    private long timestamp;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
