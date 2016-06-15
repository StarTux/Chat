package com.winthier.chat.channel;

import com.winthier.chat.Message;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public interface Channel extends CommandResponder {
    String getTitle();
    String getKey();
    String getTag();
    void setFocusChannel(UUID player);
    void joinChannel(UUID player);
    void leaveChannel(UUID player);
    boolean isJoined(UUID player);
    void handleMessage(Message message);
    List<Option> getOptions();
    boolean hasPermission(Player player);
}
