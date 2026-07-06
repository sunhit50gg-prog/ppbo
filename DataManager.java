package com.ppba.plugin.data;

import com.ppba.plugin.PPBAPlugin;
import com.ppba.plugin.config.Config;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * Manages all database operations for PPBA - uses SQLite
 */
public class DataManager {
    
    private final PPBAPlugin plugin;
    private final Config config;
    private Connection connection;
    private final String dbPath;
    
    public DataManager(PPBAPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.dbPath = plugin.getDataFolder() + File.separator + "data.db";
    }
    
    /**
     * Initialize database and create tables if they don't exist
     */
    public void initialize() throws SQLException {
        // Create data folder if it doesn't exist
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Load SQLite driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        
        // Create connection
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        plugin.getLogger().info("SQLite connection established at: " + dbPath);
        
        // Create tables
        createTables();
    }
    
    /**
     * Create database tables
     */
    private void createTables() throws SQLException {
        String[] tables = {
            // Players table
            "CREATE TABLE IF NOT EXISTS players (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  player_name TEXT NOT NULL UNIQUE," +
            "  uuid TEXT NOT NULL UNIQUE," +
            "  first_seen INTEGER NOT NULL," +
            "  last_seen INTEGER NOT NULL," +
            "  playtime_seconds INTEGER DEFAULT 0" +
            ")",
            
            // Chat history table
            "CREATE TABLE IF NOT EXISTS chat_logs (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  player_name TEXT NOT NULL," +
            "  uuid TEXT NOT NULL," +
            "  message TEXT NOT NULL," +
            "  context TEXT," +
            "  timestamp INTEGER NOT NULL," +
            "  FOREIGN KEY(player_name) REFERENCES players(player_name)" +
            ")",
            
            // Combat metrics table
            "CREATE TABLE IF NOT EXISTS combat_metrics (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  killer_name TEXT NOT NULL," +
            "  killer_uuid TEXT NOT NULL," +
            "  victim_name TEXT," +
            "  kill_type TEXT," +
            "  fair_context INTEGER DEFAULT 0," +
            "  timestamp INTEGER NOT NULL," +
            "  FOREIGN KEY(killer_name) REFERENCES players(player_name)" +
            ")",
            
            // Reports table
            "CREATE TABLE IF NOT EXISTS reports (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  player_name TEXT NOT NULL," +
            "  report_content TEXT NOT NULL," +
            "  timestamp INTEGER NOT NULL" +
            ")",
            
            // Create indexes for performance
            "CREATE INDEX IF NOT EXISTS idx_chat_player ON chat_logs(player_name)",
            "CREATE INDEX IF NOT EXISTS idx_combat_killer ON combat_metrics(killer_name)",
            "CREATE INDEX IF NOT EXISTS idx_timestamp ON chat_logs(timestamp)"
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String table : tables) {
                stmt.execute(table);
            }
        }
        plugin.getLogger().info("Database tables initialized successfully");
    }
    
    /**
     * Initialize or update player profile
     */
    public void initializeOrUpdatePlayer(Player player) throws SQLException {
        String playerName = player.getName();
        String uuid = player.getUniqueId().toString();
        long now = System.currentTimeMillis();
        
        String checkSQL = "SELECT id FROM players WHERE player_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(checkSQL)) {
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Update existing player
                String updateSQL = "UPDATE players SET last_seen = ? WHERE player_name = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                    updateStmt.setLong(1, now);
                    updateStmt.setString(2, playerName);
                    updateStmt.executeUpdate();
                }
            } else {
                // Create new player
                String insertSQL = "INSERT INTO players (player_name, uuid, first_seen, last_seen) VALUES (?, ?, ?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {
                    insertStmt.setString(1, playerName);
                    insertStmt.setString(2, uuid);
                    insertStmt.setLong(3, now);
                    insertStmt.setLong(4, now);
                    insertStmt.executeUpdate();
                }
            }
        }
    }
    
    /**
     * Log a chat message
     */
    public void logChatMessage(String playerName, String uuid, String message, String context) throws SQLException {
        String sql = "INSERT INTO chat_logs (player_name, uuid, message, context, timestamp) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            stmt.setString(2, uuid);
            stmt.setString(3, message);
            stmt.setString(4, context);
            stmt.setLong(5, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
    
    /**
     * Log a kill
     */
    public void logKill(String killerName, String killerUUID, String victimName, Object context) throws SQLException {
        String killType = "standard";
        boolean fairContext = true;
        
        if (context instanceof com.ppba.plugin.listeners.CombatListener.KillContext) {
            com.ppba.plugin.listeners.CombatListener.KillContext ctx = 
                    (com.ppba.plugin.listeners.CombatListener.KillContext) context;
            killType = ctx.type;
            fairContext = ctx.fairContext;
        }
        
        String sql = "INSERT INTO combat_metrics (killer_name, killer_uuid, victim_name, kill_type, fair_context, timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, killerName);
            stmt.setString(2, killerUUID);
            stmt.setString(3, victimName);
            stmt.setString(4, killType);
            stmt.setInt(5, fairContext ? 1 : 0);
            stmt.setLong(6, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
    
    /**
     * Log a death
     */
    public void logDeath(String playerName, String uuid, String killerName) throws SQLException {
        // Deaths are implicitly tracked via combat_metrics. This is for explicit death logging if needed.
    }
    
    /**
     * Check if player exists in database
     */
    public boolean playerExists(String playerName) throws SQLException {
        String sql = "SELECT id FROM players WHERE player_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }
    
    /**
     * Get player profile with all data
     */
    public PlayerProfile getPlayerProfile(String playerName) throws SQLException {
        // Get basic player info
        String playerSQL = "SELECT uuid, first_seen, last_seen, playtime_seconds FROM players WHERE player_name = ?";
        PlayerProfile profile = null;
        
        try (PreparedStatement stmt = connection.prepareStatement(playerSQL)) {
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                profile = new PlayerProfile(playerName, rs.getString("uuid"));
                profile.setLastSeen(rs.getLong("last_seen"));
                profile.setTotalPlaytimeSeconds(rs.getLong("playtime_seconds"));
            }
        }
        
        if (profile == null) {
            throw new SQLException("Player not found: " + playerName);
        }
        
        // Get chat history
        String chatSQL = "SELECT message, context, timestamp FROM chat_logs WHERE player_name = ? ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement stmt = connection.prepareStatement(chatSQL)) {
            stmt.setString(1, playerName);
            stmt.setInt(2, config.getMaxChatHistory());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ChatMessage msg = new ChatMessage(rs.getString("message"), rs.getString("context"));
                profile.addChatMessage(msg);
            }
        }
        
        // Get combat metrics
        String combatSQL = "SELECT COUNT(*) as total, " +
                          "SUM(CASE WHEN fair_context = 1 THEN 1 ELSE 0 END) as fair_kills, " +
                          "SUM(CASE WHEN kill_type = 'predatory' THEN 1 ELSE 0 END) as predatory_kills " +
                          "FROM combat_metrics WHERE killer_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(combatSQL)) {
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int totalKills = rs.getInt("total");
                int fairKills = rs.getInt("fair_kills");
                int predatoryKills = rs.getInt("predatory_kills");
                
                for (int i = 0; i < totalKills; i++) {
                    profile.incrementKills();
                }
                // Additional metrics can be set here
            }
        }
        
        return profile;
    }
    
    /**
     * Log a report to the reports table
     */
    public void logReport(String playerName, String reportContent) throws SQLException {
        String sql = "INSERT INTO reports (player_name, report_content, timestamp) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            stmt.setString(2, reportContent);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        }
    }
    
    /**
     * Clean up old chat logs based on retention days
     */
    public void cleanupOldLogs(int retentionDays) throws SQLException {
        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L);
        String sql = "DELETE FROM chat_logs WHERE timestamp < ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, cutoffTime);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                plugin.getLogger().fine("Cleaned up " + deleted + " old chat logs");
            }
        }
    }
    
    /**
     * Shutdown and close database connection
     */
    public void shutdown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            plugin.getLogger().info("Database connection closed");
        }
    }
}
