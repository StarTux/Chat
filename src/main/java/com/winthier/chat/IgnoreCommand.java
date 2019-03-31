package com.winthier.chat;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;

public class IgnoreCommand extends AbstractChatCommand {
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
