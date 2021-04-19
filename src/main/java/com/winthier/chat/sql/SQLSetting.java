package com.winthier.chat.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;

@Table(name = "settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"uuid", "channel", "setting_key"}))
@Data
public final class SQLSetting {
    @Value
    private static final class Key {
        private final UUID uuid;
        private final String channel;
        private final String key;
    }

    @Value
    public static final class Settings {
        private final Map<Key, SQLSetting> map = new HashMap<>();
    }

    // Cache
    private static final Map<UUID, Settings> CACHE = new HashMap<>();
    @Getter private static Settings defaultSettings = new Settings();

    // Content
    @Id private Integer id;
    private UUID uuid;
    @Column(nullable = true, length = 16) private String channel;
    @Column(nullable = false, length = 32) private String settingKey;
    @Column(nullable = true, length = 64) private String settingValue;

    public SQLSetting() { }

    public SQLSetting(final UUID uuid, final String channel, final String key, final Object value) {
        setUuid(uuid);
        setChannel(channel);
        setSettingKey(key);
        setGenericValue(value);
    }

    void setGenericValue(Object value) {
        if (value == null) {
            setSettingValue(null);
        } else if (value instanceof String) {
            setSettingValue((String) value);
        } else if (value instanceof Boolean) {
            Boolean bv = (Boolean) value;
            setSettingValue(bv ? "1" : "0");
        } else if (value instanceof ChatColor) {
            ChatColor color = (ChatColor) value;
            setSettingValue(color.name().toLowerCase());
        } else {
            setSettingValue(value.toString());
        }
    }

    private static Settings makeSettings(List<SQLSetting> list) {
        Settings result = new Settings();
        for (SQLSetting setting: list) {
            Key key = new Key(setting.getUuid(), setting.getChannel(), setting.getSettingKey());
            result.map.put(key, setting);
        }
        return result;
    }

    public static SQLSetting find(UUID uuid, String channel, String key) {
        if (uuid == null) return defaultSettings.map.get(new Key(uuid, channel, key));
        Settings settings = CACHE.get(uuid);
        if (settings == null) return null;
        return settings.map.get(new Key(uuid, channel, key));
    }

    static void clearCache(UUID uuid) {
        if (uuid == null) {
            defaultSettings = null;
        } else {
            CACHE.remove(uuid);
        }
    }

    static void clearCache() {
        CACHE.clear();
        defaultSettings = null;
    }

    public static SQLSetting set(UUID uuid, String channel, String key, Object value) {
        SQLSetting result = find(uuid, channel, key);
        if (result == null) {
            result = new SQLSetting(uuid, channel, key, value);
            if (uuid == null) {
                defaultSettings.map.put(new Key(uuid, channel, key), result);
            } else {
                Settings settings = CACHE.get(uuid);
                if (settings == null) {
                    settings = new Settings();
                    CACHE.put(uuid, settings);
                }
                settings.map.put(new Key(uuid, channel, key), result);
            }
            SQLDB.get().saveAsync(result, null);
        } else {
            result.setGenericValue(value);
            SQLDB.get().saveAsync(result, null);
        }
        return result;
    }

    Integer getInt() {
        if (getSettingValue() == null) return null;
        try {
            return Integer.parseInt(getSettingValue());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    Boolean getBoolean() {
        Integer i = getInt();
        if (i == null) return null;
        return i != 0;
    }

    ChatColor getChatColor() {
        String v = getSettingValue();
        if (v == null) return null;
        try {
            return ChatColor.valueOf(v.toUpperCase());
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    TextColor getTextColor() {
        String v = getSettingValue();
        if (v == null) return null;
        try {
            return NamedTextColor.NAMES.value(v);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    UUID getSettingUuid() {
        String v = getSettingValue();
        if (v == null) return null;
        try {
            return UUID.fromString(v);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }

    public static String getString(UUID uuid, String channel, String key, String dfl) {
        SQLSetting setting;
        if (uuid != null) {
            setting = find(uuid, channel, key);
            if (setting != null && setting.getSettingValue() != null) return setting.getSettingValue();
        }
        setting = find(null, channel, key);
        if (setting != null && setting.getSettingValue() != null) return setting.getSettingValue();
        return dfl;
    }

    public static int getInt(UUID uuid, String channel, String key, int dfl) {
        SQLSetting setting;
        if (uuid != null) {
            setting = find(uuid, channel, key);
            if (setting != null && setting.getInt() != null) return setting.getInt();
        }
        setting = find(null, channel, key);
        if (setting != null && setting.getInt() != null) return setting.getInt();
        return dfl;
    }

    public static boolean getBoolean(UUID uuid, String channel, String key, boolean dfl) {
        SQLSetting setting;
        if (uuid != null) {
            setting = find(uuid, channel, key);
            if (setting != null && setting.getBoolean() != null) return setting.getBoolean();
        }
        setting = find(null, channel, key);
        if (setting != null && setting.getBoolean() != null) return setting.getBoolean();
        return dfl;
    }

    public static ChatColor getChatColor(UUID uuid, String channel, String key, ChatColor dfl) {
        SQLSetting setting;
        if (uuid != null) {
            setting = find(uuid, channel, key);
            if (setting != null && setting.getChatColor() != null) return setting.getChatColor();
        }
        setting = find(null, channel, key);
        if (setting != null && setting.getChatColor() != null) return setting.getChatColor();
        return dfl;
    }

    public static TextColor getTextColor(UUID uuid, String channel, String key, TextColor dfl) {
        SQLSetting setting;
        if (uuid != null) {
            setting = find(uuid, channel, key);
            if (setting != null) {
                TextColor textColor = setting.getTextColor();
                if (textColor != null) return textColor;
            }
        }
        setting = find(null, channel, key);
        if (setting != null) {
            TextColor textColor = setting.getTextColor();
            if (textColor != null) return textColor;
        }
        return dfl;
    }

    public static UUID getUuid(UUID uuid, String channel, String key, UUID dfl) {
        SQLSetting setting;
        if (uuid != null) {
            setting = find(uuid, channel, key);
            if (setting != null && setting.getSettingUuid() != null) return setting.getSettingUuid();
        }
        setting = find(null, channel, key);
        if (setting != null && setting.getSettingUuid() != null) return setting.getSettingUuid();
        return dfl;
    }

    protected static void loadDefaultSettingsAsync() {
        SQLDB.get().find(SQLSetting.class).where().isNull("uuid").findListAsync(list -> {
                defaultSettings = makeSettings(list);
            });
    }

    protected static void loadSettingsAsync(UUID uuid) {
        SQLDB.get().find(SQLSetting.class).where().eq("uuid", uuid).findListAsync(list -> {
                Settings old = CACHE.get(uuid);
                Settings settings = makeSettings(list);
                CACHE.put(uuid, settings);
                if (old != null) {
                    settings.map.putAll(old.map);
                }
            });
    }
}
