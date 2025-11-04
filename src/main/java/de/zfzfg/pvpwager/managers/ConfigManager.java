package de.zfzfg.pvpwager.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.utils.MessageUtil;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final PvPWager plugin;
    private FileConfiguration arenaConfig;
    private File arenaConfigFile;
    private FileConfiguration equipmentConfig;
    private File equipmentConfigFile;
    private FileConfiguration messagesConfig;
    private File messagesConfigFile;
    
    public ConfigManager(PvPWager plugin) {
        this.plugin = plugin;
        loadConfig();
        loadArenaConfig();
        loadEquipmentConfig();
        loadMessagesConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }
    
    private void loadArenaConfig() {
        arenaConfigFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenaConfigFile.exists()) {
            arenaConfigFile.getParentFile().mkdirs();
            plugin.saveResource("arenas.yml", false);
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaConfigFile);
    }
    
    private void loadEquipmentConfig() {
        equipmentConfigFile = new File(plugin.getDataFolder(), "equipment.yml");
        if (!equipmentConfigFile.exists()) {
            equipmentConfigFile.getParentFile().mkdirs();
            plugin.saveResource("equipment.yml", false);
        }
        equipmentConfig = YamlConfiguration.loadConfiguration(equipmentConfigFile);
    }
    
    private void loadMessagesConfig() {
        messagesConfigFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesConfigFile.exists()) {
            messagesConfigFile.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesConfigFile);
    }
    
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
    
    public FileConfiguration getArenaConfig() {
        return arenaConfig;
    }
    
    public FileConfiguration getEquipmentConfig() {
        return equipmentConfig;
    }
    
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    /**
     * Get a message from messages.yml with automatic color codes
     * @param path The path in the messages.yml (e.g. "request.sent")
     * @return The colored message
     */
    public String getMessage(String path) {
        String message = messagesConfig.getString("messages." + path);
        if (message == null) {
            plugin.getLogger().warning("Missing message: messages." + path);
            return MessageUtil.color("&cMissing message: " + path);
        }
        return MessageUtil.color(message);
    }
    
    /**
     * Get a message with placeholder replacements
     * @param path The path in the messages.yml
     * @param placeholders Key-value pairs for replacements (e.g. "player", "Steve")
     * @return The colored and processed message
     */
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = "{" + placeholders[i] + "}";
            String value = placeholders[i + 1];
            message = message.replace(placeholder, value);
        }
        
        return message;
    }
    
    /**
     * Get the configured prefix
     */
    public String getPrefix() {
        return getMessage("prefix");
    }
    
    /**
     * Reload all configurations
     */
    public void reloadAll() {
        plugin.reloadConfig();
        arenaConfig = YamlConfiguration.loadConfiguration(arenaConfigFile);
        equipmentConfig = YamlConfiguration.loadConfiguration(equipmentConfigFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesConfigFile);
    }
    
    /**
     * Save arena configuration
     */
    public void saveArenaConfig() {
        try {
            arenaConfig.save(arenaConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save arenas.yml: " + e.getMessage());
        }
    }
    
    /**
     * Save equipment configuration
     */
    public void saveEquipmentConfig() {
        try {
            equipmentConfig.save(equipmentConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save equipment.yml: " + e.getMessage());
        }
    }
    
    /**
     * Save messages configuration
     */
    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
}