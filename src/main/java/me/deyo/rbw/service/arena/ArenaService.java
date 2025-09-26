package me.deyo.rbw.service.arena;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import me.deyo.rbw.RBWPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ArenaService {
    
    private final RBWPlugin plugin;
    private final Random random = new Random();
    
    public ArenaService(RBWPlugin plugin) {
        this.plugin = plugin;
    }
    
    public List<IArena> getAvailableRBWArenas() {
        if (plugin.getBedWarsAPI() == null) {
            plugin.getLogger().warning("BedWars API not available!");
            return new ArrayList<>();
        }
        
        try {
            return plugin.getBedWarsAPI().getArenaUtil().getArenas()
                    .stream()
                    .filter(arena -> arena.getStatus() == GameState.waiting)
                    .filter(arena -> plugin.getConfigManager().isRBWMode(arena.getGroup()))
                    .filter(this::isArenaAvailable)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get available RBW arenas: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public IArena getArenaByName(String arenaName) {
        if (plugin.getBedWarsAPI() == null || arenaName == null) return null;
        
        try {
            return plugin.getBedWarsAPI().getArenaUtil().getArenas()
                    .stream()
                    .filter(arena -> arena.getArenaName().equalsIgnoreCase(arenaName))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get arena: " + arenaName + " - " + e.getMessage());
            return null;
        }
    }
    
    public boolean isArenaAvailable(IArena arena) {
        if (arena == null) return false;
        return arena.getStatus() == GameState.waiting && arena.getPlayers().isEmpty();
    }
    
    public boolean isRBWArena(IArena arena) {
        if (arena == null) return false;
        return plugin.getConfigManager().isRBWMode(arena.getGroup());
    }
    
    public boolean isPlayerInGame(String playerName) {
        if (plugin.getBedWarsAPI() == null || playerName == null) return false;
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return false;
        
        try {
            return plugin.getBedWarsAPI().getArenaUtil().isPlaying(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check if player is in game: " + playerName + " - " + e.getMessage());
            return false;
        }
    }
    
    public IArena getPlayerArena(String playerName) {
        if (plugin.getBedWarsAPI() == null || playerName == null) return null;
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return null;
        
        try {
            return plugin.getBedWarsAPI().getArenaUtil().getArenaByPlayer(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player arena: " + playerName + " - " + e.getMessage());
            return null;
        }
    }
    
    public ITeam getPlayerTeam(String playerName) {
        IArena arena = getPlayerArena(playerName);
        if (arena == null) return null;
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) return null;
        
        try {
            return arena.getTeam(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get player team: " + playerName + " - " + e.getMessage());
            return null;
        }
    }
    
    public void forceStartArena(IArena arena) {
        if (arena == null) return;
        
        if (arena.getStatus() == GameState.starting && arena.getStartingTask() != null) {
            arena.getStartingTask().setCountdown(0);
        }
    }
    
    public List<Player> getAllRBWPlayers() {
        List<Player> rbwPlayers = new ArrayList<>();
        
        if (plugin.getBedWarsAPI() == null) return rbwPlayers;
        
        try {
            for (IArena arena : plugin.getBedWarsAPI().getArenaUtil().getArenas()) {
                if (isRBWArena(arena)) {
                    rbwPlayers.addAll(arena.getPlayers());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get RBW players: " + e.getMessage());
        }
        
        return rbwPlayers;
    }
    
    public String selectRandomRBWArena() {
        List<IArena> availableArenas = getAvailableRBWArenas();
        
        if (availableArenas.isEmpty()) {
            plugin.getLogger().warning("No available RBW arenas for random selection!");
            return null;
        }
        
        IArena selectedArena = availableArenas.get(random.nextInt(availableArenas.size()));
        String arenaName = selectedArena.getArenaName();
        
        plugin.getLogger().info("Randomly selected arena: " + arenaName);
        return arenaName;
    }
    
    public String getArenaInfo(IArena arena) {
        if (arena == null) return "Arena: null";
        
        return String.format("Arena: %s | Status: %s | Players: %d/%d | Group: %s", 
                arena.getArenaName(), 
                arena.getStatus(), 
                arena.getPlayers().size(), 
                arena.getMaxPlayers(), 
                arena.getGroup());
    }
    
    public void addPlayersToArena(Collection<Player> players, IArena arena) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : players) {
                arena.addPlayer(player, false);
            }
            arena.changeStatus(GameState.starting);
            
            if (arena.getStartingTask() != null && arena.getStartingTask().getCountdown() > 5) {
                arena.getStartingTask().setCountdown(5);
            }
        }, 20L);
    }
    
    public void kickAllPlayers(IArena arena) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player player : new ArrayList<>(arena.getPlayers())) {
                arena.removePlayer(player, false);
            }
        });
    }
    
    public String resolveTeamKey(IArena arena, boolean isGreen) {
        String[] candidates = isGreen
                ? new String[]{"Green", "GREEN", "green", "G"}
                : new String[]{"Red", "RED", "red", "R"};
        
        for (String key : candidates) {
            try {
                if (arena.getTeam(key) != null) return key;
            } catch (Exception ignored) {}
        }
        
        try {
            for (ITeam team : arena.getTeams()) {
                if (team == null) continue;
                String name = team.getName();
                if (name == null) continue;
                String lower = name.toLowerCase();
                if (isGreen && ("green".equals(lower) || lower.startsWith("g"))) return name;
                if (!isGreen && ("red".equals(lower) || lower.startsWith("r"))) return name;
            }
        } catch (Exception ignored) {}
        
        return null;
    }
}

