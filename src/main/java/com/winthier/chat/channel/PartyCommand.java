package com.winthier.chat.channel;

import cn.nukkit.Player;
import com.winthier.chat.ChatPlugin;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
public class PartyCommand implements CommandResponder {
    private final List<String> aliases = Arrays.<String>asList("party");

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        ChatPlugin.getInstance().getPartyChannel().partyCommand(context);
    }

    @Override
    public void consoleDidUseCommand(String msg) {
    }

    @Override
    public boolean hasPermission(Player player) {
        return ChatPlugin.getInstance().getPartyChannel().hasPermission(player);
    }

    @Override
    public Channel getChannel() {
        return ChatPlugin.getInstance().getPartyChannel();
    }
}
