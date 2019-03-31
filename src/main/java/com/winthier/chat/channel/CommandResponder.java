package com.winthier.chat.channel;

import cn.nukkit.Player;
import java.util.List;

public interface CommandResponder {
    List<String> getAliases();
    void playerDidUseCommand(PlayerCommandContext context);
    void consoleDidUseCommand(String msg);
    boolean hasPermission(Player player);
    Channel getChannel();
}
