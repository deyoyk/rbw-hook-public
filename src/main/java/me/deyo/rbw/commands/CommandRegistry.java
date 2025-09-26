package me.deyo.rbw.commands;

import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.commands.impl.CallCommand;
import me.deyo.rbw.commands.impl.ConfigCommand;
import me.deyo.rbw.commands.impl.SSCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;

public class CommandRegistry {
    
    private final RBWPlugin plugin;
    private final CallCommand callCommand;
    private final SSCommand ssCommand;
    private final ConfigCommand configCommand;
    
    public CommandRegistry(RBWPlugin plugin) {
        this.plugin = plugin;
        this.callCommand = new CallCommand(plugin);
        this.ssCommand = new SSCommand(plugin);
        this.configCommand = new ConfigCommand(plugin);
    }
    
    public void registerAll() {
        registerCommand("ss", ssCommand);
        registerCommand("call", callCommand);
        registerCommand("config", configCommand);
    }
    
    private void registerCommand(String commandName, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(commandName);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            plugin.getLogger().warning("Command " + commandName + " not found in plugin.yml");
        }
    }
}

