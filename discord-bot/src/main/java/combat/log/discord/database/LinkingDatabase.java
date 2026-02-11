package combat.log.discord.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Database for storing Discord <-> Minecraft player links and whitelist requests
 */
public class LinkingDatabase {
    private static final Logger logger = LoggerFactory.getLogger(LinkingDatabase.class);
    private final String databasePath;
    private Connection connection;

    public LinkingDatabase(String databasePath) {
        this.databasePath = databasePath;
        initialize();
    }

    /**
     * Initialize database connection and create tables
     */
    private void initialize() {
        try {
            // Create database directory if needed
            java.io.File dbFile = new java.io.File(databasePath);
            dbFile.getParentFile().mkdirs();

            // Connect to database
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            logger.info("Connected to linking database: {}", databasePath);

            // Enable WAL mode for better concurrency
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
            }

            // Create tables
            createTables();
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Create database tables
     */
    private void createTables() throws SQLException {
        String createLinksTable = """
            CREATE TABLE IF NOT EXISTS whitelist_links (
                discord_id VARCHAR(20) PRIMARY KEY,
                minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
                minecraft_name VARCHAR(16) NOT NULL,
                linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                whitelisted BOOLEAN DEFAULT 1,
                linked_by VARCHAR(20),
                notes TEXT
            )
            """;

        String createRequestsTable = """
            CREATE TABLE IF NOT EXISTS whitelist_requests (
                request_id VARCHAR(36) PRIMARY KEY,
                discord_id VARCHAR(20) NOT NULL,
                discord_username VARCHAR(100) NOT NULL,
                minecraft_name VARCHAR(16) NOT NULL,
                minecraft_uuid VARCHAR(36),
                requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status VARCHAR(20) DEFAULT 'PENDING',
                reviewed_by VARCHAR(20),
                reviewed_at TIMESTAMP,
                reason TEXT,
                thread_id VARCHAR(20)
            )
            """;

        String createLinksUuidIndex = "CREATE INDEX IF NOT EXISTS idx_minecraft_uuid ON whitelist_links(minecraft_uuid)";
        String createLinksNameIndex = "CREATE INDEX IF NOT EXISTS idx_minecraft_name ON whitelist_links(minecraft_name)";
        String createRequestsStatusIndex = "CREATE INDEX IF NOT EXISTS idx_request_status ON whitelist_requests(status)";
        String createRequestsDiscordIndex = "CREATE INDEX IF NOT EXISTS idx_request_discord_id ON whitelist_requests(discord_id)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createLinksTable);
            stmt.execute(createRequestsTable);
            stmt.execute(createLinksUuidIndex);
            stmt.execute(createLinksNameIndex);
            stmt.execute(createRequestsStatusIndex);
            stmt.execute(createRequestsDiscordIndex);
            logger.info("Database tables created/verified");
        }
    }

    /**
     * Store a new Discord <-> Minecraft link
     */
    public void addLink(String discordId, String minecraftUuid, String minecraftName, String linkedBy, String notes) throws SQLException {
        String sql = "INSERT INTO whitelist_links (discord_id, minecraft_uuid, minecraft_name, linked_by, notes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            stmt.setString(2, minecraftUuid);
            stmt.setString(3, minecraftName);
            stmt.setString(4, linkedBy);
            stmt.setString(5, notes);
            stmt.executeUpdate();
            logger.info("Added link: Discord {} <-> Minecraft {} ({})", discordId, minecraftName, minecraftUuid);
        }
    }

    /**
     * Remove a Discord <-> Minecraft link
     * Returns the Minecraft name if link was found and removed, empty otherwise
     */
    public Optional<String> removeLink(String minecraftUuid) throws SQLException {
        // First get the minecraft name before deleting
        String selectSql = "SELECT minecraft_name FROM whitelist_links WHERE minecraft_uuid = ?";
        String minecraftName = null;
        try (PreparedStatement stmt = connection.prepareStatement(selectSql)) {
            stmt.setString(1, minecraftUuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                minecraftName = rs.getString("minecraft_name");
            }
        }

        if (minecraftName != null) {
            // Delete the link
            String deleteSql = "DELETE FROM whitelist_links WHERE minecraft_uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                stmt.setString(1, minecraftUuid);
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    logger.info("Removed link for Minecraft UUID: {} ({})", minecraftUuid, minecraftName);
                    return Optional.of(minecraftName);
                }
            }
        }
        
        return Optional.empty();
    }

    /**
     * Get Discord ID from Minecraft UUID
     */
    public Optional<String> getDiscordId(String minecraftUuid) {
        String sql = "SELECT discord_id FROM whitelist_links WHERE minecraft_uuid = ? AND whitelisted = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftUuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getString("discord_id"));
            }
        } catch (SQLException e) {
            logger.error("Failed to get Discord ID for UUID: {}", minecraftUuid, e);
        }
        return Optional.empty();
    }

    /**
     * Get Minecraft UUID from Discord ID
     */
    public Optional<String> getMinecraftUuid(String discordId) {
        String sql = "SELECT minecraft_uuid FROM whitelist_links WHERE discord_id = ? AND whitelisted = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getString("minecraft_uuid"));
            }
        } catch (SQLException e) {
            logger.error("Failed to get Minecraft UUID for Discord ID: {}", discordId, e);
        }
        return Optional.empty();
    }

    /**
     * Get Minecraft UUID from Minecraft name (case-sensitive match)
     */
    public Optional<String> getMinecraftUuidByName(String minecraftName) {
        String sql = "SELECT minecraft_uuid FROM whitelist_links WHERE minecraft_name = ? AND whitelisted = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getString("minecraft_uuid"));
            }
        } catch (SQLException e) {
            logger.error("Failed to get Minecraft UUID for name: {}", minecraftName, e);
        }
        return Optional.empty();
    }

    /**
     * List entries of currently whitelisted players with their Discord IDs.
     */
    public java.util.List<LinkEntry> listWhitelistedEntries() {
        java.util.List<LinkEntry> entries = new java.util.ArrayList<>();
        String sql = "SELECT minecraft_name, discord_id FROM whitelist_links WHERE whitelisted = 1 ORDER BY linked_at";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                entries.add(new LinkEntry(rs.getString("minecraft_name"), rs.getString("discord_id")));
            }
        } catch (SQLException e) {
            logger.error("Failed to list whitelisted entries", e);
        }
        return entries;
    }

    /**
     * Check if Discord ID is already linked
     */
    public boolean isDiscordLinked(String discordId) {
        String sql = "SELECT 1 FROM whitelist_links WHERE discord_id = ? AND whitelisted = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Failed to check Discord link status", e);
            return false;
        }
    }

    /**
     * Check if Minecraft UUID is already linked
     */
    public boolean isMinecraftLinked(String minecraftUuid) {
        String sql = "SELECT 1 FROM whitelist_links WHERE minecraft_uuid = ? AND whitelisted = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftUuid);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Failed to check Minecraft link status", e);
            return false;
        }
    }

    /**
     * Create a new whitelist request
     */
    public void createRequest(String requestId, String discordId, String discordUsername, String minecraftName, String minecraftUuid, String threadId) throws SQLException {
        String sql = "INSERT INTO whitelist_requests (request_id, discord_id, discord_username, minecraft_name, minecraft_uuid, thread_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            stmt.setString(2, discordId);
            stmt.setString(3, discordUsername);
            stmt.setString(4, minecraftName);
            stmt.setString(5, minecraftUuid);
            stmt.setString(6, threadId);
            stmt.executeUpdate();
            logger.info("Created whitelist request: {} for {} ({})", requestId, minecraftName, discordUsername);
        }
    }

    /**
     * Update request status
     */
    public void updateRequestStatus(String requestId, String status, String reviewedBy, String reason) throws SQLException {
        String sql = "UPDATE whitelist_requests SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, reason = ? WHERE request_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, reviewedBy);
            stmt.setString(3, reason);
            stmt.setString(4, requestId);
            stmt.executeUpdate();
            logger.info("Updated request {} to status: {}", requestId, status);
        }
    }

    /**
     * Check if there's already a pending request for this Discord user
     */
    public boolean hasPendingRequest(String discordId) {
        String sql = "SELECT 1 FROM whitelist_requests WHERE discord_id = ? AND status = 'PENDING'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Failed to check pending request", e);
            return false;
        }
    }

    /**
     * Get request by ID
     */
    public Optional<WhitelistRequestData> getRequest(String requestId) {
        String sql = "SELECT * FROM whitelist_requests WHERE request_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new WhitelistRequestData(
                    rs.getString("request_id"),
                    rs.getString("discord_id"),
                    rs.getString("discord_username"),
                    rs.getString("minecraft_name"),
                    rs.getString("minecraft_uuid"),
                    rs.getString("status"),
                    rs.getString("thread_id")
                ));
            }
        } catch (SQLException e) {
            logger.error("Failed to get request", e);
        }
        return Optional.empty();
    }

    /**
     * Simple DTO for a whitelist link entry
     */
    public static class LinkEntry {
        public final String minecraftName;
        public final String discordId;

        public LinkEntry(String minecraftName, String discordId) {
            this.minecraftName = minecraftName;
            this.discordId = discordId;
        }
    }

    /**
     * Close the database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection", e);
        }
    }

    /**
     * Data class for whitelist request
     */
    public static class WhitelistRequestData {
        public final String requestId;
        public final String discordId;
        public final String discordUsername;
        public final String minecraftName;
        public final String minecraftUuid;
        public final String status;
        public final String threadId;

        public WhitelistRequestData(String requestId, String discordId, String discordUsername, 
                                    String minecraftName, String minecraftUuid, String status, String threadId) {
            this.requestId = requestId;
            this.discordId = discordId;
            this.discordUsername = discordUsername;
            this.minecraftName = minecraftName;
            this.minecraftUuid = minecraftUuid;
            this.status = status;
            this.threadId = threadId;
        }
    }
}
