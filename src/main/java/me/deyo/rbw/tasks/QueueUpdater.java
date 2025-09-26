package me.deyo.rbw.tasks;

import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.model.QueueInfo;
import me.deyo.rbw.utils.ActionBarUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class QueueUpdater {
    
    private final RBWPlugin plugin;
    private int taskId = -1;
    
    public QueueUpdater(RBWPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        if (taskId != -1) {
            stop();
        }
        
        taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                updateActionBars();
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating action bars: " + e.getMessage());
            }
        }, 20L, 20L).getTaskId();
    }
    
    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
    
    private void updateActionBars() {
        if (plugin.getWebSocketManager() == null || !plugin.getWebSocketManager().isConnected()) {
            return;
        }
        
        Map<String, QueueInfo> queues = plugin.getWebSocketManager().getLatestQueuesSnapshot();
        if (queues.isEmpty()) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName();
            String message = getQueueMessage(playerName, queues);
            
            if (message != null && !message.isEmpty()) {
                ActionBarUtils.send(player, message);
            }
        }
    }
    
    private String getQueueMessage(String playerName, Map<String, QueueInfo> queues) {
        String lowerPlayerName = playerName.toLowerCase();
        Map.Entry<String, QueueInfo> playerQueue = null;
        Map.Entry<String, QueueInfo> defaultQueue = null;
        
        for (Map.Entry<String, QueueInfo> entry : queues.entrySet()) {
            QueueInfo queueInfo = entry.getValue();
            if (queueInfo == null) continue;
            
            if (defaultQueue == null) {
                defaultQueue = entry;
            }
            
            if (queueInfo.getPlayers().stream()
                    .anyMatch(name -> name != null && name.toLowerCase().equals(lowerPlayerName))) {
                playerQueue = entry;
                break;
            }
        }
        
        Map.Entry<String, QueueInfo> selectedQueue = playerQueue != null ? playerQueue : defaultQueue;
        if (selectedQueue == null) {
            return null;
        }
        
        QueueInfo queueInfo = selectedQueue.getValue();
        return String.format("Queue: %s \u0007 %d/%d \u0007 ELO %s",
                selectedQueue.getKey(),
                queueInfo.getPlayerCount(),
                queueInfo.getCapacity(),
                queueInfo.getEloRange().toString());
    }
}
