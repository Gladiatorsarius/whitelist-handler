package whitelist.handler.discord.models;

import java.util.List;

/**
 * Response containing the full whitelist entries from the Minecraft server or DB
 */
public class WhitelistListResponse extends SocketMessage {
    private List<Entry> entries;

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public static class Entry {
        private String minecraftName;
        private String discordId;

        public String getMinecraftName() {
            return minecraftName;
        }

        public void setMinecraftName(String minecraftName) {
            this.minecraftName = minecraftName;
        }

        public String getDiscordId() {
            return discordId;
        }

        public void setDiscordId(String discordId) {
            this.discordId = discordId;
        }
    }
}
