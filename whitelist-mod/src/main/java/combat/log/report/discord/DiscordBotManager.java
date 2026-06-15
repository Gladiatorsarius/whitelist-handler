package whitelisting.swordsmp.discord;

import whitelisting.swordsmp.whitelist.WhitelistCommandHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

/**
 * Simple embedded Discord bot manager. Reads configuration from config/whitelisting-via-discord.json
 */
public class DiscordBotManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordBotManager.class);
    private static JDA jda;
    private static WhitelistManager whitelistManager;
    private static DiscordConfig config = new DiscordConfig();

    private static void loadConfig() {
        try {
            config = DiscordConfig.loadOrCreate(Paths.get("config"));
            LOGGER.info("Discord config loaded from config/whitelisting-via-discord.json");
        } catch (Exception e) {
            LOGGER.warn("Failed to load Discord config; using defaults", e);
            config = new DiscordConfig();
        }
    }

    public static synchronized void initialize(MinecraftServer server) {
        if (jda != null) return;

        loadConfig();

        String token = config.getToken();
        if (token == null || token.isBlank()) {
            LOGGER.warn("Discord bot token not configured in config/whitelisting-via-discord.json; embedded Discord bot will not start");
            return;
        }

        try {
            WhitelistCommandHandler commandHandler = new WhitelistCommandHandler(server);
            WhitelistCommandHandler handler = commandHandler;

            Thread initThread = new Thread(() -> {
                try {
                    JDA built = JDABuilder.createDefault(token,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.MESSAGE_CONTENT)
                            .build();

                    // Wait for JDA to be ready, but do this off the Minecraft server thread
                    built.awaitReady();

                    synchronized (DiscordBotManager.class) {
                        jda = built;
                        whitelistManager = new WhitelistManager(jda, handler);
                        jda.addEventListener(
                            new WhitelistCommands(whitelistManager),
                            new WhitelistButtonHandler(whitelistManager),
                            new WhitelistModalHandler(whitelistManager)
                        );
                        registerCommands();
                    }

                    LOGGER.info("Embedded Discord bot connected as {}", jda.getSelfUser().getAsTag());
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize embedded Discord bot", e);
                }
            }, "Whitelisting-Discord-Init");

            initThread.setDaemon(true);
            initThread.start();
        } catch (Exception e) {
            // Any immediate failures (e.g. constructing command handler) should be logged
            LOGGER.error("Failed to start Discord initialization thread", e);
        }
    }

    public static void shutdown() {
        if (jda != null) {
            try {
                jda.shutdown();
                LOGGER.info("Discord bot shut down");
            } catch (Exception e) {
                LOGGER.warn("Error shutting down Discord bot", e);
            } finally {
                jda = null;
                whitelistManager = null;
            }
        }
    }

    public static JDA getJda() {
        return jda;
    }

    public static WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public static DiscordConfig getConfig() {
        return config;
    }

    public static void onInGameWhitelistAdd(String playerName, String playerUuid) {
        if (whitelistManager == null) return;
        try {
            whitelistManager.postToLogChannel("✅ **" + playerName + "** was whitelisted (via console/ingame)");
            whitelistManager.updateWhitelistList();
        } catch (Exception e) {
            LOGGER.warn("Failed to handle ingame whitelist add", e);
        }
    }

    public static void onInGameWhitelistRemove(String playerName, String playerUuid) {
        if (whitelistManager == null) return;
        try {
            whitelistManager.postToLogChannel("🔴 **" + playerName + "** was removed from the whitelist (via console/ingame)");
            whitelistManager.updateWhitelistList();
        } catch (Exception e) {
            LOGGER.warn("Failed to handle ingame whitelist remove", e);
        }
    }

    private static void registerCommands() {
        if (jda == null) {
            return;
        }

        var whitelistCommand = Commands.slash("whitelist", "Whitelist management")
            .addSubcommands(
                new SubcommandData("tickets", "Post the whitelist request panel")
                    .addOption(OptionType.CHANNEL, "channel", "Channel to post the whitelist request panel in", true),
                new SubcommandData("log", "Set the whitelist log channel")
                    .addOption(OptionType.CHANNEL, "channel", "Channel to post whitelist logs in", true),
                new SubcommandData("unlink", "Unlink a Discord user")
                    .addOption(OptionType.USER, "user", "Discord user to unlink", true)
            );

        String guildId = config.getGuildId();
        if (guildId != null && !guildId.isBlank()) {
            final String targetGuildId = guildId;
            var guild = jda.getGuildById(guildId);
            if (guild == null) {
                LOGGER.warn("DISCORD_GUILD_ID {} not found; registering global commands instead", guildId);
            } else {
                guild.updateCommands().addCommands(whitelistCommand).queue(
                    success -> LOGGER.info("Registered whitelist commands for guild {}", targetGuildId),
                    error -> LOGGER.warn("Failed to register guild commands", error)
                );
                return;
            }
        }

        jda.updateCommands().addCommands(whitelistCommand).queue(
            success -> LOGGER.info("Registered global whitelist commands"),
            error -> LOGGER.warn("Failed to register global commands", error)
        );
    }

    public static void sendWhitelistConfirmation(String requestId, boolean success, String playerName, String discordDisplayName, String error, String requestedBy) {
        if (jda == null) return;

        try {
            if (whitelistManager == null) return;

            if (success) {
                whitelistManager.updateRequestMessage(requestId, "✅ Your whitelist request for **" + playerName + "** has been approved!");
                String approverNote = "discord".equals(requestedBy) ? "" : " (by " + requestedBy + ")";
                whitelistManager.postToLogChannel("✅ **" + playerName + "** was whitelisted (Discord: " + discordDisplayName + ")" + approverNote);
                whitelistManager.updateWhitelistList();
            } else {
                String errMsg = error != null ? error : "Unknown error";
                whitelistManager.updateRequestMessage(requestId, "❌ Your whitelist request failed: " + errMsg);
                whitelistManager.postToLogChannel("❌ Whitelist request for **" + playerName + "** failed: " + errMsg);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to post whitelist confirmation to Discord", e);
        }
    }

    public static void sendWhitelistRemoveNotification(String playerName, boolean success, String error, String cause) {
        sendWhitelistRemoveNotification(playerName, success, error);
    }

    public static void sendWhitelistRemoveNotification(String playerName, boolean success, String error) {
        if (jda == null) return;

        try {
            if (whitelistManager == null) return;

            if (success) {
                String msg = "🔴 **" + playerName + "** was removed from the whitelist" + (error != null && !error.isBlank() ? " (" + error + ")" : "");
                whitelistManager.postToLogChannel(msg);
                whitelistManager.updateWhitelistList();
            } else {
                String errMsg = error != null ? error : "Unknown error";
                whitelistManager.postToLogChannel("❌ Failed to remove **" + playerName + "** from whitelist: " + errMsg);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to post whitelist remove notification to Discord", e);
        }
    }
}
