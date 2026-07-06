package com.ppba.plugin.formatting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/**
 * Formats psychological analysis reports for display in Minecraft chat
 * Uses Adventure API for color and formatting
 */
public class ReportFormatter {
    
    private final String playerName;
    private final String rawReport;
    
    public ReportFormatter(String playerName, String rawReport) {
        this.playerName = playerName;
        this.rawReport = rawReport;
    }
    
    /**
     * Send formatted report to command sender in game chat
     */
    public void sendToCommandSender(CommandSender sender) {
        sender.sendMessage(formatReport());
    }
    
    /**
     * Build and format the complete report
     */
    private Component formatReport() {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("\n");
        sb.append("═══════════════════════════════════════════\n");
        sb.append("PLAYER PSYCHOLOGICAL PROFILE\n");
        sb.append("Player: ").append(playerName).append("\n");
        sb.append("═══════════════════════════════════════════\n");
        
        // Parse raw report sections
        String[] sections = rawReport.split("\n");
        
        for (String line : sections) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty()) continue;
            
            // Section headers
            if (trimmed.startsWith("[PLAYER PSYCHOLOGICAL PROFILE")) {
                sb.append("\n").append(colorize("§c" + trimmed + "§r")).append("\n");
            }
            // Primary Type
            else if (trimmed.startsWith("Primary Type:")) {
                sb.append(colorize("§6" + trimmed + "§r")).append("\n");
            }
            // Axis Scores
            else if (trimmed.startsWith("Axis Scores:")) {
                sb.append(colorize("§9" + trimmed + "§r")).append("\n");
            }
            // Confidence Score
            else if (trimmed.startsWith("Confidence Score:")) {
                sb.append(colorize("§a" + trimmed + "§r")).append("\n");
            }
            // Analysis sections
            else if (trimmed.startsWith("Deep Behavioral Analysis:") || 
                     trimmed.startsWith("Plugin Backend Recommendation:")) {
                sb.append("\n").append(colorize("§d" + trimmed + "§r")).append("\n");
            }
            // Trigger points (in red if predatory)
            else if (trimmed.contains("predatory") || trimmed.contains("toxic")) {
                sb.append(colorize("§c" + trimmed + "§r")).append("\n");
            }
            // Altruistic indicators (in green)
            else if (trimmed.contains("Altruistic") || trimmed.contains("Guardian") || 
                     trimmed.contains("help")) {
                sb.append(colorize("§a" + trimmed + "§r")).append("\n");
            }
            // Standard content
            else {
                sb.append(colorize(trimmed)).append("\n");
            }
        }
        
        sb.append("═══════════════════════════════════════════\n");
        
        return Component.text(sb.toString()).color(NamedTextColor.WHITE);
    }
    
    /**
     * Apply legacy color codes (§ format)
     */
    private String colorize(String text) {
        if (text == null) return "";
        
        // Convert legacy color codes
        return text
                .replace("&0", "§0")
                .replace("&1", "§1")
                .replace("&2", "§2")
                .replace("&3", "§3")
                .replace("&4", "§4")
                .replace("&5", "§5")
                .replace("&6", "§6")
                .replace("&7", "§7")
                .replace("&8", "§8")
                .replace("&9", "§9")
                .replace("&a", "§a")
                .replace("&b", "§b")
                .replace("&c", "§c")
                .replace("&d", "§d")
                .replace("&e", "§e")
                .replace("&f", "§f")
                .replace("&l", "§l")
                .replace("&o", "§o")
                .replace("&n", "§n")
                .replace("&m", "§m")
                .replace("&k", "§k")
                .replace("&r", "§r");
    }
    
    /**
     * Extract specific metric from raw report
     */
    private String extractMetric(String key) {
        int index = rawReport.indexOf(key);
        if (index == -1) return "N/A";
        
        int endIndex = rawReport.indexOf("\n", index);
        if (endIndex == -1) endIndex = rawReport.length();
        
        return rawReport.substring(index, endIndex).trim();
    }
    
    /**
     * Get confidence score from report
     */
    public int getConfidenceScore() {
        String confidenceLine = extractMetric("Confidence Score:");
        try {
            return Integer.parseInt(confidenceLine.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Get primary personality type
     */
    public String getPrimaryType() {
        return extractMetric("Primary Type:");
    }
    
    /**
     * Export report to plain text format for file logging
     */
    public String exportPlainText() {
        return "=== PLAYER PSYCHOLOGICAL PROFILE ===\n" +
               "Player: " + playerName + "\n" +
               "Generated: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n" +
               "==========================================\n\n" +
               rawReport + "\n\n" +
               "==========================================\n";
    }
}
