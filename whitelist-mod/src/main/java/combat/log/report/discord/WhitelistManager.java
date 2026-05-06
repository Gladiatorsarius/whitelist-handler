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

            // Create the link immediately (server authoritative is still player-links.json)
            LinkingService.getInstance().createLink(user.getId(), profile.getName(), true);

            // Ask server to whitelist the player
            String requestId = java.util.UUID.randomUUID().toString();
            // store minimal pending request so approve/deny UI can reference it
            PendingWhitelistRequest pending = new PendingWhitelistRequest(user.getId(), user.getAsTag(), profile.getName(), uuid, System.currentTimeMillis(), hook);
            pendingRequests.put(requestId, pending);

            commandHandler.handleWhitelistAdd(requestId, profile.getName(), uuid, user.getId(), user.getAsTag(), "discord");

            if (hook != null) {
                try { hook.editOriginal("✅ Your request was processed.").queue(); } catch (Exception ignored) {}
            }

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
            LinkingService.getInstance().removeLinkByUuid(uuid);

            // Also remove from whitelist on server
            var linkOpt = PlayerLinkingManager.getInstance().getLinkByUuid(uuid);
            if (linkOpt.isPresent()) {
                var link = linkOpt.get();
                commandHandler.handleWhitelistRemove(link.getMinecraftUuid(), link.getMinecraftName(), "player");
            }

            return WhitelistResult.success("Unlinked your account.");
        } catch (Exception e) {
            LOGGER.error("Failed to unlink", e);
            return WhitelistResult.error("Failed to unlink your account.");
        }
    }

    public String message(String key, String fallback) {
        return fallback == null ? "" : fallback;
    }

    public boolean hasStaffPermission(Member member) {
        if (member == null) return false;
        if (member.hasPermission(Permission.MANAGE_SERVER)) return true;
        String staffRole = System.getenv("DISCORD_STAFF_ROLE_ID");
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
        // Create authoritative link and whitelist
        LinkingService.getInstance().createLink(pending.discordId, pending.minecraftName, true);
        commandHandler.handleWhitelistAdd(requestId, pending.minecraftName, pending.minecraftUuid, pending.discordId, pending.discordTag, staffId);
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
        return true;
    }

    public void postWhitelistListNow() {
        if (jda == null || whitelistLogChannelId == null || whitelistLogChannelId.isBlank()) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(whitelistLogChannelId);
        if (channel == null) {
            return;
        }

        StringBuilder message = new StringBuilder("Current whitelisted players:\n");
        var links = PlayerLinkingManager.getInstance().getAllLinks();
        if (links.isEmpty()) {
            message.append("- None\n");
        } else {
            for (var link : links) {
                message.append("- ").append(link.getMinecraftName()).append(" (Discord: ").append(link.getDiscordId()).append(")\n");
            }
        }

        channel.sendMessage(message.toString()).queue();
    }

    public WhitelistResult adminUnlinkDiscordUser(User user, String reason, String adminTag) {
        try {
            var linkOpt = PlayerLinkingManager.getInstance().getLinkByDiscord(user.getId());
            if (linkOpt.isEmpty()) {
                return WhitelistResult.success("User is not linked.");
            }
            var link = linkOpt.get();
            LinkingService.getInstance().removeLinkByUuid(link.getMinecraftUuid());
            commandHandler.handleWhitelistRemove(link.getMinecraftUuid(), link.getMinecraftName(), "admin");
            return WhitelistResult.success("User unlinked.");
        } catch (Exception e) {
            LOGGER.error("Failed admin unlink", e);
            return WhitelistResult.error("Failed to unlink user.");
        }
    }

    public WhitelistResult linkFromVanillaThread(String payload, String discordRaw, String userId) {
        // Not implemented in simplified port.
        return WhitelistResult.error("Not implemented in embedded mode");
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
}
