package com.ppba.plugin.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import com.ppba.plugin.PPBAPlugin;

/**
 * Listens for combat/PvP events and tracks kill metrics
 */
public class CombatListener implements Listener {
    
    private final PPBAPlugin plugin;
    private static final int NEW_PLAYER_THRESHOLD = 3600; // 1 hour playtime in seconds
    
    public CombatListener(PPBAPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Track player deaths and attribute kills
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Only track player-to-player kills
        if (killer == null) {
            return;
        }
        
        // Skip if killer has bypass permission
        if (killer.hasPermission("ppba.bypass.tracking")) {
            return;
        }
        
        // Async safe database logging
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Analyze kill context
                KillContext context = analyzeKillContext(killer, victim);
                
                // Log the kill
                plugin.getDataManager().logKill(
                        killer.getName(),
                        killer.getUniqueId().toString(),
                        victim.getName(),
                        context
                );
                
                // Log the death
                plugin.getDataManager().logDeath(
                        victim.getName(),
                        victim.getUniqueId().toString(),
                        killer.getName()
                );
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error logging combat event: " + e.getMessage());
            }
        });
    }
    
    /**
     * Analyze context of a kill to determine if it's fair or predatory
     */
    private KillContext analyzeKillContext(Player killer, Player victim) {
        KillContext context = new KillContext();
        
        // Check if victim is new player
        if (victim.getStatistic(org.bukkit.Statistic.PLAY_ONE_TICK) < NEW_PLAYER_THRESHOLD) {
            context.isNewPlayerVictim = true;
        }
        
        // Check if victim was unarmed
        context.victimUnarmed = isPlayerUnarmed(victim);
        
        // Check if kill was in designated PvP zone (optional - depends on your server)
        context.inPvpZone = isInPvpZone(killer.getLocation());
        
        // Check if killer was significantly stronger
        double killerHealth = killer.getHealth();
        double victimHealth = victim.getHealth();
        context.healthDisparityFavor = killerHealth > (victimHealth * 1.5);
        
        // Determine overall context
        if (context.inPvpZone && !context.victimUnarmed) {
            context.fairContext = true;
            context.type = "fair_pvp";
        } else if (context.isNewPlayerVictim || context.victimUnarmed) {
            context.fairContext = false;
            context.type = "predatory";
        } else {
            context.fairContext = true;
            context.type = "standard_pvp";
        }
        
        return context;
    }
    
    /**
     * Check if a player is unarmed (no combat items in hand)
     */
    private boolean isPlayerUnarmed(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        return (mainHand == null || mainHand.isEmpty()) && 
               (offHand == null || offHand.isEmpty());
    }
    
    /**
     * Check if a location is in a designated PvP zone
     * (You can expand this to check actual PvP zone plugins like WorldGuard)
     */
    private boolean isInPvpZone(org.bukkit.Location location) {
        // Simple implementation - you can integrate with WorldGuard/other plugins here
        // For now, returns false (conservative approach)
        return false;
    }
    
    /**
     * Inner class to represent kill context
     */
    public static class KillContext {
        public boolean isNewPlayerVictim = false;
        public boolean victimUnarmed = false;
        public boolean inPvpZone = false;
        public boolean healthDisparityFavor = false;
        public boolean fairContext = false;
        public String type = "unknown";
    }
}
