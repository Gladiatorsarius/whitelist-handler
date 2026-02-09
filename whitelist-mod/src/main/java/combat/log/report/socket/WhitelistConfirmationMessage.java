package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

/**
 * Confirmation message sent from Minecraft back to Discord after whitelist command
 */
public class WhitelistConfirmationMessage extends SocketMessage {
    @SerializedName("requestId")
    private final String requestId;

    @SerializedName("success")
    private final boolean success;

    @SerializedName("playerName")
    private final String playerName;

    @SerializedName("error")
    private final String error;

    public WhitelistConfirmationMessage(String requestId, boolean success, String playerName, String error) {
        super("whitelist_confirmation");
        this.requestId = requestId;
        this.success = success;
        this.playerName = playerName;
        this.error = error;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getError() {
        return error;
    }
}