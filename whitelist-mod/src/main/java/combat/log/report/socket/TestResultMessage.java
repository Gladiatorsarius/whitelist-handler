package combat.log.report.socket;

import com.google.gson.annotations.SerializedName;

/**
 * Test result message sent in response to a test request.
 */
public class TestResultMessage extends SocketMessage {
    @SerializedName("success")
    private final boolean success;

    @SerializedName("message")
    private final String message;

    public TestResultMessage(boolean success, String message) {
        super("test_result");
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
