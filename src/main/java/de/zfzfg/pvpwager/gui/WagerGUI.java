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
        
        // Open for both players
        openForPlayer(player1, match);
        openForPlayer(player2, match);
    }
    
    private void openForPlayer(Player player, Match match) {
        Player opponent = match.getOpponent(player);
        if (opponent == null) return;
        
        // Create inventory
        Inventory inventory = Bukkit.createInventory(null, 54, 
            MessageUtil.color("&6&lWager Setup - " + match.getPlayer1().getName() + " vs " + match.getPlayer2().getName()));
        
        // Fill backgrounds
        fillPlayerSections(inventory, match.getPlayer1(), match.getPlayer2());
        
        // Setup control buttons
        setupControlButtons(inventory, match, player);
        
        // Setup money display and buttons
        setupMoneyControls(inventory, match, player);
        
        // Show existing wager items
        updateWagerDisplay(inventory, match);
        
        player.openInventory(inventory);
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
    
    private void setupControlButtons(Inventory inventory, Match match, Player viewer) {
        // Skip Wager Button (Center top - Slot 4)
        ItemStack skipButton = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta skipMeta = skipButton.getItemMeta();
        if (skipMeta != null) {
            boolean hasVoted = match.hasPlayerVotedToSkip(viewer);
            
            if (hasVoted) {
                skipMeta.setDisplayName(MessageUtil.color("&e&l✓ SKIP VOTED"));
                skipMeta.setLore(Arrays.asList(
                    MessageUtil.color("&7Du hast für Skip gestimmt"),
                    MessageUtil.color("&7Warte auf deinen Gegner..."),
                    MessageUtil.color(""),
                    MessageUtil.color("&eKlicke erneut um abzubrechen")
                ));
            } else if (match.getWagerSkipVotes().size() > 0) {
                skipMeta.setDisplayName(MessageUtil.color("&e&lSKIP WAGER"));
                skipMeta.setLore(Arrays.asList(
                    MessageUtil.color("&7Dein Gegner möchte ohne"),
                    MessageUtil.color("&7Wetteinsatz kämpfen!"),
                    MessageUtil.color(""),
                    MessageUtil.color("&aKlicke um zuzustimmen")
                ));
            } else {
                skipMeta.setDisplayName(MessageUtil.color("&e&lSKIP WAGER"));
                skipMeta.setLore(Arrays.asList(
                    MessageUtil.color("&7Kämpfe ohne Wetteinsatz!"),
                    MessageUtil.color(""),
                    MessageUtil.color("&7Beide Spieler müssen"),
                    MessageUtil.color("&7zustimmen um zu skippen"),
                    MessageUtil.color(""),
                    MessageUtil.color("&eKlicke zum Abstimmen")
                ));
            }
            skipButton.setItemMeta(skipMeta);
        }
        inventory.setItem(4, skipButton);
        
        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(MessageUtil.color("&a&lCONFIRM WAGER"));
            confirmMeta.setLore(Arrays.asList(
                MessageUtil.color("&7Klicke um deinen Einsatz"),
                MessageUtil.color("&7zu bestätigen")
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
                MessageUtil.color("&7Klicke um das Match"),
                MessageUtil.color("&7abzubrechen")
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
            lore.add(MessageUtil.color("&7Dein Einsatz: &6$" + currentBet));
            
            if (plugin.hasEconomy()) {
                double balance = plugin.getEconomy().getBalance(viewer);
                lore.add(MessageUtil.color("&7Kontostand: &6$" + balance));
            }
            
            lore.add("");
            lore.add(MessageUtil.color("&7Nutze die Buttons zum Anpassen"));
            
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
                MessageUtil.color("&7Klicke um &6$" + Math.abs(amount)),
                MessageUtil.color("&7hinzuzufügen/abzuziehen")
            ));
            button.setItemMeta(meta);
        }
        inventory.setItem(slot, button);
    }
    
    private void updateWagerDisplay(Inventory inventory, Match match) {
        // Clear old items first
        for (int i = 0; i < 27; i++) {
            if (i % 9 < 4) {
                ItemStack item = inventory.getItem(i);
                if (item != null && !item.getType().toString().contains("STAINED_GLASS_PANE")) {
                    inventory.setItem(i, null);
                }
            }
        }
        for (int i = 27; i < 54; i++) {
            if ((i % 9) >= 5) {
                ItemStack item = inventory.getItem(i);
                if (item != null && !item.getType().toString().contains("STAINED_GLASS_PANE")) {
                    inventory.setItem(i, null);
                }
            }
        }
        
        // Display player1's items (slots 0-26, but only in first 4 columns)
        List<ItemStack> p1Items = match.getWagerItems(match.getPlayer1());
        int p1Index = 0;
        for (int i = 0; i < 27 && p1Index < p1Items.size(); i++) {
            if (i % 9 < 4) {
                inventory.setItem(i, p1Items.get(p1Index++).clone());
            }
        }
        
        // Display player2's items (slots 27-53, but only in last 4 columns)
        List<ItemStack> p2Items = match.getWagerItems(match.getPlayer2());
        int p2Index = 0;
        for (int i = 27; i < 54 && p2Index < p2Items.size(); i++) {
            if ((i % 9) >= 5) {
                inventory.setItem(i, p2Items.get(p2Index++).clone());
            }
        }
    }
    
    /**
     * Optimized refresh - only updates inventory contents without closing/reopening
     */
    public void refresh(Match match) {
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();
        
        // Check if players have the GUI open
        if (player1.getOpenInventory() != null && 
            player1.getOpenInventory().getTitle().contains("Wager Setup")) {
            refreshPlayerInventory(player1, match);
        }
        
        if (player2.getOpenInventory() != null && 
            player2.getOpenInventory().getTitle().contains("Wager Setup")) {
            refreshPlayerInventory(player2, match);
        }
    }
    
    private void refreshPlayerInventory(Player player, Match match) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        
        // Update wager items display
        updateWagerDisplay(inventory, match);
        
        // Update money display
        ItemStack moneyDisplay = inventory.getItem(50);
        if (moneyDisplay != null) {
            double currentBet = match.getWagerMoney(player);
            ItemMeta meta = moneyDisplay.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add(MessageUtil.color("&7Dein Einsatz: &6$" + currentBet));
                
                if (plugin.hasEconomy()) {
                    double balance = plugin.getEconomy().getBalance(player);
                    lore.add(MessageUtil.color("&7Kontostand: &6$" + balance));
                }
                
                lore.add("");
                lore.add(MessageUtil.color("&7Nutze die Buttons zum Anpassen"));
                
                meta.setLore(lore);
                moneyDisplay.setItemMeta(meta);
            }
        }
        
        // Update skip button
        ItemStack skipButton = inventory.getItem(4);
        if (skipButton != null) {
            ItemMeta meta = skipButton.getItemMeta();
            if (meta != null) {
                boolean hasVoted = match.hasPlayerVotedToSkip(player);
                
                if (hasVoted) {
                    meta.setDisplayName(MessageUtil.color("&e&l✓ SKIP VOTED"));
                    meta.setLore(Arrays.asList(
                        MessageUtil.color("&7Du hast für Skip gestimmt"),
                        MessageUtil.color("&7Warte auf deinen Gegner..."),
                        MessageUtil.color(""),
                        MessageUtil.color("&eKlicke erneut um abzubrechen")
                    ));
                } else if (match.getWagerSkipVotes().size() > 0) {
                    meta.setDisplayName(MessageUtil.color("&e&lSKIP WAGER"));
                    meta.setLore(Arrays.asList(
                        MessageUtil.color("&7Dein Gegner möchte ohne"),
                        MessageUtil.color("&7Wetteinsatz kämpfen!"),
                        MessageUtil.color(""),
                        MessageUtil.color("&aKlicke um zuzustimmen")
                    ));
                } else {
                    meta.setDisplayName(MessageUtil.color("&e&lSKIP WAGER"));
                    meta.setLore(Arrays.asList(
                        MessageUtil.color("&7Kämpfe ohne Wetteinsatz!"),
                        MessageUtil.color(""),
                        MessageUtil.color("&7Beide Spieler müssen"),
                        MessageUtil.color("&7zustimmen um zu skippen"),
                        MessageUtil.color(""),
                        MessageUtil.color("&eKlicke zum Abstimmen")
                    ));
                }
                skipButton.setItemMeta(meta);
            }
        }
    }
}