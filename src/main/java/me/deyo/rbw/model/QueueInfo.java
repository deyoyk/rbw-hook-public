package me.deyo.rbw.model;

import java.util.ArrayList;
import java.util.List;

public class QueueInfo {
    
    private final List<String> players;
    private final EloRange eloRange;
    private final int capacity;
    
    public QueueInfo(List<String> players, EloRange eloRange, int capacity) {
        this.players = new ArrayList<>(players);
        this.eloRange = eloRange;
        this.capacity = capacity;
    }
    
    public List<String> getPlayers() {
        return players;
    }
    
    public EloRange getEloRange() {
        return eloRange;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public int getPlayerCount() {
        return players.size();
    }
    
    public boolean isFull() {
        return players.size() >= capacity;
    }
    
    public boolean isEmpty() {
        return players.isEmpty();
    }
    
    public static class EloRange {
        private final int min;
        private final int max;
        
        public EloRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
        
        public int getMin() {
            return min;
        }
        
        public int getMax() {
            return max;
        }
        
        public boolean contains(int elo) {
            return elo >= min && elo <= max;
        }
        
        @Override
        public String toString() {
            return min + "-" + max;
        }
    }
}

