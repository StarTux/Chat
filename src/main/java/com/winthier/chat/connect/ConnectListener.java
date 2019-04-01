package com.winthier.chat.connect;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.util.Msg;
import com.winthier.connect.Connect;
import com.winthier.connect.OnlinePlayer;
import com.winthier.connect.event.ConnectMessageEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ConnectListener implements Listener {
    private static final String CHANNEL = "Chat";

    @EventHandler
    public void onConnectMessage(ConnectMessageEvent event) {
        String channel = event.getMessage().getChannel();
        if (channel.equals(CHANNEL)) {
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
        } else if (channel.equals("BUNGEE_PLAYER_JOIN")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)event.getMessage().getPayload();
            UUID uuid = UUID.fromString((String)map.get("uuid"));
            String name = (String)map.get("name");
            ChatPlugin.getInstance().onBungeeJoin(uuid, name);
        } else if (channel.equals("BUNGEE_PLAYER_QUIT")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>)event.getMessage().getPayload();
            UUID uuid = UUID.fromString((String)map.get("uuid"));
            String name = (String)map.get("name");
            ChatPlugin.getInstance().onBungeeQuit(uuid, name);
        }
    }

    public String getServerName() {
        return Connect.getInstance().getServerName();
    }

    public String getServerDisplayName() {
        return Msg.camelCase(Connect.getInstance().getServerName());
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
