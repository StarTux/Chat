package com.winthier.chat.channel;

import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface Channel extends CommandResponder {
    String getTitle();
    String getKey();
    String getTag();
    String getDescription();
    String getAlias();
    int getRange();
    void setFocusChannel(UUID player);
    void joinChannel(UUID player);
    void leaveChannel(UUID player);
    boolean isJoined(UUID player);
    boolean hasPermission(UUID player);
    void handleMessage(Message message);
    List<Option> getOptions();
    void playerDidUseChat(PlayerCommandContext context);
    void announce(String sender, Object msg);
    List<Chatter> getOnlineMembers();
    List<Player> getLocalMembers();
    void exampleOutput(Player player);
}
