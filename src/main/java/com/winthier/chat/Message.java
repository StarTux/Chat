package com.winthier.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Message {
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
    public List<Object> json;
    public List<Object> languageFilterJson;

    private static void store(Map<String, Object> map, String key, Object value) {
        if (value == null) return;
        map.put(key, value);
    }

    private static String fetchString(Map<String, Object> map, String key) {
        Object result = map.get(key);
        return key == null ? null : key.toString();
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
        store(map, "sender", sender.toString());
        store(map, "senderName", senderName);
        store(map, "channel", channel);
        store(map, "special", special);
        store(map, "target", target.toString());
        store(map, "targetName", targetName);
        store(map, "senderTitle", senderTitle);
        store(map, "senderTitleDescription", senderTitleDescription);
        store(map, "senderServer", senderServer);
        store(map, "senderServerDisplayName", senderServerDisplayName);
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
        senderServer = fetchString(map, "senderServerDisplayName");
        message = fetchString(map, "message");
        json = fetchList(map, "json");
        languageFilterJson = fetchList(map, "languageFilterJson");
    }

    public static Message deserialize(Map<String, Object> map) {
        Message result = new Message();
        result.read(map);
        return result;
    }
}
