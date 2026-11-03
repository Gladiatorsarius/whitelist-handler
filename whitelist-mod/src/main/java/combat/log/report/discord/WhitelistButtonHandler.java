package whitelisting.swordsmp.discord;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

/**
 * Handles button clicks for whitelist requests inside the mod.
 */
public class WhitelistButtonHandler extends ListenerAdapter {
    private final WhitelistManager whitelistManager;

    public WhitelistButtonHandler(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if ("whitelist_request".equals(componentId)) {
            showWhitelistModal(event);
            return;
        }

        if ("whitelist_unlink".equals(componentId)) {
            handleUnlink(event);
            return;
        }

        if (componentId.startsWith("whitelist_approve:")) {
            String requestId = componentId.substring("whitelist_approve:".length());
            handleApprove(event, requestId);
            return;
        }

        if (componentId.startsWith("whitelist_deny:")) {
            String requestId = componentId.substring("whitelist_deny:".length());
            showDenyModal(event, requestId);
            return;
        }
    }

    private void showWhitelistModal(ButtonInteractionEvent event) {
        TextInput usernameInput = TextInput.create(
                "minecraft_username",
                whitelistManager.message("whitelist.modal.usernameLabel", "Minecraft Username"),
                TextInputStyle.SHORT
            )
            .setPlaceholder(whitelistManager.message("whitelist.modal.usernamePlaceholder", "Enter your Minecraft username"))
            .setMinLength(3)
            .setMaxLength(16)
            .setRequired(true)
            .build();

        Modal modal = Modal.create("whitelist_modal", whitelistManager.message("whitelist.modal.title", "Request Whitelist"))
            .addActionRow(usernameInput)
            .build();

        event.replyModal(modal).queue();
    }

    private void handleApprove(ButtonInteractionEvent event, String requestId) {
        event.editButton(event.getButton().asDisabled()).queue();
        whitelistManager.approveRequest(requestId, event.getUser().getId(), event.getUser().getAsTag());
        event.getHook().sendMessage(whitelistManager.message("whitelist.approve.actionMessage", "✅ Request approved! Whitelisting player...")).setEphemeral(true).queue();
    }

    private void showDenyModal(ButtonInteractionEvent event, String requestId) {
        TextInput reasonInput = TextInput.create(
                "deny_reason",
                whitelistManager.message("whitelist.deny.modal.reasonLabel", "Reason for Denial"),
                TextInputStyle.PARAGRAPH
            )
            .setPlaceholder(whitelistManager.message("whitelist.deny.modal.reasonPlaceholder", "Enter reason for denial (optional)"))
            .setMinLength(0)
            .setMaxLength(500)
            .setRequired(false)
            .build();

        Modal modal = Modal.create("whitelist_deny_modal:" + requestId, whitelistManager.message("whitelist.deny.modal.title", "Deny Whitelist Request"))
            .addActionRow(reasonInput)
            .build();

        event.replyModal(modal).queue();
    }

    private void handleUnlink(ButtonInteractionEvent event) {
        if (event.isAcknowledged()) {
            var result = whitelistManager.unlinkDiscord(event.getUser());
            event.getHook().sendMessage(result.message).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            var result = whitelistManager.unlinkDiscord(event.getUser());
            hook.editOriginal(result.message).queue();
        });
    }
}
