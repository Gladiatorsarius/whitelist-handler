package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

public class TestResultMessage extends SocketMessage {
    @SerializedName("success")
    private final boolean success;

    @SerializedName("message")
    private final String message;

    public TestResultMessage(boolean success, String message) {
        setType("test_result");
        setTimestamp(System.currentTimeMillis());
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
