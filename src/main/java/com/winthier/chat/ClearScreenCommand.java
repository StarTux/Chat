package com.winthier.chat;

import com.cavetale.core.command.AbstractCommand;
import static com.winthier.chat.ChatPlugin.plugin;

public final class ClearScreenCommand extends AbstractCommand<ChatPlugin> {
    protected ClearScreenCommand() {
        super(plugin(), "clearscreen");
    }

    protected void onEnable() {
        rootNode.description("Clear chat screen")
            .denyTabCompletion()
            .playerCaller(p -> plugin.getChatCommand().clearScreen(p));
    }
}
