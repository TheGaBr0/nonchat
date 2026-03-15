package com.nonxedy.nonchat.listener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nonxedy.nonchat.chat.channel.ChannelManager;
import com.nonxedy.nonchat.config.PluginConfig;
import com.nonxedy.nonchat.util.chat.packets.DisplayEntityUtil;
import com.nonxedy.nonchat.util.core.colors.ColorUtil;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Handles player join and quit events
 * Displays customizable messages when players join or leave the server
 */
public class JoinQuitListener implements Listener {
    
    private final PluginConfig config;
    private final ChannelManager channelManager;
    private final Map<Player, List<TextDisplay>> bubbles = new HashMap<>();
    private final Path firstJoinFile;
    
    public JoinQuitListener(PluginConfig config, ChannelManager channelManager) {
        this.config = config;
        this.channelManager = channelManager;
        this.firstJoinFile = Paths.get("plugins/nonchat/first_joins.txt");
    }

    /**
     * Cleans up sound names by converting to proper Minecraft resource location format
     * @param soundName The original sound name
     * @return The cleaned sound name in proper format (lowercase with dots)
     */
    private String cleanSoundName(String soundName) {
        if (soundName == null) {
            return "entity.experience_orb.pickup"; // fallback sound
        }
        
        // Remove "minecraft:" prefix if present
        String cleaned = soundName.replace("minecraft:", "");
        
        // Convert from old format (ENTITY_EXPERIENCE_ORB_PICKUP) to new format (entity.experience_orb.pickup)
        cleaned = cleaned.toLowerCase().replace('_', '.');
        
        return cleaned;
    }
    
    /**
     * Handles player join events
     * @param event Join event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isJoinMessageEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if this is the player's first join
        boolean isFirstJoin = isFirstJoin(player);
        
        String joinFormat;
        if (isFirstJoin) {
            // Use first join message format
            joinFormat = config.getFirstJoinFormat();
            // Mark player as having joined before
            markPlayerAsJoined(player);
        } else {
            // Use regular join message format
            joinFormat = config.getJoinFormat();
        }
        
        // Apply PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                joinFormat = PlaceholderAPI.setPlaceholders(player, joinFormat);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Error processing join message placeholders: {0}", e.getMessage());
            }
        }
        
        event.joinMessage(ColorUtil.parseComponent(joinFormat));
        
        // Play join sound if enabled for join events
        if (config.isJoinSoundEnabled()) {
            try {
                String soundName = cleanSoundName(config.getJoinSound());
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), 
                        soundName, 
                        config.getJoinSoundVolume(), config.getJoinSoundPitch());
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Error playing join sound: {0}", e.getMessage());
            }
        }
    }
    
    /**
     * Handles player quit events
     * @param event Quit event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up player data from ChannelManager to prevent memory leaks
        if (channelManager != null) {
            channelManager.cleanupPlayer(player);
        }
        
        // Clean up bubbles
        List<TextDisplay> playerBubbles = bubbles.remove(player);
        if (playerBubbles != null) {
            DisplayEntityUtil.removeBubbles(playerBubbles);
        }

        if (!config.isQuitMessageEnabled()) {
            return;
        }
        
        String quitFormat = config.getQuitFormat();
        
        // Apply PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                quitFormat = PlaceholderAPI.setPlaceholders(player, quitFormat);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Error processing quit message placeholders: {0}", e.getMessage());
            }
        }
        
        event.quitMessage(ColorUtil.parseComponent(quitFormat));
        
        // Play quit sound if enabled for quit events
        if (config.isQuitSoundEnabled()) {
            try {
                String soundName = cleanSoundName(config.getQuitSound());
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), 
                        soundName, 
                        config.getQuitSoundVolume(), config.getQuitSoundPitch());
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Error playing quit sound: {0}", e.getMessage());
            }
        }
    }
    
    /**
     * Checks if a player is joining for the first time
     * @param player The player to check
     * @return true if this is the player's first join
     */
    private boolean isFirstJoin(Player player) {
        try {
            if (!Files.exists(firstJoinFile)) {
                Files.createDirectories(firstJoinFile.getParent());
                Files.createFile(firstJoinFile);
                return true;
            }
            
            List<String> joinedPlayers = Files.readAllLines(firstJoinFile);
            return !joinedPlayers.contains(player.getUniqueId().toString());
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Error checking first join status: {0}", e.getMessage());
            return true; // Default to first join on error
        }
    }
    
    /**
     * Marks a player as having joined the server
     * @param player The player to mark
     */
    private void markPlayerAsJoined(Player player) {
        try {
            if (!Files.exists(firstJoinFile)) {
                Files.createDirectories(firstJoinFile.getParent());
                Files.createFile(firstJoinFile);
            }
            
            Files.write(firstJoinFile, (player.getUniqueId().toString() + "\n").getBytes());
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Error marking player as joined: {0}", e.getMessage());
        }
    }
}
