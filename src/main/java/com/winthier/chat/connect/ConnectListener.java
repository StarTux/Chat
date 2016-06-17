package com.winthier.chat.connect;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.connect.Connect;
import com.winthier.connect.OnlinePlayer;
import com.winthier.connect.bukkit.event.ConnectMessageEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ConnectListener implements Listener {
    final static String CHANNEL = "Chat";
    
    @EventHandler
    public void onConnectMessage(ConnectMessageEvent event) {
        if (!event.getMessage().getChannel().equals(CHANNEL)) return;
        Object o = event.getMessage().getPayload();
        if (o instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)o;
            Message message = Message.deserialize(map);
            if (message == null) {
                ChatPlugin.getInstance().getLogger().warning("Failed to deserialize message: " + map);
            } else {
                ChatPlugin.getInstance().didReceiveMessage(message);
            }
        }
    }

    public String getServerName() {
        return Connect.getInstance().getServer().getName();
    }

    public String getServerDisplayName() {
        return Connect.getInstance().getServer().getDisplayName();
    }

    public void broadcastMessage(Message message) {
        Connect.getInstance().broadcast(CHANNEL, message.serialize());
    }

    public Chatter findPlayer(String name) {
        OnlinePlayer op = Connect.getInstance().findOnlinePlayer(name);
        if (op == null) return null;
        return new Chatter(op.getUuid(), op.getName());
    }

    public List<Chatter> getOnlinePlayers() {
        List<Chatter> result = new ArrayList<>();
        for (OnlinePlayer op: Connect.getInstance().getOnlinePlayers()) {
            result.add(new Chatter(op.getUuid(), op.getName()));
        }
        return result;
    }
}
