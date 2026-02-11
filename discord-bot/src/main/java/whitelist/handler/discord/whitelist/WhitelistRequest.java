package whitelist.handler.discord.whitelist;

import java.util.UUID;

/**
 * Represents a whitelist request
 */
public class WhitelistRequest {
    private final String requestId;
    private final String discordId;
    private final String discordUsername;
    private final String minecraftName;
    private final String minecraftUuid;
    private final long requestedAt;
    private String status;
    private String threadId;

    public WhitelistRequest(String discordId, String discordUsername, String minecraftName, String minecraftUuid) {
        this.requestId = UUID.randomUUID().toString();
        this.discordId = discordId;
        this.discordUsername = discordUsername;
        this.minecraftName = minecraftName;
        this.minecraftUuid = minecraftUuid;
        this.requestedAt = System.currentTimeMillis();
        this.status = "PENDING";
    }

    public String getRequestId() {
        return requestId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getDiscordUsername() {
        return discordUsername;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public String getMinecraftUuid() {
        return minecraftUuid;
    }

    public long getRequestedAt() {
        return requestedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
}
