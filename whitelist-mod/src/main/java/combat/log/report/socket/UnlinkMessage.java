package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

/**
 * Message from Discord to Minecraft to unlink a player and remove from whitelist
 */
public class UnlinkMessage extends SocketMessage {
    @SerializedName("playerUuid")
    private final String playerUuid;

    @SerializedName("playerName")
    private final String playerName;

    @SerializedName("cause")
    private final String cause;

    // Constructor for GSON deserialization
    public UnlinkMessage(String playerUuid, String playerName) {
        super("unlink_player");
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.cause = null;
    }

    public UnlinkMessage(String playerUuid, String playerName, String cause) {
        super("unlink_player");
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.cause = cause;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCause() {
        return cause;
    }
}