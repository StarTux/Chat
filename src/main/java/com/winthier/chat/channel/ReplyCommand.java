package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public final class ReplyCommand implements CommandResponder {
    private final List<String> aliases = Arrays.<String>asList("reply", "r");

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        ChatPlugin.getInstance().getPrivateChannel().reply(context);
    }

    @Override
    public void consoleDidUseCommand(String msg) {
    }

    @Override
    public boolean hasPermission(Player player) {
        return ChatPlugin.getInstance().getPrivateChannel().hasPermission(player);
    }

    @Override
    public Channel getChannel() {
        return ChatPlugin.getInstance().getPrivateChannel();
    }
}
