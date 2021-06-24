package com.winthier.chat;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * Abstract command to complete player names from all servers.
 */
public abstract class AbstractChatCommand implements TabExecutor {
    /**
     * Overriders should call super.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        return ChatPlugin.getInstance().completeChatArg(sender, arg);
    }
}
