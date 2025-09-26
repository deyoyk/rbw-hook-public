package me.deyo.rbw.service.game;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.handler.RBWTeamAssigner;
import me.deyo.rbw.service.arena.ArenaService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GameSetupService {
    
    private final RBWPlugin plugin;
    private final ArenaService arenaService;
    
    private static final ConcurrentHashMap<String, RankedGame> arenaToGame = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompletableFuture<Void>> arenaStartFutures = new ConcurrentHashMap<>();
    
    public GameSetupService(RBWPlugin plugin) {
        this.plugin = plugin;
        this.arenaService = new ArenaService(plugin);
    }
    
    public static ConcurrentHashMap<String, RankedGame> getArenaToGameMapping() {
        return arenaToGame;
    }
    
    public static ConcurrentHashMap<String, CompletableFuture<Void>> getArenaStartFutures() {
        return arenaStartFutures;
    }
    
    public static Result setupGame(RBWPlugin plugin, String preferredMapName, List<String> team1, List<String> team2, boolean isRanked, String gameId) {
        GameSetupService service = new GameSetupService(plugin);
        return service.setupGameInternal(preferredMapName, team1, team2, isRanked, gameId);
    }
    
    private Result setupGameInternal(String preferredMapName, List<String> team1, List<String> team2, boolean isRanked, String gameId) {
        try {
            List<Player> team1Players = new ArrayList<>();
            List<Player> team2Players = new ArrayList<>();
            List<String> offlinePlayers = new ArrayList<>();
            
            for (String playerName : team1) {
                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null || plugin.getBedWarsAPI().getArenaUtil().getArenaByPlayer(player) != null) {
                    offlinePlayers.add(playerName);
                } else {
                    team1Players.add(player);
                }
            }
            
            for (String playerName : team2) {
                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null || plugin.getBedWarsAPI().getArenaUtil().getArenaByPlayer(player) != null) {
                    offlinePlayers.add(playerName);
                } else {
                    team2Players.add(player);
                }
            }
            
            if (!offlinePlayers.isEmpty()) {
                return new Result(false, "Some players are offline or not ready", null, offlinePlayers);
            }
            
            List<List<Player>> teams = new ArrayList<>();
            teams.add(team1Players);
            teams.add(team2Players);
            
            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(team1Players);
            allPlayers.addAll(team2Players);
            
            IArena selectedArena = selectArena(preferredMapName, allPlayers.size());
            if (selectedArena == null) {
                return new Result(false, "No available arena found", null, new ArrayList<>());
            }
            
            CompletableFuture<Void> gamePromise = new CompletableFuture<>();
            
            List<String> allPlayerNames = allPlayers.stream().map(Player::getName).toList();
            List<List<String>> teamNames = new ArrayList<>();
            for (List<Player> team : teams) {
                teamNames.add(team.stream().map(Player::getName).toList());
            }
            
            RankedGame rankedGame = new RankedGame(gameId, 
                preferredMapName != null && !preferredMapName.equals("random") ? List.of(preferredMapName) : null,
                allPlayerNames, teamNames, gamePromise);
            
            arenaToGame.put(selectedArena.getArenaName(), rankedGame);
            arenaStartFutures.put(selectedArena.getArenaName(), gamePromise);
            
            selectedArena.setTeamAssigner(new RBWTeamAssigner());
            
            sendMessageToPlayersBefore(allPlayers.stream().map(Player::getName).toList(), selectedArena.getDisplayName());
            
            warpAll(allPlayers, selectedArena);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (selectedArena.getStatus() == GameState.waiting) {
                    gamePromise.completeExceptionally(new RuntimeException("Game not getting started"));
                    cancelArenaStart(selectedArena);
                }
            }, 30L);
            
            return new Result(true, "", selectedArena.getArenaName(), new ArrayList<>());
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in game setup: " + e.getMessage());
            return new Result(false, "Internal error: " + e.getMessage(), null, new ArrayList<>());
        }
    }
    
    private void cancelArenaStart(IArena arena) {
        try {
            for (Player player : new ArrayList<>(arena.getPlayers())) {
                arena.removePlayer(player, false);
            }
            
            arenaToGame.remove(arena.getArenaName());
            arenaStartFutures.remove(arena.getArenaName());
            
            plugin.getLogger().info("Cancelled arena start for: " + arena.getArenaName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling arena start: " + e.getMessage());
        }
    }
    
    private void warpAll(List<Player> players, IArena arena) {
        for (Player player : players) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                IArena currentArena = plugin.getBedWarsAPI().getArenaUtil().getArenaByPlayer(player);
                if (currentArena != null) {
                    if (currentArena.isPlayer(player)) {
                        currentArena.removePlayer(player, false);
                    }
                    if (currentArena.isSpectator(player)) {
                        currentArena.removeSpectator(player, false);
                    }
                }
            });
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : players) {
                arena.addPlayer(player, false);
            }
            arena.changeStatus(GameState.starting);
            if (arena.getStartingTask().getCountdown() > 5) {
                arena.getStartingTask().setCountdown(5);
            }
        }, 20L);
    }
    
    private IArena selectArena(String preferredMapName, int playerCount) {
        List<IArena> availableArenas = arenaService.getAvailableRBWArenas();
        
        if (availableArenas.isEmpty()) {
            plugin.getLogger().warning("No available RBW arenas found!");
            return null;
        }
        
        List<IArena> suitableArenas = availableArenas.stream()
            .filter(arena -> arena.getStatus() == GameState.waiting)
            .filter(arena -> arena.getPlayers().isEmpty())
            .filter(arena -> arena.getMaxPlayers() >= playerCount)
            .filter(arena -> arena.getTeams().size() >= 2)
            .toList();
        
        if (suitableArenas.isEmpty()) {
            plugin.getLogger().warning("No suitable arenas found!");
            return null;
        }
        
        if (preferredMapName != null && !preferredMapName.isEmpty() && !"random".equalsIgnoreCase(preferredMapName)) {
            List<IArena> mapArenas = suitableArenas.stream()
                .filter(arena -> preferredMapName.equalsIgnoreCase(arena.getArenaName()))
                .toList();
            
            if (!mapArenas.isEmpty()) {
                plugin.getLogger().info("Selected preferred arena: " + mapArenas.get(0).getArenaName());
                return mapArenas.get(0);
            } else {
                plugin.getLogger().warning("Preferred arena '" + preferredMapName + "' not available, selecting alternative");
            }
        }
        
        IArena selectedArena = suitableArenas.get((int) (Math.random() * suitableArenas.size()));
        plugin.getLogger().info("Selected arena: " + selectedArena.getArenaName());
        return selectedArena;
    }
    
    private void sendMessageToPlayersBefore(List<String> playerNames, String arenaName) {
        for (String playerName : playerNames) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "RBW " + ChatColor.RED + "» " + 
                    ChatColor.RED + "System tring to warp you to the game. if you are not getting warped to the map " + 
                    ChatColor.GREEN + arenaName + ChatColor.RED + " , Please go to lobby! ");
            }
        }
    }
    
    public static class Result {
        private final boolean success;
        private final String message;
        private final String arenaName;
        private final List<String> offlinePlayers;
        
        public Result(boolean success, String message, String arenaName, List<String> offlinePlayers) {
            this.success = success;
            this.message = message;
            this.arenaName = arenaName;
            this.offlinePlayers = offlinePlayers == null ? new ArrayList<>() : new ArrayList<>(offlinePlayers);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getArenaName() {
            return arenaName;
        }
        
        public List<String> getOfflinePlayers() {
            return offlinePlayers;
        }
    }
    
    public static class RankedGame {
        private final String id;
        private final List<String> mapNames;
        private final List<String> playerNames;
        private final List<List<String>> teamNames;
        private final CompletableFuture<Void> promise;
        
        public RankedGame(String id, List<String> mapNames, List<String> playerNames, List<List<String>> teamNames, CompletableFuture<Void> promise) {
            this.id = id;
            this.mapNames = mapNames;
            this.playerNames = playerNames;
            this.teamNames = teamNames;
            this.promise = promise;
        }
        
        public String getId() { return id; }
        public List<String> getMapNames() { return mapNames; }
        public List<String> getPlayerNames() { return playerNames; }
        public List<List<String>> getTeamNames() { return teamNames; }
        public CompletableFuture<Void> getPromise() { return promise; }
        
        public List<Player> getPlayers() {
            return playerNames.stream()
                .map(Bukkit::getPlayerExact)
                .filter(player -> player != null)
                .toList();
        }
    }
}
