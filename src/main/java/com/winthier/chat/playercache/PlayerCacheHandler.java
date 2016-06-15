package com.winthier.chat.playercache;

import com.winthier.playercache.PlayerCache;
import java.util.UUID;

public class PlayerCacheHandler {
    public static UUID uuidForName(String name) {
        return PlayerCache.uuidForName(name);
    }

    public static String nameForUuid(UUID uuid) {
        return PlayerCache.nameForUuid(uuid);
    }
}
