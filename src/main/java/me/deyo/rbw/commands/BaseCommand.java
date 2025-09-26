package me.deyo.rbw.commands;

import me.deyo.rbw.RBWPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor {
    
    protected final RBWPlugin plugin;
    private final String name;
    private final String permission;
    private final String description;
    private final String usage;
    
    public BaseCommand(RBWPlugin plugin, String name, String permission, String description, String usage) {
        this.plugin = plugin;
        this.name = name;
        this.permission = permission;
        this.description = description;
        this.usage = usage;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(permission)) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }
        
        if (sender instanceof Player) {
            return execute(sender, args);
        } else {
            sendMessage(sender, "&cOnly players can use this command.");
            return true;
        }
    }
    
    public abstract boolean execute(CommandSender sender, String[] args);
    
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    protected void sendUsage(CommandSender sender) {
        sendMessage(sender, "&cUsage: " + usage);
    }
    
    protected void sendError(CommandSender sender, String error) {
        sendMessage(sender, "&cError: " + error);
    }
    
    protected void sendSuccess(CommandSender sender, String message) {
        sendMessage(sender, "&a" + message);
    }
    
    protected void sendInfo(CommandSender sender, String message) {
        sendMessage(sender, "&e" + message);
    }
    
    public String getName() {
        return name;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getUsage() {
        return usage;
    }
}