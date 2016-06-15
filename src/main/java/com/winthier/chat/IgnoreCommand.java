package com.winthier.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IgnoreCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        if (args.length == 0) {
            ChatPlugin.getInstance().getChatCommand().listIgnores(player);
        } else if (args.length == 1) {
            ChatPlugin.getInstance().getChatCommand().toggleIgnore(player, args[0]);
        } else {
            return false;
        }
        return true;
    }
}
