package com.winthier.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Message {
    public UUID sender;
    public String channel;
    public UUID recipient;
    public String senderName;
    public String senderTitle;
    public String senderTitleDescription;
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
        store(map, "channel", channel);
        store(map, "recipient", recipient.toString());
        store(map, "senderName", senderName);
        store(map, "senderTitle", senderTitle);
        store(map, "senderTitleDescription", senderTitleDescription);
        store(map, "json", json);
        store(map, "languageFilterJson", languageFilterJson);
        return map;
    }

    private void read(Map<String, Object> map) {
        sender = fetchUuid(map, "sender");
        channel = fetchString(map, "channel");
        recipient = fetchUuid(map, "recipient");
        senderName = fetchString(map, "senderName");
        senderTitle = fetchString(map, "senderTitle");
        senderTitleDescription = fetchString(map, "senderTitleDescription");
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
