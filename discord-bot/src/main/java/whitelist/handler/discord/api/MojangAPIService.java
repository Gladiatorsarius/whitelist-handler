package whitelist.handler.discord.api;

import java.util.Optional;

/**
 * Minimal Mojang API service stub for compilation/testing.
 * In production this should call Mojang's APIs, but a simple deterministic UUID generator is sufficient here.
 */
public class MojangAPIService {
    public boolean isValidUsernameFormat(String name) {
        if (name == null) return false;
        return name.matches("[A-Za-z0-9_]{3,16}");
    }

    public Optional<MojangProfile> getProfile(String name) {
        // Minimal stub: return a profile with a fake UUID for compilation/testing; in production use real Mojang API.
        String uuid = java.util.UUID.nameUUIDFromBytes(name.getBytes()).toString().replaceAll("-", "");
        return Optional.of(new MojangProfile(uuid, name));
    }
}
