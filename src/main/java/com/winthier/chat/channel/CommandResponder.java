package com.winthier.chat.channel;

import java.util.List;
import org.bukkit.entity.Player;

public interface CommandResponder {
    List<String> getAliases();
    void playerDidUseCommand(PlayerCommandContext context);
    boolean hasPermission(Player player);
}
