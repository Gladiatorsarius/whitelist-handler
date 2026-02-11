package combat.log.discord.models;

import com.google.gson.annotations.SerializedName;

/**
 * Decision message sent to Minecraft server
 */
public class IncidentDecision extends SocketMessage {
    @SerializedName("incidentId")
    private String incidentId;
    
    @SerializedName("status")
    private String status; // APPROVED, DENIED, AUTO_DENIED
    
    @SerializedName("adminName")
    private String adminName;
    
    @SerializedName("reason")
    private String reason;

    public IncidentDecision(String incidentId, String status, String adminName, String reason) {
        this.setType("incident_decision");
        this.setTimestamp(System.currentTimeMillis());
        this.incidentId = incidentId;
        this.status = status;
        this.adminName = adminName;
        this.reason = reason;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getStatus() {
        return status;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getReason() {
        return reason;
    }
}
