package com.winthier.chat.connect;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.connect.Connect;
import com.winthier.connect.bukkit.event.ConnectMessageEvent;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ConnectListener implements Listener {
    final static String channel = "Chat";
    
    @EventHandler
    public void onConnectMessage(ConnectMessageEvent event) {
        if (!event.getMessage().getChannel().equals("Chat")) return;
        Object o = event.getMessage().getPayload();
        if (o instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)o;
            Message message = Message.deserialize(map);
            if (message != null) {
                ChatPlugin.getInstance().didReceiveMessage(message);
            }
        }
    }

    public String getServerName() {
        return Connect.getInstance().getServer().getName();
    }

    public void broadcastMessage(Message message) {
        Connect.getInstance().broadcast("Chat", message.serialize());
    }
}
