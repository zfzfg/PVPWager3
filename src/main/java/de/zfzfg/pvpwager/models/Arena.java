package de.zfzfg.pvpwager.models;

import org.bukkit.Location;

public class Arena {
    private String id;
    private String displayName;
    private String arenaWorld;
    private boolean regenerateWorld;
    private Location player1Spawn;
    private Location player2Spawn;
    private Location spectatorSpawn;
    private Boundaries boundaries;
    
    public Arena(String id, String displayName, String arenaWorld, boolean regenerateWorld,
                Location player1Spawn, Location player2Spawn, Location spectatorSpawn, Boundaries boundaries) {
        this.id = id;
        this.displayName = displayName;
        this.arenaWorld = arenaWorld;
        this.regenerateWorld = regenerateWorld;
        this.player1Spawn = player1Spawn;
        this.player2Spawn = player2Spawn;
        this.spectatorSpawn = spectatorSpawn;
        this.boundaries = boundaries;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getArenaWorld() { return arenaWorld; }
    public boolean isRegenerateWorld() { return regenerateWorld; }
    public Location getPlayer1Spawn() { return player1Spawn; }
    public Location getPlayer2Spawn() { return player2Spawn; }
    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public Boundaries getBoundaries() { return boundaries; }
}