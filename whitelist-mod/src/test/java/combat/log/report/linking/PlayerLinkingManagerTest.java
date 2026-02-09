package combat.log.report.linking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PlayerLinkingManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Initialize the singleton with temp dir
        PlayerLinkingManager.initialize(tempDir);
    }

    @AfterEach
    void tearDown() {
        // Reset singleton for next test (if needed, but since it's static, may need to clear state)
        // For simplicity, we'll rely on temp dir isolation
    }

    @Test
    void testAddAndGetLink() {
        String discordId = "123456789";
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        String name = "TestPlayer";

        PlayerLinkingManager manager = PlayerLinkingManager.getInstance();
        manager.addLink(discordId, uuid, name, true);

        Optional<String> result = manager.getMinecraftUuid(discordId);
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
    }

    @Test
    void testRemoveLink() {
        String discordId = "123456789";
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        String name = "TestPlayer";

        PlayerLinkingManager manager = PlayerLinkingManager.getInstance();
        manager.addLink(discordId, uuid, name, true);
        assertTrue(manager.getMinecraftUuid(discordId).isPresent());

        manager.removeLink(uuid);
        assertFalse(manager.getMinecraftUuid(discordId).isPresent());
    }

    @Test
    void testGetNonExistentLink() {
        PlayerLinkingManager manager = PlayerLinkingManager.getInstance();
        Optional<String> result = manager.getMinecraftUuid("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void testPersistence() {
        String discordId = "123456789";
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        String name = "TestPlayer";

        PlayerLinkingManager manager = PlayerLinkingManager.getInstance();
        manager.addLink(discordId, uuid, name, true);

        // Create new manager instance by re-initializing (simulates restart)
        PlayerLinkingManager.initialize(tempDir);
        PlayerLinkingManager newManager = PlayerLinkingManager.getInstance();
        Optional<String> result = newManager.getMinecraftUuid(discordId);
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
    }
}