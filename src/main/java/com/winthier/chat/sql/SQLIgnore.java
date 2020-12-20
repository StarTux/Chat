package com.winthier.chat.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ignores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"player", "ignoree"}))
@Getter @Setter @NoArgsConstructor
public final class SQLIgnore {
    // Cache
    private static class Ignores {
        private final Map<UUID, SQLIgnore> map = new HashMap<>();
    }
    private static final Map<UUID, Ignores> CACHE = new HashMap<>();

    // Content
    @Id private Integer id;
    @Column(nullable = false) private UUID player;
    @Column(nullable = false) private UUID ignoree;

    private SQLIgnore(final UUID player, final UUID ignoree) {
        setPlayer(player);
        setIgnoree(ignoree);
    }

    public static Ignores findIgnores(final UUID uuid) {
        return CACHE.computeIfAbsent(uuid, u -> new Ignores());
    }

    public static boolean ignore(UUID player, UUID ignoree, boolean shouldIgnore) {
        Ignores ignores = findIgnores(player);
        if (shouldIgnore) {
            if (ignores.map.containsKey(ignoree)) {
                return false;
            } else {
                SQLIgnore entry = new SQLIgnore(player, ignoree);
                ignores.map.put(ignoree, entry);
                SQLDB.get().saveAsync(entry, null);
                return true;
            }
        } else {
            SQLIgnore entry = ignores.map.remove(ignoree);
            if (entry == null) {
                return false;
            } else {
                SQLDB.get().deleteAsync(entry, null);
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
        CACHE.remove(uuid);
    }

    static void clearCache() {
        CACHE.clear();
    }

    public static void loadIgnoresAsync(UUID uuid) {
        SQLDB.get().find(SQLIgnore.class).where().eq("player", uuid).findListAsync(list -> {
                Ignores old = CACHE.get(uuid);
                Ignores ignores = new Ignores();
                for (SQLIgnore ignore : list) {
                    ignores.map.put(ignore.getIgnoree(), ignore);
                }
                CACHE.put(uuid, ignores);
                if (old != null) {
                    ignores.map.putAll(old.map);
                }
            });
    }
}
