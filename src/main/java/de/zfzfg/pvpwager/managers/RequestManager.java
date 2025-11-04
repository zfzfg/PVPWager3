package de.zfzfg.pvpwager.managers;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.PvPRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import de.zfzfg.pvpwager.utils.MessageUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RequestManager {
    private final PvPWager plugin;
    private final Map<UUID, PvPRequest> requests = new HashMap<>();
    
    public RequestManager(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    public void sendRequest(Player sender, Player target) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        // Cancel existing requests between these players
        cancelRequest(senderId, targetId);
        cancelRequest(targetId, senderId);
        
        PvPRequest request = new PvPRequest(senderId, targetId);
        requests.put(senderId, request);
        
        // Send clickable message to target
        String message = "&e" + sender.getName() + " &7has sent you a PvP wager request! &a[ACCEPT] &7or type &c/pvp deny " + sender.getName();
        target.sendMessage(MessageUtil.color(message));
        
        // Create clickable accept component
        /* In a real implementation, this would use adventure text components for clickable messages
        Component acceptComponent = Component.text("[ACCEPT]")
            .color(TextColor.fromHexString("#55FF55"))
            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/pvp accept " + sender.getName()))
            .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Accept the PvP wager request")));
        
        target.sendMessage(acceptComponent);
        */
        
        // Schedule auto-expire
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (requests.containsKey(senderId)) {
                cancelRequest(senderId, targetId);
                MessageUtil.sendMessage(sender, "&cYour PvP wager request to &e" + target.getName() + " &chas expired!");
                MessageUtil.sendMessage(target, "&c" + sender.getName() + "'s &cPvP wager request has expired!");
            }
        }, 1200); // 60 seconds
    }
    
    public boolean acceptRequest(Player target, Player sender) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();
        
        if (requests.containsKey(senderId)) {
            PvPRequest request = requests.get(senderId);
            
            if (request.getTargetId().equals(targetId)) {
                // Remove request
                requests.remove(senderId);
                
                // Start match setup
                plugin.getMatchManager().startMatchSetup(sender, target);
                return true;
            }
        }
        
        return false;
    }
    
    public void cancelRequest(UUID senderId, UUID targetId) {
        if (requests.containsKey(senderId)) {
            PvPRequest request = requests.get(senderId);
            if (request.getTargetId().equals(targetId)) {
                requests.remove(senderId);
            }
        }
    }
    
    public boolean hasPendingRequest(Player player) {
        UUID playerId = player.getUniqueId();
        return requests.containsKey(playerId) || 
               requests.values().stream().anyMatch(req -> req.getTargetId().equals(playerId));
    }
    
    public void cleanup() {
        requests.clear();
    }
}