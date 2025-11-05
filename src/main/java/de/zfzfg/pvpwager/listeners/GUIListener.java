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
        int slot = event.getRawSlot();
        
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
        
        // Allow bottom inventory (player inventory) clicks
        if (slot >= event.getView().getTopInventory().getSize()) {
            return; // Allow normal inventory interaction
        }
        
        event.setCancelled(true); // Cancel top inventory clicks by default
        
        if (slot < 0) return;
        
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
            match.removeSkipVote(player);
            MessageUtil.sendMessage(player, "&7Du hast deine Skip-Abstimmung zurueckgezogen.");
            MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &7hat die Skip-Abstimmung abgebrochen.");
        } else {
            match.addSkipVote(player);
            MessageUtil.sendMessage(player, "&aDu hast fuer Skip gestimmt!");
            MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &amoechte ohne Wetteinsatz kaempfen! Stimme zu mit dem Skip-Button.");
            
            if (match.bothPlayersVotedToSkip()) {
                match.setNoWagerMode(true);
                
                match.broadcast("");
                match.broadcast("&a&l━━━━━━━━━━━━━━━━━━━━━━━");
                match.broadcast("&a&lKEIN WETTEINSATZ!");
                match.broadcast("&a&l━━━━━━━━━━━━━━━━━━━━━━━");
                match.broadcast("");
                match.broadcast("&7Beide Spieler kaempfen ohne Einsatz.");
                match.broadcast("&7Weiter zur Arena-Auswahl...");
                match.broadcast("");
                
                player.closeInventory();
                opponent.closeInventory();
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGUIManager().openArenaSelectionGUI(match.getPlayer1(), match.getPlayer2());
                }, 20L);
                
                return;
            }
        }
        
        plugin.getGUIManager().getWagerGUI().refresh(match);
    }
    
    private void handlePlayerItemClick(InventoryClickEvent event, Player player, Match match) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        // Check if clicked item is a glass pane (UI element)
        if (clicked != null && clicked.getType().toString().contains("STAINED_GLASS_PANE")) {
            return;
        }
        
        // Player wants to ADD an item from their inventory
        if (event.getClickedInventory() == player.getInventory()) {
            if (clicked != null && clicked.getType() != Material.AIR) {
                // Clone the item and add to wager
                ItemStack toAdd = clicked.clone();
                match.getWagerItems(player).add(toAdd);
                
                // Remove from player inventory
                player.getInventory().removeItem(toAdd);
                
                MessageUtil.sendMessage(player, "&aItem hinzugefuegt!");
                refreshWagerGUI(match);
            }
            return;
        }
        
        // Player wants to REMOVE an item (click on wager item in top inventory)
        if (clicked != null && clicked.getType() != Material.AIR) {
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
                MessageUtil.sendMessage(player, "&cItem entfernt!");
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
        
        if (newBet < 0) {
            newBet = 0;
        }
        
        if (newBet > balance) {
            MessageUtil.sendMessage(player, "&cNicht genug Geld! Benoetigt: &6$" + newBet);
            return;
        }
        
        double maxBet = plugin.getConfig().getDouble("settings.max-bet-money", 100000);
        if (!player.hasPermission("pvpwager.bypass.betlimit") && newBet > maxBet) {
            MessageUtil.sendMessage(player, "&cMaximaler Einsatz: &6$" + maxBet);
            newBet = maxBet;
        }
        
        match.getWagerMoney().put(player.getUniqueId(), newBet);
        MessageUtil.sendMessage(player, "&aGeld-Einsatz: &6$" + newBet);
        
        refreshWagerGUI(match);
    }
    
    private void handleWagerConfirm(Player player, Player opponent, Match match) {
        if (match.isNoWagerMode()) {
            player.closeInventory();
            opponent.closeInventory();
            plugin.getGUIManager().openArenaSelectionGUI(player, opponent);
            return;
        }
        
        int minItems = plugin.getConfig().getInt("settings.checks.minimum-bet-items", 1);
        double minMoney = plugin.getConfig().getDouble("settings.checks.minimum-bet-money", 0);
        
        int totalItems = match.getWagerItems(player).size() + match.getWagerItems(opponent).size();
        double totalMoney = match.getWagerMoney(player) + match.getWagerMoney(opponent);
        
        if (totalItems < minItems && totalMoney < minMoney) {
            MessageUtil.sendMessage(player, "&cMindest-Einsatz: " + minItems + " Items oder $" + minMoney);
            MessageUtil.sendMessage(player, "&7Oder nutze den &e&lSKIP WAGER &7Button!");
            return;
        }
        
        plugin.getMatchManager().handleWagerConfirmation(player, opponent);
    }
    
    private void handleWagerCancel(Player player, Player opponent, Match match) {
        match.broadcast("&cWetteinsatz abgebrochen!");
        plugin.getMatchManager().endMatch(match, null, true);
        player.closeInventory();
        opponent.closeInventory();
    }
    
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
        
        // Cancel button (slot 18)
        if (slot == 18) {
            match.broadcast("&cMatch abgebrochen!");
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            opponent.closeInventory();
            return;
        }
        
        // Confirm button (slot 20)
        if (slot == 20) {
            Arena selectedArena = match.getArena();
            if (selectedArena != null) {
                MessageUtil.sendMessage(player, "&aArena bestaetigt: &e" + selectedArena.getDisplayName());
                MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &ahat Arena ausgewaehlt!");
                
                player.closeInventory();
                opponent.closeInventory();
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGUIManager().openEquipmentSelectionGUI(match.getPlayer1(), match.getPlayer2());
                }, 20L);
            } else {
                MessageUtil.sendMessage(player, "&cBitte waehle zuerst eine Arena aus!");
            }
            return;
        }
        
        // Close button (slot 22)
        if (slot == 22) {
            player.closeInventory();
            return;
        }
        
        // Arena selection slots (11-15)
        if (slot >= 11 && slot <= 15) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                
                // Find arena by display name
                for (Arena arena : plugin.getArenaManager().getArenas().values()) {
                    if (displayName.contains(arena.getDisplayName())) {
                        match.setArena(arena);
                        
                        // Update visual feedback
                        updateArenaSelection(event.getInventory(), slot);
                        
                        MessageUtil.sendMessage(player, "&aArena gewaehlt: &e" + arena.getDisplayName());
                        MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &7hat eine Arena gewaehlt.");
                        break;
                    }
                }
            }
        }
    }
    
    private void updateArenaSelection(Inventory inventory, int selectedSlot) {
        for (int i = 11; i <= 15; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String name = meta.getDisplayName();
                
                // Remove color codes
                name = name.replaceAll("§[0-9a-fk-or]", "");
                
                if (i == selectedSlot) {
                    meta.setDisplayName(MessageUtil.color("&a&l" + name));
                } else {
                    meta.setDisplayName(MessageUtil.color("&7" + name));
                }
                
                item.setItemMeta(meta);
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
        
        // Cancel button
        if (slot == 18) {
            match.broadcast("&cMatch abgebrochen!");
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            opponent.closeInventory();
            return;
        }
        
        // Confirm button
        if (slot == 20) {
            EquipmentSet playerEquipment = player.equals(match.getPlayer1()) ? 
                match.getPlayer1Equipment() : match.getPlayer2Equipment();
            
            if (playerEquipment != null) {
                MessageUtil.sendMessage(player, "&aEquipment bestaetigt!");
                
                // Check if both selected
                if (match.getPlayer1Equipment() != null && match.getPlayer2Equipment() != null) {
                    player.closeInventory();
                    opponent.closeInventory();
                    
                    plugin.getMatchManager().handleEquipmentSelection(
                        match.getPlayer1(), 
                        match.getPlayer2(), 
                        match.getPlayer1Equipment(), 
                        match.getPlayer2Equipment()
                    );
                }
            } else {
                MessageUtil.sendMessage(player, "&cBitte waehle zuerst ein Equipment!");
            }
            return;
        }
        
        // Close button
        if (slot == 22) {
            player.closeInventory();
            return;
        }
        
        // Equipment selection slots (11-15)
        if (slot >= 11 && slot <= 15) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                
                // Find equipment by display name
                for (EquipmentSet equipment : plugin.getEquipmentManager().getEquipmentSets().values()) {
                    if (displayName.contains(equipment.getDisplayName())) {
                        if (player.equals(match.getPlayer1())) {
                            match.setPlayer1Equipment(equipment);
                        } else {
                            match.setPlayer2Equipment(equipment);
                        }
                        
                        // Update visual feedback
                        updateEquipmentSelection(event.getInventory(), slot);
                        
                        MessageUtil.sendMessage(player, "&aEquipment gewaehlt: &e" + equipment.getDisplayName());
                        MessageUtil.sendMessage(opponent, "&e" + player.getName() + " &7hat Equipment gewaehlt.");
                        break;
                    }
                }
            }
        }
    }
    
    private void updateEquipmentSelection(Inventory inventory, int selectedSlot) {
        for (int i = 11; i <= 15; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String name = meta.getDisplayName();
                
                name = name.replaceAll("§[0-9a-fk-or]", "");
                
                if (i == selectedSlot) {
                    meta.setDisplayName(MessageUtil.color("&a&l" + name));
                } else {
                    meta.setDisplayName(MessageUtil.color("&7" + name));
                }
                
                item.setItemMeta(meta);
            }
        }
    }
    
    private void handleSpectatorGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        if (slot < 0) return;
        
        Match match = plugin.getMatchManager().getMatchByPlayer(player);
        if (match == null) return;
        
        if (slot == 22) {
            plugin.getMatchManager().endMatch(match, null, true);
            player.closeInventory();
            return;
        }
        
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