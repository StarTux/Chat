package com.winthier.chat;

import cn.nukkit.command.CommandExecutor;

public abstract class AbstractChatCommand implements CommandExecutor {
    /* TabExecutor removed for Nukkit port */
    // @Override
    // public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    //     if (args.length == 0) return null;
    //     String arg = args[args.length - 1];
    //     return ChatPlugin.getInstance().completePlayerName(arg);
    // }
}
