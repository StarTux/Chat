package com.winthier.chat.sql;

import com.winthier.chat.ChatPlugin;
import com.winthier.sql.SQLDatabase;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.persistence.PersistenceException;

public class SQLDB {
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
        SQLSetting.cache.clear();
        SQLSetting.defaultSettings = null;
        SQLIgnore.cache.clear();
        SQLPattern.cache = null;
    }

    public static void clear(UUID uuid) {
        SQLSetting.clearCache(uuid);
        SQLIgnore.clearCache(uuid);
    }
}
