package com.winthier.chat;

import com.winthier.chat.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Location;

@Data
public final class Message {
    public UUID sender;
    public String senderName;
    public String channel;
    public String special;
    public UUID target;
    public String targetName;
    public String senderTitle;
    public String senderTitleDescription;
    public String senderTitleJson;
    public String senderServer;
    public String senderServerDisplayName;

    public String message;
    public String languageFilterMessage;
    public List<Object> json;
    public List<Object> languageFilterJson;

    protected boolean hideSenderTags;
    protected boolean passive;

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

    public String serialize() {
        return Msg.GSON.toJson(this);
    }

    public static Message deserialize(String in) {
        return Msg.GSON.fromJson(in, Message.class);
    }
}
