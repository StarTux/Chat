package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import java.util.Arrays;
import java.util.List;
import lombok.Value;
import org.bukkit.entity.Player;

@Value
public class ReplyCommand implements CommandResponder {
    List<String> aliases = Arrays.<String>asList("reply", "r");

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        ChatPlugin.getInstance().getPrivateChannel().reply(context);
    }

    @Override
    public boolean hasPermission(Player player) {
        return ChatPlugin.getInstance().getPrivateChannel().hasPermission(player);
    }
}
