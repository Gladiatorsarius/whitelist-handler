package combat.log.discord.whitelist;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles button clicks for whitelist requests
 */
public class WhitelistButtonHandler extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistButtonHandler.class);
    private final WhitelistManager whitelistManager;

    public WhitelistButtonHandler(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        // Handle whitelist request button
        if ("whitelist_request".equals(componentId)) {
            showWhitelistModal(event);
            return;
        }

        // Handle unlink button
        if ("whitelist_unlink".equals(componentId)) {
            handleUnlink(event);
            return;
        }

        // Handle link button from vanilla whitelist add threads
        if (componentId.startsWith("whitelist_link:")) {
            String payload = componentId.substring("whitelist_link:".length());
            showLinkModal(event, payload);
            return;
        }

        // Handle approve button
        if (componentId.startsWith("whitelist_approve:")) {
            String requestId = componentId.substring("whitelist_approve:".length());
            handleApprove(event, requestId);
            return;
        }

        // Handle deny button
        if (componentId.startsWith("whitelist_deny:")) {
            String requestId = componentId.substring("whitelist_deny:".length());
            showDenyModal(event, requestId);
            return;
        }
    }

    /**
     * Show modal for requesting whitelist
     */
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

    /**
     * Handle approve button click
     */
    private void handleApprove(ButtonInteractionEvent event, String requestId) {
        logger.info("Approve button clicked by {} for request {}", event.getUser().getAsTag(), requestId);

        // Disable buttons
        event.editButton(event.getButton().asDisabled()).queue();

        // Approve the request
        whitelistManager.approveRequest(
            requestId,
            event.getUser().getId(),
            event.getUser().getAsTag()
        );

        event.getHook().sendMessage(whitelistManager.message("whitelist.approve.actionMessage", "✅ Request approved! Whitelisting player..."))
            .setEphemeral(true)
            .queue();
    }

    /**
     * Show modal for denying request
     */
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

        Modal modal = Modal.create("whitelist_deny_modal:" + requestId,
                whitelistManager.message("whitelist.deny.modal.title", "Deny Whitelist Request"))
            .addActionRow(reasonInput)
            .build();

        event.replyModal(modal).queue();
    }

    /**
     * Handle unlink button click
     */
    private void handleUnlink(ButtonInteractionEvent event) {
        if (event.isAcknowledged()) {
            WhitelistManager.WhitelistResult result = whitelistManager.unlinkDiscord(event.getUser());
            event.getHook().sendMessage(result.message).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(hook -> {
            WhitelistManager.WhitelistResult result = whitelistManager.unlinkDiscord(event.getUser());
            hook.editOriginal(result.message).queue();
        });
    }

    private void showLinkModal(ButtonInteractionEvent event, String payload) {
        TextInput discordInput = TextInput.create(
                "discord_user",
                whitelistManager.message("whitelist.link.modal.label", "Discord User (mention or ID)"),
                TextInputStyle.SHORT
            )
            .setPlaceholder(whitelistManager.message("whitelist.link.modal.placeholder", "@User or 1234567890"))
            .setMinLength(2)
            .setMaxLength(64)
            .setRequired(true)
            .build();

        Modal modal = Modal.create("whitelist_link_modal:" + payload,
                whitelistManager.message("whitelist.link.modal.title", "Link Discord Account"))
            .addActionRow(discordInput)
            .build();

        event.replyModal(modal).queue();
    }
}
