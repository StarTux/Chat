package com.winthier.chat.channel;

import java.util.List;

public interface CommandResponder {
    List<String> getAliases();
    void playerDidUseCommand(PlayerCommandContext context);
}
