package whitelist.handler.discord.websocket;

import whitelist.handler.discord.config.BotConfig;
import whitelist.handler.discord.models.SocketMessage;
import whitelist.handler.discord.models.UnlinkMessage;
import whitelist.handler.discord.models.VanillaWhitelistAddMessage;
import whitelist.handler.discord.models.WhitelistConfirmation;
import whitelist.handler.discord.models.WhitelistRemoveConfirmation;
import whitelist.handler.discord.whitelist.WhitelistManager;
import whitelist.handler.discord.models.LinkCreatedMessage;
import whitelist.handler.discord.models.LinkLookupResponse;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket server that handles whitelist operations with Minecraft server.
 */
public class WhitelistWebSocketServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistWebSocketServer.class);
    private final Gson gson = new Gson();
    private final BotConfig config;
    private WhitelistManager whitelistManager;
    private WebSocket minecraftConnection;

    // No shared-secret authentication: accept connections by default

    public WhitelistWebSocketServer(BotConfig config) {
        super(new InetSocketAddress(config.websocket.host, config.websocket.port));
        this.config = config;
    }

    public void setWhitelistManager(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New connection from: {}", conn.getRemoteSocketAddress());

        // No authorization required for WebSocket connections in this deployment

        synchronized (this) {
            minecraftConnection = conn;
        }

        if (whitelistManager != null) {
            try {
                whitelistManager.handleMinecraftConnected();
            } catch (Exception e) {
                logger.error("Error while handling minecraft connected callback", e);
            }
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.warn("Connection closed: {} - {}", code, reason);
        synchronized (this) {
            if (conn == minecraftConnection) {
                minecraftConnection = null;
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.info("Received message: {}", message);

        try {
            SocketMessage baseMessage = gson.fromJson(message, SocketMessage.class);

            if ("whitelist_confirmation".equals(baseMessage.getType())) {
                WhitelistConfirmation confirmation = gson.fromJson(message, WhitelistConfirmation.class);
                handleWhitelistConfirmation(confirmation);
            } else if ("whitelist_remove_confirmation".equals(baseMessage.getType())) {
                WhitelistRemoveConfirmation confirmation = gson.fromJson(message, WhitelistRemoveConfirmation.class);
                handleWhitelistRemoveConfirmation(confirmation);
            } else if ("unlink_player".equals(baseMessage.getType())) {
                UnlinkMessage unlinkMsg = gson.fromJson(message, UnlinkMessage.class);
                handleUnlink(unlinkMsg);
            } else if ("vanilla_whitelist_add".equals(baseMessage.getType())) {
                VanillaWhitelistAddMessage addMsg = gson.fromJson(message, VanillaWhitelistAddMessage.class);
                handleVanillaWhitelistAdd(addMsg);
            } else if ("link_created".equals(baseMessage.getType())) {
                LinkCreatedMessage created = gson.fromJson(message, LinkCreatedMessage.class);
                if (whitelistManager != null) whitelistManager.handleLinkCreated(created);
            } else if ("link_lookup_response".equals(baseMessage.getType())) {
                LinkLookupResponse resp = gson.fromJson(message, LinkLookupResponse.class);
                if (whitelistManager != null) whitelistManager.handleLinkLookupResponse(resp);
            } else if ("test_request".equals(baseMessage.getType())) {
                // Trigger bot-side test runner
                if (whitelistManager != null) {
                    try {
                        whitelistManager.runRemoteTest(null);
                    } catch (Exception e) {
                        logger.error("Error running remote test", e);
                    }
                }
            } else if ("whitelist_list_response".equals(baseMessage.getType())) {
                // Received full whitelist list from server
                try {
                    whitelist.handler.discord.models.WhitelistListResponse resp = gson.fromJson(message, whitelist.handler.discord.models.WhitelistListResponse.class);
                    if (whitelistManager != null) whitelistManager.handleWhitelistListResponse(resp);
                } catch (Exception e) {
                    logger.error("Failed to handle whitelist_list_response", e);
                }
            } else if ("test_result".equals(baseMessage.getType())) {
                // Received a test result from mod
                try {
                    // Simply log and post to channel via manager
                    com.google.gson.JsonObject obj = gson.fromJson(message, com.google.gson.JsonObject.class);
                    boolean success = obj.has("success") && obj.get("success").getAsBoolean();
                    String msg = obj.has("message") ? obj.get("message").getAsString() : (success ? "Test completed successfully" : "Test failed");
                    if (whitelistManager != null) whitelistManager.postTestResultToLogChannel(success, msg);
                } catch (Exception e) {
                    logger.error("Failed to handle test_result message", e);
                }
            } else {
                logger.warn("Unknown message type: {}", baseMessage.getType());
            }
        } catch (Exception e) {
            logger.error("Failed to parse message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("WebSocket error: {}", ex.getMessage(), ex);
    }

    @Override
    public void onStart() {
        logger.info("WebSocket server started on {}:{}", config.websocket.host, config.websocket.port);
    }

    private void handleUnlink(UnlinkMessage message) {
        logger.info("Processing unlink request for player {} ({})", message.getPlayerName(), message.getPlayerUuid());

        if (whitelistManager != null) {
            whitelistManager.handleMinecraftUnlink(message, null); // Discord ID will be looked up by whitelist manager via WebSocket
        }
    }

    private void handleWhitelistConfirmation(WhitelistConfirmation confirmation) {
        logger.info("Received whitelist confirmation for {} (success={})", confirmation.getPlayerName(), confirmation.isSuccess());

        if (whitelistManager != null) {
            whitelistManager.handleWhitelistConfirmation(confirmation);
        }
    }

    private void handleWhitelistRemoveConfirmation(WhitelistRemoveConfirmation confirmation) {
        logger.info("Received whitelist remove confirmation for {} (success={})", confirmation.getPlayerName(), confirmation.isSuccess());

        if (whitelistManager != null) {
            whitelistManager.handleWhitelistRemoveConfirmation(confirmation);
        }
    }

    private void handleVanillaWhitelistAdd(VanillaWhitelistAddMessage message) {
        logger.info("Received vanilla whitelist add for {} ({})", message.getPlayerName(), message.getPlayerUuid());
        if (whitelistManager != null) {
            whitelistManager.handleVanillaWhitelistAdd(message);
        }
    }

    public boolean isMinecraftConnected() {
        synchronized (this) {
            return minecraftConnection != null && minecraftConnection.isOpen();
        }
    }

    public void broadcast(String message) {
        synchronized (this) {
            if (minecraftConnection != null && minecraftConnection.isOpen()) {
                try {
                    minecraftConnection.send(message);
                    logger.debug("Broadcasted message to Minecraft");
                } catch (Exception e) {
                    logger.error("Failed to send message to Minecraft connection", e);
                }
                return;
            }
        }
        logger.warn("Cannot broadcast - Minecraft not connected");
    }
}
