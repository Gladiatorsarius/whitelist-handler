package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Confirmation message sent from Minecraft after whitelist removal
 */
public class WhitelistRemoveConfirmation extends SocketMessage {
    @SerializedName("success")
    private boolean success;

    @SerializedName("playerName")
    private String playerName;

    @SerializedName("error")
    private String error;

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
