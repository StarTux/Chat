package com.winthier.chat.channel;

import cn.nukkit.Player;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class PlayerCommandContext {
    final Player player;
    final String label;
    final String message;
}
