package combat.log.discord.websocket;

import combat.log.discord.config.BotConfig;
import combat.log.discord.models.SocketMessage;
import combat.log.discord.models.UnlinkMessage;
import combat.log.discord.models.VanillaWhitelistAddMessage;
import combat.log.discord.models.WhitelistConfirmation;
import combat.log.discord.models.WhitelistRemoveConfirmation;
import combat.log.discord.whitelist.WhitelistManager;
import combat.log.discord.models.LinkCreatedMessage;
import combat.log.discord.models.LinkLookupResponse;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * WebSocket server that handles whitelist operations with Minecraft server.
 */
public class WhitelistWebSocketServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistWebSocketServer.class);
    private final Gson gson = new Gson();
    private final BotConfig config;
    private WhitelistManager whitelistManager;
    private WebSocket minecraftConnection;

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

        // Validate Authorization header if configured
        String authHeader = handshake.getFieldValue("Authorization");
        if (config.websocket.authToken != null && !config.websocket.authToken.isBlank()) {
            String expected = "Bearer " + config.websocket.authToken;
            if (!expected.equals(authHeader)) {
                logger.warn("Rejecting connection due to missing/invalid Authorization header from {}", conn.getRemoteSocketAddress());
                conn.close(1008, "Unauthorized");
                return;
            }
        }

        minecraftConnection = conn;

        if (whitelistManager != null) {
            whitelistManager.handleMinecraftConnected();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.warn("Connection closed: {} - {}", code, reason);
        if (conn == minecraftConnection) {
            minecraftConnection = null;
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
        return minecraftConnection != null && minecraftConnection.isOpen();
    }

    public void broadcast(String message) {
        if (minecraftConnection != null && minecraftConnection.isOpen()) {
            minecraftConnection.send(message);
            logger.debug("Broadcasted message to Minecraft");
        } else {
            logger.warn("Cannot broadcast - Minecraft not connected");
        }
    }
}
