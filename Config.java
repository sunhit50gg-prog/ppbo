package com.ppba.plugin.config;

import com.ppba.plugin.PPBAPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Manages plugin configuration from config.yml
 */
public class Config {
    
    private final PPBAPlugin plugin;
    private final File configFile;
    private YamlConfiguration config;
    
    // Configuration values
    private String databaseType;
    private String sqlitePath;
    private int maxChatHistory;
    
    private String apiProvider;
    private String apiKey;
    private String apiModel;
    private int timeoutSeconds;
    
    private int minDataPoints;
    private boolean enableAsync;
    private String webhookUrl;
    
    private boolean maskPlayerNames;
    private boolean logToFile;
    private int retentionDays;
    
    public Config(PPBAPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * Load configuration from file
     */
    public void load() throws Exception {
        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        // Load YAML configuration
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Parse values
        parseConfiguration();
        
        plugin.getLogger().info("Configuration loaded from: " + configFile.getAbsolutePath());
    }
    
    /**
     * Create default config.yml from template
     */
    private void createDefaultConfig() throws Exception {
        // Copy default config from resources
        InputStream inputStream = plugin.getResource("config.yml");
        
        if (inputStream != null) {
            Files.copy(inputStream, configFile.toPath());
            plugin.getLogger().info("Default config.yml created");
        } else {
            // Fallback: create basic config
            String defaultConfig = """
# PPBA Plugin Configuration
# Minecraft Paper 1.21.1

database:
  type: "sqlite"
  sqlite-path: "plugins/PPBA/data.db"
  max-chat-history: 100

api:
  provider: "claude"
  api-key: "YOUR_API_KEY_HERE"
  model: "claude-sonnet-4-6"
  timeout-seconds: 30

analysis:
  min-data-points: 10
  enable-async: true
  webhook-url: ""

privacy:
  mask-player-names-in-logs: false
  log-reports-to-file: true
  retention-days: 30
""";
            Files.writeString(configFile.toPath(), defaultConfig);
            plugin.getLogger().info("Basic config.yml created - please update with API key");
        }
    }
    
    /**
     * Parse configuration values from YAML
     */
    private void parseConfiguration() {
        // Database config
        databaseType = config.getString("database.type", "sqlite");
        sqlitePath = config.getString("database.sqlite-path", "plugins/PPBA/data.db");
        maxChatHistory = config.getInt("database.max-chat-history", 100);
        
        // API config
        apiProvider = config.getString("api.provider", "claude");
        apiKey = config.getString("api.api-key", "");
        apiModel = config.getString("api.model", "claude-sonnet-4-6");
        timeoutSeconds = config.getInt("api.timeout-seconds", 30);
        
        // Analysis config
        minDataPoints = config.getInt("analysis.min-data-points", 10);
        enableAsync = config.getBoolean("analysis.enable-async", true);
        webhookUrl = config.getString("analysis.webhook-url", "");
        
        // Privacy config
        maskPlayerNames = config.getBoolean("privacy.mask-player-names-in-logs", false);
        logToFile = config.getBoolean("privacy.log-reports-to-file", true);
        retentionDays = config.getInt("privacy.retention-days", 30);
        
        // Validate configuration
        validateConfig();
    }
    
    /**
     * Validate configuration values
     */
    private void validateConfig() {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            plugin.getLogger().warning("⚠ Claude API key not configured!");
            plugin.getLogger().warning("⚠ Set 'api.api-key' in config.yml to enable psychological analysis");
        }
        
        if (maxChatHistory < 5) {
            plugin.getLogger().warning("⚠ max-chat-history is too low (minimum 5)");
            maxChatHistory = 5;
        }
        
        if (timeoutSeconds < 10) {
            plugin.getLogger().warning("⚠ API timeout is too low (minimum 10s)");
            timeoutSeconds = 10;
        }
        
        if (minDataPoints < 1) {
            plugin.getLogger().warning("⚠ min-data-points is too low (minimum 1)");
            minDataPoints = 1;
        }
    }
    
    // ===== GETTERS =====
    
    public String getDatabaseType() { return databaseType; }
    public String getSqlitePath() { return sqlitePath; }
    public int getMaxChatHistory() { return maxChatHistory; }
    
    public String getApiProvider() { return apiProvider; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return apiModel; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    
    public int getMinDataPoints() { return minDataPoints; }
    public boolean isAsyncEnabled() { return enableAsync; }
    public String getWebhookUrl() { return webhookUrl; }
    
    public boolean isMaskPlayerNames() { return maskPlayerNames; }
    public boolean isLogToFile() { return logToFile; }
    public int getRetentionDays() { return retentionDays; }
}
