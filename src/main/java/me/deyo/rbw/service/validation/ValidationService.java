package me.deyo.rbw.service.validation;

import com.andrei1058.bedwars.api.arena.IArena;
import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.service.arena.ArenaService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ValidationService {
    
    private final RBWPlugin plugin;
    private final ArenaService arenaService;
    
    public ValidationService(RBWPlugin plugin) {
        this.plugin = plugin;
        this.arenaService = new ArenaService(plugin);
    }
    
    public boolean validateBedWarsAPI() {
        if (plugin.getBedWarsAPI() == null) {
            plugin.getLogger().severe("BedWars API is not available! Plugin functionality may be limited.");
            return false;
        }
        return true;
    }
    
    public boolean validatePlayer(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            plugin.getLogger().warning("Player name is null or empty");
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerName);
        if (player == null || !player.isOnline()) {
            plugin.getLogger().warning("Player " + playerName + " is not online");
            return false;
        }
        
        return true;
    }
    
    public boolean validateRBWArena(IArena arena) {
        if (arena == null) {
            plugin.getLogger().warning("Arena is null");
            return false;
        }
        
        if (!arenaService.isRBWArena(arena)) {
            plugin.getLogger().warning("Arena " + arena.getArenaName() + " is not an RBW arena");
            return false;
        }
        
        return true;
    }
    
    public boolean validateArenaAvailable(IArena arena) {
        if (!validateRBWArena(arena)) {
            return false;
        }
        
        if (!arenaService.isArenaAvailable(arena)) {
            plugin.getLogger().warning("Arena " + arena.getArenaName() + " is not available (Status: " + 
                arena.getStatus() + ", Players: " + arena.getPlayers().size() + ")");
            return false;
        }
        
        return true;
    }
    
    public List<String> validatePlayerList(List<String> players) {
        List<String> validPlayers = new ArrayList<>();
        List<String> invalidPlayers = new ArrayList<>();
        
        if (players == null) {
            plugin.getLogger().warning("Player list is null");
            return validPlayers;
        }
        
        for (String playerName : players) {
            if (validatePlayer(playerName)) {
                if (arenaService.isPlayerInGame(playerName)) {
                    invalidPlayers.add(playerName + " (already in game)");
                } else {
                    validPlayers.add(playerName);
                }
            } else {
                invalidPlayers.add(playerName);
            }
        }
        
        if (!invalidPlayers.isEmpty()) {
            plugin.getLogger().warning("Invalid players found: " + String.join(", ", invalidPlayers));
        }
        
        return validPlayers;
    }
    
    public ValidationResult validateGameSetup(String preferredMapName, List<String> team1, List<String> team2) {
        ValidationResult result = new ValidationResult();
        
        if (!validateBedWarsAPI()) {
            result.addError("BedWars API not available");
            return result;
        }
        
        List<String> validTeam1 = validatePlayerList(team1);
        List<String> validTeam2 = validatePlayerList(team2);
        
        if (validTeam1.isEmpty() && validTeam2.isEmpty()) {
            result.addError("No valid players provided");
            return result;
        }
        
        result.setValidTeam1(validTeam1);
        result.setValidTeam2(validTeam2);
        
        if (preferredMapName != null && !preferredMapName.trim().isEmpty()) {
            IArena preferredArena = arenaService.getArenaByName(preferredMapName);
            if (preferredArena == null) {
                result.addWarning("Preferred map '" + preferredMapName + "' not found");
            } else if (!validateArenaAvailable(preferredArena)) {
                result.addWarning("Preferred map '" + preferredMapName + "' is not available");
            }
        }
        
        List<IArena> availableArenas = arenaService.getAvailableRBWArenas();
        if (availableArenas.isEmpty()) {
            result.addError("No available RBW arenas found");
            return result;
        }
        
        result.setSuccess(true);
        return result;
    }
    
    public boolean safeArenaOperation(String operation, IArena arena, Runnable operationCode) {
        if (!validateRBWArena(arena)) {
            return false;
        }
        
        try {
            operationCode.run();
            plugin.getLogger().info("Successfully executed " + operation + " on arena: " + arena.getArenaName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to execute " + operation + " on arena " + arena.getArenaName() + ": " + e.getMessage());
            return false;
        }
    }
    
    public static class ValidationResult {
        private boolean success = false;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> validTeam1 = new ArrayList<>();
        private List<String> validTeam2 = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public List<String> getValidTeam1() {
            return validTeam1;
        }
        
        public void setValidTeam1(List<String> validTeam1) {
            this.validTeam1 = validTeam1;
        }
        
        public List<String> getValidTeam2() {
            return validTeam2;
        }
        
        public void setValidTeam2(List<String> validTeam2) {
            this.validTeam2 = validTeam2;
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
        
        public String getWarningMessage() {
            return String.join("; ", warnings);
        }
    }
}
