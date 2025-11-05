package de.zfzfg.pvpwager.listeners;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Arena;
import de.zfzfg.pvpwager.models.EquipmentSet;
import de.zfzfg.pvpwager.models.Match;
import de.zfzfg.pvpwager.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GUIListener implements Listener {
    
    private final PvPWager plugin;
    
    public GUIListener(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        
        if (title.contains("Wager Setup")) {
            handleWagerGUI(event, player);
        } else if (title.contains("Select Arena")) {
            handleArenaSelectionGUI(event, player);
        } else if (title.contains("Select Equipment")) {
            handleEquipmentSelectionGUI(event, player);
        } else if (title.contains("Match:")) {
            handleSpectatorGUI(event, player);
        }
    }
    
    private void handleWagerGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0) return;
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        if (match == null) {
            player.closeInventory();
            return;
        }
        
        Player opponent = match.getOpponent(player);
        if (opponent == null) {
            player.closeInventory();
            return;
        }
        
        // ===== SKIP WAGER BUTTON (Slot 4) =====
        if (slot == 4) {
            handleSkipWager(player, opponent, match);
            return;
        }
        
        // ===== CONFIRM BUTTON (Slot 49) =====
        if (slot == 49) {
            handleWagerConfirm(player, opponent, match);
            return;
        }
        
        // ===== CANCEL BUTTON (Slot 48) =====
        if (slot == 48) {
            handleWagerCancel(player, opponent, match);
            return;
        }
        
        // ===== MONEY BUTTONS =====
        if (slot >= 45 && slot <= 47 || slot >= 51 && slot <= 53) {
            handleMoneyButtons(event, player, match);
            return;
        }
        
        // ===== ITEM HANDLING =====
        
        // Player's own item section (first 4 columns, slots 0-26)
        if (slot < 27 && slot % 9 < 4) {
            handlePlayerItemClick(event, player, match);
            return;
        }
        
        // Opponent's section is view-only
        if (slot >= 27 && slot < 54 && slot % 9 >= 5) {
            MessageUtil.sendMessage(player, "&cDu kannst die Items deines Gegners nicht bearbeiten!");
            return;
        }
    }
    
    private void handleSkipWager(Player player, Player opponent, Match match) {
        boolean hasVoted = match.hasPlayerVotedToSkip(player);
        
        if (hasVoted) {
            // Cancel vote
            match.removeSkipVote(player);
            MessageUtil.sendMessage(player, "&7Du hast deine Skip-Abstimmung zurückgezogen.");
            MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &7hat die Skip-Abstimmung abgebrochen.");
        } else {
            // Add vote
            match.addSkipVote(player);
            MessageUtil.sendMessage(player, "&aDu hast für Skip gestimmt!");
            MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &amöchte ohne Wetteinsatz kämpfen! Stimme zu mit dem Skip-Button.");
            
            // Check if both voted
            if (match.bothPlayersVotedToSkip()) {
                match.setNoWagerMode(true);
                
                match.broadcast("");
                match.broadcast("&a&l━━━━━━━━━━━━━━━━━━━━━━━");
                match.broadcast("&a&lKEIN WETTEINSATZ!");
                match.broadcast("&a&l━━━━━━━━━━━━━━━━━━━━━━━");
                match.broadcast("");
                match.broadcast("&7Beide Spieler kämpfen ohne Einsatz.");
                match.broadcast("&7Weiter zur Arena-Auswahl...");
                match.broadcast("");
                
                // Close GUIs
                player.closeInventory();
                opponent.closeInventory();
                
                // Open arena selection after short delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGUIManager().openArenaSelectionGUI(match.getPlayer1(), match.getPlayer2());
                }, 20L);
                
                return;
            }
        }
        
        // Optimized refresh - no close/reopen
        plugin.getGUIManager().getWagerGUI().refresh(match);
    }
    
    private void handlePlayerItemClick(InventoryClickEvent event, Player player, Match match) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        // Player wants to ADD an item (has item on cursor)
        if (cursor != null && cursor.getType() != Material.AIR) {
            // Check if item is from player's actual inventory
            if (event.getClickedInventory() != player.getOpenInventory().getTopInventory()) {
                // Player clicked their bottom inventory with cursor item
                return; // Let them move items in their inventory
            }
            
            // Add item to wager
            ItemStack toAdd = cursor.clone();
            match.getWagerItems(player).add(toAdd);
            
            // Remove from player inventory
            player.getInventory().removeItem(toAdd);
            event.setCursor(null);
            
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("wager.item-added"));
            
            // Optimized refresh
            refreshWagerGUI(match);
            return;
        }
        
        // Player wants to REMOVE an item (clicks on wager item)
        if (clicked != null && clicked.getType() != Material.AIR) {
            // Check if this is a wager item (not a glass pane)
            if (clicked.getType().toString().contains("STAINED_GLASS_PANE")) {
                return; // Ignore glass panes
            }
            
            // Find and remove item from wager
            boolean removed = false;
            for (int i = 0; i < match.getWagerItems(player).size(); i++) {
                ItemStack wagerItem = match.getWagerItems(player).get(i);
                if (wagerItem.isSimilar(clicked)) {
                    match.getWagerItems(player).remove(i);
                    player.getInventory().addItem(wagerItem);
                    removed = true;
                    break;
                }
            }
            
            if (removed) {
                MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("wager.item-removed"));
                refreshWagerGUI(match);
            }
        }
    }
    
    private void handleMoneyButtons(InventoryClickEvent event, Player player, Match match) {
        if (!plugin.hasEconomy()) {
            MessageUtil.sendMessage(player, "&cVault Economy ist nicht aktiviert!");
            return;
        }
        
        int slot = event.getRawSlot();
        double currentBet = match.getWagerMoney(player);
        double balance = plugin.getEconomy().getBalance(player);
        
        double change = 0;
        switch (slot) {
            case 45: change = 10; break;
            case 46: change = 100; break;
            case 47: change = 1000; break;
            case 51: change = -10; break;
            case 52: change = -100; break;
            case 53: change = -1000; break;
        }
        
        double newBet = currentBet + change;
        
        // Validate
        if (newBet < 0) {
            newBet = 0;
        }
        
        if (newBet > balance) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("wager.not-enough-money", 
                "amount", String.valueOf(newBet)));
            return;
        }
        
        // Max bet limit (if no bypass permission)
        double maxBet = plugin.getConfig().getDouble("settings.max-bet-money", 100000);
        if (!player.hasPermission("pvpwager.bypass.betlimit") && newBet > maxBet) {
            MessageUtil.sendMessage(player, "&cMaximaler Einsatz: &6$" + maxBet);
            newBet = maxBet;
        }
        
        // Update bet
        match.getWagerMoney().put(player.getUniqueId(), newBet);
        
        MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("wager.money-set", 
            "amount", String.valueOf(newBet)));
        
        // Optimized refresh
        refreshWagerGUI(match);
    }
    
    private void handleWagerConfirm(Player player, Player opponent, Match match) {
        // If in no-wager mode, skip validation
        if (match.isNoWagerMode()) {
            player.closeInventory();
            opponent.closeInventory();
            plugin.getGUIManager().openArenaSelectionGUI(player, opponent);
            return;
        }
        
        // Validate minimum wager
        int minItems = plugin.getConfig().getInt("settings.checks.minimum-bet-items", 1);
        double minMoney = plugin.getConfig().getDouble("settings.checks.minimum-bet-money", 0);
        
        int totalItems = match.getWagerItems(player).size() + match.getWagerItems(opponent).size();
        double totalMoney = match.getWagerMoney(player) + match.getWagerMoney(opponent);
        
        if (totalItems < minItems && totalMoney < minMoney) {
            MessageUtil.sendMessage(player, plugin.getConfigManager().getMessage("wager.min-bet-required",
                "amount", String.valueOf(minItems),
                "money", String.valueOf(minMoney)));
            MessageUtil.sendMessage(player, "&7Oder nutze den &e&lSKIP WAGER &7Button um ohne Einsatz zu kämpfen!");
            return;
        }
        
        // Check if both players have confirmed
        plugin.getMatchManager().handleWagerConfirmation(player, opponent);
    }
    
    private void handleWagerCancel(Player player, Player opponent, Match match) {
        match.broadcast(plugin.getConfigManager().getMessage("wager.cancelled"));
        plugin.getMatchManager().endMatch(match, null, true);
        player.closeInventory();
        opponent.closeInventory();
    }
    
    /**
     * Optimized refresh - only updates contents, no close/reopen
     */
    private void refreshWagerGUI(Match match) {
        plugin.getGUIManager().getWagerGUI().refresh(match);
    }
    
    private void handleArenaSelectionGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot < 0) return;
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        if (match == null) return;
        
        Player opponent = match.getOpponent(player);
        if (opponent == null) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Control buttons
        if (slot == 18) { // Cancel
            match.broadcast(plugin.getConfigManager().getMessage("wager.cancelled"));
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            opponent.closeInventory();
            return;
        }
        
        if (slot == 20) { // Confirm
            Arena selectedArena = getSelectedArena(event.getInventory());
            if (selectedArena != null) {
                plugin.getMatchManager().handleArenaSelection(player, opponent, selectedArena);
                player.closeInventory();
                opponent.closeInventory();
            } else {
                MessageUtil.sendMessage(player, "&cBitte wähle zuerst eine Arena aus!");
            }
            return;
        }
        
        if (slot == 22) { // Close
            player.closeInventory();
            return;
        }
        
        // Arena selection slots
        if (slot >= 10 && slot <= 16) {
            handleArenaSelection(event, player, match);
        }
    }
    
    private Arena getSelectedArena(Inventory inventory) {
        for (int i = 10; i <= 16; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName.contains("§a§l")) { // Selected (green)
                    for (Arena arena : plugin.getArenaManager().getArenas().values()) {
                        if (displayName.contains(arena.getDisplayName())) {
                            return arena;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private void handleArenaSelection(InventoryClickEvent event, Player player, Match match) {
        int slot = event.getRawSlot();
        
        // Update visual feedback
        for (int i = 10; i <= 16; i++) {
            ItemStack item = event.getInventory().getItem(i);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String name = meta.getDisplayName();
                    if (i == slot) {
                        name = MessageUtil.color("§a§l" + name.replaceAll("§[a-fA-F0-9l]", ""));
                    } else {
                        name = MessageUtil.color("§7" + name.replaceAll("§[a-fA-F0-9l]", ""));
                    }
                    meta.setDisplayName(name);
                    item.setItemMeta(meta);
                }
            }
        }
    }
    
    private void handleEquipmentSelectionGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot < 0) return;
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        if (match == null) return;
        
        Player opponent = match.getOpponent(player);
        if (opponent == null) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Control buttons
        if (slot == 18) { // Cancel
            match.broadcast(plugin.getConfigManager().getMessage("wager.cancelled"));
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            opponent.closeInventory();
            return;
        }
        
        if (slot == 20) { // Confirm
            EquipmentSet selectedEquipment = getSelectedEquipment(event.getInventory());
            if (selectedEquipment != null) {
                if (player.equals(match.getPlayer1())) {
                    match.setPlayer1Equipment(selectedEquipment);
                } else {
                    match.setPlayer2Equipment(selectedEquipment);
                }
                
                // Check if both players have selected
                if (match.getPlayer1Equipment() != null && match.getPlayer2Equipment() != null) {
                    plugin.getMatchManager().handleEquipmentSelection(
                        match.getPlayer1(), 
                        match.getPlayer2(), 
                        match.getPlayer1Equipment(), 
                        match.getPlayer2Equipment()
                    );
                    player.closeInventory();
                    opponent.closeInventory();
                } else {
                    MessageUtil.sendMessage(player, "&aEquipment ausgewählt! Warte auf Gegner...");
                }
            } else {
                MessageUtil.sendMessage(player, "&cBitte wähle zuerst ein Equipment-Set!");
            }
            return;
        }
        
        if (slot == 22) { // Close
            player.closeInventory();
            return;
        }
        
        // Equipment selection slots
        if (slot >= 10 && slot <= 16) {
            handleEquipmentSelection(event, player, match);
        }
    }
    
    private EquipmentSet getSelectedEquipment(Inventory inventory) {
        for (int i = 10; i <= 16; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName.contains("§a§l")) { // Selected (green)
                    for (EquipmentSet equipment : plugin.getEquipmentManager().getEquipmentSets().values()) {
                        if (displayName.contains(equipment.getDisplayName())) {
                            return equipment;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private void handleEquipmentSelection(InventoryClickEvent event, Player player, Match match) {
        int slot = event.getRawSlot();
        
        // Update visual feedback
        for (int i = 10; i <= 16; i++) {
            ItemStack item = event.getInventory().getItem(i);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String name = meta.getDisplayName();
                    if (i == slot) {
                        name = MessageUtil.color("§a§l" + name.replaceAll("§[a-fA-F0-9l]", ""));
                    } else {
                        name = MessageUtil.color("§7" + name.replaceAll("§[a-fA-F0-9l]", ""));
                    }
                    meta.setDisplayName(name);
                    item.setItemMeta(meta);
                }
            }
        }
    }
    
    private void handleSpectatorGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot < 0) return;
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        if (match == null) return;
        
        // Leave button
        if (slot == 22) {
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            return;
        }
        
        // Refresh button
        if (slot == 24) {
            player.closeInventory();
            plugin.getGUIManager().openSpectatorGUI(player, match.getPlayer1(), match.getPlayer2());
            return;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Cleanup if needed
    }
}