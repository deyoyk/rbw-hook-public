package me.deyo.rbw.model;

public class GameStats {
    private final int activeGames;
    private final int totalPlayers;
    
    public GameStats(int activeGames, int totalPlayers) {
        this.activeGames = activeGames;
        this.totalPlayers = totalPlayers;
    }
    
    public int getActiveGames() {
        return activeGames;
    }
    
    public int getTotalPlayers() {
        return totalPlayers;
    }
}

