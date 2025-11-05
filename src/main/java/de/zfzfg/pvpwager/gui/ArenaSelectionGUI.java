package de.zfzfg.pvpwager.gui;

import de.zfzfg.pvpwager.PvPWager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArenaSelectionGUI {
    private final PvPWager plugin;
    
    public ArenaSelectionGUI(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player player1, Player player2) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§6Select Arena");
        
        // Fill with background
        for (int i = 0; i < 27; i++) {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            meta.setDisplayName("§8");
            glass.setItemMeta(meta);
            inventory.setItem(i, glass);
        }
        
        // Add arena items (placeholder)
        ItemStack standardArena = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta standardMeta = standardArena.getItemMeta();
        standardMeta.setDisplayName("§a§lStandard Arena");
        standardArena.setItemMeta(standardMeta);
        inventory.setItem(11, standardArena);
        
        ItemStack skybaseArena = new ItemStack(Material.IRON_BLOCK);
        ItemMeta skybaseMeta = skybaseArena.getItemMeta();
        skybaseMeta.setDisplayName("§b§lSkybase Arena");
        skybaseArena.setItemMeta(skybaseMeta);
        inventory.setItem(12, skybaseArena);
        
        ItemStack netherArena = new ItemStack(Material.NETHERRACK);
        ItemMeta netherMeta = netherArena.getItemMeta();
        netherMeta.setDisplayName("§c§lNether Arena");
        netherArena.setItemMeta(netherMeta);
        inventory.setItem(13, netherArena);
        
        // Add control buttons
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lCANCEL");
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(18, cancel);
        
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lCONFIRM");
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(20, confirm);
        
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c§lCLOSE");
        close.setItemMeta(closeMeta);
        inventory.setItem(22, close);
        
        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }
}