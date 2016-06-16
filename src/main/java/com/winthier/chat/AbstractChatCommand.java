package com.winthier.chat;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public abstract class AbstractChatCommand implements TabExecutor {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        System.out.println("complete abstract " + alias + " " + args.length);
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        return ChatPlugin.getInstance().completePlayerName(arg);
    }
}
