package whitelist.handler.discord.whitelist;

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
            // ...existing code...
        }
    }

    private void handleDeny(ModalInteractionEvent event, String requestId) {
        // ...existing code...
    }

    private void handleLink(ModalInteractionEvent event, String payload) {
        // ...existing code...
    }

    private void replyEphemeral(ModalInteractionEvent event, String message) {
        event.reply(message).setEphemeral(true).queue();
    }
}
