package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Confirmation message sent from Minecraft back to Discord after whitelist command
 */
public class WhitelistConfirmation extends SocketMessage {
    @SerializedName("requestId")
    private String requestId;
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("playerName")
    private String playerName;
    
    @SerializedName("error")
    private String error;

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
