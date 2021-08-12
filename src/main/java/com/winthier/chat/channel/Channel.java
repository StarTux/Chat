package com.winthier.chat.channel;

import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface Channel extends CommandResponder {
    String getTitle();
    String getKey();
    String getTag();
    String getDescription();
    String getAlias();
    int getRange();
    void setFocusChannel(UUID player);
    void setFocusChannel(Player player);
    void joinChannel(UUID player);
    void leaveChannel(UUID player);
    boolean isJoined(UUID player);
    boolean canJoin(UUID player);
    boolean canTalk(UUID player);
    void handleMessage(Message message);
    List<Option> getOptions();
    void playerDidUseChat(PlayerCommandContext context);
    void announce(Component msg);
    void announceLocal(Component msg);
    List<Chatter> getOnlineMembers();
    List<Player> getLocalMembers();
    Component makeOutput(Message message, Player player);
    Component makeExampleOutput(Player player);
    boolean playSoundCue(Player player);
}
