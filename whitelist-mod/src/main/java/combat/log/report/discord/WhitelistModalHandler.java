package whitelisting.swordsmp.discord;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

/**
 * Handles modal submissions for whitelist requests inside the mod.
 */
public class WhitelistModalHandler extends ListenerAdapter {
    private final WhitelistManager whitelistManager;

    public WhitelistModalHandler(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if ("whitelist_modal".equals(modalId)) {
            handleWhitelistRequest(event);
            return;
        }

        if (modalId.startsWith("whitelist_deny_modal:")) {
            String requestId = modalId.substring("whitelist_deny_modal:".length());
            handleDeny(event, requestId);
            return;
        }

        if (modalId.startsWith("whitelist_link_modal:")) {
            String payload = modalId.substring("whitelist_link_modal:".length());
            handleLink(event, payload);
            return;
        }
    }

    private void handleWhitelistRequest(ModalInteractionEvent event) {
        ModalMapping usernameMapping = event.getValue("minecraft_username");
        if (usernameMapping == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.modal.usernameMissing", "❌ Error: Username not provided"));
            return;
        }

        String minecraftName = usernameMapping.getAsString().trim();

        if (event.isAcknowledged()) {
            var result = whitelistManager.processRequest(event.getUser(), minecraftName, event.getHook());
            event.getHook().sendMessage(result.message).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            var result = whitelistManager.processRequest(event.getUser(), minecraftName, hook);
            hook.editOriginal(result.message).queue();
        });
    }

    private void handleDeny(ModalInteractionEvent event, String requestId) {
        ModalMapping reasonMapping = event.getValue("deny_reason");
        String reason = reasonMapping != null ? reasonMapping.getAsString().trim() : "No reason provided";

        // Deny the request
        whitelistManager.denyRequest(requestId, event.getUser().getId(), event.getUser().getAsTag(), reason);

        replyEphemeral(event, whitelistManager.message("whitelist.deny.success", "❌ Request denied."));
    }

    private void handleLink(ModalInteractionEvent event, String payload) {
        ModalMapping discordMapping = event.getValue("discord_user");
        if (discordMapping == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.link.invalidUser", "❌ Invalid Discord user. Please use a mention or ID."));
            return;
        }

        String discordRaw = discordMapping.getAsString().trim();
        var result = whitelistManager.linkFromVanillaThread(payload, discordRaw, event.getUser().getId());

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
