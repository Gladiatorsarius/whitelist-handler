package whitelisting.swordsmp.discord;

import whitelisting.swordsmp.linking.LinkingService;
import whitelisting.swordsmp.linking.PlayerLinkingManager;
import whitelisting.swordsmp.whitelist.WhitelistCommandHandler;
import whitelisting.swordsmp.discord.MojangAPIService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.Optional;

/**
 * Simplified WhitelistManager ported into the mod. Uses LinkingService for persistence
 * and WhitelistCommandHandler to perform server-side whitelist actions.
 */
public class WhitelistManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistManager.class);

    private final JDA jda;
    private final MojangAPIService mojangAPI;
    private final WhitelistCommandHandler commandHandler;
    private volatile String whitelistLogChannelId;
    // Minimal pending request tracking to support approve/deny actions
    private final java.util.concurrent.ConcurrentMap<String, PendingWhitelistRequest> pendingRequests = new java.util.concurrent.ConcurrentHashMap<>();

    public WhitelistManager(JDA jda, WhitelistCommandHandler commandHandler) {
        this.jda = jda;
        this.commandHandler = commandHandler;
        this.mojangAPI = new MojangAPIService();
    }

    public static class WhitelistResult {
        public final boolean success;
        public final String message;

        private WhitelistResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static WhitelistResult success(String msg) { return new WhitelistResult(true, msg); }
        public static WhitelistResult error(String msg) { return new WhitelistResult(false, msg); }
    }

    public WhitelistResult processRequest(User user, String minecraftName, InteractionHook hook) {
        try {
            if (!mojangAPI.isValidUsernameFormat(minecraftName)) {
                return WhitelistResult.error("Invalid Minecraft username format.");
            }

            Optional<whitelisting.swordsmp.linking.PlayerLink> existing = LinkingService.getInstance().getLinkByName(minecraftName);
            if (existing.isPresent()) {
                return WhitelistResult.error("That Minecraft name is already linked.");
            }

            var profileOpt = mojangAPI.getProfile(minecraftName);
            if (profileOpt.isEmpty()) {
                return WhitelistResult.error("Minecraft username not found.");
            }

            var profile = profileOpt.get();
            String uuid = profile.getFormattedUuid();

            // Resolved-UUID ownership check: a renamed account must not be claimable
            // by a different Discord user. The link itself is written only after the
            // server confirms the whitelist state (see WhitelistCommandHandler).
            var byUuid = PlayerLinkingManager.getInstance().getLinkByUuid(uuid);
            if (byUuid.isPresent() && !byUuid.get().getDiscordId().equals(user.getId())) {
                return WhitelistResult.error("That Minecraft account is already linked to a different Discord user.");
            }

            // Ask server to whitelist the player
            String requestId = java.util.UUID.randomUUID().toString();
            // store minimal pending request so approve/deny UI can reference it
            PendingWhitelistRequest pending = new PendingWhitelistRequest(user.getId(), user.getAsTag(), profile.getName(), uuid, System.currentTimeMillis(), hook);
            pendingRequests.put(requestId, pending);

            commandHandler.handleWhitelistAdd(requestId, profile.getName(), uuid, user.getId(), user.getAsTag(), "discord");

            return WhitelistResult.success("Request submitted — waiting for Minecraft confirmation.");
        } catch (Exception e) {
            LOGGER.error("Error processing whitelist request", e);
            return WhitelistResult.error("Failed to process request");
        }
    }

    public WhitelistResult unlinkDiscord(User user) {
        try {
            var opt = LinkingService.getInstance().getMinecraftUuid(user.getId());
            if (opt.isEmpty()) {
                return WhitelistResult.success("Your Discord account is already unlinked.");
            }

            String uuid = opt.get();
            String playerName = null;
            var linkOpt = PlayerLinkingManager.getInstance().getLinkByUuid(uuid);
            if (linkOpt.isPresent()) {
                playerName = linkOpt.get().getMinecraftName();
            }
            LinkingService.getInstance().removeLinkByUuid(uuid);

            if (playerName != null) {
                commandHandler.handleWhitelistRemove(uuid, playerName, "player", "Unlinked on Discord by **" + user.getAsTag() + "**");
                postToLogChannel("🔴 **" + playerName + "** was removed from the whitelist (Unlinked on Discord by **" + user.getAsTag() + "**)");
            } else {
                postToLogChannel("🔓 **" + user.getAsTag() + "** unlinked their account.");
            }
            updateWhitelistList();

            return WhitelistResult.success("Unlinked your account.");
        } catch (Exception e) {
            LOGGER.error("Failed to unlink", e);
            return WhitelistResult.error("Failed to unlink your account.");
        }
    }

    public void updateRequestMessage(String requestId, String newMessage) {
        PendingWhitelistRequest pending = pendingRequests.remove(requestId);
        if (pending != null && pending.hook != null) {
            try { pending.hook.editOriginal(newMessage).queue(); } catch (Exception ignored) {}
        }
    }

    /** Queue a message to the log channel. Returns true when queued, not when delivered. */
    public boolean postToLogChannel(String message) {
        if (jda == null || whitelistLogChannelId == null || whitelistLogChannelId.isBlank()) return false;
        TextChannel channel = jda.getTextChannelById(whitelistLogChannelId);
        if (channel == null) return false;
        try {
            channel.sendMessage(message).queue(
                null,
                error -> LOGGER.warn("Failed to deliver message to whitelist log channel: {}", error.getMessage())
            );
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to queue message to whitelist log channel", e);
            return false;
        }
    }

    public String message(String key, String fallback) {
        return fallback == null ? "" : fallback;
    }

    public boolean hasStaffPermission(Member member) {
        if (member == null) return false;
        if (member.hasPermission(Permission.MANAGE_SERVER)) return true;
        String staffRole = DiscordBotManager.getConfig().getStaffRoleId();
        if (staffRole.isBlank()) staffRole = System.getenv("DISCORD_STAFF_ROLE_ID");
        if (staffRole == null || staffRole.isBlank()) return false;
        final String targetStaffRole = staffRole;
        return member.getRoles().stream().anyMatch(r -> targetStaffRole.equals(r.getId()));
    }

    public void approveRequest(String requestId, String staffId, String staffName) {
        PendingWhitelistRequest pending = pendingRequests.remove(requestId);
        if (pending == null) {
            LOGGER.warn("No pending request to approve: {}", requestId);
            return;
        }
        LOGGER.info("Approving request {} by {}", requestId, staffName);
        // Whitelist on the server; the link is written by WhitelistCommandHandler
        // (checked insert) only after the whitelist state is confirmed.
        commandHandler.handleWhitelistAdd(requestId, pending.minecraftName, pending.minecraftUuid, pending.discordId, pending.discordTag, staffId);
        postToLogChannel("✅ Request for **" + pending.minecraftName + "** approved by " + staffName);
        if (pending.hook != null) {
            try { pending.hook.editOriginal("✅ Request approved and whitelisting initiated.").queue(); } catch (Exception ignored) {}
        }
    }

    public void denyRequest(String requestId, String staffId, String staffName, String reason) {
        PendingWhitelistRequest pending = pendingRequests.remove(requestId);
        if (pending == null) {
            LOGGER.warn("No pending request to deny: {}", requestId);
            return;
        }
        LOGGER.info("Denying request {} by {} - {}", requestId, staffName, reason);
        postToLogChannel("❌ Request for **" + pending.minecraftName + "** denied by " + staffName + ": " + reason);
        if (pending.hook != null) {
            try { pending.hook.editOriginal("❌ Your whitelist request was denied: " + reason).queue(); } catch (Exception ignored) {}
        }
    }

    public boolean setupWhitelistChannel(String channelId) {
        if (jda == null || channelId == null) return false;
        try {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) return false;
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Request Whitelist")
                .setDescription("Click the button to request whitelist access.")
                .setColor(Color.BLUE);
            Button requestButton = Button.primary("whitelist_request", "🎫 Request Whitelist");
            Button unlinkButton = Button.danger("whitelist_unlink", "🔓 Unlink");
            channel.sendMessageEmbeds(embed.build()).setActionRow(requestButton, unlinkButton).queue();
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to setup whitelist channel", e);
            return false;
        }
    }

    public boolean setupWhitelistLogChannel(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return false;
        }

        whitelistLogChannelId = channelId;
        whitelistListMessageId = null;
        findAndSetWhitelistListMessage();
        return true;
    }

    public void postWhitelistListNow() {
        updateWhitelistList();
    }

    public WhitelistResult adminUnlinkDiscordUser(User user, String reason, String adminTag) {
        try {
            var linkOpt = PlayerLinkingManager.getInstance().getLinkByDiscord(user.getId());
            if (linkOpt.isEmpty()) {
                return WhitelistResult.success("User is not linked.");
            }
            var link = linkOpt.get();
            LinkingService.getInstance().removeLinkByUuid(link.getMinecraftUuid());
            commandHandler.handleWhitelistRemove(link.getMinecraftUuid(), link.getMinecraftName(), "admin", "Unlinked by admin **" + adminTag + "**");
            return WhitelistResult.success("User unlinked.");
        } catch (Exception e) {
            LOGGER.error("Failed admin unlink", e);
            return WhitelistResult.error("Failed to unlink user.");
        }
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

    private volatile String whitelistListMessageId;
    private volatile long whitelistListChannelId;

    public synchronized void updateWhitelistList() {
        if (jda == null || whitelistLogChannelId == null || whitelistLogChannelId.isBlank()) return;
        TextChannel channel = jda.getTextChannelById(whitelistLogChannelId);
        if (channel == null) return;

        String content = buildWhitelistListContent();

        if (whitelistListMessageId != null && !whitelistListMessageId.isBlank()) {
            try {
                channel.editMessageById(whitelistListMessageId, content).queue();
                return;
            } catch (Exception ignored) {}
        }

        channel.sendMessage(content).queue(success -> {
            whitelistListMessageId = success.getId();
            whitelistListChannelId = channel.getIdLong();
        });
    }

    public synchronized void findAndSetWhitelistListMessage() {
        if (jda == null || whitelistLogChannelId == null || whitelistLogChannelId.isBlank()) return;
        if (whitelistListMessageId != null && !whitelistListMessageId.isBlank()) return;
        TextChannel channel = jda.getTextChannelById(whitelistLogChannelId);
        if (channel == null) return;

        channel.getHistory().retrievePast(10).queue(messages -> {
            for (var msg : messages) {
                if (msg != null && msg.getContentRaw().startsWith("Current whitelisted players:")) {
                    whitelistListMessageId = msg.getId();
                    whitelistListChannelId = channel.getIdLong();
                    LOGGER.info("Found existing whitelist list message: {}", whitelistListMessageId);
                    return;
                }
            }
        });
    }

    public String buildWhitelistListContent() {
        StringBuilder message = new StringBuilder("Current whitelisted players:\n");
        var links = PlayerLinkingManager.getInstance().getAllLinks();
        if (links.isEmpty()) {
            message.append("- None\n");
        } else {
            for (var link : links) {
                message.append("- ").append(link.getMinecraftName()).append(" (Discord: ").append(link.getDiscordId()).append(")\n");
            }
        }
        return message.toString();
    }
}
