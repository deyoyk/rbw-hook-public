package me.deyo.rbw.core;

import me.deyo.rbw.RBWPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    
    private final RBWPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    private String websocketHost;
    private int websocketPort;
    private List<String> rbwModes;
    private boolean debug;
    
    public ConfigManager(RBWPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
        
        plugin.getLogger().info("Configuration loaded successfully!");
    }
    
    public void reloadConfig() {
        if (configFile != null && configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
            loadValues();
            plugin.getLogger().info("Configuration reloaded!");
        }
    }
    
    private void loadValues() {
        websocketHost = config.getString("bot.host", "127.0.0.1");
        websocketPort = config.getInt("bot.port", 25513);
        
        List<String> modes = config.getStringList("rbw_modes");
        if (modes.isEmpty()) {
            modes = new ArrayList<>();
            modes.add("RBW");
        }
        rbwModes = modes;
        
        debug = config.getBoolean("debug", false);
    }
    
    public String getWebsocketHost() {
        return websocketHost;
    }
    
    public int getWebsocketPort() {
        return websocketPort;
    }
    
    public List<String> getRBWModes() {
        return new ArrayList<>(rbwModes);
    }
    
    public boolean isDebugEnabled() {
        return debug;
    }
    
    public boolean isRBWMode(String groupName) {
        if (groupName == null || rbwModes == null) return false;
        
        String upperGroup = groupName.toUpperCase();
        return rbwModes.stream()
                .anyMatch(mode -> upperGroup.contains(mode.toUpperCase()));
    }
    
    public void saveConfig() {
        if (config != null && configFile != null) {
            try {
                config.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save config: " + e.getMessage());
            }
        }
    }
}

