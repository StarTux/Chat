package com.winthier.chat.channel;

import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public final class PlayerCommandContext {
    final Player player;
    final String label;
    final String message;
}
