package whitelisting.swordsmp.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles whitelist-related slash commands.
 */
public class WhitelistCommands extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistCommands.class);
    private final WhitelistManager whitelistManager;

    public WhitelistCommands(WhitelistManager whitelistManager) {
        this.whitelistManager = whitelistManager;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"whitelist".equals(event.getName())) {
            return;
        }

        String sub = event.getSubcommandName();
        if (sub == null) {
            replyEphemeral(event, "Subcommand required.");
            return;
        }

        switch (sub) {
            case "tickets" -> handleWhitelistSetup(event);
            case "log" -> handleWhitelistLogSetup(event);
            case "unlink" -> handleUnlink(event);
            default -> replyEphemeral(event, "Unknown subcommand: " + sub);
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
        OptionMapping channelOption = event.getOption("channel");
        if (channelOption == null || channelOption.getAsChannel() == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.setup.channelRequired", "❌ Channel ID is required"));
            return;
        }

        String channelId = channelOption.getAsChannel().getId();
        boolean ok = whitelistManager.setupWhitelistChannel(channelId);
        if (ok) {
            replyEphemeral(event, whitelistManager.message("whitelist.setup.success", "✅ Whitelist channel setup complete!"));
        } else {
            replyEphemeral(event, "❌ Failed to setup whitelist channel: bot cannot send messages to that channel or an error occurred. Check bot permissions and try again.");
        }
    }

    private void handleWhitelistLogSetup(SlashCommandInteractionEvent event) {
        if (!whitelistManager.hasStaffPermission(event.getMember())) {
            replyEphemeral(event, whitelistManager.message("whitelist.unlink.noPermission", "❌ You don't have permission to use this command."));
            return;
        }

        OptionMapping channelOption = event.getOption("channel");
        if (channelOption == null || channelOption.getAsChannel() == null) {
            replyEphemeral(event, whitelistManager.message("whitelist.setup.channelRequired", "❌ Channel ID is required"));
            return;
        }

        String channelId = channelOption.getAsChannel().getId();
        boolean ok = whitelistManager.setupWhitelistLogChannel(channelId);
        if (ok) {
            replyEphemeral(event, "✅ Whitelist log channel set to <#" + channelId + ">.");
            try {
                whitelistManager.postWhitelistListNow();
            } catch (Exception ignore) {
                LOGGER.warn("Failed to post whitelist list immediately", ignore);
            }
        } else {
            replyEphemeral(event, "❌ Failed to set whitelist log channel. See bot logs.");
        }
    }

    private void replyEphemeral(SlashCommandInteractionEvent event, String message) {
        event.reply(message).setEphemeral(true).queue();
    }
}
