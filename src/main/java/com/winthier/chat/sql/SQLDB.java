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
            SQLChannel.class
            );
    }

    public static void reload() {
        SQLSetting.clearCache();
        SQLIgnore.clearCache();
    }

    public static void clear(UUID uuid) {
        SQLSetting.clearCache(uuid);
        SQLIgnore.clearCache(uuid);
    }
}
