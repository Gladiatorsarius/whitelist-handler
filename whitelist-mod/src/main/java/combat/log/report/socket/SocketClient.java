package combat.log.report.socket;

import combat.log.report.linking.PlayerLinkingManager;
import combat.log.report.whitelist.WhitelistCommandHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket client for communicating with Discord bot (authoritative server-side implementation)
 */
public class SocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketClient.class);
    private static final SocketClient INSTANCE = new SocketClient();
    private final Gson gson = new Gson();
    private final OkHttpClient httpClient;
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();

    private WebSocket webSocket;
    private String serverUrl = "ws://localhost:8080/combat-log"; // Default, can be configured
    private boolean connected = false;
    private boolean reconnecting = false;
    private WhitelistCommandHandler whitelistHandler;
    private final Path pendingQueuePath = Paths.get("data", "pending-whitelist.log");
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_BACKOFF_SECONDS = 300; // 5 minutes

    private SocketClient() {
        this.httpClient = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();
    }

    public static SocketClient getInstance() {
        return INSTANCE;
    }

    /**
     * Configure the WebSocket server URL
     */
    public void configure(String serverUrl) {
        this.serverUrl = serverUrl;
        LOGGER.info("Configured socket server URL: {}", serverUrl);
    }

    /**
     * Set the auth token for WebSocket authentication
     */


    /**
     * Set the whitelist command handler
     */
    public void setWhitelistHandler(WhitelistCommandHandler handler) {
        this.whitelistHandler = handler;
    }

    /**
     * Connect to the Discord bot WebSocket server
     */
    public void connect() {
        synchronized (this) {
            if (connected || reconnecting) {
                return;
            }
            reconnecting = true;
        }

        Request request = new Request.Builder()
            .url(serverUrl)
            .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                synchronized (SocketClient.this) {
                    connected = true;
                    reconnecting = false;
                    reconnectAttempts.set(0);
                }
                LOGGER.info("Connected to Discord bot WebSocket server");

                // Send in-memory queued messages
                String queuedMessage;
                while ((queuedMessage = messageQueue.poll()) != null) {
                    try {
                        webSocket.send(queuedMessage);
                        LOGGER.info("Sent queued message (in-memory)");
                    } catch (Exception e) {
                        LOGGER.error("Failed to send queued message", e);
                        messageQueue.offer(queuedMessage);
                        break;
                    }
                }

                // Flush persisted queue
                if (Files.exists(pendingQueuePath)) {
                    try {
                        List<String> lines = Files.readAllLines(pendingQueuePath, StandardCharsets.UTF_8);
                        for (String l : lines) {
                            if (l != null && !l.isBlank()) {
                                webSocket.send(l);
                                LOGGER.info("Sent persisted queued message");
                            }
                        }
                        Files.deleteIfExists(pendingQueuePath);
                    } catch (IOException ioe) {
                        LOGGER.error("Failed to flush persisted pending messages", ioe);
                    }
                }
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                handleMessage(text);
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                webSocket.close(1000, null);
                synchronized (SocketClient.this) {
                    connected = false;
                    reconnecting = false;
                }
                LOGGER.warn("WebSocket closing: {} - {}", code, reason);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
                synchronized (SocketClient.this) {
                    connected = false;
                    reconnecting = false;
                }
                LOGGER.error("WebSocket connection failed: {}", t.getMessage());

                // Schedule reconnection with backoff
                scheduleReconnectWithBackoff();
            }
        });
    }

    /**
     * Send a message to the Discord bot (now public for whitelist use)
     */
    public void sendMessage(SocketMessage message) {
        String json = gson.toJson(message);
        sendMessageJson(json);
        LOGGER.debug("Enqueued sendMessage for type={}", message.getType());
    }

    /**
     * Send a JSON message to the Discord bot
     */
    private void sendMessageJson(String message) {
        synchronized (this) {
            if (connected && webSocket != null) {
                try {
                    webSocket.send(message);
                    LOGGER.info("Sent message to Discord bot");
                    return;
                } catch (Exception e) {
                    LOGGER.error("Failed to send message, will queue", e);
                }
            }
        }

        // Persist to disk first
        try {
            Files.createDirectories(pendingQueuePath.getParent());
            Files.write(pendingQueuePath, (message + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.error("Failed to persist pending message, falling back to in-memory queue", e);
            messageQueue.offer(message);
        }

        // Try to connect if not already trying
        synchronized (this) {
            messageQueue.offer(message);
            if (!reconnecting) {
                connect();
            }
        }
    }

    /**
     * Handle incoming message from Discord bot
     */
    private void handleMessage(String text) {
        try {
            JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : null;
            if (type == null || type.isBlank()) {
                LOGGER.warn("Received message without type: {}", text);
                return;
            }

            if ("whitelist_add".equals(type)) {
                WhitelistAddMessage whitelistMsg = gson.fromJson(text, WhitelistAddMessage.class);
                handleWhitelistAdd(whitelistMsg);
            } else if ("link_lookup".equals(type)) {
                LinkLookupMessage lookup = gson.fromJson(text, LinkLookupMessage.class);
                handleLinkLookup(lookup);
            } else if ("link_create_request".equals(type)) {
                LinkCreateRequest req = gson.fromJson(text, LinkCreateRequest.class);
                handleLinkCreateRequest(req);
            } else if ("link_player".equals(type)) {
                PlayerLinkMessage linkMsg = gson.fromJson(text, PlayerLinkMessage.class);
                handlePlayerLink(linkMsg);
            } else if ("unlink_player".equals(type)) {
                UnlinkMessage unlinkMsg = gson.fromJson(text, UnlinkMessage.class);
                handleUnlink(unlinkMsg);
            } else if ("incident_decision".equals(type) || "link_removed".equals(type)) {
                // Stubbed: not migrating incident/punishment logic to whitelist-mod
                LOGGER.info("Received stubbed message type: {} (not handled in whitelist-mod)", type);
            } else {
                LOGGER.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse message from Discord bot: {}", e.getMessage());
        }
    }

    private void handleLinkLookup(LinkLookupMessage lookup) {
        try {
            String q = lookup.getQuery();
            String v = lookup.getValue();
            var linkManager = PlayerLinkingManager.getInstance();

            String discordId = null;
            String uuid = null;
            String name = null;
            boolean whitelisted = false;
            boolean found = false;

            if ("byUuid".equalsIgnoreCase(q)) {
                var linkOpt = linkManager.getLinkByUuid(v);
                if (linkOpt.isPresent()) {
                    var link = linkOpt.get();
                    discordId = link.getDiscordId();
                    uuid = link.getMinecraftUuid();
                    name = link.getMinecraftName();
                    whitelisted = link.isWhitelisted();
                    found = true;
                }
            } else if ("byDiscord".equalsIgnoreCase(q)) {
                var linkOpt = linkManager.getLinkByDiscord(v);
                if (linkOpt.isPresent()) {
                    var link = linkOpt.get();
                    discordId = link.getDiscordId();
                    uuid = link.getMinecraftUuid();
                    name = link.getMinecraftName();
                    whitelisted = link.isWhitelisted();
                    found = true;
                }
            } else if ("byName".equalsIgnoreCase(q)) {
                var linkOpt = linkManager.getLinkByName(v);
                if (linkOpt.isPresent()) {
                    var link = linkOpt.get();
                    discordId = link.getDiscordId();
                    uuid = link.getMinecraftUuid();
                    name = link.getMinecraftName();
                    whitelisted = link.isWhitelisted();
                    found = true;
                }
            }

            LinkLookupResponse resp = new LinkLookupResponse(lookup.getRequestId(), found, discordId, uuid, name, whitelisted);
            sendMessage(resp);
        } catch (Exception e) {
            LOGGER.error("Failed to handle link_lookup: {}", e.getMessage());
        }
    }

    private void handleLinkCreateRequest(LinkCreateRequest req) {
        try {
            PlayerLinkingManager linkManager = PlayerLinkingManager.getInstance();
            linkManager.addLink(req.getDiscordId(), req.getPlayerUuid(), req.getPlayerName(), req.isWhitelisted());

            // If requested to whitelist immediately, delegate to whitelist handler
            if (req.isWhitelisted() && whitelistHandler != null) {
                WhitelistAddMessage msg = new WhitelistAddMessage(req.getRequestId(), req.getPlayerName(), req.getPlayerUuid(), req.getDiscordId(), req.getRequestedBy());
                whitelistHandler.handleWhitelistAdd(msg);
            }

            LinkCreatedMessage created = new LinkCreatedMessage(req.getRequestId(), req.getDiscordId(), req.getPlayerUuid(), req.getPlayerName());
            sendMessage(created);
        } catch (Exception e) {
            LOGGER.error("Failed to handle link_create_request: {}", e.getMessage());
        }
    }

    /**
     * Handle whitelist add command from Discord bot
     */
    private void handleWhitelistAdd(WhitelistAddMessage message) {
        LOGGER.info("Received whitelist add command for: {}", message.getPlayerName());
        if (whitelistHandler != null) {
            whitelistHandler.handleWhitelistAdd(message);
        } else {
            LOGGER.warn("WhitelistCommandHandler not set, ignoring whitelist command");
        }
    }

    /**
     * Handle player link message from Discord bot
     */
    private void handlePlayerLink(PlayerLinkMessage message) {
        LOGGER.info("Received player link: Discord {} <-> Minecraft {} ({})",
            message.getDiscordId(), message.getPlayerName(), message.getPlayerUuid());

        PlayerLinkingManager linkManager = PlayerLinkingManager.getInstance();
        linkManager.addLink(
            message.getDiscordId(),
            message.getPlayerUuid(),
            message.getPlayerName(),
            message.isWhitelisted()
        );
    }

    /**
     * Handle unlink message from Discord bot
     */
    private void handleUnlink(UnlinkMessage message) {
        LOGGER.info("Received unlink command for: {} ({})",
            message.getPlayerName(), message.getPlayerUuid());

        // Remove from linking manager
        PlayerLinkingManager linkManager = PlayerLinkingManager.getInstance();
        linkManager.removeLink(message.getPlayerUuid());

        if (whitelistHandler != null) {
            whitelistHandler.handleWhitelistRemove(message);
        } else {
            LOGGER.warn("WhitelistCommandHandler not set, cannot remove from whitelist");
        }
    }

    /**
     * Schedule reconnection attempt
     */
    private void scheduleReconnect() {
        // Deprecated: kept for compatibility; prefer scheduleReconnectWithBackoff
        scheduleReconnectWithBackoff();
    }

    private void scheduleReconnectWithBackoff() {
        int attempt = reconnectAttempts.incrementAndGet();
        long delay = Math.min((1L << Math.min(attempt, 8)), MAX_RECONNECT_BACKOFF_SECONDS);
        LOGGER.info("Scheduling reconnect attempt #{} in {}s", attempt, delay);
        reconnectScheduler.schedule(() -> {
            synchronized (SocketClient.this) {
                if (connected || reconnecting) return;
                reconnecting = true;
            }
            connect();
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * Disconnect from the server
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Server shutting down");
            connected = false;
        }
    }

    /**
     * Force a disconnect/reconnect cycle to re-establish the WebSocket.
     */
    public synchronized void forceReconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Manual reconnect");
            webSocket = null;
        }

        connected = false;
        reconnecting = false;
        LOGGER.info("Manual reconnect requested; reconnecting to Discord bot...");
        connect();
    }

    public boolean isConnected() {
        return connected;
    }

    public int getQueuedMessageCount() {
        return messageQueue.size();
    }
}
