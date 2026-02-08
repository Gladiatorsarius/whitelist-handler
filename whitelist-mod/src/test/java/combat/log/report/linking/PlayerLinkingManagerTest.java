package combat.log.report.linking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PlayerLinkingManagerTest {

    @TempDir
    Path tempDir;

    private PlayerLinkingManager manager;
    private Path dbFile;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("player-links.json");
        manager = new PlayerLinkingManager(dbFile);
    }

    @Test
    void testAddAndGetLink() {
        String discordId = "123456789";
        String uuid = "550e8400-e29b-41d4-a716-446655440000";

        manager.addLink(discordId, uuid);

        Optional<String> result = manager.getMinecraftUuid(discordId);
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
    }

    @Test
    void testRemoveLink() {
        String discordId = "123456789";
        String uuid = "550e8400-e29b-41d4-a716-446655440000";

        manager.addLink(discordId, uuid);
        assertTrue(manager.getMinecraftUuid(discordId).isPresent());

        manager.removeLink(discordId);
        assertFalse(manager.getMinecraftUuid(discordId).isPresent());
    }

    @Test
    void testGetNonExistentLink() {
        Optional<String> result = manager.getMinecraftUuid("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void testPersistence() {
        String discordId = "123456789";
        String uuid = "550e8400-e29b-41d4-a716-446655440000";

        manager.addLink(discordId, uuid);

        // Create new manager instance to test loading from file
        PlayerLinkingManager newManager = new PlayerLinkingManager(dbFile);
        Optional<String> result = newManager.getMinecraftUuid(discordId);
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
    }
}