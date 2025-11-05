package de.zfzfg.pvpwager.gui;

import de.zfzfg.pvpwager.PvPWager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpectatorGUI {
    private final PvPWager plugin;
    
    public SpectatorGUI(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player spectator, Player player1, Player player2) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§6Match: " + player1.getName() + " vs " + player2.getName());
        
        // Fill with background
        for (int i = 0; i < 27; i++) {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            meta.setDisplayName("§8");
            glass.setItemMeta(meta);
            inventory.setItem(i, glass);
        }
        
        // Add player info items
        ItemStack player1Head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta p1Meta = player1Head.getItemMeta();
        p1Meta.setDisplayName("§e§l" + player1.getName());
        player1Head.setItemMeta(p1Meta);
        inventory.setItem(11, player1Head);
        
        ItemStack player2Head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta p2Meta = player2Head.getItemMeta();
        p2Meta.setDisplayName("§e§l" + player2.getName());
        player2Head.setItemMeta(p2Meta);
        inventory.setItem(15, player2Head);
        
        // Add match info
        ItemStack info = new ItemStack(Material.COMPASS);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§lMatch Information");
        info.setItemMeta(infoMeta);
        inventory.setItem(13, info);
        
        // Add control buttons
        ItemStack leave = new ItemStack(Material.BARRIER);
        ItemMeta leaveMeta = leave.getItemMeta();
        leaveMeta.setDisplayName("§c§lLEAVE MATCH");
        leave.setItemMeta(leaveMeta);
        inventory.setItem(22, leave);
        
        ItemStack refresh = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName("§a§lREFRESH");
        refresh.setItemMeta(refreshMeta);
        inventory.setItem(24, refresh);
        
        spectator.openInventory(inventory);
    }
}