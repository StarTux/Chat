package com.winthier.chat;

import com.winthier.chat.channel.Channel;
import com.winthier.chat.util.Msg;
import java.util.UUID;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@AllArgsConstructor
public final class JoinLeaveCommand extends AbstractChatCommand {
    private final boolean join;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player == null) return false;
        if (args.length != 1) return false;
        String arg = args[0];
        Channel channel = ChatPlugin.getInstance().findChannel(arg);
        if (channel == null || !channel.hasPermission(player)) {
            Msg.warn(player, Component.text("Channel not found: " + arg, NamedTextColor.RED));
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (this.join) {
            if (channel.isJoined(uuid)) {
                Msg.warn(player, Component.text("You already joined " + channel.getTitle(), NamedTextColor.RED));
            } else {
                channel.joinChannel(uuid);
                Msg.info(player, Component.text("Joined " + channel.getTitle(), NamedTextColor.WHITE));
            }
        } else {
            if (!channel.isJoined(uuid)) {
                Msg.warn(player, Component.text("You did not join " + channel.getTitle(), NamedTextColor.RED));
            } else {
                channel.leaveChannel(uuid);
                Msg.info(player, Component.text("You left " + channel.getTitle(), NamedTextColor.YELLOW));
            }
        }
        return true;
    }
}
