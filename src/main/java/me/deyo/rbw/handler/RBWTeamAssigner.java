package me.deyo.rbw.handler;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.ITeamAssigner;
import me.deyo.rbw.service.game.GameSetupService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class RBWTeamAssigner implements ITeamAssigner {
    
    @Override
    public void assignTeams(IArena arena) {
        GameSetupService.RankedGame rankedGame = GameSetupService.getArenaToGameMapping().get(arena.getArenaName());
        if (rankedGame == null) {
            return;
        }
        
        List<List<String>> teamNames = rankedGame.getTeamNames();
        if (teamNames.size() < 2) {
            return;
        }
        
        List<ITeam> arenaTeams = arena.getTeams();
        if (arenaTeams.size() < 2) {
            return;
        }
        
        List<String> team1Names = teamNames.get(0);
        ITeam team1 = arenaTeams.get(0);
        for (String playerName : team1Names) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && arena.getPlayers().contains(player)) {
                team1.addPlayers(player);
            }
        }
        
        List<String> team2Names = teamNames.get(1);
        ITeam team2 = arenaTeams.get(1);
        for (String playerName : team2Names) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && arena.getPlayers().contains(player)) {
                team2.addPlayers(player);
            }
        }
    }
}

