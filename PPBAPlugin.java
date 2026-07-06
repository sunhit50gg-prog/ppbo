package com.ppba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import com.ppba.plugin.commands.PPBACommand;
import com.ppba.plugin.listeners.ChatListener;
import com.ppba.plugin.listeners.CombatListener;
import com.ppba.plugin.data.DataManager;
import com.ppba.plugin.api.ClaudeAPIClient;
import com.ppba.plugin.config.Config;

public class PPBAPlugin extends JavaPlugin {
    
    private static PPBAPlugin instance;
    private DataManager dataManager;
    private ClaudeAPIClient apiClient;
    private Config config;
    
    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();
        
        getLogger().info("═══════════════════════════════════════════");
        getLogger().info("PPBA Plugin v2.1.0 - Starting Initialization");
        getLogger().info("Target: Minecraft Paper 1.21.1");
        getLogger().info("═══════════════════════════════════════════");
        
        // 1. Load Configuration
        try {
            config = new Config(this);
            config.load();
            getLogger().info("✓ Configuration loaded successfully");
        } catch (Exception e) {
            getLogger().severe("✗ Failed to load configuration!");
            e.printStackTrace();
            setEnabled(false);
            return;
        }
        
        // 2. Initialize Database
        try {
            dataManager = new DataManager(this, config);
            dataManager.initialize();
            getLogger().info("✓ Database initialized (SQLite)");
        } catch (Exception e) {
            getLogger().severe("✗ Database initialization failed!");
            e.printStackTrace();
            setEnabled(false);
            return;
        }
        
        // 3. Initialize API Client
        try {
            apiClient = new ClaudeAPIClient(this, config);
            getLogger().info("✓ Claude API client configured");
        } catch (Exception e) {
            getLogger().severe("✗ API client initialization failed!");
            e.printStackTrace();
            setEnabled(false);
            return;
        }
        
        // 4. Register Commands
        try {
            getCommand("ppba").setExecutor(new PPBACommand(this));
            getLogger().info("✓ Commands registered (/ppba <player> report)");
        } catch (Exception e) {
            getLogger().severe("✗ Command registration failed!");
            e.printStackTrace();
            setEnabled(false);
            return;
        }
        
        // 5. Register Event Listeners
        try {
            Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
            Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
            getLogger().info("✓ Event listeners registered");
        } catch (Exception e) {
            getLogger().severe("✗ Event listener registration failed!");
            e.printStackTrace();
            setEnabled(false);
            return;
        }
        
        // 6. Start Scheduled Tasks (Optional: cleanup old data)
        scheduleMaintenanceTasks();
        
        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("═══════════════════════════════════════════");
        getLogger().info("✓ PPBA Plugin loaded successfully!");
        getLogger().info("Load time: " + loadTime + "ms");
        getLogger().info("═══════════════════════════════════════════");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("PPBA Plugin shutting down...");
        if (dataManager != null) {
            try {
                dataManager.shutdown();
                getLogger().info("✓ Database connection closed");
            } catch (Exception e) {
                getLogger().warning("Error closing database connection");
                e.printStackTrace();
            }
        }
        getLogger().info("✓ PPBA Plugin disabled");
    }
    
    private void scheduleMaintenanceTasks() {
        // Cleanup old chat logs every 1 hour (based on retention-days config)
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            try {
                if (dataManager != null) {
                    dataManager.cleanupOldLogs(config.getRetentionDays());
                }
            } catch (Exception e) {
                getLogger().warning("Error during maintenance cleanup: " + e.getMessage());
            }
        }, 20L * 60 * 60, 20L * 60 * 60); // 1 hour delay, 1 hour repeat
    }
    
    // ===== STATIC ACCESSORS =====
    public static PPBAPlugin getInstance() {
        return instance;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public ClaudeAPIClient getApiClient() {
        return apiClient;
    }
    
    public Config getPluginConfig() {
        return config;
    }
}
