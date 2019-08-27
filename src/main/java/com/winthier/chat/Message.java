package com.winthier.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

public final class Message {
    public UUID sender;
    public String senderName;
    public String senderDisplayName;
    public String channel;
    public String special;
    public UUID target;
    public String targetName;
    public String senderTitle;
    public String senderTitleDescription;
    public String senderServer;
    public String senderServerDisplayName;
    public String message;

    transient public Location location;
    transient public boolean shouldCancel = false;
    transient public String prefix = null;

    transient public boolean local;

    private static void store(Map<String, Object> map, String key, Object value) {
        if (value == null) return;
        if (value instanceof UUID) {
            map.put(key, ((UUID)value).toString());
        } else {
            map.put(key, value);
        }
    }

    private static String fetchString(Map<String, Object> map, String key) {
        Object result = map.get(key);
        return result == null ? null : result.toString();
    }

    private static UUID fetchUuid(Map<String, Object> map, String key) {
        String result = fetchString(map, key);
        return result == null ? null : UUID.fromString(result);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> fetchList(Map<String, Object> map, String key) {
        Object result = map.get(key);
        if (result == null) return null;
        return result instanceof List ? (List<Object>)result : null;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        store(map, "sender", sender);
        store(map, "senderName", senderName);
        store(map, "senderDisplayName", senderDisplayName);
        store(map, "channel", channel);
        store(map, "special", special);
        store(map, "target", target);
        store(map, "targetName", targetName);
        store(map, "senderTitle", senderTitle);
        store(map, "senderTitleDescription", senderTitleDescription);
        store(map, "senderServer", senderServer);
        store(map, "senderServerDisplayName", senderServerDisplayName);
        store(map, "message", message);
        return map;
    }

    private void read(Map<String, Object> map) {
        sender = fetchUuid(map, "sender");
        senderName = fetchString(map, "senderName");
        senderDisplayName = fetchString(map, "senderDisplayName");
        channel = fetchString(map, "channel");
        special = fetchString(map, "special");
        target = fetchUuid(map, "target");
        targetName = fetchString(map, "targetName");
        senderTitle = fetchString(map, "senderTitle");
        senderTitleDescription = fetchString(map, "senderTitleDescription");
        senderServer = fetchString(map, "senderServer");
        senderServerDisplayName = fetchString(map, "senderServerDisplayName");
        message = fetchString(map, "message");
    }

    public static Message deserialize(Map<String, Object> map) {
        Message result = new Message();
        result.read(map);
        return result;
    }
}
