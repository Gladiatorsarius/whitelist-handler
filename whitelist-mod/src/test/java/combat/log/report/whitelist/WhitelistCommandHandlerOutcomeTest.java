package whitelisting.swordsmp.whitelist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Outcome semantics for whitelist add/remove verification.
 * The verification helper returns null when the state cannot be verified
 * (exception, invalid UUID); unknown must be treated as failure, never as absence.
 */
class WhitelistCommandHandlerOutcomeTest {

    @Test
    void testAddOutcome() {
        assertTrue(WhitelistCommandHandler.addSucceeded(Boolean.TRUE));
        assertFalse(WhitelistCommandHandler.addSucceeded(Boolean.FALSE));
        assertFalse(WhitelistCommandHandler.addSucceeded(null)); // unknown is not success
    }

    @Test
    void testRemoveOutcome() {
        assertTrue(WhitelistCommandHandler.removeSucceeded(Boolean.FALSE));
        assertFalse(WhitelistCommandHandler.removeSucceeded(Boolean.TRUE));
        assertFalse(WhitelistCommandHandler.removeSucceeded(null)); // unknown is not confirmed absence
    }
}
