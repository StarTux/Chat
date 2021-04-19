package com.winthier.chat.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter @AllArgsConstructor
public final class PlayerCommandContext {
    private final Player player;
    private final String label;
    private final String message;
}
