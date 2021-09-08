package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@Getter @RequiredArgsConstructor
public final class PartyCommand implements CommandResponder {
    private final ChatPlugin plugin;
    private final List<String> aliases = Arrays.<String>asList("party");

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        plugin.getPartyChannel().partyCommand(context);
    }

    @Override
    public void consoleDidUseCommand(String msg) {
    }

    @Override
    public boolean hasPermission(Player player) {
        return plugin.getPartyChannel().hasPermission(player);
    }

    @Override
    public Channel getChannel() {
        return plugin.getPartyChannel();
    }

    @Override
    public void registerCommand() { }

    @Override
    public void unregisterCommand() { }
}
