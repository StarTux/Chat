package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import java.util.List;
import org.bukkit.entity.Player;

public interface CommandResponder {
    ChatPlugin getPlugin();
    List<String> getAliases();
    void playerDidUseCommand(PlayerCommandContext context);
    void consoleDidUseCommand(String msg);
    boolean hasPermission(Player player);
    Channel getChannel();
}
