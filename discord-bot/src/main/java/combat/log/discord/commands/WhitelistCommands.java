package combat.log.discord.commands;

import combat.log.discord.whitelist.WhitelistManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles whitelist-related slash commands
 */
public class WhitelistCommands extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistCommands.class);
    private final WhitelistManager whitelistManager;

    public WhitelistCommands(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if ("whitelist-setup".equals(event.getName())) {
            handleWhitelistSetup(event);
            return;
        }

        if ("unlink".equals(event.getName())) {
            handleUnlink(event);
        }
    }

    private void handleUnlink(SlashCommandInteractionEvent event) {
        if (!whitelistManager.hasStaffPermission(event.getMember())) {
            replyEphemeral(event, whitelistManager.message("whitelist.unlink.noPermission", "❌ You don't have permission to use this command."));
            return;
        }

        OptionMapping userOpt = event.getOption("user");
        if (userOpt == null || userOpt.getAsUser() == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.unlink.missingUser", "❌ Discord user is required."));
            return;
        }

        WhitelistManager.WhitelistResult result = whitelistManager.adminUnlinkDiscordUser(
            userOpt.getAsUser(),
            event.getUser().getId(),
            event.getUser().getAsTag()
        );
        replyEphemeral(event, result.message);
    }

    private void handleWhitelistSetup(SlashCommandInteractionEvent event) {
        OptionMapping channelOption = event.getOption("channel_id");
        if (channelOption == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.setup.channelRequired", "❌ Channel ID is required"));
            return;
        }

        String channelId = channelOption.getAsString();
        logger.info("Setting up whitelist channel: {} by {}", channelId, event.getUser().getAsTag());

        try {
            whitelistManager.setupWhitelistChannel(channelId);
            replyEphemeral(event, whitelistManager.message("whitelist.setup.success", "✅ Whitelist channel setup complete!"));
        } catch (Exception e) {
            logger.error("Failed to setup whitelist channel", e);
            String msg = whitelistManager.message("whitelist.setup.failure", "❌ Failed to setup whitelist channel: {error}")
                .replace("{error}", e.getMessage());
            replyEphemeral(event, msg);
        }
    }

    private void replyEphemeral(SlashCommandInteractionEvent event, String message) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(message).setEphemeral(true).queue();
            return;
        }

        event.reply(message).setEphemeral(true).queue();
    }
}
