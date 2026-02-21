package whitelist.handler.discord.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import whitelist.handler.discord.config.BotConfig;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WhitelistWebSocketIntegrationTest {

    private WhitelistWebSocketServer server;

    @AfterEach
    public void tearDown() throws Exception {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void testAcceptsAndClosesConnection() throws Exception {
        BotConfig cfg = new BotConfig();
        cfg.websocket.port = 0; // let the system pick an available port
        cfg.websocket.host = "127.0.0.1";
        server = new WhitelistWebSocketServer(cfg);
        server.start();

        // Wait for server to start and bind a port
        int attempts = 0;
        while (server.getPort() == 0 && attempts++ < 40) {
            Thread.sleep(50);
        }
        int port = server.getPort();
        CountDownLatch openLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI("ws://127.0.0.1:" + port)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                openLatch.countDown();
            }

            @Override
            public void onMessage(String message) {
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };

        client.connectBlocking(3, TimeUnit.SECONDS);
        boolean opened = openLatch.await(3, TimeUnit.SECONDS);
        Assertions.assertTrue(opened, "Client failed to open");

        // give server a moment to register connection
        Thread.sleep(100);
        Assertions.assertTrue(server.isMinecraftConnected(), "Server did not report connection after open");

        client.close();
        Thread.sleep(100);
        Assertions.assertFalse(server.isMinecraftConnected(), "Server still reports connected after close");

        client.closeBlocking();
    }
}
