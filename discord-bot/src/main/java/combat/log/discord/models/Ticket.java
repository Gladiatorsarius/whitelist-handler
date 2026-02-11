package combat.log.discord.models;

import java.time.Instant;

/**
 * Represents an active combat log ticket
 */
public class Ticket {
    private final String incidentId;
    private final String playerUuid;
    private final String playerName;
    private final double combatTimeRemaining;
    private final Instant createdAt;
    private final String channelId; // Discord channel/thread ID
    
    private Instant expiresAt;
    private String clipUrl;
    private Instant clipSubmittedAt;
    private TicketStatus status;

    public enum TicketStatus {
        PENDING,
        CLIP_UPLOADED,
        APPROVED,
        DENIED,
        AUTO_DENIED,
        EXTENDED
    }

    public Ticket(String incidentId, String playerUuid, String playerName, 
                  double combatTimeRemaining, String channelId, long timeoutMinutes) {
        this.incidentId = incidentId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.combatTimeRemaining = combatTimeRemaining;
        this.channelId = channelId;
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(timeoutMinutes * 60);
        this.status = TicketStatus.PENDING;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public double getCombatTimeRemaining() {
        return combatTimeRemaining;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getChannelId() {
        return channelId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getClipUrl() {
        return clipUrl;
    }

    public void setClipUrl(String clipUrl) {
        this.clipUrl = clipUrl;
        this.clipSubmittedAt = Instant.now();
        this.status = TicketStatus.CLIP_UPLOADED;
    }

    public Instant getClipSubmittedAt() {
        return clipSubmittedAt;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public long getSecondsRemaining() {
        long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, seconds);
    }
}
