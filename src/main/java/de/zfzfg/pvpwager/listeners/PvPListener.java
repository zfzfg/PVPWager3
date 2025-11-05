package de.zfzfg.pvpwager.listeners;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Match;
import de.zfzfg.pvpwager.models.MatchState;
import de.zfzfg.pvpwager.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PvPListener implements Listener {
    
    private final PvPWager plugin;
    
    public PvPListener(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Check if attacker is a spectator
        Match attackerMatch = plugin.getMatchManager().getMatchByPlayer(attacker);
        if (attackerMatch != null && attackerMatch.getSpectators().contains(attacker.getUniqueId())) {
            event.setCancelled(true);
            MessageUtil.sendMessage(attacker, "&cDu kannst als Zuschauer nicht angreifen!");
            return;
        }
        
        // Check if victim is a spectator
        Match victimMatch = plugin.getMatchManager().getMatchByPlayer(victim);
        if (victimMatch != null && victimMatch.getSpectators().contains(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        
        // Check if both are in the same match
        Match match = plugin.getMatchManager().getMatch(attacker, victim);
        if (match == null) {
            // Block PvP outside of matches (if configured)
            if (plugin.getConfig().getBoolean("settings.block-pvp-outside-matches", true)) {
                event.setCancelled(true);
                MessageUtil.sendMessage(attacker, "&cPvP ist nur in Matches erlaubt!");
            }
            return;
        }
        
        // Check match state
        if (match.getState() != MatchState.FIGHTING) {
            event.setCancelled(true);
            MessageUtil.sendMessage(attacker, "&cDas Match hat noch nicht begonnen oder ist beendet!");
            return;
        }
        
        // Allow damage in active match
    }
    
    @EventHandler
    public void onSpectatorDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        
        // Protect spectators from all damage
        if (match != null && match.getSpectators().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        
        if (match != null && match.getState() == MatchState.FIGHTING) {
            Player killer = player.getKiller();
            if (killer != null && (killer.equals(match.getPlayer1()) || killer.equals(match.getPlayer2()))) {
                // Player was killed by opponent in match
                plugin.getMatchManager().endMatch(match, killer, false);
                
                // Custom death message
                event.setDeathMessage(MessageUtil.color(
                    "&c" + player.getName() + " &7wurde von &c" + killer.getName() + 
                    " &7im PvP-Match besiegt!"
                ));
                
                // Prevent item/XP drops
                event.getDrops().clear();
                event.setDroppedExp(0);
                
                // Keep inventory
                event.setKeepInventory(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        
        if (match != null) {
            // Check if player is spectator
            if (match.getSpectators().contains(player.getUniqueId())) {
                match.getSpectators().remove(player.getUniqueId());
                match.broadcast(plugin.getConfigManager().getMessage("match.spectator-leave-announce",
                    "player", player.getName()));
                return;
            }
            
            // Player disconnected during match setup or fighting
            if (match.getState() == MatchState.SETUP || match.getState() == MatchState.STARTING) {
                // Cancel match setup
                Player opponent = match.getOpponent(player);
                if (opponent != null) {
                    MessageUtil.sendMessage(opponent, 
                        "&e" + player.getName() + " &chat die Verbindung getrennt! Match abgebrochen.");
                }
                plugin.getMatchManager().endMatch(match, null, true);
            } else if (match.getState() == MatchState.FIGHTING) {
                // Player disconnected during fight - opponent wins
                Player opponent = match.getOpponent(player);
                if (opponent != null) {
                    plugin.getMatchManager().endMatch(match, opponent, false);
                    MessageUtil.sendMessage(opponent, 
                        "&e" + player.getName() + " &chat die Verbindung getrennt! Du gewinnst das Match!");
                }
            }
        }
    }
}