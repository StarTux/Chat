package com.winthier.chat;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.util.Msg;
import java.util.UUID;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class JoinLeaveCommand extends AbstractChatCommand {
    private final boolean join;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (player == null) return false;
        if (args.length != 1) return false;
        String arg = args[0];
        Channel channel = ChatPlugin.getInstance().findChannel(arg);
        if (channel == null || !channel.hasPermission(player)) {
            Msg.warn(player, "Channel not found: %s", arg);
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (this.join) {
            if (channel.isJoined(uuid)) {
                Msg.warn(player, "You already joined %s.", channel.getTitle());
            } else {
                channel.joinChannel(uuid);
                Msg.info(player, "Joined %s.", channel.getTitle());
            }
        } else {
            if (!channel.isJoined(uuid)) {
                Msg.warn(player, "You did not join %s.", channel.getTitle());
            } else {
                channel.leaveChannel(uuid);
                Msg.info(player, "Left %s.", channel.getTitle());
            }
        }
        return true;
    }
}
