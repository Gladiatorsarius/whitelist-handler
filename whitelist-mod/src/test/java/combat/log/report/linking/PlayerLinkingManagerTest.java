package whitelisting.swordsmp.linking;

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

    @Test
    void testAddLinkCheckedRejectsUuidOwnedByOtherDiscord() {
        PlayerLinkingManager manager = PlayerLinkingManager.getInstance();
        String uuid = "cccccccc-3333-3333-3333-333333333333";

        assertEquals(PlayerLinkingManager.AddLinkResult.OK,
                manager.addLinkChecked("ownerA", uuid, "OldName", true));

        // A renamed account (same UUID, new name) must not be claimable by another
        // Discord user, and the original owner's mapping must stay intact.
        assertEquals(PlayerLinkingManager.AddLinkResult.CONFLICT_UUID_OTHER_OWNER,
                manager.addLinkChecked("ownerB", uuid, "NewName", true));

        assertEquals("ownerA", manager.getLinkByUuid(uuid).orElseThrow().getDiscordId());
        assertTrue(manager.getMinecraftUuid("ownerA").isPresent());
        assertTrue(manager.getLinkByDiscord("ownerB").isEmpty());
    }

    @Test
    void testAddLinkCheckedAllowsSameOwnerRelink() {
        PlayerLinkingManager manager = PlayerLinkingManager.getInstance();
        String uuid = "dddddddd-4444-4444-4444-444444444444";

        assertEquals(PlayerLinkingManager.AddLinkResult.OK,
                manager.addLinkChecked("ownerC", uuid, "SomeName", true));
        assertEquals(PlayerLinkingManager.AddLinkResult.OK,
                manager.addLinkChecked("ownerC", uuid, "RenamedName", true));
        assertEquals("RenamedName", manager.getLinkByUuid(uuid).orElseThrow().getMinecraftName());
    }

    @Test
    void testPlaceholderDiscordIdNotIndexed() {
        PlayerLinkingManager manager = PlayerLinkingManager.getInstance();
        manager.addLink("none", "aaaaaaaa-1111-1111-1111-111111111111", "InGamePlayerOne", true);
        manager.addLink("none", "bbbbbbbb-2222-2222-2222-222222222222", "InGamePlayerTwo", true);

        // Placeholder ids must not be indexed by discord id, otherwise every
        // in-game-added link would share one map entry and removals would clash.
        assertTrue(manager.getLinkByDiscord("none").isEmpty());
        assertTrue(manager.getLinkByUuid("aaaaaaaa-1111-1111-1111-111111111111").isPresent());
        assertTrue(manager.getLinkByUuid("bbbbbbbb-2222-2222-2222-222222222222").isPresent());

        manager.removeLink("aaaaaaaa-1111-1111-1111-111111111111");
        assertTrue(manager.getLinkByUuid("bbbbbbbb-2222-2222-2222-222222222222").isPresent());
    }
}