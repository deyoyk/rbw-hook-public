package me.deyo.rbw.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.core.WebSocketManager;
import me.deyo.rbw.model.QueueInfo;
import me.deyo.rbw.service.game.GameSetupService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageService {
    
    private final WebSocketManager webSocketManager;
    private final RBWPlugin plugin;
    
    public MessageService(WebSocketManager webSocketManager, RBWPlugin plugin) {
        this.webSocketManager = webSocketManager;
        this.plugin = plugin;
    }
    
    public void handleMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        if (isNonJsonContent(message)) {
            return;
        }
        
        try {
            JsonObject json = new JsonParser().parse(message).getAsJsonObject();
            if (!json.has("type")) {
                return;
            }
            
            String type = json.get("type").getAsString();
            handleMessageByType(type, json);
            
        } catch (Exception e) {
            if (message.trim().startsWith("{") || message.trim().startsWith("[")) {
                plugin.getLogger().warning("Failed to parse JSON: " + message);
            }
        }
    }
    
    private boolean isNonJsonContent(String message) {
        String trimmed = message.trim();
        return trimmed.startsWith("HTTP/") ||
               trimmed.startsWith("Date:") ||
               trimmed.startsWith("Server:") ||
               trimmed.startsWith("Content-") ||
               trimmed.startsWith("Connection:") ||
               trimmed.startsWith("Failed to open") ||
               (!trimmed.startsWith("{") && !trimmed.startsWith("["));
    }
    
    private void handleMessageByType(String type, JsonObject json) {
        switch (type.toUpperCase()) {
            case "SERVER_STATUS":
            case "SERVERSTATUS":
                handleServerStatus(json);
                break;
            case "WARP_PLAYERS":
            case "WARPPLAYERS":
                handleWarpPlayers(json);
                break;
            case "PING":
                handlePing(json);
                break;
            case "QUEUESTATUS":
                handleQueueStatus(json);
                break;
            case "CHECK_PLAYER":
            case "CHECKPLAYER":
                handleCheckPlayer(json);
                break;
            case "RETRYGAME":
                handleRetryGame(json);
                break;
            case "VERIFICATION_CODE":
            case "VERIFICATIONCODE":
                handleVerificationCode(json);
                break;
            case "CALL_SUCCESS":
            case "CALLSUCCESS":
                handleCallSuccess(json);
                break;
            case "CALL_FAILURE":
            case "CALLFAILURE":
                handleCallFailure(json);
                break;
            case "AUTOSS_SUCCESS":
            case "AUTOSSSUCCESS":
                handleAutossSuccess(json);
                break;
            case "AUTOSS_ERROR":
            case "AUTOSSERROR":
                handleAutossError(json);
                break;
            case "SCREENSHAREDONTLOG_SUCCESS":
            case "SCREENSHAREDONTLOGSUCCESS":
                handleScreenshareDontLogSuccess(json);
                break;
            case "SCREENSHAREDONTLOG_ERROR":
            case "SCREENSHAREDONTLOGERROR":
                handleScreenshareDontLogError(json);
                break;
            case "PLAYER_STATUS":
            case "PLAYERSTATUS":
                handlePlayerStatus(json);
                break;
            default:
                plugin.getLogger().info("Unknown message type: " + type);
        }
    }
    
    private void handleServerStatus(JsonObject json) {
        String status = json.has("status") ? json.get("status").getAsString() : "unknown";
        plugin.getLogger().info("Server status: " + status);
    }
    
    private void handlePing(JsonObject json) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "PONG");
        response.addProperty("timestamp", System.currentTimeMillis() / 1000.0);
        if (json.has("request_id")) {
            response.addProperty("request_id", json.get("request_id").getAsString());
        }
        webSocketManager.sendMessage(response);
    }
    
    private void handleWarpPlayers(JsonObject json) {
        String requestId = json.has("request_id") ? json.get("request_id").getAsString() : null;
        
        try {
            String gameId = json.get("game_id").getAsString();
            String mapName = json.get("map").getAsString();
            boolean isRanked = json.get("is_ranked").getAsBoolean();
            
            List<String> team1 = extractPlayerNames(json.getAsJsonArray("team1"));
            List<String> team2 = extractPlayerNames(json.getAsJsonArray("team2"));
            
            GameSetupService.Result result = GameSetupService.setupGame(plugin, mapName, team1, team2, isRanked, gameId);
            
            if (result.isSuccess()) {
                plugin.getGameManager().registerGame(gameId, result.getArenaName(), team1, team2);
                sendWarpSuccessResponse(gameId, result.getArenaName(), requestId);
            } else {
                sendWarpFailureResponse(gameId, mapName, result, requestId);
            }
        } catch (Exception e) {
            sendErrorResponse("Failed to process warp_players", e.getMessage(), requestId);
        }
    }
    
    private void handleQueueStatus(JsonObject json) {
        try {
            if (!json.has("queues") || !json.get("queues").isJsonObject()) {
                return;
            }
            
            JsonObject queuesObj = json.getAsJsonObject("queues");
            Map<String, QueueInfo> newQueues = new HashMap<>();
            
            for (Map.Entry<String, com.google.gson.JsonElement> entry : queuesObj.entrySet()) {
                String queueName = entry.getKey();
                JsonObject queueJson = entry.getValue().getAsJsonObject();
                
                List<String> players = extractPlayerNames(queueJson.getAsJsonArray("players"));
                QueueInfo.EloRange eloRange = extractEloRange(queueJson.getAsJsonObject("elo_range"));
                int capacity = queueJson.has("capacity") ? queueJson.get("capacity").getAsInt() : 0;
                
                newQueues.put(queueName, new QueueInfo(players, eloRange, capacity));
            }
            
            webSocketManager.updateQueues(newQueues);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process queue status: " + e.getMessage());
        }
    }
    
    private void handleCheckPlayer(JsonObject json) {
        try {
            if (!json.has("ign")) return;
            
            String playerName = json.get("ign").getAsString();
            String requestId = json.has("request_id") ? json.get("request_id").getAsString() : null;
            
            boolean isOnline = Bukkit.getPlayerExact(playerName) != null;
            webSocketManager.sendPlayerStatus(playerName, isOnline, requestId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process check player: " + e.getMessage());
        }
    }
    
    private void handleRetryGame(JsonObject json) {
        try {
            if (!json.has("gameid")) return;
            
            String gameId = json.get("gameid").getAsString();
            String mapName = json.has("map") ? json.get("map").getAsString() : null;
            boolean isRanked = json.has("is_ranked") ? json.get("is_ranked").getAsBoolean() : true;
            
            List<String> team1 = extractPlayerNames(json.getAsJsonArray("team1"));
            List<String> team2 = extractPlayerNames(json.getAsJsonArray("team2"));
            
            GameSetupService.Result result = GameSetupService.setupGame(plugin, mapName, team1, team2, isRanked, gameId);
            
            if (result.isSuccess()) {
                plugin.getGameManager().registerGame(gameId, result.getArenaName(), team1, team2);
                sendRetrySuccessResponse(gameId);
            } else {
                sendRetryFailureResponse(gameId, result.getMessage());
            }
        } catch (Exception e) {
            sendRetryErrorResponse(e.getMessage());
        }
    }
    
    private void handleVerificationCode(JsonObject json) {
        try {
            String playerName = json.has("ign") ? json.get("ign").getAsString() : null;
            String message = json.has("message") ? json.get("message").getAsString() : null;
            String code = json.has("code") ? json.get("code").getAsString() : null;
            
            if (playerName != null && message != null) {
                Player player = Bukkit.getPlayerExact(playerName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.GOLD + "[Ranked] " + ChatColor.GREEN + 
                        "Your Ranked BedWars Verification Code is: " + ChatColor.AQUA + code);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process verification code: " + e.getMessage());
        }
    }
    
    private void handleCallSuccess(JsonObject json) {
        try {
            String requesterName = json.has("requester_ign") ? json.get("requester_ign").getAsString() : null;
            String targetName = json.has("target_ign") ? json.get("target_ign").getAsString() : null;
            
            if (requesterName != null) {
                Player player = Bukkit.getPlayerExact(requesterName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.GREEN + "Voice call request successful! " + 
                        (targetName != null ? targetName + " can now join/speak in the voice channel." : ""));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process call success: " + e.getMessage());
        }
    }
    
    private void handleCallFailure(JsonObject json) {
        try {
            String requesterName = json.has("requester_ign") ? json.get("requester_ign").getAsString() : null;
            String reason = json.has("reason") ? json.get("reason").getAsString() : "Unknown error";
            
            if (requesterName != null) {
                Player player = Bukkit.getPlayerExact(requesterName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Voice call request failed: " + reason);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process call failure: " + e.getMessage());
        }
    }
    
    private void handleAutossSuccess(JsonObject json) {
        try {
            String requesterName = json.has("requester_ign") ? json.get("requester_ign").getAsString() : null;
            String targetName = json.has("target_ign") ? json.get("target_ign").getAsString() : null;
            String screenshareId = json.has("screenshare_id") ? json.get("screenshare_id").getAsString() : null;
            String message = json.has("message") ? json.get("message").getAsString() : null;
            
            if (requesterName != null) {
                Player player = Bukkit.getPlayerExact(requesterName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.GREEN + "Screenshare created successfully for " + targetName + 
                        (screenshareId != null ? " (ID: " + screenshareId + ")" : ""));
                    if (message != null) {
                        player.sendMessage(ChatColor.GRAY + message);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process autoss success: " + e.getMessage());
        }
    }
    
    private void handleAutossError(JsonObject json) {
        try {
            String requesterName = json.has("requester_ign") ? json.get("requester_ign").getAsString() : null;
            String error = json.has("error") ? json.get("error").getAsString() : "Unknown error";
            
            if (requesterName != null) {
                Player player = Bukkit.getPlayerExact(requesterName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Screenshare request failed: " + error);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process autoss error: " + e.getMessage());
        }
    }
    
    private void handleScreenshareDontLogSuccess(JsonObject json) {
        try {
            String targetName = json.has("target_ign") ? json.get("target_ign").getAsString() : null;
            String message = json.has("message") ? json.get("message").getAsString() : null;
            
            if (targetName != null) {
                Player player = Bukkit.getPlayerExact(targetName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "You are currently being screenshared");
                    if (message != null) {
                        player.sendMessage(ChatColor.GRAY + message);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process screenshare dont log success: " + e.getMessage());
        }
    }
    
    private void handleScreenshareDontLogError(JsonObject json) {
        try {
            String targetName = json.has("target_ign") ? json.get("target_ign").getAsString() : null;
            String error = json.has("error") ? json.get("error").getAsString() : "Unknown error";
            
            if (targetName != null) {
                Player player = Bukkit.getPlayerExact(targetName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Screenshare logging toggle failed: " + error);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process screenshare dont log error: " + e.getMessage());
        }
    }
    
    private void handlePlayerStatus(JsonObject json) {
        try {
            String playerName = json.has("ign") ? json.get("ign").getAsString() : null;
            boolean online = json.has("online") ? json.get("online").getAsBoolean() : false;
            String requestId = json.has("request_id") ? json.get("request_id").getAsString() : null;
            
            if (requestId != null) {
                webSocketManager.sendPlayerStatus(playerName, online, requestId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to process player status: " + e.getMessage());
        }
    }
    
    private List<String> extractPlayerNames(JsonArray playersArray) {
        List<String> names = new ArrayList<>();
        if (playersArray == null) return names;
        
        for (int i = 0; i < playersArray.size(); i++) {
            try {
                JsonObject playerJson = playersArray.get(i).getAsJsonObject();
                if (playerJson.has("ign")) {
                    names.add(playerJson.get("ign").getAsString());
                }
            } catch (Exception ignored) {}
        }
        return names;
    }
    
    private QueueInfo.EloRange extractEloRange(JsonObject eloJson) {
        if (eloJson == null) {
            return new QueueInfo.EloRange(0, 0);
        }
        
        int min = eloJson.has("min") ? eloJson.get("min").getAsInt() : 0;
        int max = eloJson.has("max") ? eloJson.get("max").getAsInt() : 0;
        return new QueueInfo.EloRange(min, max);
    }
    
    private void sendWarpSuccessResponse(String gameId, String arenaName, String requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "WARP_SUCCESS");
        response.addProperty("game_id", gameId);
        response.addProperty("map", arenaName);
        if (requestId != null) response.addProperty("request_id", requestId);
        webSocketManager.sendMessage(response);
    }
    
    private void sendWarpFailureResponse(String gameId, String mapName, GameSetupService.Result result, String requestId) {
        JsonObject response = new JsonObject();
        
        if (result.getArenaName() == null && (result.getOfflinePlayers() == null || result.getOfflinePlayers().isEmpty())) {
            response.addProperty("type", "WARP_FAILED_ARENA_NOT_FOUND");
        } else if (result.getOfflinePlayers() != null && !result.getOfflinePlayers().isEmpty()) {
            response.addProperty("type", "WARP_FAILED_OFFLINE_PLAYERS");
            JsonArray offlinePlayers = new JsonArray();
            for (String player : result.getOfflinePlayers()) {
                offlinePlayers.add(new com.google.gson.JsonPrimitive(player));
            }
            response.add("offline_players", offlinePlayers);
        } else {
            response.addProperty("type", "ERROR");
            response.addProperty("error", result.getMessage() != null ? result.getMessage() : "Unknown warp failure");
        }
        
        response.addProperty("game_id", gameId);
        response.addProperty("map", mapName);
        if (requestId != null) response.addProperty("request_id", requestId);
        webSocketManager.sendMessage(response);
    }
    
    private void sendErrorResponse(String error, String details, String requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "ERROR");
        response.addProperty("error", error);
        response.addProperty("details", details);
        if (requestId != null) response.addProperty("request_id", requestId);
        webSocketManager.sendMessage(response);
    }
    
    private void sendRetrySuccessResponse(String gameId) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "RETRY_SUCCESS");
        response.addProperty("game_id", gameId);
        webSocketManager.sendMessage(response);
    }
    
    private void sendRetryFailureResponse(String gameId, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "RETRY_FAILED");
        response.addProperty("game_id", gameId);
        response.addProperty("error", error != null ? error : "Unknown retry failure");
        webSocketManager.sendMessage(response);
    }
    
    private void sendRetryErrorResponse(String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "RETRY_ERROR");
        response.addProperty("error", "Failed to process retry game");
        response.addProperty("details", error);
        webSocketManager.sendMessage(response);
    }
}

