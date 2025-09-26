package me.deyo.rbw.listeners;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent;
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent;
import com.andrei1058.bedwars.api.events.server.ArenaEnableEvent;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.model.Game;
import me.deyo.rbw.service.arena.ArenaService;
import me.deyo.rbw.service.game.GameSetupService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;

public class GameListener implements Listener {
    
    private final RBWPlugin plugin;
    private final ArenaService arenaService;
    
    public GameListener(RBWPlugin plugin) {
        this.plugin = plugin;
        this.arenaService = new ArenaService(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArenaEnable(ArenaEnableEvent event) {
        IArena arena = event.getArena();
        if (arena.getStatus() == GameState.waiting && 
            arena.getGroup() != null && 
            plugin.getConfigManager().isRBWMode(arena.getGroup())) {
            
            plugin.getLogger().info("RBW arena enabled: " + arenaService.getArenaInfo(arena));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(GameEndEvent event) {
        IArena arena = event.getArena();
        String arenaName = arena.getArenaName();
        Game game = plugin.getGameManager().getGameByArena(arenaName);
        
        if (game == null) {
            return;
        }
        
        game.endGame();
        game.recalculateAwards();
        
        int winningTeamNumber = determineWinningTeamNumber(event, game);
        sendScoringData(game, winningTeamNumber);
        
        game.cleanup();
        plugin.getGameManager().unregisterGameByArena(arenaName);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKill(PlayerKillEvent event) {
        IArena arena = event.getArena();
        Game game = plugin.getGameManager().getGameByArena(arena.getArenaName());
        
        if (game == null) return;
        
        String killer = event.getKiller() != null ? event.getKiller().getName() : null;
        String victim = event.getVictim() != null ? event.getVictim().getName() : null;
        
        if (killer != null) game.getOrCreatePlayerStats(killer).addKills(1);
        if (victim != null) game.getOrCreatePlayerStats(victim).addDeaths(1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameManager().getGameByPlayer(player.getName());
        
        if (game == null) return;
        
        Material type = event.getItem().getItemStack().getType();
        int amount = event.getItem().getItemStack().getAmount();
        Game.PlayerStats stats = game.getOrCreatePlayerStats(player.getName());
        
        if (type == Material.DIAMOND) {
            stats.addDiamonds(amount);
        } else if (type == Material.IRON_INGOT) {
            stats.addIrons(amount);
        } else if (type == Material.GOLD_INGOT) {
            stats.addGold(amount);
        } else if (type == Material.EMERALD) {
            stats.addEmeralds(amount);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        Game game = plugin.getGameManager().getGameByPlayer(player.getName());
        if (game == null) return;
        
        game.getOrCreatePlayerStats(player.getName()).addBlocksPlaced(1);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedBreak(PlayerBedBreakEvent event) {
        IArena arena = event.getArena();
        Game game = plugin.getGameManager().getGameByArena(arena.getArenaName());
        
        if (game == null) return;
        
        String player = event.getPlayer() != null ? event.getPlayer().getName() : null;
        if (player != null) {
            game.getOrCreatePlayerStats(player).addBedsBroken(1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        if (arenaService.isPlayerInGame(playerName)) {
            IArena arena = arenaService.getPlayerArena(playerName);
            if (arena != null && arenaService.isRBWArena(arena)) {
                plugin.getLogger().info("Player " + playerName + " reconnected to RBW game in arena: " + arena.getArenaName());
            }
        }
        
        if (plugin.getWebSocketManager() != null) {
            plugin.getWebSocketManager().sendPlayerStatus(playerName, true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        
        if (arenaService.isPlayerInGame(playerName)) {
            IArena arena = arenaService.getPlayerArena(playerName);
            if (arena != null && arenaService.isRBWArena(arena)) {
                plugin.getLogger().info("Player " + playerName + " left RBW game in arena: " + arena.getArenaName());
            }
        }
        
        if (plugin.getWebSocketManager() != null) {
            plugin.getWebSocketManager().sendPlayerStatus(playerName, false);
        }
    }
    
    private int determineWinningTeamNumber(GameEndEvent event, Game game) {
        java.util.List<java.util.UUID> winners = event.getWinners();
        if (winners != null && !winners.isEmpty()) {
            int team1Count = 0, team2Count = 0;
            for (java.util.UUID uuid : winners) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String playerName = offlinePlayer != null ? offlinePlayer.getName() : null;
                if (playerName == null) continue;
                
                if (game.getTeam1Players().contains(playerName)) team1Count++;
                if (game.getTeam2Players().contains(playerName)) team2Count++;
            }
            return team2Count > team1Count ? 2 : 1;
        }
        return 1;
    }
    
    private void sendScoringData(Game game, int winningTeamNumber) {
        if (plugin.getWebSocketManager() == null || !plugin.getWebSocketManager().isConnected()) {
            return;
        }
        
        JsonObject scoring = new JsonObject();
        scoring.addProperty("type", "scoring");
        scoring.addProperty("gameid", game.getGameId());
        scoring.addProperty("winningTeamNumber", winningTeamNumber);
        
        JsonObject players = new JsonObject();
        Set<String> allPlayers = new HashSet<>();
        allPlayers.addAll(game.getTeam1Players());
        allPlayers.addAll(game.getTeam2Players());
        
        for (String playerName : allPlayers) {
            Game.PlayerStats stats = game.getOrCreatePlayerStats(playerName);
            JsonObject playerStats = new JsonObject();
            playerStats.addProperty("kills", stats.getKills());
            playerStats.addProperty("deaths", stats.getDeaths());
            playerStats.addProperty("bedbroken", stats.getBedsBroken() > 0);
            playerStats.addProperty("finalkills", stats.getFinalKills());
            playerStats.addProperty("diamonds", stats.getDiamonds());
            playerStats.addProperty("irons", stats.getIrons());
            playerStats.addProperty("gold", stats.getGold());
            playerStats.addProperty("emeralds", stats.getEmeralds());
            playerStats.addProperty("blocksplaced", stats.getBlocksPlaced());
            players.add(playerName, playerStats);
        }
        scoring.add("players", players);
        
        JsonArray mvps = new JsonArray();
        for (String mvp : game.getMvps()) {
            mvps.add(new com.google.gson.JsonPrimitive(mvp));
        }
        if (mvps.size() > 0) scoring.add("mvps", mvps);
        
        JsonArray bedBreakers = new JsonArray();
        for (String bedBreaker : game.getBedBreakers()) {
            bedBreakers.add(new com.google.gson.JsonPrimitive(bedBreaker));
        }
        if (bedBreakers.size() > 0) scoring.add("bedsbroken", bedBreakers);
        
        plugin.getWebSocketManager().sendMessage(scoring);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameStateChange(GameStateChangeEvent event) {
        if (event.getNewState() != GameState.playing) return;
        
        String arenaName = event.getArena().getArenaName();
        var future = GameSetupService.getArenaStartFutures().remove(arenaName);
        if (future != null) {
            future.complete(null);
            plugin.getLogger().info("Game started successfully in arena: " + arenaName);
        }
    }
}
