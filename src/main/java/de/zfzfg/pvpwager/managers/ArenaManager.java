package de.zfzfg.pvpwager.managers;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Arena;
import de.zfzfg.pvpwager.models.Boundaries;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

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
                
                // Load world settings
                ConfigurationSection worldsSection = arenaSection.getConfigurationSection("worlds");
                String arenaWorld = worldsSection.getString("arena-world");
                boolean regenerateWorld = worldsSection.getBoolean("regenerate-world", false);
                
                // Load spectator spawn
                ConfigurationSection spectatorSection = arenaSection.getConfigurationSection("spawns.spectator");
                Location spectatorSpawn = loadLocation(spectatorSection, arenaWorld);
                
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
                
                // Parse Spawn-Type
                ConfigurationSection spawnSettings = arenaSection.getConfigurationSection("spawn-settings");
                Arena.SpawnType spawnType = Arena.SpawnType.FIXED_SPAWNS;
                Arena.SpawnConfig spawnConfig = new Arena.SpawnConfig();
                
                if (spawnSettings != null && spawnSettings.contains("spawn-type")) {
                    String spawnTypeStr = spawnSettings.getString("spawn-type", "FIXED_SPAWNS");
                    spawnType = Arena.SpawnType.valueOf(spawnTypeStr);
                    
                    switch (spawnType) {
                        case FIXED_SPAWNS:
                            List<Location> fixedSpawns = new ArrayList<>();
                            ConfigurationSection player1Section = arenaSection.getConfigurationSection("spawns.player1");
                            ConfigurationSection player2Section = arenaSection.getConfigurationSection("spawns.player2");
                            if (player1Section != null) fixedSpawns.add(loadLocation(player1Section, arenaWorld));
                            if (player2Section != null) fixedSpawns.add(loadLocation(player2Section, arenaWorld));
                            spawnConfig.setFixedSpawns(fixedSpawns);
                            break;
                            
                        case RANDOM_RADIUS:
                            ConfigurationSection radiusSection = spawnSettings.getConfigurationSection("random-radius");
                            if (radiusSection != null) {
                                Arena.RandomRadiusConfig radiusConfig = new Arena.RandomRadiusConfig(
                                    radiusSection.getDouble("center-x", 0),
                                    radiusSection.getDouble("center-z", 0),
                                    radiusSection.getDouble("radius", 100),
                                    radiusSection.getDouble("min-distance", 10)
                                );
                                spawnConfig.setRandomRadius(radiusConfig);
                            }
                            break;
                            
                        case RANDOM_AREA:
                            ConfigurationSection areaSection = spawnSettings.getConfigurationSection("random-area");
                            if (areaSection != null) {
                                ConfigurationSection point1 = areaSection.getConfigurationSection("point1");
                                ConfigurationSection point2 = areaSection.getConfigurationSection("point2");
                                Arena.RandomAreaConfig areaConfig = new Arena.RandomAreaConfig(
                                    point1.getDouble("x", 0),
                                    point1.getDouble("z", 0),
                                    point2.getDouble("x", 100),
                                    point2.getDouble("z", 100),
                                    areaSection.getDouble("min-distance", 10)
                                );
                                spawnConfig.setRandomArea(areaConfig);
                            }
                            break;
                            
                        case RANDOM_CUBE:
                            ConfigurationSection cubeSection = spawnSettings.getConfigurationSection("random-cube");
                            if (cubeSection != null) {
                                ConfigurationSection point1 = cubeSection.getConfigurationSection("point1");
                                ConfigurationSection point2 = cubeSection.getConfigurationSection("point2");
                                Arena.RandomCubeConfig cubeConfig = new Arena.RandomCubeConfig(
                                    point1.getDouble("x", 0),
                                    point1.getDouble("y", 50),
                                    point1.getDouble("z", 0),
                                    point2.getDouble("x", 100),
                                    point2.getDouble("y", 150),
                                    point2.getDouble("z", 100),
                                    cubeSection.getDouble("min-distance", 10)
                                );
                                spawnConfig.setRandomCube(cubeConfig);
                            }
                            break;
                            
                        case MULTIPLE_SPAWNS:
                            List<Location> multipleSpawns = new ArrayList<>();
                            ConfigurationSection spawnsSection = arenaSection.getConfigurationSection("spawns");
                            if (spawnsSection != null) {
                                for (String key : spawnsSection.getKeys(false)) {
                                    if (!key.equals("spectator")) {
                                        ConfigurationSection spawnSection = spawnsSection.getConfigurationSection(key);
                                        multipleSpawns.add(loadLocation(spawnSection, arenaWorld));
                                    }
                                }
                            }
                            spawnConfig.setMultipleSpawns(multipleSpawns);
                            break;
                            
                        case COMMAND:
                            ConfigurationSection cmdSection = spawnSettings.getConfigurationSection("command");
                            if (cmdSection != null) {
                                String command = cmdSection.getString("command", "");
                                Map<String, String> placeholders = new HashMap<>();
                                
                                ConfigurationSection placeholderSection = cmdSection.getConfigurationSection("placeholders");
                                if (placeholderSection != null) {
                                    for (String key : placeholderSection.getKeys(false)) {
                                        placeholders.put(key, placeholderSection.getString(key));
                                    }
                                }
                                
                                Arena.CommandConfig commandConfig = new Arena.CommandConfig(command, placeholders);
                                spawnConfig.setCommandConfig(commandConfig);
                            }
                            break;
                    }
                } else {
                    // Legacy: Alte Config ohne spawn-type
                    List<Location> fixedSpawns = new ArrayList<>();
                    ConfigurationSection player1Section = arenaSection.getConfigurationSection("spawns.player1");
                    ConfigurationSection player2Section = arenaSection.getConfigurationSection("spawns.player2");
                    if (player1Section != null) fixedSpawns.add(loadLocation(player1Section, arenaWorld));
                    if (player2Section != null) fixedSpawns.add(loadLocation(player2Section, arenaWorld));
                    spawnConfig.setFixedSpawns(fixedSpawns);
                }
                
                Arena arena = new Arena(arenaId, displayName, arenaWorld, regenerateWorld,
                                      spectatorSpawn, boundaries, spawnType, spawnConfig);
                arenas.put(arenaId, arena);
                
                plugin.getLogger().info("Loaded arena: " + arenaId + " (" + displayName + ") - SpawnType: " + spawnType);
                
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
                world = Bukkit.getWorlds().get(0); // Fallback
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
    
    /**
     * Regeneriert Arena-Welt mit Multiverse Commands
     */
    public void regenerateArenaWorld(String worldName) {
        plugin.getLogger().info("Starting Multiverse regeneration for: " + worldName);
        
        // Prüfe ob Multiverse verfügbar ist
        if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") == null) {
            plugin.getLogger().severe("Multiverse-Core not found! Cannot regenerate world: " + worldName);
            return;
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' is not loaded, skipping regeneration");
            return;
        }
        
        // Teleportiere alle Spieler aus der Welt
        World mainWorld = Bukkit.getWorld(plugin.getConfig().getString("settings.main-world", "world"));
        if (mainWorld == null) {
            mainWorld = Bukkit.getWorlds().get(0);
        }
        
        for (org.bukkit.entity.Player p : world.getPlayers()) {
            p.teleport(mainWorld.getSpawnLocation());
            plugin.getLogger().info("Teleported " + p.getName() + " out of " + worldName);
        }
        
        // Warte kurz, dann starte Regeneration
        final World finalMainWorld = mainWorld;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("Executing: /mv regen " + worldName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv regen " + worldName);
            
            // Nach 2 Sekunden: Confirm
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info("Executing: /mv confirm");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv confirm");
                
                plugin.getLogger().info("Arena regeneration completed: " + worldName);
                
            }, 40L); // 2 Sekunden
            
        }, 40L); // 2 Sekunden Verzögerung
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
            
            // Try to load with Multiverse first
            if (Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null) {
                plugin.getLogger().info("Loading arena world via Multiverse: " + worldName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load " + worldName);
                
                // Wait a bit for world to load
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (Bukkit.getWorld(worldName) != null) {
                        plugin.getLogger().info("Successfully loaded world: " + worldName);
                    } else {
                        plugin.getLogger().warning("Multiverse failed to load world: " + worldName);
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
                plugin.getLogger().info("Unloading arena world via Multiverse: " + worldName);
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