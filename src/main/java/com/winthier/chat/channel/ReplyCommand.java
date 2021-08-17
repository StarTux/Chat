package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@Getter @RequiredArgsConstructor
public final class ReplyCommand implements CommandResponder {
    private final ChatPlugin plugin;
    private final List<String> aliases = Arrays.<String>asList("reply", "r");

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        plugin.getPrivateChannel().reply(context);
    }

    @Override
    public void consoleDidUseCommand(String msg) {
    }

    @Override
    public boolean hasPermission(Player player) {
        return plugin.getPrivateChannel().hasPermission(player);
    }

    @Override
    public Channel getChannel() {
        return plugin.getPrivateChannel();
    }
}
