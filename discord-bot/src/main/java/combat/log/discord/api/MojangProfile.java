package combat.log.discord.api;

public class MojangProfile {
    private final String id;
    private final String name;

    public MojangProfile(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getFormattedUuid() { return id; }
    public String getName() { return name; }
}
