package combat.log.report.socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SocketClientTest {
    private final Path pending = Paths.get("data", "pending-whitelist.log");

    @AfterEach
    public void cleanup() throws IOException {
        if (Files.exists(pending)) Files.delete(pending);
    }

    @Test
    public void testSendQueuesWhenDisconnected() throws Exception {
        SocketClient client = SocketClient.getInstance();
        client.configure("ws://localhost:1/invalid");

        // Send a simple test message via a small anonymous subclass of SocketMessage
        SocketMessage msg = new SocketMessage("test_message") {};
        client.sendMessage(msg);

        // The client is not connected in tests, so it should persist the message
        // Wait briefly for IO
        Thread.sleep(200);
        assertTrue(Files.exists(pending), "pending queue file should exist after send when disconnected");
    }
}
