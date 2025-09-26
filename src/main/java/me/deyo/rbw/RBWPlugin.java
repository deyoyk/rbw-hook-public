package me.deyo.rbw;

import com.andrei1058.bedwars.api.BedWars;
import me.deyo.rbw.core.GameManager;
import me.deyo.rbw.core.WebSocketManager;
import me.deyo.rbw.core.ConfigManager;
import me.deyo.rbw.commands.CommandRegistry;
import me.deyo.rbw.listeners.GameListener;
import me.deyo.rbw.tasks.QueueUpdater;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RBWPlugin extends JavaPlugin {
    
    private static RBWPlugin instance;
    private BedWars bedWarsAPI;
    private ConfigManager configManager;
    private WebSocketManager webSocketManager;
    private GameManager gameManager;
    private CommandRegistry commandRegistry;
    private QueueUpdater queueUpdater;
    
    @Override
    public void onEnable() {
        instance = this;
        
        if (!initializeBedWarsAPI()) {
            getLogger().severe("BedWars1058 plugin not found! Disabling RBW plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        initializeServices();
        registerListeners();
        registerCommands();
        startServices();
        
        getLogger().info("RBW Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        if (queueUpdater != null) {
            queueUpdater.stop();
        }
        
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
        
        if (gameManager != null) {
            gameManager.cleanup();
        }
        
        getLogger().info("RBW Plugin disabled successfully!");
    }
    
    private boolean initializeBedWarsAPI() {
        if (Bukkit.getPluginManager().getPlugin("BedWars1058") == null) {
            return false;
        }
        
        bedWarsAPI = Bukkit.getServicesManager().getRegistration(BedWars.class).getProvider();
        return bedWarsAPI != null;
    }
    
    private void initializeServices() {
        configManager = new ConfigManager(this);
        webSocketManager = new WebSocketManager(this);
        gameManager = new GameManager(this);
        commandRegistry = new CommandRegistry(this);
        queueUpdater = new QueueUpdater(this);
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
    }
    
    private void registerCommands() {
        commandRegistry.registerAll();
    }
    
    private void startServices() {
        webSocketManager.connect();
        queueUpdater.start();
    }
    
    public static RBWPlugin getInstance() {
        return instance;
    }
    
    public BedWars getBedWarsAPI() {
        return bedWarsAPI;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }
}