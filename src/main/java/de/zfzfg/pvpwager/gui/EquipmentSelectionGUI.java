package de.zfzfg.pvpwager.gui;

import de.zfzfg.pvpwager.PvPWager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EquipmentSelectionGUI {
    private final PvPWager plugin;
    
    public EquipmentSelectionGUI(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player player1, Player player2) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§6Select Equipment");
        
        // Fill with background
        for (int i = 0; i < 27; i++) {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            meta.setDisplayName("§8");
            glass.setItemMeta(meta);
            inventory.setItem(i, glass);
        }
        
        // Add equipment items (placeholder)
        ItemStack standardSet = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta standardMeta = standardSet.getItemMeta();
        standardMeta.setDisplayName("§b§lStandard Set");
        standardSet.setItemMeta(standardMeta);
        inventory.setItem(11, standardSet);
        
        ItemStack diamondSet = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta diamondMeta = diamondSet.getItemMeta();
        diamondMeta.setDisplayName("§3§lDiamond Set");
        diamondSet.setItemMeta(diamondMeta);
        inventory.setItem(12, diamondSet);
        
        ItemStack netheriteSet = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta netheriteMeta = netheriteSet.getItemMeta();
        netheriteMeta.setDisplayName("§4§lNetherite Set");
        netheriteSet.setItemMeta(netheriteMeta);
        inventory.setItem(13, netheriteSet);
        
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