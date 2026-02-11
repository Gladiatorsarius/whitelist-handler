package whitelist.handler.discord.commands;

import whitelist.handler.discord.whitelist.WhitelistManager;
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
        // ...existing code...
    }

    private void replyEphemeral(SlashCommandInteractionEvent event, String message) {
        event.reply(message).setEphemeral(true).queue();
    }
}
