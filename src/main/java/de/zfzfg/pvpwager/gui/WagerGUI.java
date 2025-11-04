package de.zfzfg.pvpwager.gui;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Match;
import de.zfzfg.pvpwager.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WagerGUI {
    private final PvPWager plugin;
    
    public WagerGUI(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player player1, Player player2) {
        Match match = plugin.getMatchManager().getMatch(player1, player2);
        if (match == null) return;
        
        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, 54, MessageUtil.color("&6&lWager Setup - " + player1.getName() + " vs " + player2.getName()));
        
        // Fill backgrounds
        fillPlayerSections(inventory, player1, player2);
        
        // Setup control buttons
        setupControlButtons(inventory, match);
        
        // Setup money display and buttons
        setupMoneyControls(inventory, match, player1);
        
        // Show existing wager items
        updateWagerDisplay(inventory, match, player1, player2);
        
        // Open for both players
        player1.openInventory(inventory);
        
        // Clone for player2 with updated money
        Inventory inventory2 = Bukkit.createInventory(null, 54, MessageUtil.color("&6&lWager Setup - " + player1.getName() + " vs " + player2.getName()));
        fillPlayerSections(inventory2, player1, player2);
        setupControlButtons(inventory2, match);
        setupMoneyControls(inventory2, match, player2);
        updateWagerDisplay(inventory2, match, player1, player2);
        player2.openInventory(inventory2);
    }
    
    private void fillPlayerSections(Inventory inventory, Player player1, Player player2) {
        // Player 1's section (left side, slots 0-26)
        for (int i = 0; i < 27; i++) {
            if (i % 9 < 4) { // First 4 columns
                ItemStack glass = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
                ItemMeta meta = glass.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(MessageUtil.color("&b" + player1.getName() + "'s Items"));
                    glass.setItemMeta(meta);
                }
                inventory.setItem(i, glass);
            }
        }
        
        // Player 2's section (right side, slots 27-53)
        for (int i = 27; i < 54; i++) {
            if ((i % 9) >= 5) { // Last 4 columns
                ItemStack glass = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
                ItemMeta meta = glass.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(MessageUtil.color("&d" + player2.getName() + "'s Items"));
                    glass.setItemMeta(meta);
                }
                inventory.setItem(i, glass);
            }
        }
    }
    
    private void setupControlButtons(Inventory inventory, Match match) {
        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(MessageUtil.color("&a&lCONFIRM WAGER"));
            confirmMeta.setLore(Arrays.asList(
                MessageUtil.color("&7Click to confirm your wager"),
                MessageUtil.color("&7and proceed to arena selection")
            ));
            confirm.setItemMeta(confirmMeta);
        }
        inventory.setItem(49, confirm);
        
        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName(MessageUtil.color("&c&lCANCEL"));
            cancelMeta.setLore(Arrays.asList(
                MessageUtil.color("&7Click to cancel the match")
            ));
            cancel.setItemMeta(cancelMeta);
        }
        inventory.setItem(48, cancel);
    }
    
    private void setupMoneyControls(Inventory inventory, Match match, Player viewer) {
        // Money display
        double currentBet = match.getWagerMoney(viewer);
        
        ItemStack moneyDisplay = new ItemStack(Material.GOLD_INGOT);
        ItemMeta moneyMeta = moneyDisplay.getItemMeta();
        if (moneyMeta != null) {
            moneyMeta.setDisplayName(MessageUtil.color("&6&lMONEY BET"));
            
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.color("&7Your bet: &6$" + currentBet));
            
            if (plugin.hasEconomy()) {
                double balance = plugin.getEconomy().getBalance(viewer);
                lore.add(MessageUtil.color("&7Balance: &6$" + balance));
            }
            
            lore.add("");
            lore.add(MessageUtil.color("&7Use buttons below to adjust"));
            
            moneyMeta.setLore(lore);
            moneyDisplay.setItemMeta(moneyMeta);
        }
        inventory.setItem(50, moneyDisplay);
        
        // Money control buttons
        createMoneyButton(inventory, 45, 10, "&a+$10");
        createMoneyButton(inventory, 46, 100, "&a+$100");
        createMoneyButton(inventory, 47, 1000, "&a+$1000");
        createMoneyButton(inventory, 51, -10, "&c-$10");
        createMoneyButton(inventory, 52, -100, "&c-$100");
        createMoneyButton(inventory, 53, -1000, "&c-$1000");
    }
    
    private void createMoneyButton(Inventory inventory, int slot, int amount, String displayName) {
        Material material = amount > 0 ? Material.LIME_DYE : Material.RED_DYE;
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.color(displayName));
            meta.setLore(Arrays.asList(
                MessageUtil.color("&7Click to adjust bet by &6$" + Math.abs(amount))
            ));
            button.setItemMeta(meta);
        }
        inventory.setItem(slot, button);
    }
    
    private void updateWagerDisplay(Inventory inventory, Match match, Player player1, Player player2) {
        // Display player1's items (slots 0-26, but only in first 4 columns)
        List<ItemStack> p1Items = match.getWagerItems(player1);
        int p1Index = 0;
        for (int i = 0; i < 27 && p1Index < p1Items.size(); i++) {
            if (i % 9 < 4) {
                inventory.setItem(i, p1Items.get(p1Index++).clone());
            }
        }
        
        // Display player2's items (slots 27-53, but only in last 4 columns)
        List<ItemStack> p2Items = match.getWagerItems(player2);
        int p2Index = 0;
        for (int i = 27; i < 54 && p2Index < p2Items.size(); i++) {
            if ((i % 9) >= 5) {
                inventory.setItem(i, p2Items.get(p2Index++).clone());
            }
        }
    }
    
    public void refresh(Player player, Match match) {
        if (player.getOpenInventory() == null) return;
        
        String title = player.getOpenInventory().getTitle();
        if (!title.contains("Wager Setup")) return;
        
        Player opponent = match.getOpponent(player);
        if (opponent == null) return;
        
        // Close and reopen
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            open(match.getPlayer1(), match.getPlayer2());
        });
    }
}