package combat.log.report.socket;

/**
 * Minimal placeholder socket client for whitelist-mod. The real mod uses the project's existing socket
 * code; this placeholder allows the new bundle to compile while we integrate the real classes.
 */
public class SocketClient {
    private final String serverUrl;

    public SocketClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void connect() {
        // No-op placeholder. Real implementation uses existing websocket client in the server.
    }

    public void sendMessage(String json) {
        // placeholder
    }
}
