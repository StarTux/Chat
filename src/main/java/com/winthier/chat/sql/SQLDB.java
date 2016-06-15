package com.winthier.chat.sql;

import com.avaje.ebean.EbeanServer;
import com.winthier.chat.ChatPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.persistence.PersistenceException;

public class SQLDB {
    static EbeanServer get() {
        return ChatPlugin.getInstance().getDatabase();
    }

    public static boolean probe() {
        try {
            for (Class<?> clazz : getDatabaseClasses()) {
                get().find(clazz).findRowCount();
            }
        } catch (PersistenceException ex) {
            return false;
        }
        return true;
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
