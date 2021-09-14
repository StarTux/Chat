package com.winthier.chat;

import java.util.UUID;
import lombok.Value;
import org.bukkit.entity.Player;

@Value
public final class Chatter {
    public static final Chatter CONSOLE = new Chatter(new UUID(0L, 0L), "#console");
    public final UUID uuid;
    public final String name;

    public boolean isConsole() {
        return this == CONSOLE || CONSOLE.uuid.equals(uuid);
    }

    public static Chatter of(Player player) {
        return new Chatter(player.getUniqueId(), player.getName());
    }
}
