package me.deyo.rbw.model;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Game {
    
    private final String gameId;
    private final String arenaName;
    private final List<String> team1Players;
    private final List<String> team2Players;
    
    private Instant startTime;
    private Instant endTime;
    
    private final Set<String> winners = ConcurrentHashMap.newKeySet();
    private final Set<String> losers = ConcurrentHashMap.newKeySet();
    private final Set<String> mvps = ConcurrentHashMap.newKeySet();
    private final Set<String> bedBreakers = ConcurrentHashMap.newKeySet();
    
    private final Map<String, PlayerStats> playerStats = new ConcurrentHashMap<>();
    
    public Game(String gameId, String arenaName, List<String> team1Players, List<String> team2Players) {
        this.gameId = gameId;
        this.arenaName = arenaName;
        this.team1Players = new ArrayList<>(team1Players);
        this.team2Players = new ArrayList<>(team2Players);
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public String getArenaName() {
        return arenaName;
    }
    
    public List<String> getTeam1Players() {
        return Collections.unmodifiableList(team1Players);
    }
    
    public List<String> getTeam2Players() {
        return Collections.unmodifiableList(team2Players);
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void startGame() {
        this.startTime = Instant.now();
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void endGame() {
        this.endTime = Instant.now();
    }
    
    public Set<String> getWinners() {
        return Collections.unmodifiableSet(winners);
    }
    
    public Set<String> getLosers() {
        return Collections.unmodifiableSet(losers);
    }
    
    public Set<String> getMvps() {
        return Collections.unmodifiableSet(mvps);
    }
    
    public Set<String> getBedBreakers() {
        return Collections.unmodifiableSet(bedBreakers);
    }
    
    public Map<String, PlayerStats> getPlayerStats() {
        return Collections.unmodifiableMap(playerStats);
    }
    
    public PlayerStats getOrCreatePlayerStats(String playerName) {
        return playerStats.computeIfAbsent(playerName, k -> new PlayerStats());
    }
    
    public void recalculateAwards() {
        mvps.clear();
        bedBreakers.clear();
        
        int maxKills = playerStats.values().stream()
                .mapToInt(PlayerStats::getKills)
                .max().orElse(0);
        
        playerStats.entrySet().stream()
                .filter(entry -> maxKills > 0 && entry.getValue().getKills() == maxKills)
                .forEach(entry -> mvps.add(entry.getKey()));
        
        playerStats.entrySet().stream()
                .filter(entry -> entry.getValue().getBedsBroken() > 0)
                .forEach(entry -> bedBreakers.add(entry.getKey()));
    }
    
    public void cleanup() {
        winners.clear();
        losers.clear();
        mvps.clear();
        bedBreakers.clear();
        playerStats.clear();
    }
    
    public boolean containsPlayer(String playerName) {
        return team1Players.contains(playerName) || team2Players.contains(playerName);
    }
    
    public static class PlayerStats {
        private volatile int kills;
        private volatile int deaths;
        private volatile int finalKills;
        private volatile int finalDeaths;
        private volatile int bedsBroken;
        private volatile boolean won;
        private volatile int diamonds;
        private volatile int irons;
        private volatile int gold;
        private volatile int emeralds;
        private volatile int blocksPlaced;
        
        public int getKills() { return kills; }
        public void addKills(int amount) { this.kills += amount; }
        
        public int getDeaths() { return deaths; }
        public void addDeaths(int amount) { this.deaths += amount; }
        
        public int getFinalKills() { return finalKills; }
        public void addFinalKills(int amount) { this.finalKills += amount; }
        
        public int getFinalDeaths() { return finalDeaths; }
        public void addFinalDeaths(int amount) { this.finalDeaths += amount; }
        
        public int getBedsBroken() { return bedsBroken; }
        public void addBedsBroken(int amount) { this.bedsBroken += amount; }
        
        public boolean hasWon() { return won; }
        public void setWon(boolean won) { this.won = won; }
        
        public int getDiamonds() { return diamonds; }
        public void addDiamonds(int amount) { this.diamonds += amount; }
        
        public int getIrons() { return irons; }
        public void addIrons(int amount) { this.irons += amount; }
        
        public int getGold() { return gold; }
        public void addGold(int amount) { this.gold += amount; }
        
        public int getEmeralds() { return emeralds; }
        public void addEmeralds(int amount) { this.emeralds += amount; }
        
        public int getBlocksPlaced() { return blocksPlaced; }
        public void addBlocksPlaced(int amount) { this.blocksPlaced += amount; }
    }
}
