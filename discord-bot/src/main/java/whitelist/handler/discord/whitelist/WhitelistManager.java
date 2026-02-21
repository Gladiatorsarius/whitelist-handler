package whitelist.handler.discord.whitelist;

import whitelist.handler.discord.api.MojangAPIService;
import whitelist.handler.discord.api.MojangProfile;
import whitelist.handler.discord.config.BotConfig;
import whitelist.handler.discord.models.PlayerLinkMessage;
import whitelist.handler.discord.models.UnlinkMessage;
import whitelist.handler.discord.models.VanillaWhitelistAddMessage;
import whitelist.handler.discord.models.WhitelistAddMessage;
import whitelist.handler.discord.models.LinkCreateRequest;
import whitelist.handler.discord.models.LinkCreatedMessage;
import whitelist.handler.discord.models.LinkLookupMessage;
import whitelist.handler.discord.models.LinkLookupResponse;
import whitelist.handler.discord.models.WhitelistConfirmation;
import whitelist.handler.discord.models.WhitelistRemoveConfirmation;
import whitelist.handler.discord.websocket.WhitelistWebSocketServer;
import whitelist.handler.discord.util.MessageFormatter;
import com.google.gson.Gson;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages whitelist requests and approvals
 */
public class WhitelistManager {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistManager.class);
    
    private final JDA jda;
    private final BotConfig config;
    private final MojangAPIService mojangAPI;
    private WhitelistWebSocketServer webSocketServer;
    private final File configFile;
    private final Gson gson = new Gson();
    private final ConcurrentMap<String, CompletableFuture<LinkLookupResponse>> pendingLinkLookups = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<LinkCreatedMessage>> pendingCreateFutures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PendingWhitelistRequest> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PendingUnlinkRequest> pendingUnlinkRequests = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> pendingWhitelistMessages = new ConcurrentLinkedQueue<>();
    private volatile String whitelistStatusMessageId;
    private static final String WHITELIST_STATUS_TITLE = "📜 Whitelisted Players";
    private final ScheduledExecutorService lookupScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Path pendingQueuePath = Paths.get("data", "pending-whitelist.log");
    private static final Pattern DISCORD_ID_PATTERN = Pattern.compile("(\\d{6,})");

    public WhitelistManager(JDA jda, BotConfig config, MojangAPIService mojangAPI, File configFile) {
        this.jda = jda;
        this.config = config;
        this.mojangAPI = mojangAPI;
        this.configFile = configFile;
    }

    // Backwards-compatible constructor used by older tests/code that passed three args
    public WhitelistManager(JDA jda, BotConfig config, MojangAPIService mojangAPI) {
        this(jda, config, mojangAPI, null);
    }

    public void setWebSocketServer(WhitelistWebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }

    private CompletableFuture<LinkLookupResponse> lookupLink(String query, String value) {
        String requestId = UUID.randomUUID().toString();
        LinkLookupMessage msg = new LinkLookupMessage(requestId, query, value);
        CompletableFuture<LinkLookupResponse> future = new CompletableFuture<>();
        pendingLinkLookups.put(requestId, future);

        if (webSocketServer != null) {
            try {
                webSocketServer.broadcast(gson.toJson(msg));
            } catch (Exception e) {
                pendingLinkLookups.remove(requestId);
                future.completeExceptionally(e);
                return future;
            }

            // Schedule a timeout to remove the future if no response arrives
            lookupScheduler.schedule(() -> {
                CompletableFuture<LinkLookupResponse> removed = pendingLinkLookups.remove(requestId);
                if (removed != null && !removed.isDone()) {
                    removed.completeExceptionally(new RuntimeException("Link lookup timed out"));
                }
            }, 6, TimeUnit.SECONDS);
        } else {
            // Clean up mapping to avoid leaks
            pendingLinkLookups.remove(requestId);
            future.completeExceptionally(new RuntimeException("WebSocket not connected"));
        }

        return future;
    }

    public String message(String key, String fallback) {
        return config.message(key, fallback);
    }

    public void handleMinecraftConnected() {
        flushPendingWhitelistMessages();
    }

    public boolean hasStaffPermission(Member member) {
        if (member == null) {
            return false;
        }
        if (member.hasPermission(Permission.MANAGE_SERVER)) {
            return true;
        }
        String staffRoleId = config.discord.staffRoleId;
        if (staffRoleId == null || staffRoleId.isBlank()) {
            return false;
        }
        return member.getRoles().stream().anyMatch(role -> staffRoleId.equals(role.getId()));
    }

    private EmbedBuilder buildWhitelistStatusEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(WHITELIST_STATUS_TITLE);
        embed.setColor(Color.BLUE);
        embed.setDescription("Whitelist information is managed server-side.\nUse `/whitelist check` to verify individual players.");
        embed.setFooter("Server authoritative - All whitelist data stored on Minecraft server");
        embed.setTimestamp(Instant.now());
        return embed;
    }

    /**
     * Immediately post the current whitelist list to the configured log channel.
     * Preference: read from local DB at data/linking.db if present, otherwise request via WebSocket.
     */
    public void postWhitelistListNow() {
        TextChannel ch = resolveWhitelistLogChannel();
        if (ch == null || !ch.canTalk()) {
            logger.warn("Cannot post whitelist list - channel not configured or bot cannot talk");
            return;
        }

        // Try local DB first (direct JDBC query to avoid compile-time dependency)
        java.io.File dbFile = new java.io.File("data" + java.io.File.separator + "linking.db");
        if (dbFile.exists()) {
            String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            String sql = "SELECT minecraft_name, discord_id FROM whitelist_links WHERE whitelisted = 1 ORDER BY linked_at";
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(jdbcUrl);
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                 java.sql.ResultSet rs = stmt.executeQuery()) {

                StringBuilder sb = new StringBuilder();
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    String minecraftName = rs.getString("minecraft_name");
                    String discordId = rs.getString("discord_id");
                    String discordPart = (discordId != null && !discordId.isBlank()) ? "<@" + discordId + ">" : "(no discord)";
                    sb.append("• ").append(minecraftName).append(" — ").append(discordPart).append('\n');
                }

                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle(WHITELIST_STATUS_TITLE)
                    .setColor(java.awt.Color.BLUE)
                    .setTimestamp(Instant.now());
                if (!any) {
                    embed.setDescription("No whitelisted entries found.");
                } else {
                    embed.setDescription(sb.toString());
                }
                ch.sendMessageEmbeds(embed.build()).queue();
                return;
            } catch (Exception ex) {
                logger.error("Failed to read local linking DB via JDBC", ex);
            }
        }

        // If no DB, try asking Minecraft via websocket
        if (webSocketServer != null && webSocketServer.isMinecraftConnected()) {
            try {
                // send a simple request message
                whitelist.handler.discord.models.SocketMessage req = new whitelist.handler.discord.models.SocketMessage();
                req.setType("request_whitelist_list");
                webSocketServer.broadcast(new Gson().toJson(req));
                ch.sendMessage("Requested whitelist from server; will post the list when received.").queue();
                return;
            } catch (Exception ex) {
                logger.error("Failed to request whitelist list via websocket", ex);
            }
        }

        // Nothing available — still post an informative embed so channel shows a status
        EmbedBuilder noEmbed = new EmbedBuilder()
            .setTitle(WHITELIST_STATUS_TITLE)
            .setDescription("No local database available and the Minecraft server is not connected; cannot retrieve whitelist.")
            .setColor(java.awt.Color.GRAY)
            .setTimestamp(Instant.now());
        ch.sendMessageEmbeds(noEmbed.build()).queue();
    }

    /**
     * Handle a whitelist list response arriving from the Minecraft server via websocket.
     */
    public void handleWhitelistListResponse(whitelist.handler.discord.models.WhitelistListResponse resp) {
        TextChannel ch = resolveWhitelistLogChannel();
        if (ch == null || !ch.canTalk()) return;

        if (resp == null || resp.getEntries() == null || resp.getEntries().isEmpty()) {
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(WHITELIST_STATUS_TITLE)
                .setDescription("No whitelisted entries returned from server.")
                .setColor(java.awt.Color.GRAY)
                .setTimestamp(Instant.now());
            ch.sendMessageEmbeds(embed.build()).queue();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (whitelist.handler.discord.models.WhitelistListResponse.Entry e : resp.getEntries()) {
            String discordPart = (e.getDiscordId() != null && !e.getDiscordId().isBlank()) ? "<@" + e.getDiscordId() + ">" : "(no discord)";
            sb.append("• ").append(e.getMinecraftName()).append(" — ").append(discordPart).append('\n');
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(WHITELIST_STATUS_TITLE)
            .setDescription(sb.toString())
            .setColor(java.awt.Color.BLUE)
            .setTimestamp(Instant.now());

        ch.sendMessageEmbeds(embed.build()).queue();
    }

    /**
     * Process a new whitelist request
     */
    public WhitelistResult processRequest(User user, String minecraftName, InteractionHook hook) {
        logger.info("Processing whitelist request from {} for Minecraft name: {}", 
            user.getAsTag(), minecraftName);

        try {
            // Validate username format
            if (!mojangAPI.isValidUsernameFormat(minecraftName)) {
                return WhitelistResult.error(config.message("whitelist.modal.requestInvalidFormat",
                    "❌ Invalid Minecraft username format. Username must be 3-16 characters long and contain only letters, numbers, and underscores."));
            }

            // Check if Discord user is already linked
            try {
                LinkLookupResponse resp = lookupLink("discord_id", user.getId()).get(5, TimeUnit.SECONDS);
                if (resp.isFound()) {
                    return WhitelistResult.error(config.message("whitelist.modal.requestAlreadyLinked",
                        "❌ Your Discord account is already linked to a Minecraft account."));
                }
            } catch (Exception e) {
                logger.warn("Failed to lookup link for Discord ID {}: {}", user.getId(), e.getMessage());
                // Assume not linked if lookup fails
            }

            // Query Mojang API for UUID
            Optional<MojangProfile> profileOpt = mojangAPI.getProfile(minecraftName);
            if (profileOpt.isEmpty()) {
                return WhitelistResult.error(config.message("whitelist.modal.requestNameNotFound",
                    "❌ Minecraft username not found. Please check the spelling and try again."));
            }

            MojangProfile profile = profileOpt.get();
            String minecraftUuid = profile.getFormattedUuid();

            // Check if Minecraft account is already linked
            try {
                LinkLookupResponse resp = lookupLink("minecraft_uuid", minecraftUuid).get(5, TimeUnit.SECONDS);
                if (resp.isFound()) {
                    return WhitelistResult.error(config.message("whitelist.modal.requestMinecraftLinked",
                        "❌ This Minecraft account is already linked to another Discord account."));
                }
            } catch (Exception e) {
                logger.warn("Failed to lookup link for Minecraft UUID {}: {}", minecraftUuid, e.getMessage());
                // Assume not linked if lookup fails
            }

            // Automatically approve and whitelist the player
                // Request server to create the authoritative link (server is authoritative)
                // Generate a single requestId for both the pending request and the server create request
                String requestId = UUID.randomUUID().toString();
                try {
                    LinkCreateRequest createReq = new LinkCreateRequest(
                        requestId,
                        user.getId(),
                        minecraftUuid,
                        profile.getName(),
                        "AUTO_APPROVED",
                        true
                    );
                    String createJson = new Gson().toJson(createReq);
                    sendOrQueueWhitelistMessage(createJson);
                    logger.info("Requested server to create link: Discord {} <-> Minecraft {} ({})", user.getId(), profile.getName(), minecraftUuid);
                } catch (Exception e) {
                    logger.error("Failed to request link creation", e);
                    return WhitelistResult.error(config.message("whitelist.modal.linkError",
                        "❌ An error occurred while linking your account. Please try again later."));
                }

            // Send whitelist command to Minecraft
            PendingWhitelistRequest pending = new PendingWhitelistRequest(
                user.getId(),
                user.getAsTag(),
                profile.getName(),
                minecraftUuid,
                System.currentTimeMillis(),
                hook
            );
            pendingRequests.put(requestId, pending);

            WhitelistAddMessage whitelistMsg = new WhitelistAddMessage(
                requestId,
                profile.getName(),
                minecraftUuid,
                user.getId(),
                "AUTO_APPROVED"
            );
            String json = new Gson().toJson(whitelistMsg);
            sendOrQueueWhitelistMessage(json);
            logger.info("Sent whitelist command to Minecraft for: {}", profile.getName());

            // Also send link message
            PlayerLinkMessage linkMsg = new PlayerLinkMessage(
                user.getId(),
                minecraftUuid,
                profile.getName(),
                true
            );
            json = new Gson().toJson(linkMsg);
            sendOrQueueWhitelistMessage(json);

            return WhitelistResult.success(config.message("whitelist.modal.waiting",
                "⏳ Waiting for Minecraft confirmation..."));
        } catch (Exception e) {
            logger.error("Failed to process whitelist request", e);
            return WhitelistResult.error(config.message("whitelist.modal.processError",
                "❌ An error occurred while processing your request. Please try again later."));
        }
    }

    /**
     * Handle whitelist confirmation from Minecraft
     */
    public void handleWhitelistConfirmation(WhitelistConfirmation confirmation) {
        PendingWhitelistRequest pending = pendingRequests.remove(confirmation.getRequestId());
        String playerName = confirmation.getPlayerName();

        if (pending == null) {
            logger.warn("No pending request found for confirmation: {}", confirmation.getRequestId());
            return;
        }

        updateWhitelistRequestLog(pending, confirmation.isSuccess(), confirmation.getError());

        String message = confirmation.isSuccess()
            ? MessageFormatter.format(
                config.message("whitelist.confirm.success", "✅ Whitelist successful for **{playerName}**."),
                Map.of("playerName", playerName)
            )
            : MessageFormatter.format(
                config.message("whitelist.confirm.fail", "❌ Whitelist failed for **{playerName}**: {error}"),
                Map.of(
                    "playerName", playerName,
                    "error", confirmation.getError() != null ? confirmation.getError() : "Unknown error"
                )
            );

        if (pending.hook != null) {
            pending.hook.editOriginal(message).queue();
        }

        if (confirmation.isSuccess()) {
            updateWhitelistStatusMessage();
        }
    }

    /**
     * Handle confirmation from server that a link was created.
     */
    public void handleLinkCreated(LinkCreatedMessage created) {
        String requestId = created.getRequestId();
        // If a test create future is waiting for this requestId, complete it first
        CompletableFuture<LinkCreatedMessage> createFuture = pendingCreateFutures.remove(requestId);
        if (createFuture != null) {
            createFuture.complete(created);
        }
        PendingWhitelistRequest pending = pendingRequests.remove(requestId);

        // Link created on server - no local caching needed since server is authoritative
        logger.info("Link created on server: {} <-> {}", created.getDiscordId(), created.getPlayerUuid());

        if (pending != null) {
            // Notify original requester if a hook exists
            if (pending.hook != null) {
                String message = MessageFormatter.format(
                    config.message("whitelist.confirm.success", "✅ Whitelist successful for **{playerName}**."),
                    Map.of("playerName", created.getPlayerName())
                );
                pending.hook.editOriginal(message).queue();
            }
            updateWhitelistStatusMessage();
        }
    }

    /**
     * Run a remote end-to-end test: request create -> lookup -> unlink and post result to log channel.
     * Runs asynchronously on the lookupScheduler to avoid blocking event threads.
     */
    public void runRemoteTest(Member requestedBy) {
        lookupScheduler.execute(() -> {
            String requestId = UUID.randomUUID().toString();
            try {
                // Create test identifiers
                String testDiscordId = "TEST_DISCORD_" + UUID.randomUUID().toString().substring(0,8);
                String testUuid = UUID.randomUUID().toString();
                String testName = "testplayer_" + UUID.randomUUID().toString().substring(0,6);

                LinkCreateRequest createReq = new LinkCreateRequest(requestId, testDiscordId, testUuid, testName, "BOT_TEST", true);
                String json = gson.toJson(createReq);

                CompletableFuture<LinkCreatedMessage> createFuture = new CompletableFuture<>();
                pendingCreateFutures.put(requestId, createFuture);

                if (webSocketServer == null) {
                    postTestResultToLogChannel(false, "WebSocket server not available");
                    pendingCreateFutures.remove(requestId);
                    return;
                }

                webSocketServer.broadcast(json);

                LinkCreatedMessage created = null;
                try {
                    created = createFuture.get(8, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    postTestResultToLogChannel(false, "Timed out waiting for LinkCreated: " + ex.getMessage());
                    pendingCreateFutures.remove(requestId);
                    return;
                }

                // Lookup the created link by UUID
                try {
                    LinkLookupResponse lookupResp = lookupLink("minecraft_uuid", created.getPlayerUuid()).get(6, TimeUnit.SECONDS);
                    if (!lookupResp.isFound()) {
                        postTestResultToLogChannel(false, "Lookup did not find the created link");
                        return;
                    }
                } catch (Exception ex) {
                    postTestResultToLogChannel(false, "Lookup failed: " + ex.getMessage());
                    return;
                }

                // Send unlink to remove test link
                try {
                    UnlinkMessage unlink = new UnlinkMessage(created.getPlayerUuid(), created.getPlayerName(), "test");
                    webSocketServer.broadcast(gson.toJson(unlink));
                } catch (Exception ex) {
                    postTestResultToLogChannel(false, "Failed to send unlink: " + ex.getMessage());
                    return;
                }

                postTestResultToLogChannel(true, "Test completed successfully (created, lookup OK, removed)");
            } catch (Exception e) {
                logger.error("Unexpected error during runRemoteTest", e);
                postTestResultToLogChannel(false, "Unexpected error: " + e.getMessage());
            } finally {
                pendingCreateFutures.remove(requestId);
            }
        });
    }

    public void postTestResultToLogChannel(boolean success, String message) {
        TextChannel ch = resolveWhitelistLogChannel();
        if (ch != null) {
            String out = (success ? "✅ " : "❌ ") + message;
            ch.sendMessage(out).queue(null, err -> logger.error("Failed to post test result to log channel", err));
        } else {
            logger.info("Test result: {} - {}", success, message);
        }
    }

    public void handleLinkLookupResponse(LinkLookupResponse resp) {
        CompletableFuture<LinkLookupResponse> future = pendingLinkLookups.remove(resp.getRequestId());
        if (future != null) {
            future.complete(resp);
        }
        if (resp.isFound()) {
            // Link found - no local caching needed since server is authoritative
        }
    }

    public void handleVanillaWhitelistAdd(VanillaWhitelistAddMessage message) {
        // Handle vanilla whitelist add - currently no-op for whitelist-handler
        logger.info("Received vanilla whitelist add for {} ({})", message.getPlayerName(), message.getPlayerUuid());
    }

    /**
     * Create a review thread for staff
     */
    private void createReviewThread(WhitelistRequest request) {
        try {
            Guild guild = jda.getGuildById(config.discord.guildId);
            if (guild == null) {
                logger.error("Guild not found: {}", config.discord.guildId);
                return;
            }

            if (!config.features.whitelistEnabled || config.channels.reviewChannelId == null) {
                logger.warn("Whitelist not configured properly");
                return;
            }

            // Get review channel
            TextChannel reviewChannel = guild.getTextChannelById(config.channels.reviewChannelId);
            if (reviewChannel == null) {
                logger.error("Review channel not found: {}", config.channels.reviewChannelId);
                return;
            }

            // Create embed for review
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎫 New Whitelist Request")
                .setColor(Color.YELLOW)
                .addField("Discord User", "<@" + request.getDiscordId() + "> (" + request.getDiscordUsername() + ")", false)
                .addField("Minecraft Username", request.getMinecraftName(), true)
                .addField("Minecraft UUID", "`" + request.getMinecraftUuid() + "`", true)
                .addField("Request ID", "`" + request.getRequestId() + "`", false)
                .setTimestamp(Instant.ofEpochMilli(request.getRequestedAt()))
                .setFooter("Whitelist Request System");

            // Create buttons
            Button approveButton = Button.success(
                "whitelist_approve:" + request.getRequestId(),
                resolveLabel(config.buttons.whitelist.approve, "✅ Approve")
            );
            Button denyButton = Button.danger(
                "whitelist_deny:" + request.getRequestId(),
                resolveLabel(config.buttons.whitelist.deny, "❌ Deny")
            );

            // Create thread (non-blocking)
            String threadName = "Whitelist: " + request.getMinecraftName();
            reviewChannel.createThreadChannel(threadName).queue(thread -> {
                // Post message with buttons
                thread.sendMessageEmbeds(embed.build())
                    .setActionRow(approveButton, denyButton)
                    .queue(sent -> {
                        // Tag staff role if configured
                        if (config.discord.staffRoleId != null) {
                            thread.sendMessage("<@&" + config.discord.staffRoleId + ">").queue();
                        }

                        // Store request in pending requests map
                        request.setThreadId(thread.getId());
                        logger.info("Created review thread for request: {}", request.getRequestId());
                    }, error -> logger.error("Failed to post review message in thread {}", thread.getId(), error));
            }, error -> logger.error("Failed to create review thread in channel {}", reviewChannel.getId(), error));
        } catch (Exception e) {
            logger.error("Failed to create review thread", e);
        }
    }

    /**
     * Approve a whitelist request
     */
    public void approveRequest(String requestId, String staffId, String staffName) {
        logger.info("Approving whitelist request: {} by {}", requestId, staffName);

        // Get request from pending requests map
        PendingWhitelistRequest request = pendingRequests.get(requestId);
        if (request == null) {
            logger.error("Request not found in pending requests: {}", requestId);
            return;
        }

        // Request server to create the authoritative link
        String createRequestId = UUID.randomUUID().toString();
        LinkCreateRequest createReq = new LinkCreateRequest(
            createRequestId,
            request.discordId,
            request.minecraftUuid,
            request.minecraftName,
            staffId,
            true
        );
        String createJson = new Gson().toJson(createReq);
        sendOrQueueWhitelistMessage(createJson);

        // Remove from pending requests (approved)
        pendingRequests.remove(requestId);

        // Send whitelist command to Minecraft
        if (webSocketServer != null && webSocketServer.isMinecraftConnected()) {
            WhitelistAddMessage whitelistMsg = new WhitelistAddMessage(
                requestId,
                request.minecraftName,
                request.minecraftUuid,
                request.discordId,
                staffId
            );
            String json = new Gson().toJson(whitelistMsg);
            webSocketServer.broadcast(json);
                logger.info("Sent whitelist command to Minecraft for: {}", request.minecraftName);

                // Link creation request already sent to server; no local authoritative write.
            } else {
                logger.warn("Cannot send whitelist command - Minecraft not connected");
            }
    }

    /**
     * Deny a whitelist request
     */
    public void denyRequest(String requestId, String staffId, String staffName, String reason) {
        logger.info("Denying whitelist request: {} by {} - Reason: {}", requestId, staffName, reason);

        // Get request from pending requests map
        PendingWhitelistRequest request = pendingRequests.get(requestId);
        if (request == null) {
            logger.error("Request not found in pending requests: {}", requestId);
            return;
        }

        // Remove from pending requests (denied)
        pendingRequests.remove(requestId);

        logger.info("Request {} denied by {}", requestId, staffName);
    }

    /**
     * Update review thread with decision
     */
    private void updateReviewThread(String threadId, boolean approved, String staffName) {
        try {
            ThreadChannel thread = jda.getThreadChannelById(threadId);
            if (thread == null) {
                logger.warn("Thread not found: {}", threadId);
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                .setTimestamp(Instant.now());

            if (approved) {
                embed.setTitle("✅ Request Approved")
                    .setColor(Color.GREEN)
                    .setDescription("This whitelist request has been approved by " + staffName)
                    .addField("Status", "Player whitelisted and link stored", false);
            } else {
                embed.setTitle("❌ Request Denied")
                    .setColor(Color.RED)
                    .setDescription("This whitelist request has been denied by " + staffName);
            }

            thread.sendMessageEmbeds(embed.build()).queue(msg -> {
                // Archive thread after posting
                thread.getManager().setArchived(true).queue();
            });

        } catch (Exception e) {
            logger.error("Failed to update review thread", e);
        }
    }

    public static class WhitelistResult {
        public final boolean success;
        public final String message;

        private WhitelistResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static WhitelistResult success(String message) {
            return new WhitelistResult(true, message);
        }

        public static WhitelistResult error(String message) {
            return new WhitelistResult(false, message);
        }
    }

    /**
     * Setup whitelist channel with button
     */
    public boolean setupWhitelistChannel(String channelId) {
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Channel not found: {}", channelId);
                return false;
            }

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle(config.whitelist.buttonMessage.title)
                .setDescription(config.whitelist.buttonMessage.description)
                .setColor(Color.decode(config.whitelist.buttonMessage.color))
                .setFooter("Click the button below to request whitelist access");

            Button requestButton = Button.primary(
                "whitelist_request",
                resolveLabel(config.buttons.whitelist.request, "🎫 Request Whitelist")
            );

            Button unlinkButton = Button.danger(
                "whitelist_unlink",
                resolveLabel(config.buttons.whitelist.unlink, "🔓 Unlink")
            );

            if (!channel.canTalk()) {
                logger.warn("Bot cannot send messages to channel {} (missing permission)", channelId);
                return false;
            }

            channel.sendMessageEmbeds(embed.build())
                .setActionRow(requestButton, unlinkButton)
                .queue();

            logger.info("Setup whitelist channel: {}", channelId);
            return true;

        } catch (Exception e) {
            logger.error("Failed to setup whitelist channel", e);
            return false;
        }
    }

    /**
     * Setup the whitelist log channel (where whitelist events are posted).
     * Persists the choice to the config file.
     * @return true if successful
     */
    public boolean setupWhitelistLogChannel(String channelId) {
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                logger.error("Log channel not found: {}", channelId);
                return false;
            }

            // Save into runtime config and persist
            config.channels.whitelistLogChannelId = channelId;
            if (configFile != null) {
                BotConfig.save(configFile, config);
            }

            // Notify in the channel
            try {
                channel.sendMessage("This channel has been configured as the whitelist log channel.").queue();
            } catch (Exception ignored) {}

            logger.info("Configured whitelist log channel: {}", channelId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to setup whitelist log channel", e);
            return false;
        }
    }

    private String resolveLabel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    /**
     * Unlink the Discord user from their Minecraft account
     */
    public WhitelistResult unlinkDiscord(User user) {
        try {
            // Check if Discord user is linked by querying server
            LinkLookupResponse resp = lookupLink("discord_id", user.getId()).get(5, TimeUnit.SECONDS);
            if (!resp.isFound()) {
                return WhitelistResult.success(config.message("whitelist.unlink.already",
                    "✅ Your Discord account is already unlinked."));
            }

            String minecraftUuid = resp.getMinecraftUuid();
            String minecraftName = resp.getMinecraftName();

            // Send unlink request to server
            if (webSocketServer != null && webSocketServer.isMinecraftConnected()) {
                UnlinkMessage unlinkMessage = new UnlinkMessage(minecraftUuid, minecraftName, "self");
                String json = new Gson().toJson(unlinkMessage);
                sendOrQueueWhitelistMessage(json);
                logger.info("Sent unlink command to Minecraft for: {}", minecraftName);
            } else {
                UnlinkMessage unlinkMessage = new UnlinkMessage(minecraftUuid, minecraftName, "self");
                String json = new Gson().toJson(unlinkMessage);
                sendOrQueueWhitelistMessage(json);
                logger.warn("Cannot send unlink command - Minecraft not connected");
            }

            enqueueUnlinkChannelMessage(user.getId(), minecraftName);
            updateWhitelistStatusMessage();
            notifyUserInPrivateThread(user, "Unlink", config.message("whitelist.notify.unlinked",
                "You unlinked your Discord. Re-link in Discord to rejoin."));

            String unlinkSuccess = MessageFormatter.format(
                config.message("whitelist.unlink.success", "✅ Your Discord account has been unlinked from {minecraftName}."),
                Map.of("minecraftName", minecraftName)
            );
            return WhitelistResult.success(unlinkSuccess);
        } catch (Exception e) {
            logger.error("Failed to unlink Discord account", e);
            return WhitelistResult.error(config.message("whitelist.unlink.error",
                "❌ Failed to unlink your account. Please try again later."));
        }
    }

    /**
     * Handle unlink initiated from Minecraft
     */
    public void handleMinecraftUnlink(UnlinkMessage message, String discordId) {
        String playerName = message.getPlayerName();
        String removeTitle = config.message("whitelist.log.remove.title", "🗑️ Whitelist Removed");
        String removeDesc = MessageFormatter.format(
            config.message("whitelist.log.remove.desc", "Minecraft: {playerName}"),
            Map.of("playerName", playerName)
        );
        logWhitelistEvent(removeTitle, removeDesc, Color.ORANGE);
        updateWhitelistStatusMessage();

        if (discordId != null && !discordId.isBlank()) {
            User user = jda.getUserById(discordId);
            if (user != null) {
                notifyUserInPrivateThread(user, "Whitelist Removed",
                    config.message("whitelist.notify.whitelistRemoved", "Your whitelist entry was removed. Go to Discord to re-link."));
            }
        }
    }

    public void handleWhitelistRemoveConfirmation(WhitelistRemoveConfirmation confirmation) {
        String playerName = confirmation.getPlayerName();
        PendingUnlinkRequest pending = pendingUnlinkRequests.remove(playerName.toLowerCase());

        if (pending != null) {
            updateUnlinkChannelMessage(pending, confirmation.isSuccess(), confirmation.getError());
        }

        if (!confirmation.isSuccess()) {
            return;
        }

        String removeTitle = config.message("whitelist.log.remove.title", "🗑️ Whitelist Removed");
        String removeDesc = MessageFormatter.format(
            config.message("whitelist.log.remove.desc", "Minecraft: {playerName}"),
            Map.of("playerName", playerName)
        );
        logWhitelistEvent(removeTitle, removeDesc, Color.ORANGE);
        updateWhitelistStatusMessage();
    }

    private void logWhitelistRequest(PendingWhitelistRequest pending) {
        // Intentionally left blank to disable request logging.
    }

    private void updateWhitelistRequestLog(PendingWhitelistRequest pending, boolean success, String error) {
        TextChannel channel = resolveWhitelistLogChannel();
        if (channel == null || !channel.canTalk()) {
            return;
        }

        String title = success
            ? config.message("whitelist.log.success.title", "✅ Whitelist Successful")
            : config.message("whitelist.log.fail.title", "❌ Whitelist Failed");
        String description = buildWhitelistResultDescription(pending, success, error);

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(success ? Color.GREEN : Color.RED)
            .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    private String buildWhitelistResultDescription(PendingWhitelistRequest pending, boolean success, String error) {
        String errorText = error != null && !error.isBlank() ? error : "Unknown error";
        String template = success
            ? config.message("whitelist.log.success.desc", "Discord: <@{discordId}>\nMinecraft: {playerName}")
            : config.message("whitelist.log.fail.desc", "Discord: <@{discordId}>\nMinecraft: {playerName}\nError: {error}");

        return MessageFormatter.format(template, Map.of(
            "discordId", pending.discordId,
            "playerName", pending.minecraftName,
            "error", errorText
        ));
    }

    private void sendOrQueueWhitelistMessage(String json) {
        if (webSocketServer != null && webSocketServer.isMinecraftConnected()) {
            webSocketServer.broadcast(json);
            return;
        }

        // Persist to disk first
        try {
            Files.createDirectories(pendingQueuePath.getParent());
            Files.write(pendingQueuePath, (json + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("Persisted pending whitelist message to {}", pendingQueuePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to persist pending whitelist message, falling back to memory queue", e);
            pendingWhitelistMessages.offer(json);
            return;
        }

        // Also keep in-memory as a fast-access queue
        pendingWhitelistMessages.offer(json);
    }

    private void flushPendingWhitelistMessages() {
        if (webSocketServer == null || !webSocketServer.isMinecraftConnected()) {
            return;
        }

        // First flush in-memory queue
        String message;
        while ((message = pendingWhitelistMessages.poll()) != null) {
            webSocketServer.broadcast(message);
        }

        // Then flush persisted queue atomically
        if (Files.exists(pendingQueuePath)) {
            List<String> lines = new ArrayList<>();
            try {
                lines = Files.readAllLines(pendingQueuePath, StandardCharsets.UTF_8);
                for (String l : lines) {
                    if (l != null && !l.isBlank()) {
                        webSocketServer.broadcast(l);
                    }
                }
                // Delete the file after successful flush
                Files.deleteIfExists(pendingQueuePath);
            } catch (IOException e) {
                logger.error("Failed to flush persisted pending whitelist messages", e);
            }
        }
    }

    private TextChannel resolveWhitelistLogChannel() {
        Guild guild = jda.getGuildById(config.discord.guildId);
        if (guild == null) {
            logger.error("Guild not found: {}", config.discord.guildId);
            return null;
        }

        String channelId = config.channels.whitelistLogChannelId;
        if (channelId == null || channelId.isBlank()) {
            channelId = config.channels.reviewChannelId;
        }

        if (channelId == null || channelId.isBlank()) {
            return null;
        }

        return guild.getTextChannelById(channelId);
    }

    private TextChannel resolveWhitelistChannel() {
        Guild guild = jda.getGuildById(config.discord.guildId);
        if (guild == null) {
            logger.error("Guild not found: {}", config.discord.guildId);
            return null;
        }

        String channelId = config.channels.whitelistChannelId;
        if (channelId == null || channelId.isBlank()) {
            return null;
        }

        return guild.getTextChannelById(channelId);
    }

        private void updateWhitelistStatusMessage() {
            TextChannel logChannel = resolveWhitelistLogChannel();
            if (logChannel == null || !logChannel.canTalk()) return;

            if (whitelistStatusMessageId == null || whitelistStatusMessageId.isBlank()) {
                logChannel.sendMessageEmbeds(buildWhitelistStatusEmbed().build())
                    .queue(message -> whitelistStatusMessageId = message.getId(),
                        error -> logger.warn("Failed to post whitelist status message", error));
                return;
            }

            logChannel.editMessageEmbedsById(whitelistStatusMessageId, buildWhitelistStatusEmbed().build())
                .queue(null, error -> whitelistStatusMessageId = null);
        }

    private static class PendingWhitelistRequest {
        private final String discordId;
        private final String discordTag;
        private final String minecraftName;
        private final String minecraftUuid;
        private final long requestedAt;
        private final InteractionHook hook;

        private PendingWhitelistRequest(
            String discordId,
            String discordTag,
            String minecraftName,
            String minecraftUuid,
            long requestedAt,
            InteractionHook hook
        ) {
            this.discordId = discordId;
            this.discordTag = discordTag;
            this.minecraftName = minecraftName;
            this.minecraftUuid = minecraftUuid;
            this.requestedAt = requestedAt;
            this.hook = hook;
        }
    }

    private static class PendingUnlinkRequest {
        private final String discordId;
        private final String minecraftName;
        private String channelMessageId;

        private PendingUnlinkRequest(String discordId, String minecraftName) {
            this.discordId = discordId;
            this.minecraftName = minecraftName;
        }
    }

    // Stub implementations for missing methods
    private void enqueueUnlinkChannelMessage(String discordId, String minecraftName) {
        // Stub implementation
    }

    private void notifyUserInPrivateThread(net.dv8tion.jda.api.entities.User user, String action, String message) {
        // Stub implementation
    }

    private void logWhitelistEvent(String title, String description, java.awt.Color color) {
        // Stub implementation
    }

    private void updateUnlinkChannelMessage(PendingUnlinkRequest pending, boolean success, String error) {
        // Stub implementation
    }

    public WhitelistResult adminUnlinkDiscordUser(net.dv8tion.jda.api.entities.User user, String reason, String adminReason) {
        // Stub implementation
        return new WhitelistResult(false, "Not implemented");
    }

    public WhitelistResult linkFromVanillaThread(String payload, String discordRaw, String userId) {
        // Stub implementation
        return new WhitelistResult(false, "Not implemented");
    }
}
