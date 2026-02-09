package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

/**
 * Confirmation message sent from Minecraft after whitelist removal
 */
public class WhitelistRemoveConfirmationMessage extends SocketMessage {
    @SerializedName("success")
    private final boolean success;

    @SerializedName("playerName")
    private final String playerName;

    @SerializedName("error")
    private final String error;

    public WhitelistRemoveConfirmationMessage(boolean success, String playerName, String error) {
        super("whitelist_remove_confirmation");
        this.success = success;
        this.playerName = playerName;
        this.error = error;
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