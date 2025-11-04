package de.zfzfg.pvpwager.listeners;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Match;
import de.zfzfg.pvpwager.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class WorldChangeListener implements Listener {
    
    private final PvPWager plugin;
    
    public WorldChangeListener(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        
        if (match == null) return;
        
        String command = event.getMessage().toLowerCase();
        
        // Allow essential commands
        if (command.startsWith("/surrender") || command.startsWith("/draw") || command.startsWith("/msg") || 
            command.startsWith("/tell") || command.startsWith("/w") || command.startsWith("/whisper") || 
            command.startsWith("/pvp leave") || command.startsWith("/stop") || command.startsWith("/shutdown")) {
            return;
        }
        
        // Block other commands during match
        if (match.getState() == de.zfzfg.pvpwager.models.MatchState.FIGHTING) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "&cCommands are restricted during matches! Use &e/surrender &cor &e/draw&c.");
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        
        if (match == null || match.getArena() == null || match.getArena().getBoundaries() == null) {
            return;
        }
        
        // Check if player is in arena boundaries
        if (!match.getArena().getBoundaries().isInside(event.getTo())) {
            // Teleport back to last valid position
            event.setTo(event.getFrom());
            MessageUtil.sendMessage(player, "&cYou cannot leave the arena boundaries!");
        }
    }
}