package whitelisting.swordsmp;

import whitelisting.swordsmp.linking.PlayerLinkingManager;
import whitelisting.swordsmp.discord.DiscordBotManager;
import whitelisting.swordsmp.whitelist.WhitelistCommandHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import whitelisting.swordsmp.config.ConfigManager;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class WhitelistingViaDiscord implements ModInitializer {
	public static final String MOD_ID = "whitelisting-via-discord";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Whitelisting via Discord mod initialized!");

		// Initialize config system (creates config directory if needed)
		ConfigManager.initialize();

		// Register server start event to initialize linking and socket
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);

		// Register server stop event to disconnect
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
	}

	private WhitelistCommandHandler whitelistHandler;

	private void onServerStart(MinecraftServer server) {
		// Initialize player linking manager
		Path configDir = ConfigManager.getConfigDir();
		PlayerLinkingManager.initialize(configDir);
		LOGGER.info("Initialized player linking system from {}", configDir.toAbsolutePath());

		// Initialize embedded Discord bot (reads token from DISCORD_BOT_TOKEN)
		DiscordBotManager.initialize(server);

		// Initialize whitelist command handler (embedded mode, no external socket)
		this.whitelistHandler = new WhitelistCommandHandler(server);
		LOGGER.info("Initialized whitelist command handler (embedded mode)");

		// Register in-game commands: /discord test and /discord unlink
		try {
			server.getCommands().getDispatcher().register(
				Commands.literal("discord")
					.then(Commands.literal("test").executes(ctx -> {
					// Embedded mode: post a test message to the configured log channel
					DiscordBotManager.getJda();
					ctx.getSource().sendSuccess(() -> Component.literal("Requested whitelist test. If the embedded bot is configured, it will post a test message to its log channel."), false);
						return 1;
					}))
					.then(Commands.literal("unlink").requires(src -> src.getEntity() instanceof ServerPlayer).executes(ctx -> {
						try {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							String uuid = player.getUUID().toString();
							var linkOpt = PlayerLinkingManager.getInstance().getLinkByUuid(uuid);
							if (linkOpt.isEmpty()) {
								player.sendSystemMessage(Component.literal("You are not linked to a Discord account."));
								return 0;
							}

							// Remove link and notify whitelist handler
							PlayerLinkingManager.getInstance().removeLink(uuid);
							if (this.whitelistHandler != null) {
                            this.whitelistHandler.handleWhitelistRemove(uuid, player.getName().getString(), "player", "Unlinked by player via /discord unlink");
							}

							player.connection.disconnect(Component.literal("You unlinked your Discord. Re-link in Discord to rejoin."));
							return 1;
						} catch (Exception e) {
							LOGGER.error("Error executing /discord unlink", e);
							return 0;
						}
					}))
			);
			LOGGER.info("Registered in-game /discord commands");
		} catch (Exception e) {
			LOGGER.warn("Failed to register /discord commands at runtime: {}", e.getMessage());
		}

		LOGGER.info("Embedded Discord bot initialized (if DISCORD_BOT_TOKEN provided)");
	}

	private void onServerStop(MinecraftServer server) {
		// Disconnect from Discord bot
		DiscordBotManager.shutdown();
	}
}