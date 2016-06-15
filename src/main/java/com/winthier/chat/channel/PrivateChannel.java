package com.winthier.chat.channel;

import com.winthier.chat.Message;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class PrivateChannel extends AbstractChannel {
    String title = "Private";
    List<String> aliases = new ArrayList<>();

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission("chat.pm");
    }

    public void handleMessage(Message message) {
        
    }
}
