package de.zfzfg.pvpwager.managers;

import de.zfzfg.pvpwager.PvPWager;
import de.zfzfg.pvpwager.gui.WagerGUI;
import de.zfzfg.pvpwager.gui.ArenaSelectionGUI;
import de.zfzfg.pvpwager.gui.EquipmentSelectionGUI;
import de.zfzfg.pvpwager.gui.SpectatorGUI;
import org.bukkit.entity.Player;

public class GUIManager {
    private final PvPWager plugin;
    private final WagerGUI wagerGUI;
    private final ArenaSelectionGUI arenaSelectionGUI;
    private final EquipmentSelectionGUI equipmentSelectionGUI;
    private final SpectatorGUI spectatorGUI;
    
    public GUIManager(PvPWager plugin) {
        this.plugin = plugin;
        this.wagerGUI = new WagerGUI(plugin);
        this.arenaSelectionGUI = new ArenaSelectionGUI(plugin);
        this.equipmentSelectionGUI = new EquipmentSelectionGUI(plugin);
        this.spectatorGUI = new SpectatorGUI(plugin);
    }
    
    public void openWagerSetupGUI(Player player1, Player player2) {
        wagerGUI.open(player1, player2);
    }
    
    public void openArenaSelectionGUI(Player player1, Player player2) {
        arenaSelectionGUI.open(player1, player2);
    }
    
    public void openEquipmentSelectionGUI(Player player1, Player player2) {
        equipmentSelectionGUI.open(player1, player2);
    }
    
    public void openSpectatorGUI(Player spectator, Player player1, Player player2) {
        spectatorGUI.open(spectator, player1, player2);
    }
}