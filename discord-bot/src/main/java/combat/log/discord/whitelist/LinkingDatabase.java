package combat.log.discord.whitelist;

import java.sql.*;
import java.util.Optional;

/**
 * Small local SQLite wrapper copied into whitelist-handler as a compatibility shim.
 * This keeps existing behavior until server-side authority is enabled.
 */
public class LinkingDatabase {
    private final String url;

    public LinkingDatabase(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void addLink(String discordId, String uuid) throws SQLException {
        try (Connection c = conn()) {
            c.setAutoCommit(true);
            try (PreparedStatement s = c.prepareStatement("INSERT INTO links(discord_id, uuid) VALUES(?,?)")) {
                s.setString(1, discordId);
                s.setString(2, uuid);
                s.executeUpdate();
            }
        }
    }

    public Optional<String> getMinecraftUuid(String discordId) throws SQLException {
        try (Connection c = conn(); PreparedStatement s = c.prepareStatement("SELECT uuid FROM links WHERE discord_id=? LIMIT 1")) {
            s.setString(1, discordId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString("uuid"));
            }
        }
        return Optional.empty();
    }
}
