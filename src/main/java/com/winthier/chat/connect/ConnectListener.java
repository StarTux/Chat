package com.winthier.chat.connect;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.MetaMessage;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.util.Msg;
import com.winthier.connect.Connect;
import com.winthier.connect.OnlinePlayer;
import com.winthier.connect.event.ConnectMessageEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ConnectListener implements Listener {
    private static final String CHANNEL = "Chat";
    private static final String META_CHANNEL = "ChatMeta";
    public static final String META_IGNORE = "ignore";

    @EventHandler
    public void onConnectMessage(ConnectMessageEvent event) {
        switch (event.getMessage().getChannel()) {
        case CHANNEL: {
            Object o = event.getMessage().getPayload();
            if (!(o instanceof String)) return;
            Message message = Message.deserialize((String) o);
            if (message == null) {
                ChatPlugin.getInstance().getLogger().warning("Failed to deserialize message: " + o);
            } else {
                ChatPlugin.getInstance().didReceiveMessage(message);
            }
            return;
        }
        case META_CHANNEL: {
            String payload = Objects.requireNonNull(event.getMessage().getPayload()).toString();
            MetaMessage metaMessage = Msg.GSON.fromJson(payload, MetaMessage.class);
            String meta = Objects.requireNonNull(metaMessage.getMeta());
            switch (meta) {
            case META_IGNORE:
                UUID uuid = Objects.requireNonNull(metaMessage.getUuid());
                SQLIgnore.loadIgnoresAsync(uuid);
                break;
            default:
                throw new IllegalArgumentException("Unknown meta: " + meta);
            }
            return;
        }
        case "BUNGEE_PLAYER_JOIN": {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) event.getMessage().getPayload();
            UUID uuid = UUID.fromString((String) map.get("uuid"));
            String name = (String) map.get("name");
            String server = (String) map.get("server");
            ChatPlugin.getInstance().onBungeeJoin(uuid, name, server);
            return;
        }
        case "BUNGEE_PLAYER_QUIT": {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) event.getMessage().getPayload();
            UUID uuid = UUID.fromString((String) map.get("uuid"));
            String name = (String) map.get("name");
            String server = (String) map.get("server");
            ChatPlugin.getInstance().onBungeeQuit(uuid, name, server);
            return;
        }
        default: return;
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

    public void broadcastMeta(String meta, UUID uuid) {
        MetaMessage metaMessage = new MetaMessage(meta, uuid);
        Connect.getInstance().broadcast(META_CHANNEL, Msg.GSON.toJson(metaMessage));
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
