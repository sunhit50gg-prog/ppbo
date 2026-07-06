package com.ppba.plugin.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player's behavioral profile with chat history and combat metrics
 */
public class PlayerProfile {
    
    private String playerName;
    private String uuid;
    private long firstSeen;
    private long lastSeen;
    private long totalPlaytimeSeconds;
    
    // Chat history
    private List<ChatMessage> chatHistory;
    
    // Combat metrics
    private int kills;
    private int deaths;
    private int newPlayerKills;
    private int unarmedKills;
    private int pvpZoneKills;
    
    public PlayerProfile(String playerName, String uuid) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.firstSeen = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.totalPlaytimeSeconds = 0;
        this.chatHistory = new ArrayList<>();
        this.kills = 0;
        this.deaths = 0;
        this.newPlayerKills = 0;
        this.unarmedKills = 0;
        this.pvpZoneKills = 0;
    }
    
    // Getters and Setters
    public String getPlayerName() { return playerName; }
    public String getUUID() { return uuid; }
    public long getFirstSeen() { return firstSeen; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long timestamp) { this.lastSeen = timestamp; }
    
    public long getTotalPlaytimeSeconds() { return totalPlaytimeSeconds; }
    public void setTotalPlaytimeSeconds(long seconds) { this.totalPlaytimeSeconds = seconds; }
    
    public double getTotalPlaytimeHours() { 
        return totalPlaytimeSeconds / 3600.0; 
    }
    
    public List<ChatMessage> getChatHistory() { return chatHistory; }
    public void addChatMessage(ChatMessage msg) { 
        chatHistory.add(msg); 
    }
    
    public int getKills() { return kills; }
    public void incrementKills() { this.kills++; }
    
    public int getDeaths() { return deaths; }
    public void incrementDeaths() { this.deaths++; }
    
    public int getNewPlayerKills() { return newPlayerKills; }
    public void incrementNewPlayerKills() { this.newPlayerKills++; }
    
    public int getUnarmedKills() { return unarmedKills; }
    public void incrementUnarmedKills() { this.unarmedKills++; }
    
    public int getPvpZoneKills() { return pvpZoneKills; }
    public void incrementPvpZoneKills() { this.pvpZoneKills++; }
    
    public double getKDRatio() {
        if (deaths == 0) return kills > 0 ? kills : 0;
        return (double) kills / deaths;
    }
    
    public int getMessageCount() { return chatHistory.size(); }
}

/**
 * Represents a single chat message with context
 */
class ChatMessage {
    
    private long timestamp;
    private String message;
    private String context; // e.g., "aggressive", "neutral", "loss_reaction"
    
    public ChatMessage(String message, String context) {
        this.timestamp = System.currentTimeMillis();
        this.message = message;
        this.context = context;
    }
    
    public long getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
    public String getContext() { return context; }
    
    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp));
    }
}
