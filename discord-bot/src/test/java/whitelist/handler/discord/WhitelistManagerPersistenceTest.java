package whitelist.handler.discord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import whitelist.handler.discord.config.BotConfig;
import whitelist.handler.discord.whitelist.WhitelistManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class WhitelistManagerPersistenceTest {
    private final Path pending = Paths.get("data", "pending-whitelist.log");

    @AfterEach
    public void cleanup() throws IOException {
        if (Files.exists(pending)) Files.delete(pending);
    }

    @Test
    public void testPersistQueueWritesFile() throws Exception {
        WhitelistManager wm = new WhitelistManager(null, new BotConfig(), null);
        Method m = WhitelistManager.class.getDeclaredMethod("sendOrQueueWhitelistMessage", String.class);
        m.setAccessible(true);
        String payload = "{\"type\":\"test_message\",\"payload\":\"hello\"}";

        // invoke
        m.invoke(wm, payload);

        assertTrue(Files.exists(pending), "pending queue file should exist after persisting message");
        String content = Files.readString(pending);
        assertTrue(content.contains("hello"));
    }
}
