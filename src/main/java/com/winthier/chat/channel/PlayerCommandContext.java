package com.winthier.chat.channel;

import java.util.UUID;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public class PlayerCommandContext {
    Player player;
    String label;
    String message;
}
