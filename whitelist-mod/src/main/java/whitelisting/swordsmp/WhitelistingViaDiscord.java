package whitelisting.swordsmp;

import combat.log.report.linking.PlayerLinkingManager;
import combat.log.report.socket.SocketClient;
import combat.log.report.socket.TestRequestMessage;
import combat.log.report.socket.UnlinkMessage;
import combat.log.report.whitelist.WhitelistCommandHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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

		// Register server start event to initialize linking and socket
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStart);

		// Register server stop event to disconnect
		ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStop);
	}

	private WhitelistCommandHandler whitelistHandler;

	private void onServerStart(MinecraftServer server) {
		// Initialize player linking manager
		Path configDir = Paths.get("config");
		PlayerLinkingManager.initialize(configDir);
		LOGGER.info("Initialized player linking system");

		// Initialize socket client
		SocketClient socketClient = SocketClient.getInstance();

		// Configure socket (use environment variables or defaults)
		String serverUrl = System.getenv().getOrDefault("DISCORD_SOCKET_URL", "ws://localhost:8080/combat-log");

		socketClient.configure(serverUrl);

		// Initialize whitelist command handler
		this.whitelistHandler = new WhitelistCommandHandler(server, socketClient);
		socketClient.setWhitelistHandler(this.whitelistHandler);
		LOGGER.info("Initialized whitelist command handler");

		// Register in-game commands: /discord test and /discord unlink
		try {
			server.getCommands().getDispatcher().register(
				Commands.literal("discord")
					.then(Commands.literal("test").executes(ctx -> {
						SocketClient.getInstance().sendMessage(new TestRequestMessage());
						ctx.getSource().sendSuccess(() -> Component.literal("Requested whitelist test. Results will appear in the bot's whitelist log channel."), false);
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

							// Construct unlink message and delegate to whitelist handler
							UnlinkMessage unlink = new UnlinkMessage(uuid, player.getName().getString(), "player");
							if (this.whitelistHandler != null) {
								this.whitelistHandler.handleWhitelistRemove(unlink);
							} else {
								// Fallback: remove link and kick
								PlayerLinkingManager.getInstance().removeLink(uuid);
								player.connection.disconnect(Component.literal("You unlinked your Discord. Re-link in Discord to rejoin."));
							}
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

		socketClient.connect();
		LOGGER.info("Attempting to connect to Discord bot at {}", serverUrl);
	}

	private void onServerStop(MinecraftServer server) {
		// Disconnect from Discord bot
		SocketClient socketClient = SocketClient.getInstance();
		if (socketClient.isConnected()) {
			LOGGER.info("Disconnecting from Discord bot...");
			socketClient.disconnect();
		}
	}
}