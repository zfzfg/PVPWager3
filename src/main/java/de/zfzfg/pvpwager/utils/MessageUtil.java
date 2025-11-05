package de.zfzfg.pvpwager.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtil {
    
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static List<String> color(List<String> messages) {
        return messages.stream().map(MessageUtil::color).collect(Collectors.toList());
    }
    
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }
    
    public static void sendMessage(Player player, String message) {
        player.sendMessage(color(message));
    }
    
    public static void sendMessages(CommandSender sender, List<String> messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }
    
    public static void sendMessages(Player player, List<String> messages) {
        for (String message : messages) {
            sendMessage(player, message);
        }
    }
    
    public static String formatTime(long seconds) {
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("%02d", seconds);
        }
    }
}