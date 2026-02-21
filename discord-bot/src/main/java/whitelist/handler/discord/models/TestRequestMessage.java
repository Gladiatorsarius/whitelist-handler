package whitelist.handler.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Test request sent from mod to bot to request a connectivity/flow test.
 */
public class TestRequestMessage extends SocketMessage {
    public TestRequestMessage() {
        setType("test_request");
        setTimestamp(System.currentTimeMillis());
    }
}
