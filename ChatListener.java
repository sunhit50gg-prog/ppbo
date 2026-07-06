package com.ppba.plugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import io.papermc.paper.event.player.ChatEvent;
import com.ppba.plugin.PPBAPlugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Listens for player chat events and logs them for behavioral analysis
 */
public class ChatListener implements Listener {
    
    private final PPBAPlugin plugin;
    
    public ChatListener(PPBAPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Track chat messages - Paper 1.21.1 uses AsyncChatEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        
        // Skip if player has bypass permission
        if (player.hasPermission("ppba.bypass.tracking")) {
            return;
        }
        
        // Extract plain text message from Adventure component
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        // Async-safe database logging
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Analyze message context for trigger detection
                String context = analyzeMessageContext(message, player);
                
                // Log to database
                plugin.getDataManager().logChatMessage(
                        player.getName(),
                        player.getUniqueId().toString(),
                        message,
                        context
                );
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error logging chat for " + player.getName() + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Track player join events - create/update player profile
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Skip if player has bypass permission
        if (player.hasPermission("ppba.bypass.tracking")) {
            return;
        }
        
        // Async initialization of player profile
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDataManager().initializeOrUpdatePlayer(player);
                plugin.getLogger().fine("Player profile updated: " + player.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Error initializing player profile: " + e.getMessage());
            }
        });
    }
    
    /**
     * Analyze chat context to detect triggers and tone indicators
     */
    private String analyzeMessageContext(String message, Player player) {
        String context = "neutral";
        String lowerMessage = message.toLowerCase();
        
        // Detect aggressive/toxic indicators
        if (containsAggressive(lowerMessage)) {
            context = "aggressive";
        }
        // Detect loss/rejection triggers
        else if (containsLossTrigger(lowerMessage)) {
            context = "loss_reaction";
        }
        // Detect appreciation/support
        else if (containsPositive(lowerMessage)) {
            context = "positive";
        }
        // Detect seeking validation
        else if (containsValidationSeeking(lowerMessage)) {
            context = "validation_seeking";
        }
        // Detect mocking/sarcasm
        else if (containsMocking(lowerMessage)) {
            context = "mocking";
        }
        
        return context;
    }
    
    private boolean containsAggressive(String msg) {
        String[] aggressive = {
            "trash", "noob", "sucks", "die", "kill", "hate", "stupid", "idiot",
            "!!!", "fuck", "shit", "ass", "rage", "pissed", "mad"
        };
        for (String word : aggressive) {
            if (msg.contains(word)) return true;
        }
        return false;
    }
    
    private boolean containsLossTrigger(String msg) {
        String[] triggers = {
            "lost", "died", "dead", "lost items", "unfair", "bullshit", "rigged",
            "spawn kill", "lag", "crash"
        };
        for (String word : triggers) {
            if (msg.contains(word)) return true;
        }
        return false;
    }
    
    private boolean containsPositive(String msg) {
        String[] positive = {
            "thanks", "thank you", "gg", "good game", "nice", "awesome", "cool",
            "help", "please", "sorry"
        };
        for (String word : positive) {
            if (msg.contains(word)) return true;
        }
        return false;
    }
    
    private boolean containsValidationSeeking(String msg) {
        String[] seeking = {
            "did i", "did you see", "that was", "right?", "agree?", "wasn't that",
            "look at", "check out", "tell me", "am i"
        };
        for (String word : seeking) {
            if (msg.contains(word)) return true;
        }
        return false;
    }
    
    private boolean containsMocking(String msg) {
        String[] mocking = {
            "lol", "haha", "ez", "easy", "you suck", "rekt", "owned", "skill issue"
        };
        for (String word : mocking) {
            if (msg.contains(word)) return true;
        }
        return false;
    }
}
