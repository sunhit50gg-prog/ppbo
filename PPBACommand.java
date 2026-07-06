package com.ppba.plugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.ppba.plugin.PPBAPlugin;
import com.ppba.plugin.data.PlayerProfile;
import com.ppba.plugin.formatting.ReportFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PPBACommand implements CommandExecutor {
    
    private final PPBAPlugin plugin;
    
    public PPBACommand(PPBAPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("ppba.admin.report")) {
            sender.sendMessage(Component.text("✗ You don't have permission to use this command")
                    .color(NamedTextColor.RED));
            return true;
        }
        
        // Syntax validation
        if (args.length < 2 || !args[1].equalsIgnoreCase("report")) {
            sender.sendMessage(Component.text("Usage: /ppba <player_name> report")
                    .color(NamedTextColor.YELLOW));
            return true;
        }
        
        String targetPlayerName = args[0];
        
        // Check if player exists in database
        if (!plugin.getDataManager().playerExists(targetPlayerName)) {
            sender.sendMessage(Component.text("✗ Player '" + targetPlayerName + "' not found in database")
                    .color(NamedTextColor.RED));
            return true;
        }
        
        // Fetch player data
        PlayerProfile playerProfile = null;
        try {
            playerProfile = plugin.getDataManager().getPlayerProfile(targetPlayerName);
        } catch (Exception e) {
            sender.sendMessage(Component.text("✗ Database error: " + e.getMessage())
                    .color(NamedTextColor.RED));
            plugin.getLogger().warning("Error fetching player profile: " + e.getMessage());
            return true;
        }
        
        // Check minimum data points for analysis
        int dataPoints = playerProfile.getChatHistory().size();
        int minDataPoints = plugin.getPluginConfig().getMinDataPoints();
        
        if (dataPoints < minDataPoints) {
            sender.sendMessage(Component.text("✗ Insufficient data for analysis")
                    .color(NamedTextColor.RED));
            sender.sendMessage(Component.text("Need: " + minDataPoints + " messages | Have: " + dataPoints)
                    .color(NamedTextColor.GRAY));
            return true;
        }
        
        // Send processing message
        sender.sendMessage(Component.text("⟳ Analyzing player data...")
                .color(NamedTextColor.BLUE));
        
        // Execute asynchronously to prevent server lag
        if (plugin.getPluginConfig().isAsyncEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                processAnalysisAsync(sender, playerProfile, targetPlayerName);
            });
        } else {
            processAnalysisAsync(sender, playerProfile, targetPlayerName);
        }
        
        return true;
    }
    
    /**
     * Async analysis processing - calls Claude API and formats output
     */
    private void processAnalysisAsync(CommandSender sender, PlayerProfile playerProfile, String playerName) {
        try {
            // 1. Prepare JSON payload with player data
            String jsonPayload = buildPlayerDataJSON(playerProfile);
            
            // 2. Call Claude API with system prompt
            String psychologicalReport = plugin.getApiClient().analyzeBehavior(jsonPayload);
            
            if (psychologicalReport == null || psychologicalReport.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(Component.text("✗ API returned empty response")
                            .color(NamedTextColor.RED));
                });
                return;
            }
            
            // 3. Format and send report back to command sender (sync)
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    ReportFormatter formatter = new ReportFormatter(playerName, psychologicalReport);
                    formatter.sendToCommandSender(sender);
                    
                    // Log to file if enabled
                    if (plugin.getPluginConfig().isLogToFile()) {
                        plugin.getDataManager().logReport(playerName, psychologicalReport);
                    }
                    
                } catch (Exception e) {
                    sender.sendMessage(Component.text("✗ Error formatting report: " + e.getMessage())
                            .color(NamedTextColor.RED));
                    plugin.getLogger().warning("Report formatting error: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(Component.text("✗ Analysis failed: " + e.getMessage())
                        .color(NamedTextColor.RED));
            });
            plugin.getLogger().warning("Analysis error for player " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Build JSON payload from PlayerProfile for API submission
     */
    private String buildPlayerDataJSON(PlayerProfile profile) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"player_name\": \"").append(escapeJSON(profile.getPlayerName())).append("\",\n");
        json.append("  \"uuid\": \"").append(profile.getUUID()).append("\",\n");
        json.append("  \"first_seen\": \"").append(profile.getFirstSeen()).append("\",\n");
        json.append("  \"last_seen\": \"").append(profile.getLastSeen()).append("\",\n");
        json.append("  \"total_playtime_hours\": ").append(profile.getTotalPlaytimeHours()).append(",\n");
        json.append("  \"chat_history\": [\n");
        
        // Add chat messages
        profile.getChatHistory().forEach(msg -> {
            json.append("    {\n");
            json.append("      \"timestamp\": \"").append(msg.getTimestamp()).append("\",\n");
            json.append("      \"message\": \"").append(escapeJSON(msg.getMessage())).append("\",\n");
            json.append("      \"context\": \"").append(msg.getContext()).append("\"\n");
            json.append("    },\n");
        });
        
        // Remove trailing comma
        if (profile.getChatHistory().size() > 0) {
            json.setLength(json.length() - 2);
            json.append("\n");
        }
        
        json.append("  ],\n");
        json.append("  \"combat_metrics\": {\n");
        json.append("    \"kills\": ").append(profile.getKills()).append(",\n");
        json.append("    \"deaths\": ").append(profile.getDeaths()).append(",\n");
        json.append("    \"new_player_kills\": ").append(profile.getNewPlayerKills()).append(",\n");
        json.append("    \"unarmed_kills\": ").append(profile.getUnarmedKills()).append(",\n");
        json.append("    \"pvp_zone_kills\": ").append(profile.getPvpZoneKills()).append("\n");
        json.append("  }\n");
        json.append("}\n");
        
        return json.toString();
    }
    
    /**
     * Escape special JSON characters
     */
    private String escapeJSON(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
