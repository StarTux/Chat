package com.winthier.chat.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
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
    @NotNull UUID player;
    @NotNull UUID ignoree;

    private SQLIgnore(UUID player, UUID ignoree) {
	setPlayer(player);
        setIgnoree(ignoree);
    }

    public static Ignores findIgnores(UUID player) {
	Ignores result = cache.get(player);
	if (result == null || result.tooOld()) {
            result = new Ignores();
	    for (SQLIgnore ignore: SQLDB.get().find(SQLIgnore.class).where().eq("player", player).findList()) {
                result.map.put(ignore.getPlayer(), ignore);
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
                SQLDB.get().save(entry);
                ignores.map.put(ignoree, entry);
                return true;
            }
        } else {
            SQLIgnore entry = ignores.map.remove(ignoree);
            if (entry == null) {
                return false;
            } else {
                SQLDB.get().delete(entry);
                return true;
            }
        }
    }

    static void clearCache(UUID uuid) {
        cache.remove(uuid);
    }
}
