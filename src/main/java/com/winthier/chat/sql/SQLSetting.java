package com.winthier.chat.sql;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import net.md_5.bungee.api.ChatColor;

@Entity
@Table(name = "settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"uuid", "channel", "setting_key"}))
@Getter @Setter @NoArgsConstructor
public final class SQLSetting {
    @Value
    private static final class Key {
        private final UUID uuid;
        private final String channel;
        private final String key;
    }

    @Getter @RequiredArgsConstructor
    public static final class Settings {
        private final Map<Key, SQLSetting> map = new HashMap<>();
        private final long created = System.currentTimeMillis();
        private boolean tooOld() {
            return !neverTooOld && System.currentTimeMillis() - created > 1000 * 60;
        }
        private boolean neverTooOld = false;
    }

    // Cache
    private static final Map<UUID, Settings> CACHE = new HashMap<>();
    private static Settings defaultSettings = null;

    // Content
    @Id private Integer id;
    private UUID uuid;
    @Column(nullable = true, length = 16) private String channel;
    @Column(nullable = false, length = 32) private String settingKey;
    @Column(nullable = true, length = 64) private String settingValue;
    @Version private Date version;

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

    public static Settings getDefaultSettings() {
        boolean refresh = false;
        if (defaultSettings == null) {
            refresh = true;
            defaultSettings = new Settings();
            defaultSettings.neverTooOld = true;
        } else if (defaultSettings.tooOld()) {
            refresh = true;
            defaultSettings.neverTooOld = true;
        }
        if (refresh) {
            SQLDB.get().find(SQLSetting.class).where().isNull("uuid").findListAsync(list -> {
                    defaultSettings = makeSettings(list);
                });
        }
        return defaultSettings;
    }

    protected static Settings findSettings(final UUID uuid) {
        Settings result = CACHE.get(uuid);
        boolean refresh = false;
        if (result == null) {
            refresh = true;
            result = new Settings();
            result.neverTooOld = true;
            CACHE.put(uuid, result);
        } else if (result.tooOld()) {
            refresh = true;
            result.neverTooOld = true;
        }
        if (refresh) {
            SQLDB.get().find(SQLSetting.class).where().eq("uuid", uuid).findListAsync(list -> {
                    Settings settings = makeSettings(list);
                    CACHE.put(uuid, settings);
                });
        }
        return result;
    }

    public static SQLSetting find(UUID uuid, String channel, String key) {
        if (uuid == null) return getDefaultSettings().map.get(new Key(uuid, channel, key));
        return findSettings(uuid).map.get(new Key(uuid, channel, key));
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
                getDefaultSettings().map.put(new Key(uuid, channel, key), result);
            } else {
                findSettings(uuid).map.put(new Key(uuid, channel, key), result);
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
}
