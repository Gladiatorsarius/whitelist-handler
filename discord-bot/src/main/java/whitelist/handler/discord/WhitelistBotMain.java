package whitelist.handler.discord;

import whitelist.handler.discord.config.BotConfig;
import whitelist.handler.discord.whitelist.WhitelistManager;
import whitelist.handler.discord.websocket.WhitelistWebSocketServer;
import whitelist.handler.discord.api.MojangAPIService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import whitelist.handler.discord.commands.WhitelistCommands;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;

/**
 * Main entry for the Whitelist Handler bot.
 */
public class WhitelistBotMain {
    public static void main(String[] args) throws Exception {
        File cfgFile = new File("config.json");
        BotConfig config = BotConfig.load(cfgFile);

        if ("YOUR_BOT_TOKEN_HERE".equals(config.discord.token)) {
            System.err.println("Please set your bot token in config.json");
            System.exit(1);
        }

        // Initialize JDA
        JDA jda = JDABuilder.createDefault(config.discord.token)
            .enableIntents(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MEMBERS
            )
            .build();
        jda.awaitReady();

        // Initialize services
        MojangAPIService mojangAPI = new MojangAPIService();
        WhitelistManager manager = new WhitelistManager(jda, config, mojangAPI, cfgFile);

        // Start WebSocket server and wire manager
        WhitelistWebSocketServer ws = new WhitelistWebSocketServer(config);
        ws.setWhitelistManager(manager);
        manager.setWebSocketServer(ws);
        ws.start();

        // Register slash commands (whitelist with subcommands)
        jda.updateCommands().addCommands(
            Commands.slash("test", "Run a whitelist end-to-end test (create → lookup → unlink)"),
            Commands.slash("whitelist", "Manage whitelist settings and actions")
                .addSubcommands(
                    new SubcommandData("tickets", "Setup the whitelist tickets channel").addOption(OptionType.CHANNEL, "channel", "Channel to use for whitelist tickets (button/setup)", true),
                    new SubcommandData("log", "Set the whitelist log channel").addOption(OptionType.CHANNEL, "channel", "Channel to use for whitelist logs", true),
                    new SubcommandData("unlink", "Unlink a user").addOption(OptionType.USER, "user", "User to unlink", true)
                )
        ).queue();

        // Register command handlers
        jda.addEventListener(new WhitelistCommands(manager));

        // Add a lightweight listener for the /test command
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
                if ("test".equals(event.getName())) {
                    event.deferReply(true).queue();
                    try {
                        manager.runRemoteTest(event.getMember());
                        event.getHook().sendMessage("Requested whitelist test. Results will appear in the bot's whitelist log channel.").setEphemeral(true).queue();
                    } catch (Exception e) {
                        event.getHook().sendMessage("Failed to start test: " + e.getMessage()).setEphemeral(true).queue();
                    }
                }
            }
        });

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ws.stop();
            } catch (Exception ignored) {}
            try {
                jda.shutdown();
            } catch (Exception ignored) {}
        }));

        System.out.println("Whitelist bot started. Listening for /test on guild (global registration may take a minute). WebSocket on port " + config.websocket.port);
    }
}
