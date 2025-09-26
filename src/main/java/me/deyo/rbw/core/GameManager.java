package me.deyo.rbw.core;

import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.model.Game;
import me.deyo.rbw.model.GameStats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    
    private final RBWPlugin plugin;
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();
    private final Map<String, String> arenaToGameId = new ConcurrentHashMap<>();
    
    public GameManager(RBWPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void registerGame(String gameId, String arenaName, List<String> team1, List<String> team2) {
        Game game = new Game(gameId, arenaName, team1, team2);
        game.startGame();
        
        activeGames.put(gameId, game);
        arenaToGameId.put(arenaName, gameId);
        
        plugin.getLogger().info("Registered game: " + gameId + " in arena: " + arenaName);
    }
    
    public void unregisterGame(String gameId) {
        Game game = activeGames.remove(gameId);
        if (game != null) {
            arenaToGameId.remove(game.getArenaName());
            game.cleanup();
            plugin.getLogger().info("Unregistered game: " + gameId);
        }
    }
    
    public void unregisterGameByArena(String arenaName) {
        String gameId = arenaToGameId.remove(arenaName);
        if (gameId != null) {
            unregisterGame(gameId);
        }
    }
    
    public Game getGame(String gameId) {
        return activeGames.get(gameId);
    }
    
    public Game getGameByArena(String arenaName) {
        String gameId = arenaToGameId.get(arenaName);
        return gameId != null ? activeGames.get(gameId) : null;
    }
    
    public Game getGameByPlayer(String playerName) {
        return activeGames.values().stream()
                .filter(game -> game.containsPlayer(playerName))
                .findFirst()
                .orElse(null);
    }
    
    public Collection<Game> getAllGames() {
        return Collections.unmodifiableCollection(activeGames.values());
    }
    
    public int getActiveGameCount() {
        return activeGames.size();
    }
    
    public boolean isPlayerInGame(String playerName) {
        return getGameByPlayer(playerName) != null;
    }
    
    public void cleanup() {
        activeGames.values().forEach(Game::cleanup);
        activeGames.clear();
        arenaToGameId.clear();
    }
    
    public GameStats getGameStats() {
        return new GameStats(
            activeGames.size(),
            activeGames.values().stream()
                .mapToInt(game -> game.getTeam1Players().size() + game.getTeam2Players().size())
                .sum()
        );
    }
}

