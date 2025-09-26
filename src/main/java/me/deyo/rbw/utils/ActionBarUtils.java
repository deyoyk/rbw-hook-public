package me.deyo.rbw.utils;

import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class ActionBarUtils {
    
    public static void send(Player player, String message) {
        if (player == null || message == null) return;
        
        try {
            IChatBaseComponent component = new ChatComponentText(message);
            PacketPlayOutChat packet = new PacketPlayOutChat(component, (byte) 2);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        } catch (Exception e) {
        }
    }
}

