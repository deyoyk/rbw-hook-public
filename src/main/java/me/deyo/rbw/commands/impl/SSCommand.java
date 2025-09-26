package me.deyo.rbw.commands.impl;

import com.google.gson.JsonObject;
import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.commands.BaseCommand;
import org.bukkit.command.CommandSender;

public class SSCommand extends BaseCommand {
    
    public SSCommand(RBWPlugin plugin) {
        super(plugin, "ss", "rbw.ss", "Request a screenshare for a player", "/ss <targetIGN>");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sendUsage(sender);
            return true;
        }
        
        String targetName = args[0];
        String requesterName = sender.getName();
        
        if (plugin.getWebSocketManager() == null || !plugin.getWebSocketManager().isConnected()) {
            sendMessage(sender, "&cSocket not connected.");
            return true;
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("type", "AUTOSS");
        json.addProperty("target_ign", targetName);
        json.addProperty("requester_ign", requesterName);
        
        plugin.getWebSocketManager().sendMessage(json);
        sendMessage(sender, "&aRequested screenshare for " + targetName + ".");
        
        return true;
    }
}