package com.winthier.chat.playercache;

import com.winthier.playercache.PlayerCache;
import java.util.UUID;

public final class PlayerCacheHandler {
    public UUID uuidForName(String name) {
        return PlayerCache.uuidForName(name);
    }

    public String nameForUuid(UUID uuid) {
        return PlayerCache.nameForUuid(uuid);
    }
}
