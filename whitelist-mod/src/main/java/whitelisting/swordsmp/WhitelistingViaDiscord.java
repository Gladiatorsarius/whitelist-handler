package whitelisting.swordsmp;

import combat.log.report.linking.PlayerLinkingManager;
import combat.log.report.socket.SocketClient;
import combat.log.report.whitelist.WhitelistCommandHandler;
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

	private void onServerStart(MinecraftServer server) {
		// Initialize player linking manager
		Path configDir = Paths.get("config");
		PlayerLinkingManager.initialize(configDir);
		LOGGER.info("Initialized player linking system");

		// Initialize socket client
		SocketClient socketClient = SocketClient.getInstance();

		// Configure socket (use environment variables or defaults)
		String serverUrl = System.getenv().getOrDefault("DISCORD_SOCKET_URL", "ws://localhost:8080/combat-log");
		String authToken = System.getenv().getOrDefault("DISCORD_AUTH_TOKEN", "");

		socketClient.configure(serverUrl);
		socketClient.setAuthToken(authToken);

		// Initialize whitelist command handler
		WhitelistCommandHandler whitelistHandler = new WhitelistCommandHandler(server, socketClient);
		socketClient.setWhitelistHandler(whitelistHandler);
		LOGGER.info("Initialized whitelist command handler");

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