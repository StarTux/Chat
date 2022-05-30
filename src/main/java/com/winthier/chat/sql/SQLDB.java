package com.winthier.chat.sql;

import com.winthier.chat.ChatPlugin;
import com.winthier.sql.SQLDatabase;
import com.winthier.sql.SQLRow;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SQLDB {
    private SQLDB() { }

    static SQLDatabase get() {
        return ChatPlugin.getInstance().getDb();
    }

    public static List<Class<? extends SQLRow>> getDatabaseClasses() {
        return List.of(SQLSetting.class,
                       SQLIgnore.class,
                       SQLLog.class,
                       SQLChannel.class,
                       SQLBadWord.class);
    }

    public static void reload() {
        clear();
        load();
    }

    public static void clear(UUID uuid) {
        SQLSetting.clearCache(uuid);
        SQLIgnore.clearCache(uuid);
    }

    public static void clear() {
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
        SQLSetting.loadDefaultSettingsAsync();
        SQLBadWord.loadBadWords();
        SQLIgnore.loadIgnores();
        for (Player player : Bukkit.getOnlinePlayers()) {
            load(player.getUniqueId());
        }
    }
}
