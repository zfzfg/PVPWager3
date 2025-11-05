package de.zfzfg.pvpwager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import de.zfzfg.pvpwager.commands.PvPCommand;
import de.zfzfg.pvpwager.commands.SurrenderCommand;
import de.zfzfg.pvpwager.commands.DrawCommand;
import de.zfzfg.pvpwager.commands.PvPRequestCommand;
import de.zfzfg.pvpwager.listeners.GUIListener;
import de.zfzfg.pvpwager.listeners.PvPListener;
import de.zfzfg.pvpwager.listeners.WorldChangeListener;
import de.zfzfg.pvpwager.managers.ConfigManager;
import de.zfzfg.pvpwager.managers.MatchManager;
import de.zfzfg.pvpwager.managers.RequestManager;
import de.zfzfg.pvpwager.managers.ArenaManager;
import de.zfzfg.pvpwager.managers.EquipmentManager;
import de.zfzfg.pvpwager.managers.GUIManager;

public class PvPWager extends JavaPlugin {
    
    private static PvPWager instance;
    private ConfigManager configManager;
    private MatchManager matchManager;
    private RequestManager requestManager;
    private ArenaManager arenaManager;
    private EquipmentManager equipmentManager;
    private GUIManager guiManager;
    private Economy economy;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Load configuration
        configManager = new ConfigManager(this);
        
        // Setup Vault Economy
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            if (setupEconomy()) {
                getLogger().info("Vault Economy successfully hooked!");
            } else {
                getLogger().warning("Vault found but no Economy plugin detected!");
            }
        } else {
            getLogger().warning("Vault not found! Money wagers will not work.");
        }
        
        // Initialize managers
        arenaManager = new ArenaManager(this);
        equipmentManager = new EquipmentManager(this);
        matchManager = new MatchManager(this);
        requestManager = new RequestManager(this);
        guiManager = new GUIManager(this);
        
        // Register commands
        getCommand("pvp").setExecutor(new PvPCommand(this));
        getCommand("pvpa").setExecutor(new PvPRequestCommand(this));
        getCommand("surrender").setExecutor(new SurrenderCommand(this));
        getCommand("draw").setExecutor(new DrawCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PvPListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
        
        getLogger().info("========================================");
        getLogger().info("PvPWager v1.0.0 enabled successfully!");
        getLogger().info("Loaded " + arenaManager.getArenas().size() + " arenas");
        getLogger().info("Loaded " + equipmentManager.getEquipmentSets().size() + " equipment sets");
        getLogger().info("Economy: " + (economy != null ? "Enabled" : "Disabled"));
        getLogger().info("========================================");
    }
    
    @Override
    public void onDisable() {
        // End all active matches
        if (matchManager != null) {
            matchManager.stopAllMatches();
        }
        
        // Cleanup requests
        if (requestManager != null) {
            requestManager.cleanup();
        }
        
        getLogger().info("PvPWager plugin has been disabled!");
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    public static PvPWager getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MatchManager getMatchManager() {
        return matchManager;
    }
    
    public RequestManager getRequestManager() {
        return requestManager;
    }
    
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    public EquipmentManager getEquipmentManager() {
        return equipmentManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public boolean hasEconomy() {
        return economy != null;
    }
}