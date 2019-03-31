package com.winthier.chat;

import cn.nukkit.level.Location;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Message {
    public UUID sender;
    public String senderName;
    public String channel;
    public String special;
    public UUID target;
    public String targetName;
    public String senderTitle;
    public String senderTitleDescription;
    public String senderServer;
    public String senderServerDisplayName;

    public String message;
    public String languageFilterMessage;
    public List<Object> json;
    public List<Object> languageFilterJson;

    public transient Location location;
    public transient boolean shouldCancel = false;
    public transient String prefix = null;

    public transient boolean local;

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
        store(map, "channel", channel);
        store(map, "special", special);
        store(map, "target", target);
        store(map, "targetName", targetName);
        store(map, "senderTitle", senderTitle);
        store(map, "senderTitleDescription", senderTitleDescription);
        store(map, "senderServer", senderServer);
        store(map, "senderServerDisplayName", senderServerDisplayName);
        store(map, "message", message);
        store(map, "languageFilterMessage", languageFilterMessage);
        store(map, "json", json);
        store(map, "languageFilterJson", languageFilterJson);
        return map;
    }

    private void read(Map<String, Object> map) {
        sender = fetchUuid(map, "sender");
        senderName = fetchString(map, "senderName");
        channel = fetchString(map, "channel");
        special = fetchString(map, "special");
        target = fetchUuid(map, "target");
        targetName = fetchString(map, "targetName");
        senderTitle = fetchString(map, "senderTitle");
        senderTitleDescription = fetchString(map, "senderTitleDescription");
        senderServer = fetchString(map, "senderServer");
        senderServerDisplayName = fetchString(map, "senderServerDisplayName");
        message = fetchString(map, "message");
        languageFilterMessage = fetchString(map, "languageFilterMessage");
        json = fetchList(map, "json");
        languageFilterJson = fetchList(map, "languageFilterJson");
    }

    public static Message deserialize(Map<String, Object> map) {
        Message result = new Message();
        result.read(map);
        return result;
    }
}
