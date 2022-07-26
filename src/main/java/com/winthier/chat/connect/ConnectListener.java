package com.winthier.chat.connect;

import com.cavetale.core.connect.Connect;
import com.cavetale.core.event.connect.ConnectMessageEvent;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.util.Json;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.MetaMessage;
import com.winthier.chat.sql.SQLIgnore;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ConnectListener implements Listener {
    private static final String CHANNEL = "Chat";
    private static final String META_CHANNEL = "ChatMeta";
    public static final String META_IGNORE = "chat:ignore";

    private static final class BungeePacket {
        private PlayerCache player;
        private String server;
    }

    @EventHandler
    public void onConnectMessage(ConnectMessageEvent event) {
        switch (event.getChannel()) {
        case CHANNEL: {
            Message message = Message.deserialize(event.getPayload());
            if (message == null) {
                ChatPlugin.getInstance().getLogger().warning("Failed to deserialize message: " + event.getPayload());
            } else {
                ChatPlugin.getInstance().didReceiveMessage(message);
            }
            return;
        }
        case META_CHANNEL: {
            MetaMessage metaMessage = Json.deserialize(event.getPayload(), MetaMessage.class);
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
            BungeePacket packet = Json.deserialize(event.getPayload(), BungeePacket.class);
            ChatPlugin.getInstance().onBungeeJoin(packet.player.uuid, packet.player.name, event.getCreated().getTime());
            return;
        }
        case "BUNGEE_PLAYER_QUIT": {
            BungeePacket packet = Json.deserialize(event.getPayload(), BungeePacket.class);
            ChatPlugin.getInstance().onBungeeQuit(packet.player.uuid, packet.player.name, event.getCreated().getTime());
            return;
        }
        default: return;
        }
    }

    public void broadcastMessage(Message message) {
        Connect.get().broadcastMessage(CHANNEL, message.serialize());
    }

    public void broadcastMeta(String meta, UUID uuid) {
        MetaMessage metaMessage = new MetaMessage(meta, uuid);
        Connect.get().broadcastMessage(META_CHANNEL, Json.serialize(metaMessage));
    }
}
