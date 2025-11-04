package de.zfzfg.pvpwager.listeners;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Match;
import de.zfzfg.pvpwager.models.MatchState;
import de.zfzfg.pvpwager.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
        
        Match match = plugin.getMatchManager().getMatch(attacker, victim);
        if (match == null) {
            // Block PvP outside of matches
            event.setCancelled(true);
            return;
        }
        
        if (match.getState() != MatchState.FIGHTING) {
            event.setCancelled(true);
            MessageUtil.sendMessage(attacker, "&cMatch has not started yet or has ended!");
            return;
        }
        
        // Allow damage in match
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
                event.setDeathMessage(MessageUtil.color("&c" + player.getName() + " &7was killed by &c" + killer.getName() + " &7in a PvP wager match!"));
                event.getDrops().clear(); // Prevent item drops
                event.setDroppedExp(0); // Prevent XP drops
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        
        if (match != null && match.getState() == MatchState.FIGHTING) {
            Player opponent = match.getOpponent(player);
            if (opponent != null) {
                // Player disconnected during match - opponent wins
                plugin.getMatchManager().endMatch(match, opponent, false);
                MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &cdisconnected! You win the match!");
            }
        }
    }
}