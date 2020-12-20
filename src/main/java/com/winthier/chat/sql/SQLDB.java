package com.winthier.chat.sql;

import com.winthier.chat.ChatPlugin;
import com.winthier.sql.SQLDatabase;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class SQLDB {
    private SQLDB() { }

    static SQLDatabase get() {
        return ChatPlugin.getInstance().getDb();
    }

    public static List<Class<?>> getDatabaseClasses() {
        return Arrays.asList(
            SQLSetting.class,
            SQLIgnore.class,
            SQLLog.class,
            SQLPattern.class,
            SQLChannel.class
            );
    }

    public static void reload() {
        SQLSetting.clearCache();
        SQLIgnore.clearCache();
        SQLPattern.clearCache();
    }

    public static void clear(UUID uuid) {
        SQLSetting.clearCache(uuid);
        SQLIgnore.clearCache(uuid);
    }

    public static void clear() {
        SQLPattern.clearCache();
        SQLSetting.clearCache();
        SQLIgnore.clearCache();
    }

    /**
     * Load player database in the cache, hopefully async!
     */
    public static void load(UUID uuid) {
        SQLSetting.loadSettingsAsync(uuid);
        SQLIgnore.loadIgnoresAsync(uuid);
    }

    public static void load() {
        SQLPattern.load();
        SQLSetting.loadDefaultSettingsAsync();
    }
}
