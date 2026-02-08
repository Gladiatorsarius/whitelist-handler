package combat.log.discord.whitelist;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles modal submissions for whitelist requests
 */
public class WhitelistModalHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistModalHandler.class);
    private final WhitelistManager whitelistManager;

    public WhitelistModalHandler(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        // Handle whitelist request modal
        if ("whitelist_modal".equals(modalId)) {
            handleWhitelistRequest(event);
            return;
        }

        // Handle deny modal
        if (modalId.startsWith("whitelist_deny_modal:")) {
            String requestId = modalId.substring("whitelist_deny_modal:".length());
            handleDeny(event, requestId);
            return;
        }

        // Handle link modal
        if (modalId.startsWith("whitelist_link_modal:")) {
            String payload = modalId.substring("whitelist_link_modal:".length());
            handleLink(event, payload);
            return;
        }
    }

    /**
     * Handle whitelist request modal submission
     */
    private void handleWhitelistRequest(ModalInteractionEvent event) {
        ModalMapping usernameMapping = event.getValue("minecraft_username");
        if (usernameMapping == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.modal.usernameMissing", "❌ Error: Username not provided"));
            return;
        }

        String minecraftName = usernameMapping.getAsString().trim();
        logger.info("Whitelist request submitted by {} for Minecraft name: {}", 
            event.getUser().getAsTag(), minecraftName);

        if (event.isAcknowledged()) {
            WhitelistManager.WhitelistResult result = whitelistManager.processRequest(event.getUser(), minecraftName, event.getHook());
            event.getHook().sendMessage(result.message).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            WhitelistManager.WhitelistResult result = whitelistManager.processRequest(event.getUser(), minecraftName, hook);
            hook.editOriginal(result.message).queue();
        });
    }

    /**
     * Handle deny modal submission
     */
    private void handleDeny(ModalInteractionEvent event, String requestId) {
        ModalMapping reasonMapping = event.getValue("deny_reason");
        String reason = reasonMapping != null ? reasonMapping.getAsString().trim() : "No reason provided";

        logger.info("Deny modal submitted by {} for request {} - Reason: {}", 
            event.getUser().getAsTag(), requestId, reason);

        // Disable buttons on the original message
        if (event.getMessage() != null) {
            event.getMessage().editMessageComponents().queue();
        }

        // Deny the request
        whitelistManager.denyRequest(
            requestId,
            event.getUser().getId(),
            event.getUser().getAsTag(),
            reason
        );

        replyEphemeral(event, whitelistManager.message("whitelist.deny.success", "❌ Request denied."));
    }

    private void handleLink(ModalInteractionEvent event, String payload) {
        ModalMapping discordMapping = event.getValue("discord_user");
        if (discordMapping == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.link.invalidUser", "❌ Invalid Discord user. Please use a mention or ID."));
            return;
        }

        String discordRaw = discordMapping.getAsString().trim();
        WhitelistManager.WhitelistResult result = whitelistManager.linkFromVanillaThread(payload, discordRaw, event.getUser());

        if (event.isAcknowledged()) {
            event.getHook().sendMessage(result.message).setEphemeral(true).queue();
            return;
        }

        event.reply(result.message).setEphemeral(true).queue();
    }

    private void replyEphemeral(ModalInteractionEvent event, String message) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(message).setEphemeral(true).queue();
            return;
        }

        event.reply(message).setEphemeral(true).queue();
    }
}
