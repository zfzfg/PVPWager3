package de.zfzfg.pvpwager.listeners;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Arena;
import de.zfzfg.pvpwager.models.EquipmentSet;
import de.zfzfg.pvpwager.models.Match;
import de.zfzfg.pvpwager.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        } else if (title.contains("Arena Selection")) {
            handleArenaSelectionGUI(event, player);
        } else if (title.contains("Equipment Selection")) {
            handleEquipmentSelectionGUI(event, player);
        } else if (title.contains("Spectator")) {
            handleSpectatorGUI(event, player);
        }
    }
    
    private void handleWagerGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot < 0) return;
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        if (match == null) return;
        
        Player opponent = match.getOpponent(player);
        if (opponent == null) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Confirm button
        if (slot == 49) {
            // Verify both players have confirmed
            plugin.getMatchManager().handleWagerConfirmation(player, opponent);
            player.closeInventory();
            opponent.closeInventory();
            return;
        }
        
        // Cancel button
        if (slot == 48) {
            match.broadcast("&cMatch setup cancelled!");
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            opponent.closeInventory();
            return;
        }
        
        // Money buttons
        if (slot >= 45 && slot <= 47 || slot >= 51 && slot <= 53) {
            handleMoneyButtons(event, player, match);
            return;
        }
        
        // Item slots
        if (slot < 27) {
            // Player's own items
            handlePlayerItems(event, player, match, true);
        } else if (slot < 54) {
            // Opponent's items (just for display)
            event.setCancelled(true);
        }
    }
    
    private void handleMoneyButtons(InventoryClickEvent event, Player player, Match match) {
        int slot = event.getRawSlot();
        double currentBet = match.getWagerMoney(player);
        
        switch (slot) {
            case 45: currentBet += 10; break;
            case 46: currentBet += 100; break;
            case 47: currentBet += 1000; break;
            case 51: currentBet -= 10; break;
            case 52: currentBet -= 100; break;
            case 53: currentBet -= 1000; break;
        }
        
        // Apply limits
        currentBet = Math.max(0, Math.min(currentBet, 100000)); // Max 100k
        
        match.getWagerMoney().put(player.getUniqueId(), currentBet);
        
        // Update money display
        ItemStack moneyDisplay = event.getInventory().getItem(50);
        if (moneyDisplay != null) {
            ItemMeta meta = moneyDisplay.getItemMeta();
            if (meta != null) {
                meta.setLore(java.util.Arrays.asList(
                    MessageUtil.color("&7Current bet: &6" + currentBet),
                    MessageUtil.color("&7Click buttons below to adjust")
                ));
                moneyDisplay.setItemMeta(meta);
            }
        }
    }
    
    private void handlePlayerItems(InventoryClickEvent event, Player player, Match match, boolean isPlayer1) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Check if player has the item
        boolean hasItem = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(clicked)) {
                hasItem = true;
                break;
            }
        }
        
        if (!hasItem) {
            MessageUtil.sendMessage(player, "&cYou don't have this item in your inventory!");
            return;
        }
        
        // Add/remove item from wager
        java.util.List<ItemStack> wagerItems = match.getWagerItems(player);
        if (event.isLeftClick()) {
            // Add item
            if (wagerItems.size() < 27) {
                wagerItems.add(clicked);
                player.getInventory().removeItem(clicked);
                MessageUtil.sendMessage(player, "&aAdded item to wager!");
            } else {
                MessageUtil.sendMessage(player, "&cWager limit reached!");
            }
        } else if (event.isRightClick()) {
            // Remove item
            wagerItems.removeIf(item -> item.isSimilar(clicked));
            player.getInventory().addItem(clicked);
            MessageUtil.sendMessage(player, "&aRemoved item from wager!");
        }
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
            match.broadcast("&cMatch setup cancelled!");
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            opponent.closeInventory();
            return;
        }
        
        if (slot == 20) { // Confirm
            // Get selected arena
            Arena selectedArena = getSelectedArena(event.getInventory());
            if (selectedArena != null) {
                plugin.getMatchManager().handleArenaSelection(player, opponent, selectedArena);
                player.closeInventory();
                opponent.closeInventory();
            } else {
                MessageUtil.sendMessage(player, "&cPlease select an arena first!");
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
    
    private Arena getSelectedArena(org.bukkit.inventory.Inventory inventory) {
        for (int i = 10; i <= 16; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                // Extract arena ID from display name or lore
                // This is a simplified version - in real implementation, store arena ID in item lore
                for (Arena arena : plugin.getArenaManager().getArenas().values()) {
                    if (displayName.contains(arena.getDisplayName())) {
                        return arena;
                    }
                }
            }
        }
        return null;
    }
    
    private void handleArenaSelection(InventoryClickEvent event, Player player, Match match) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Visual feedback for selection
        for (int i = 10; i <= 16; i++) {
            ItemStack item = event.getInventory().getItem(i);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (i == slot) {
                        meta.setDisplayName(MessageUtil.color("&a&l" + meta.getDisplayName().replace("&a&l", "").replace("&c&l", "")));
                    } else {
                        meta.setDisplayName(MessageUtil.color("&c&l" + meta.getDisplayName().replace("&a&l", "").replace("&c&l", "")));
                    }
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
            match.broadcast("&cMatch setup cancelled!");
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
                
                // Check if both players have selected equipment
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
                    MessageUtil.sendMessage(player, "&aEquipment selected! Waiting for opponent...");
                }
            } else {
                MessageUtil.sendMessage(player, "&cPlease select an equipment set first!");
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
    
    private EquipmentSet getSelectedEquipment(org.bukkit.inventory.Inventory inventory) {
        for (int i = 10; i <= 16; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                // Extract equipment ID from display name or lore
                for (EquipmentSet equipment : plugin.getEquipmentManager().getEquipmentSets().values()) {
                    if (displayName.contains(equipment.getDisplayName())) {
                        return equipment;
                    }
                }
            }
        }
        return null;
    }
    
    private void handleEquipmentSelection(InventoryClickEvent event, Player player, Match match) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Visual feedback for selection
        for (int i = 10; i <= 16; i++) {
            ItemStack item = event.getInventory().getItem(i);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (i == slot) {
                        meta.setDisplayName(MessageUtil.color("&a&l" + meta.getDisplayName().replace("&a&l", "").replace("&c&l", "")));
                    } else {
                        meta.setDisplayName(MessageUtil.color("&c&l" + meta.getDisplayName().replace("&a&l", "").replace("&c&l", "")));
                    }
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
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Control buttons
        if (slot == 22) { // Leave
            plugin.getMatchManager().endMatch(match, null, true); // This will handle spectator cleanup
            player.closeInventory();
            return;
        }
        
        if (slot == 24) { // Refresh
            player.closeInventory();
            plugin.getGUIManager().openSpectatorGUI(player, match.getPlayer1(), match.getPlayer2());
            return;
        }
        
        // Player info slots
        if (slot == 11 || slot == 15) {
            // Teleport near player
            Player targetPlayer = slot == 11 ? match.getPlayer1() : match.getPlayer2();
            Location spectatorSpawn = match.getArena().getSpectatorSpawn();
            Location nearPlayer = targetPlayer.getLocation().add(0, 5, 0);
            
            if (spectatorSpawn.getWorld().equals(nearPlayer.getWorld())) {
                player.teleport(nearPlayer);
                MessageUtil.sendMessage(player, "&aTeleported near &e" + targetPlayer.getName() + "&a!");
            } else {
                MessageUtil.sendMessage(player, "&cCannot teleport to different world!");
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title == null) return;
        
        Player player = (Player) event.getPlayer();
        
        if (title.contains("Wager Setup") || title.contains("Arena Selection") || 
            title.contains("Equipment Selection") || title.contains("Spectator")) {
            // Handle cleanup if needed
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Handle item interactions for GUI opening
        // This would be implemented for specific items that open GUIs
    }
}