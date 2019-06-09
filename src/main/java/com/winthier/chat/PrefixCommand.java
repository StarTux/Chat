package com.winthier.chat;

import com.winthier.chat.sql.SQLSetting;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class PrefixCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String alias,
                             String[] args) {
        if (!(sender instanceof Player)) return false;
        if (args.length == 0) return false;
        final Player player = (Player) sender;
        String arg = Arrays.stream(args).collect(Collectors.joining(" "));
        if (arg.equals("reset")) {
            SQLSetting.set(player.getUniqueId(),
                           null,
                           "prefix",
                           null);
            player.sendMessage(ChatColor.GOLD + "Prefix reset.");
            return true;
        }
        SQLSetting.set(player.getUniqueId(),
                       null,
                       "prefix",
                       arg);
        String format = ChatColor
            .translateAlternateColorCodes('&', arg);
        player.sendMessage(ChatColor.GOLD + "Prefix set to " + format);
        return true;
    }
}
