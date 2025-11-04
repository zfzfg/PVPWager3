package de.zfzfg.pvpwager.managers;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Arena;
import de.zfzfg.pvpwager.models.Boundaries;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ArenaManager {
    private final PvPWager plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    
    public ArenaManager(PvPWager plugin) {
        this.plugin = plugin;
        loadArenas();
    }
    
    public void loadArenas() {
        arenas.clear();
        FileConfiguration arenaConfig = plugin.getConfigManager().getArenaConfig();
        
        ConfigurationSection arenasSection = arenaConfig.getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.getLogger().warning("No arenas section found in arenas.yml!");
            return;
        }
        
        for (String arenaId : arenasSection.getKeys(false)) {
            try {
                ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaId);
                if (arenaSection == null) continue;
                
                boolean enabled = arenaSection.getBoolean("enabled", true);
                if (!enabled) {
                    plugin.getLogger().info("Arena '" + arenaId + "' is disabled, skipping...");
                    continue;
                }
                
                String displayName = arenaSection.getString("display-name", arenaId);
                String description = arenaSection.getString("description", "");
                
                // Load world settings
                ConfigurationSection worldsSection = arenaSection.getConfigurationSection("worlds");
                String arenaWorld = worldsSection.getString("arena-world");
                boolean regenerateWorld = worldsSection.getBoolean("regenerate-world", false);
                
                // Load spawns
                ConfigurationSection spawnsSection = arenaSection.getConfigurationSection("spawns");
                Location player1Spawn = loadLocation(spawnsSection.getConfigurationSection("player1"), arenaWorld);
                Location player2Spawn = loadLocation(spawnsSection.getConfigurationSection("player2"), arenaWorld);
                Location spectatorSpawn = loadLocation(spawnsSection.getConfigurationSection("spectator"), arenaWorld);
                
                if (player1Spawn == null || player2Spawn == null || spectatorSpawn == null) {
                    plugin.getLogger().warning("Arena '" + arenaId + "' has invalid spawn locations!");
                    continue;
                }
                
                // Load boundaries
                Boundaries boundaries = null;
                if (arenaSection.contains("boundaries") && arenaSection.getBoolean("boundaries.enabled", false)) {
                    ConfigurationSection boundariesSection = arenaSection.getConfigurationSection("boundaries");
                    
                    double minX = boundariesSection.getDouble("min-x");
                    double maxX = boundariesSection.getDouble("max-x");
                    double minY = boundariesSection.getDouble("min-y");
                    double maxY = boundariesSection.getDouble("max-y");
                    double minZ = boundariesSection.getDouble("min-z");
                    double maxZ = boundariesSection.getDouble("max-z");
                    
                    World world = Bukkit.getWorld(arenaWorld);
                    if (world != null) {
                        Location minLoc = new Location(world, minX, minY, minZ);
                        Location maxLoc = new Location(world, maxX, maxY, maxZ);
                        boundaries = new Boundaries(minLoc, maxLoc);
                    }
                }
                
                Arena arena = new Arena(arenaId, displayName, arenaWorld, regenerateWorld, 
                                      player1Spawn, player2Spawn, spectatorSpawn, boundaries);
                arenas.put(arenaId, arena);
                
                plugin.getLogger().info("Loaded arena: " + arenaId + " (" + displayName + ")");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error loading arena '" + arenaId + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (arenas.isEmpty()) {
            plugin.getLogger().warning("No arenas loaded! Plugin may not work correctly.");
        }
    }
    
    private Location loadLocation(ConfigurationSection section, String worldName) {
        if (section == null) return null;
        
        try {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for arena spawn!");
                // Create location anyway, world will be loaded later
                world = Bukkit.getWorlds().get(0); // Fallback to main world
            }
            
            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw", 0.0);
            float pitch = (float) section.getDouble("pitch", 0.0);
            
            return new Location(world, x, y, z, yaw, pitch);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error parsing location: " + e.getMessage());
            return null;
        }
    }
    
    public void loadArenaWorld(String worldName) {
        String worldLoading = plugin.getConfig().getString("settings.world-loading", "both");
        
        if (worldLoading.equalsIgnoreCase("none")) {
            return;
        }
        
        if (worldLoading.equalsIgnoreCase("arena") || worldLoading.equalsIgnoreCase("both")) {
            // Check if world is already loaded
            if (Bukkit.getWorld(worldName) != null) {
                plugin.getLogger().info("World '" + worldName + "' is already loaded.");
                return;
            }
            
            // Try to load with Multiverse
            if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
                plugin.getLogger().info("Loading arena world: " + worldName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load " + worldName);
                
                // Wait a bit for world to load
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (Bukkit.getWorld(worldName) != null) {
                        plugin.getLogger().info("Successfully loaded world: " + worldName);
                    } else {
                        plugin.getLogger().warning("Failed to load world: " + worldName);
                    }
                }, 40L);
            } else {
                plugin.getLogger().warning("Multiverse-Core not found! Cannot load world: " + worldName);
            }
        }
    }
    
    public void unloadArenaWorld(String worldName) {
        String worldLoading = plugin.getConfig().getString("settings.world-loading", "both");
        
        if (worldLoading.equalsIgnoreCase("none")) {
            return;
        }
        
        if (worldLoading.equalsIgnoreCase("arena") || worldLoading.equalsIgnoreCase("both")) {
            // Check if world is loaded
            if (Bukkit.getWorld(worldName) == null) {
                return;
            }
            
            // Try to unload with Multiverse
            if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
                plugin.getLogger().info("Unloading arena world: " + worldName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv unload " + worldName);
            }
        }
    }
    
    public Arena getArena(String arenaId) {
        return arenas.get(arenaId);
    }
    
    public Map<String, Arena> getArenas() {
        return new HashMap<>(arenas);
    }
    
    public void reloadArenas() {
        loadArenas();
    }
}