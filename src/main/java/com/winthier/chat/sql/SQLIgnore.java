package com.winthier.chat.sql;

import com.winthier.chat.ChatPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "ignores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player", "ignoree"}))
@Getter
@Setter
@NoArgsConstructor
public class SQLIgnore {
    // Cache
    static class Ignores {
        final Map<UUID, SQLIgnore> map = new HashMap<>();
        final long created = System.currentTimeMillis();
        boolean tooOld() {
            return System.currentTimeMillis() - created > 1000*60;
        }
    }
    final static Map<UUID, Ignores> cache = new HashMap<>();
    // Content
    @Id Integer id;
    @Column(nullable = false) UUID player;
    @Column(nullable = false) UUID ignoree;

    private SQLIgnore(UUID player, UUID ignoree) {
	setPlayer(player);
        setIgnoree(ignoree);
    }

    public static Ignores findIgnores(UUID player) {
	Ignores result = cache.get(player);
	if (result == null || result.tooOld()) {
            result = new Ignores();
	    for (SQLIgnore ignore: SQLDB.get().find(SQLIgnore.class).where().eq("player", player).findList()) {
                result.map.put(ignore.getIgnoree(), ignore);
            }
            cache.put(player, result);
	}
	return result;
    }

    public static boolean ignore(UUID player, UUID ignoree, boolean shouldIgnore) {
        Ignores ignores = findIgnores(player);
        if (shouldIgnore) {
            if (ignores.map.containsKey(ignoree)) {
                return false;
            } else {
                SQLIgnore entry = new SQLIgnore(player, ignoree);
                ignores.map.put(ignoree, entry);
                try {
                    SQLDB.get().save(entry);
                } catch (PersistenceException pe) {
                    clearCache(player);
                    ChatPlugin.getInstance().getLogger().warning(String.format("SQLIgnore: Persistence Exception while storing %s,%s. Clearing cache.", player, ignoree));
                }
                return true;
            }
        } else {
            SQLIgnore entry = ignores.map.remove(ignoree);
            if (entry == null) {
                return false;
            } else {
                try {
                    SQLDB.get().delete(entry);
                } catch (PersistenceException pe) {
                    clearCache(player);
                    ChatPlugin.getInstance().getLogger().warning(String.format("SQLIgnore: Persistence Exception while deleting %s,%s. Clearing cache.", player, ignoree));
                }
                return true;
            }
        }
    }

    public static List<UUID> listIgnores(UUID player) {
        List<UUID> result = new ArrayList<>();
        for (SQLIgnore ign: findIgnores(player).map.values()) {
            result.add(ign.getIgnoree());
        }
        return result;
    }

    public static boolean doesIgnore(UUID player, UUID ignoree) {
        return findIgnores(player).map.get(ignoree) != null;
    }

    static void clearCache(UUID uuid) {
        cache.remove(uuid);
    }
}
