package de.zfzfg.pvpwager.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class Match {
    private UUID matchId;
    private Player player1, player2;
    private MatchState state;
    private Arena arena;
    private EquipmentSet player1Equipment, player2Equipment;
    private Map<UUID, List<ItemStack>> wagerItems;
    private Map<UUID, Double> wagerMoney;
    private Set<UUID> spectators;
    private Map<UUID, Location> originalLocations;
    private long startTime;
    private boolean drawVoteActive;
    private UUID drawVoteInitiator;
    
    public Match(Player player1, Player player2) {
        this.matchId = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.state = MatchState.SETUP;
        this.wagerItems = new HashMap<>();
        this.wagerMoney = new HashMap<>();
        this.spectators = new HashSet<>();
        this.originalLocations = new HashMap<>();
        this.drawVoteActive = false;
        
        // Initialize with empty item lists
        this.wagerItems.put(player1.getUniqueId(), new ArrayList<>());
        this.wagerItems.put(player2.getUniqueId(), new ArrayList<>());
        
        // Initialize with 0 money
        this.wagerMoney.put(player1.getUniqueId(), 0.0);
        this.wagerMoney.put(player2.getUniqueId(), 0.0);
    }
    
    // Getters and setters
    public UUID getMatchId() { return matchId; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public MatchState getState() { return state; }
    public void setState(MatchState state) { this.state = state; }
    public Arena getArena() { return arena; }
    public void setArena(Arena arena) { this.arena = arena; }
    public EquipmentSet getPlayer1Equipment() { return player1Equipment; }
    public void setPlayer1Equipment(EquipmentSet player1Equipment) { this.player1Equipment = player1Equipment; }
    public EquipmentSet getPlayer2Equipment() { return player2Equipment; }
    public void setPlayer2Equipment(EquipmentSet player2Equipment) { this.player2Equipment = player2Equipment; }
    public Map<UUID, List<ItemStack>> getWagerItems() { return wagerItems; }
    public List<ItemStack> getWagerItems(Player player) { return wagerItems.get(player.getUniqueId()); }
    public Map<UUID, Double> getWagerMoney() { return wagerMoney; }
    public double getWagerMoney(Player player) { return wagerMoney.get(player.getUniqueId()); }
    public Set<UUID> getSpectators() { return spectators; }
    public Map<UUID, Location> getOriginalLocations() { return originalLocations; }
    public Location getOriginalLocation(Player player) { return originalLocations.get(player.getUniqueId()); }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public boolean isDrawVoteActive() { return drawVoteActive; }
    public void setDrawVoteActive(boolean drawVoteActive) { this.drawVoteActive = drawVoteActive; }
    public UUID getDrawVoteInitiator() { return drawVoteInitiator; }
    public void setDrawVoteInitiator(UUID drawVoteInitiator) { this.drawVoteInitiator = drawVoteInitiator; }
    
    public Player getOpponent(Player player) {
        if (player.equals(player1)) return player2;
        if (player.equals(player2)) return player1;
        return null;
    }
    
    public void broadcast(String message) {
        player1.sendMessage(message);
        player2.sendMessage(message);
        for (UUID spectatorId : spectators) {
            Player spectator = Bukkit.getPlayer(spectatorId);
            if (spectator != null && spectator.isOnline()) {
                spectator.sendMessage(message);
            }
        }
    }
}