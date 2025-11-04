package de.zfzfg.pvpwager.managers;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.EquipmentSet;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipmentManager {
    private final PvPWager plugin;
    private final Map<String, EquipmentSet> equipmentSets = new HashMap<>();
    
    public EquipmentManager(PvPWager plugin) {
        this.plugin = plugin;
        loadEquipmentSets();
    }
    
    public void loadEquipmentSets() {
        equipmentSets.clear();
        FileConfiguration equipmentConfig = plugin.getConfigManager().getEquipmentConfig();
        
        ConfigurationSection setsSection = equipmentConfig.getConfigurationSection("equipment-sets");
        if (setsSection == null) {
            plugin.getLogger().warning("No equipment-sets section found in equipment.yml!");
            return;
        }
        
        for (String setId : setsSection.getKeys(false)) {
            try {
                ConfigurationSection setSection = setsSection.getConfigurationSection(setId);
                if (setSection == null) continue;
                
                boolean enabled = setSection.getBoolean("enabled", true);
                if (!enabled) {
                    plugin.getLogger().info("Equipment set '" + setId + "' is disabled, skipping...");
                    continue;
                }
                
                String displayName = setSection.getString("display-name", setId);
                String description = setSection.getString("description", "");
                
                // Load armor
                ConfigurationSection armorSection = setSection.getConfigurationSection("armor");
                ItemStack helmet = parseItem(armorSection.getString("helmet"));
                ItemStack chestplate = parseItem(armorSection.getString("chestplate"));
                ItemStack leggings = parseItem(armorSection.getString("leggings"));
                ItemStack boots = parseItem(armorSection.getString("boots"));
                
                // Load inventory items
                Map<Integer, ItemStack> inventory = new HashMap<>();
                List<Map<?, ?>> inventoryList = setSection.getMapList("inventory");
                
                for (Map<?, ?> itemMap : inventoryList) {
                    try {
                        int slot = ((Number) itemMap.get("slot")).intValue();
                        String itemName = (String) itemMap.get("item");
                        int amount = itemMap.containsKey("amount") ? ((Number) itemMap.get("amount")).intValue() : 1;
                        
                        ItemStack item = parseItem(itemName);
                        if (item != null) {
                            item.setAmount(amount);
                            
                            // Apply enchantments if any
                            if (itemMap.containsKey("enchantments")) {
                                @SuppressWarnings("unchecked")
                                List<String> enchantments = (List<String>) itemMap.get("enchantments");
                                applyEnchantments(item, enchantments);
                            }
                            
                            inventory.put(slot, item);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error parsing inventory item in set '" + setId + "': " + e.getMessage());
                    }
                }
                
                EquipmentSet equipmentSet = new EquipmentSet(setId, displayName, helmet, chestplate, leggings, boots, inventory);
                equipmentSets.put(setId, equipmentSet);
                
                plugin.getLogger().info("Loaded equipment set: " + setId + " (" + displayName + ")");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading equipment set '" + setId + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (equipmentSets.isEmpty()) {
            plugin.getLogger().warning("No equipment sets loaded! Plugin may not work correctly.");
        }
    }
    
    private ItemStack parseItem(String materialName) {
        if (materialName == null || materialName.trim().isEmpty()) {
            return null;
        }
        
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialName);
            return null;
        }
    }
    
    private void applyEnchantments(ItemStack item, List<String> enchantments) {
        for (String enchantString : enchantments) {
            try {
                String[] parts = enchantString.split(":");
                if (parts.length != 2) continue;
                
                String enchantName = parts[0].toUpperCase();
                int level = Integer.parseInt(parts[1]);
                
                Enchantment enchantment = Enchantment.getByName(enchantName);
                if (enchantment != null) {
                    item.addUnsafeEnchantment(enchantment, level);
                } else {
                    plugin.getLogger().warning("Unknown enchantment: " + enchantName);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error parsing enchantment: " + enchantString);
            }
        }
    }
    
    public EquipmentSet getEquipmentSet(String setId) {
        return equipmentSets.get(setId);
    }
    
    public Map<String, EquipmentSet> getEquipmentSets() {
        return new HashMap<>(equipmentSets);
    }
    
    public void reloadEquipmentSets() {
        loadEquipmentSets();
    }
}