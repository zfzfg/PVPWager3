package de.zfzfg.pvpwager.managers;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.models.Match;
import de.zfzfg.pvpwager.models.MatchState;
import de.zfzfg.pvpwager.models.Arena;
import de.zfzfg.pvpwager.models.EquipmentSet;
import de.zfzfg.pvpwager.utils.MessageUtil;
import de.zfzfg.pvpwager.utils.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MatchManager {
    private final PvPWager plugin;
    private final Map<UUID, Match> matches = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> matchTimerTasks = new HashMap<>();
    
    public MatchManager(PvPWager plugin) {
        this.plugin = plugin;
    }
    
    public void startMatchSetup(Player player1, Player player2) {
        Match match = new Match(player1, player2);
        matches.put(match.getMatchId(), match);
        
        // Store original locations
        match.getOriginalLocations().put(player1.getUniqueId(), player1.getLocation());
        match.getOriginalLocations().put(player2.getUniqueId(), player2.getLocation());
        
        // Open wager setup GUI
        plugin.getGUIManager().openWagerSetupGUI(player1, player2);
    }
    
    public void handleWagerConfirmation(Player player1, Player player2) {
        Match match = getMatch(player1, player2);
        if (match == null) return;
        
        // Verify wager is valid
        if (!validateWager(match, player1, player2)) {
            return;
        }
        
        // Deduct money from both players if applicable
        if (plugin.hasEconomy()) {
            double p1Money = match.getWagerMoney(player1);
            double p2Money = match.getWagerMoney(player2);
            
            if (p1Money > 0) {
                if (!plugin.getEconomy().has(player1, p1Money)) {
                    MessageUtil.sendMessage(player1, "&cYou don't have enough money!");
                    endMatch(match, null, true);
                    return;
                }
                plugin.getEconomy().withdrawPlayer(player1, p1Money);
            }
            
            if (p2Money > 0) {
                if (!plugin.getEconomy().has(player2, p2Money)) {
                    MessageUtil.sendMessage(player2, "&cYou don't have enough money!");
                    // Return p1's money
                    if (p1Money > 0) {
                        plugin.getEconomy().depositPlayer(player1, p1Money);
                    }
                    endMatch(match, null, true);
                    return;
                }
                plugin.getEconomy().withdrawPlayer(player2, p2Money);
            }
        }
        
        // Open arena selection GUI
        plugin.getGUIManager().openArenaSelectionGUI(player1, player2);
    }
    
    private boolean validateWager(Match match, Player player1, Player player2) {
        // Check minimum wager requirements
        int minItems = plugin.getConfig().getInt("settings.checks.minimum-bet-items", 1);
        double minMoney = plugin.getConfig().getDouble("settings.checks.minimum-bet-money", 0);
        
        int p1Items = match.getWagerItems(player1).size();
        int p2Items = match.getWagerItems(player2).size();
        double p1Money = match.getWagerMoney(player1);
        double p2Money = match.getWagerMoney(player2);
        
        if ((p1Items + p2Items) < minItems && (p1Money + p2Money) < minMoney) {
            MessageUtil.sendMessage(player1, "&cMinimum wager not met! At least " + minItems + " items or $" + minMoney + " required.");
            MessageUtil.sendMessage(player2, "&cMinimum wager not met! At least " + minItems + " items or $" + minMoney + " required.");
            return false;
        }
        
        // Check inventory space
        if (plugin.getConfig().getBoolean("settings.checks.inventory-space", true)) {
            if (!InventoryUtil.canFitItems(player1, match.getWagerItems(player2))) {
                MessageUtil.sendMessage(player1, "&cYou don't have enough inventory space to receive opponent's items!");
                MessageUtil.sendMessage(player2, "&cYour opponent doesn't have enough inventory space!");
                return false;
            }
            
            if (!InventoryUtil.canFitItems(player2, match.getWagerItems(player1))) {
                MessageUtil.sendMessage(player2, "&cYou don't have enough inventory space to receive opponent's items!");
                MessageUtil.sendMessage(player1, "&cYour opponent doesn't have enough inventory space!");
                return false;
            }
        }
        
        return true;
    }
    
    public void handleArenaSelection(Player player1, Player player2, Arena arena) {
        Match match = getMatch(player1, player2);
        if (match == null) return;
        
        match.setArena(arena);
        
        // Load arena world
        plugin.getArenaManager().loadArenaWorld(arena.getArenaWorld());
        
        // Open equipment selection GUI
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getGUIManager().openEquipmentSelectionGUI(player1, player2);
        }, 60L); // Wait 3 seconds for world to load
    }
    
    public void handleEquipmentSelection(Player player1, Player player2, EquipmentSet p1Equipment, EquipmentSet p2Equipment) {
        Match match = getMatch(player1, player2);
        if (match == null) return;
        
        match.setPlayer1Equipment(p1Equipment);
        match.setPlayer2Equipment(p2Equipment);
        
        // Start the match
        startMatch(match);
    }
    
    private void startMatch(Match match) {
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();
        Arena arena = match.getArena();
        
        match.setState(MatchState.STARTING);
        match.setStartTime(System.currentTimeMillis());
        
        // Teleport players to arena
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Clear inventories
            player1.getInventory().clear();
            player2.getInventory().clear();
            player1.getInventory().setArmorContents(null);
            player2.getInventory().setArmorContents(null);
            
            // Apply equipment
            applyEquipment(player1, match.getPlayer1Equipment());
            applyEquipment(player2, match.getPlayer2Equipment());
            
            // Reset health and hunger
            player1.setHealth(20.0);
            player1.setFoodLevel(20);
            player1.setSaturation(20.0f);
            player2.setHealth(20.0);
            player2.setFoodLevel(20);
            player2.setSaturation(20.0f);
            
            // Teleport players
            player1.teleport(arena.getPlayer1Spawn());
            player2.teleport(arena.getPlayer2Spawn());
            
            // Set gamemode
            player1.setGameMode(GameMode.ADVENTURE);
            player2.setGameMode(GameMode.ADVENTURE);
            
            // Start countdown
            startCountdown(match);
            
        }, 40L); // 2 seconds delay
    }
    
    private void applyEquipment(Player player, EquipmentSet equipment) {
        if (equipment == null) return;
        
        // Apply armor
        if (equipment.getHelmet() != null) {
            player.getInventory().setHelmet(equipment.getHelmet().clone());
        }
        if (equipment.getChestplate() != null) {
            player.getInventory().setChestplate(equipment.getChestplate().clone());
        }
        if (equipment.getLeggings() != null) {
            player.getInventory().setLeggings(equipment.getLeggings().clone());
        }
        if (equipment.getBoots() != null) {
            player.getInventory().setBoots(equipment.getBoots().clone());
        }
        
        // Apply inventory items
        if (equipment.getInventory() != null) {
            for (Map.Entry<Integer, ItemStack> entry : equipment.getInventory().entrySet()) {
                player.getInventory().setItem(entry.getKey(), entry.getValue().clone());
            }
        }
    }
    
    private void startCountdown(Match match) {
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();
        
        int countdownTime = plugin.getConfig().getInt("settings.match.countdown-time", 10);
        
        for (int i = countdownTime; i > 0; i--) {
            final int seconds = i;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (match.getState() != MatchState.STARTING) return;
                
                String message = "&eMatch starts in &6&l" + seconds + " &eseconds!";
                match.broadcast(message);
                
                // Play sound
                player1.playSound(player1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player2.playSound(player2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                
            }, (countdownTime - i) * 20L);
            
            countdownTasks.put(match.getMatchId(), task);
        }
        
        // Start the match after countdown
        BukkitTask startTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (match.getState() == MatchState.STARTING) {
                startFight(match);
            }
        }, countdownTime * 20L);
        
        countdownTasks.put(match.getMatchId(), startTask);
    }
    
    private void startFight(Match match) {
        match.setState(MatchState.FIGHTING);
        
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();
        
        // Set survival mode
        player1.setGameMode(GameMode.SURVIVAL);
        player2.setGameMode(GameMode.SURVIVAL);
        
        // Broadcast
        match.broadcast("");
        match.broadcast("&a&l━━━━━━━━━━━━━━━━━━━━━━━");
        match.broadcast("&a&lFIGHT!");
        match.broadcast("&a&l━━━━━━━━━━━━━━━━━━━━━━━");
        match.broadcast("");
        
        // Play sound
        player1.playSound(player1.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 2.0f);
        player2.playSound(player2.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 2.0f);
        
        // Start match timer
        startMatchTimer(match);
    }
    
    private void startMatchTimer(Match match) {
        int maxDuration = plugin.getConfig().getInt("settings.match.max-duration", 600); // 10 minutes
        
        BukkitTask timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (match.getState() != MatchState.FIGHTING) {
                return;
            }
            
            long elapsed = (System.currentTimeMillis() - match.getStartTime()) / 1000;
            long remaining = maxDuration - elapsed;
            
            if (remaining <= 0) {
                // Match timeout - draw
                match.broadcast("");
                match.broadcast("&c&l━━━━━━━━━━━━━━━━━━━━━━━");
                match.broadcast("&c&lTIME'S UP!");
                match.broadcast("&c&l━━━━━━━━━━━━━━━━━━━━━━━");
                match.broadcast("");
                match.broadcast("&7Match ended in a draw due to timeout.");
                match.broadcast("");
                
                endMatch(match, null, true);
            } else if (remaining == 60 || remaining == 30 || remaining == 10) {
                match.broadcast("&eMatch ends in &c" + remaining + " &eseconds!");
            }
        }, 0, 20);
        
        matchTimerTasks.put(match.getMatchId(), timerTask);
    }
    
    public void endMatch(Match match, Player winner, boolean isDraw) {
        // Cancel tasks
        BukkitTask countdownTask = countdownTasks.remove(match.getMatchId());
        BukkitTask timerTask = matchTimerTasks.remove(match.getMatchId());
        
        if (countdownTask != null) countdownTask.cancel();
        if (timerTask != null) timerTask.cancel();
        
        match.setState(MatchState.ENDED);
        
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();
        
        if (isDraw) {
            distributeItemsBack(match);
        } else if (winner != null) {
            distributeWinnings(match, winner);
        } else {
            // Should not happen
            distributeItemsBack(match);
        }
        
        // Teleport players back after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            teleportPlayerBack(player1, match);
            teleportPlayerBack(player2, match);
            
            // Handle spectators
            for (UUID spectatorId : new ArrayList<>(match.getSpectators())) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null && spectator.isOnline()) {
                    teleportPlayerBack(spectator, match);
                }
            }
            
            // Unload world
            if (match.getArena() != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getArenaManager().unloadArenaWorld(match.getArena().getArenaWorld());
                }, 40L); // Wait 2 seconds after teleport
            }
            
            // Cleanup
            matches.remove(match.getMatchId());
        }, 80L); // 4 seconds delay
    }
    
    private void distributeWinnings(Match match, Player winner) {
        Player loser = match.getOpponent(winner);
        
        // Give items to winner
        List<ItemStack> allItems = new ArrayList<>();
        allItems.addAll(match.getWagerItems(match.getPlayer1()));
        allItems.addAll(match.getWagerItems(match.getPlayer2()));
        
        InventoryUtil.giveItems(winner, allItems);
        
        MessageUtil.sendMessage(winner, "");
        MessageUtil.sendMessage(winner, "&a&l━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendMessage(winner, "&a&lYOU WON!");
        MessageUtil.sendMessage(winner, "&a&l━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendMessage(winner, "");
        MessageUtil.sendMessage(winner, "&7You received all wager items!");
        
        // Give money to winner
        if (plugin.hasEconomy()) {
            double totalMoney = match.getWagerMoney(match.getPlayer1()) + match.getWagerMoney(match.getPlayer2());
            if (totalMoney > 0) {
                plugin.getEconomy().depositPlayer(winner, totalMoney);
                MessageUtil.sendMessage(winner, "&7You won &6$" + totalMoney + "&7!");
            }
        }
        
        MessageUtil.sendMessage(winner, "");
        
        // Notify loser
        MessageUtil.sendMessage(loser, "");
        MessageUtil.sendMessage(loser, "&c&l━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendMessage(loser, "&c&lYOU LOST!");
        MessageUtil.sendMessage(loser, "&c&l━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtil.sendMessage(loser, "");
        MessageUtil.sendMessage(loser, "&7Better luck next time!");
        MessageUtil.sendMessage(loser, "");
    }
    
    private void distributeItemsBack(Match match) {
        // Return items to original owners
        Player player1 = match.getPlayer1();
        Player player2 = match.getPlayer2();
        
        InventoryUtil.giveItems(player1, match.getWagerItems(player1));
        InventoryUtil.giveItems(player2, match.getWagerItems(player2));
        
        // Return money
        if (plugin.hasEconomy()) {
            double p1Money = match.getWagerMoney(player1);
            double p2Money = match.getWagerMoney(player2);
            
            if (p1Money > 0) plugin.getEconomy().depositPlayer(player1, p1Money);
            if (p2Money > 0) plugin.getEconomy().depositPlayer(player2, p2Money);
        }
        
        MessageUtil.sendMessage(player1, "&7Your wager has been returned.");
        MessageUtil.sendMessage(player2, "&7Your wager has been returned.");
    }
    
    private void teleportPlayerBack(Player player, Match match) {
        Location originalLocation = match.getOriginalLocations().get(player.getUniqueId());
        if (originalLocation != null) {
            player.teleport(originalLocation);
            
            if (match.getSpectators().contains(player.getUniqueId())) {
                player.setGameMode(GameMode.SURVIVAL);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setInvisible(false);
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }
    
    public Match getMatch(Player player1, Player player2) {
        for (Match match : matches.values()) {
            if ((match.getPlayer1().equals(player1) && match.getPlayer2().equals(player2)) ||
                (match.getPlayer1().equals(player2) && match.getPlayer2().equals(player1))) {
                return match;
            }
        }
        return null;
    }
    
    public Match getMatchByPlayer(Player player) {
        for (Match match : matches.values()) {
            if (match.getPlayer1().equals(player) || match.getPlayer2().equals(player) ||
                match.getSpectators().contains(player.getUniqueId())) {
                return match;
            }
        }
        return null;
    }
    
    public boolean isPlayerInMatch(Player player) {
        return getMatchByPlayer(player) != null;
    }
    
    public void stopAllMatches() {
        for (Match match : new ArrayList<>(matches.values())) {
            match.broadcast("&cServer is shutting down! Match cancelled.");
            endMatch(match, null, true);
        }
    }
}