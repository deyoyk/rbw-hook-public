package me.deyo.rbw.core;

import com.google.gson.JsonObject;
import me.deyo.rbw.RBWPlugin;
import me.deyo.rbw.model.QueueInfo;
import me.deyo.rbw.service.MessageService;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketManager {
    
    private final RBWPlugin plugin;
    private final MessageService messageService;
    private final AtomicBoolean shouldRetry = new AtomicBoolean(true);
    private final Object queueLock = new Object();
    
    private Map<String, QueueInfo> latestQueues = new HashMap<>();
    private int reconnectAttempts = 0;
    private boolean reconnectScheduled = false;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    
    private WebSocketClient currentClient = null;
    
    public WebSocketManager(RBWPlugin plugin) {
        this.plugin = plugin;
        this.messageService = new MessageService(this, plugin);
    }
    
    private static URI createURI(RBWPlugin plugin) {
        String host = plugin.getConfigManager().getWebsocketHost();
        int port = plugin.getConfigManager().getWebsocketPort();
        try {
            return new URI("ws://" + host + ":" + port + "/rbw/websocket");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create WebSocket URI: " + e.getMessage());
            return null;
        }
    }
    
    private WebSocketClient createWebSocketClient() {
        URI uri = createURI(plugin);
        if (uri == null) {
            return null;
        }
        
        return new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                plugin.getLogger().info("WebSocket connection established");
                reconnectAttempts = 0;
                reconnectScheduled = false;
                sendInitialData();
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                plugin.getLogger().warning("WebSocket connection closed: " + reason + " (code: " + code + ")");
                
                if (plugin.isEnabled() && !reconnectScheduled) {
                    scheduleReconnect();
                }
            }
            
            @Override
            public void onMessage(String message) {
                try {
                    messageService.handleMessage(message);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error handling WebSocket message: " + e.getMessage());
                }
            }
            
            @Override
            public void onError(Exception ex) {
                plugin.getLogger().severe("WebSocket error: " + ex.getMessage());
                
                if (!isOpen() && !reconnectScheduled && plugin.isEnabled()) {
                    scheduleReconnect();
                }
            }
        };
    }
    
    public void connect() {
        if (createURI(plugin) == null) {
            plugin.getLogger().severe("Cannot connect: Invalid WebSocket URI");
            return;
        }
        
        if (isConnected()) {
            plugin.getLogger().info("WebSocket already connected");
            return;
        }
        
        try {
            currentClient = createWebSocketClient();
            if (currentClient != null) {
                currentClient.connect();
                plugin.getLogger().info("WebSocket connection initiated");
            } else {
                plugin.getLogger().severe("Failed to create WebSocket client");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initiate WebSocket connection: " + e.getMessage());
            if (!reconnectScheduled) {
                scheduleReconnect();
            }
        }
    }
    
    public void disconnect() {
        shouldRetry.set(false);
        reconnectScheduled = false;
        
        if (currentClient != null && currentClient.isOpen()) {
            try {
                currentClient.close();
                plugin.getLogger().info("WebSocket connection closed");
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing WebSocket connection: " + e.getMessage());
            }
        }
        currentClient = null;
    }
    
    public void sendMessage(String message) {
        if (isConnected()) {
            currentClient.send(message);
        } else {
            plugin.getLogger().warning("Cannot send message: WebSocket connection is closed");
            if (!reconnectScheduled) {
                scheduleReconnect();
            }
        }
    }
    
    public void sendMessage(JsonObject json) {
        sendMessage(json.toString());
    }
    
    public void sendPlayerStatus(String playerName, boolean online) {
        sendPlayerStatus(playerName, online, null);
    }
    
    public void sendPlayerStatus(String playerName, boolean online, String requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "PLAYER_STATUS");
        response.addProperty("ign", playerName);
        response.addProperty("online", online);
        if (requestId != null) {
            response.addProperty("request_id", requestId);
        }
        sendMessage(response);
    }
    
    public Map<String, QueueInfo> getLatestQueuesSnapshot() {
        synchronized (queueLock) {
            return Collections.unmodifiableMap(new HashMap<>(latestQueues));
        }
    }
    
    public void updateQueues(Map<String, QueueInfo> queues) {
        synchronized (queueLock) {
            latestQueues = new HashMap<>(queues);
        }
    }
    
    private void sendInitialData() {
        try {
            JsonObject statusMessage = new JsonObject();
            statusMessage.addProperty("type", "SERVER_STATUS");
            statusMessage.addProperty("status", "connected");
            statusMessage.addProperty("timestamp", System.currentTimeMillis());
            sendMessage(statusMessage);
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending initial data: " + e.getMessage());
        }
    }
    
    private void scheduleReconnect() {
        if (reconnectScheduled || !plugin.isEnabled()) {
            return;
        }
        
        if (isConnected()) {
            return;
        }
        
        reconnectScheduled = true;
        reconnectAttempts++;
        
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            plugin.getLogger().severe("Failed to connect to WebSocket server after " + reconnectAttempts + " attempts");
            reconnectScheduled = false;
            return;
        }
        
        int delay = Math.min(reconnectAttempts * 5, 60);
        plugin.getLogger().info("Scheduling WebSocket reconnection in " + delay + " seconds (Attempt " + reconnectAttempts + ")");
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.isEnabled() || isConnected()) {
                reconnectScheduled = false;
                return;
            }
            
            reconnectScheduled = false;
            connect();
        }, delay * 20L);
    }
    
    public boolean isConnected() {
        return currentClient != null && currentClient.isOpen();
    }
}

