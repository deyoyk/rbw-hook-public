package me.deyo.rbw.commands.impl;

import com.google.gson.JsonObject;
import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.commands.BaseCommand;
import org.bukkit.command.CommandSender;

public class CallCommand extends BaseCommand {
    
    public CallCommand(RBWPlugin plugin) {
        super(plugin, "call", "rbw.call", "Request a voice call for a player", "/call <targetIGN>");
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
        json.addProperty("type", "CALL_CMD");
        json.addProperty("requester_ign", requesterName);
        json.addProperty("target_ign", targetName);
        
        plugin.getWebSocketManager().sendMessage(json);
        sendMessage(sender, "&aRequested voice call for " + targetName + ".");
        
        return true;
    }
}