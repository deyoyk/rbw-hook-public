package me.deyo.rbw.commands.impl;

import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.commands.BaseCommand;
import org.bukkit.command.CommandSender;

public class ConfigCommand extends BaseCommand {
    
    public ConfigCommand(RBWPlugin plugin) {
        super(plugin, "config", "rbw.admin", "Manage plugin configuration", "/config [reload]");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                plugin.getConfigManager().reloadConfig();
                sendMessage(sender, "&aConfig reloaded successfully!");
                break;
            case "info":
                sendConfigInfo(sender);
                break;
            default:
                sendMessage(sender, "&cUnknown subcommand. Available: reload, info");
                break;
        }
        
        return true;
    }
    
    private void sendConfigInfo(CommandSender sender) {
        sendMessage(sender, "&6=== RBW Plugin Configuration ===");
        sendMessage(sender, "&eWebSocket Host: &f" + plugin.getConfigManager().getWebsocketHost());
        sendMessage(sender, "&eWebSocket Port: &f" + plugin.getConfigManager().getWebsocketPort());
        sendMessage(sender, "&eRBW Modes: &f" + String.join(", ", plugin.getConfigManager().getRBWModes()));
        sendMessage(sender, "&eDebug Mode: &f" + plugin.getConfigManager().isDebugEnabled());
        sendMessage(sender, "&eActive Games: &f" + plugin.getGameManager().getActiveGameCount());
        sendMessage(sender, "&eWebSocket Connected: &f" + (plugin.getWebSocketManager().isConnected() ? "Yes" : "No"));
    }
}