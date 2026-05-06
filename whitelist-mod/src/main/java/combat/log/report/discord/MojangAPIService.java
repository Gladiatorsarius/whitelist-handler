package whitelisting.swordsmp.discord;

import java.util.Optional;

public class MojangAPIService {
    public boolean isValidUsernameFormat(String name) {
        if (name == null) return false;
        return name.matches("[A-Za-z0-9_]{3,16}");
    }

    public Optional<MojangProfile> getProfile(String name) {
        // Minimal stub: return a profile with a stable UUID for compilation/testing.
        String uuid = java.util.UUID.nameUUIDFromBytes(name.getBytes()).toString().replaceAll("-", "");
        return Optional.of(new MojangProfile(uuid, name));
    }
}

class MojangProfile {
    private final String id;
    private final String name;

    MojangProfile(String id, String name) { this.id = id; this.name = name; }
    public String getFormattedUuid() { return id; }
    public String getName() { return name; }
}
