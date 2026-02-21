package combat.log.report.socket;

/**
 * Test request sent between mod and bot to exercise create/lookup/unlink flows.
 */
public class TestRequestMessage extends SocketMessage {
    public TestRequestMessage() {
        super("test_request");
    }
}
