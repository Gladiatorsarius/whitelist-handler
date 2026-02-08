package combat.log.discord.api;

import java.util.Optional;

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
