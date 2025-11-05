package de.zfzfg.pvpwager.gui;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.EquipmentSet;
import de.zfzfg.pvpwager.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EquipmentSelectionGUI {
    private final PvPWager plugin;
    
    public EquipmentSelectionGUI(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player player1, Player player2) {
        Inventory inventory = Bukkit.createInventory(null, 27, MessageUtil.color("&6Select Equipment"));
        
        // Fill with background
        for (int i = 0; i < 27; i++) {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
            inventory.setItem(i, glass);
        }
        
        // Add equipment items dynamically
        int slot = 11;
        for (EquipmentSet equipment : plugin.getEquipmentManager().getEquipmentSets().values()) {
            if (slot > 15) break; // Max 5 equipment sets in one row
            
            ItemStack equipmentItem = new ItemStack(Material.DIAMOND_CHESTPLATE);
            ItemMeta meta = equipmentItem.getItemMeta();
            
            meta.setDisplayName(MessageUtil.color("&b" + equipment.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.color("&7Equipment: &f" + equipment.getId()));
            lore.add("");
            lore.add(MessageUtil.color("&eKlicke zum Auswaehlen"));
            meta.setLore(lore);
            
            equipmentItem.setItemMeta(meta);
            inventory.setItem(slot, equipmentItem);
            slot++;
        }
        
        // Add control buttons
        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(MessageUtil.color("&c&lABBRECHEN"));
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(18, cancel);
        
        ItemStack confirm = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(MessageUtil.color("&a&lBESTAETIGEN"));
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(20, confirm);
        
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(MessageUtil.color("&c&lSCHLIESSEN"));
        close.setItemMeta(closeMeta);
        inventory.setItem(22, close);
        
        player1.openInventory(inventory);
        player2.openInventory(inventory);
    }
}