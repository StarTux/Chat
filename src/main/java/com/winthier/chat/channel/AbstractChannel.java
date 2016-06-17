package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.MessageFilter;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Getter @Setter
public abstract class AbstractChannel implements Channel {
    String title, key, tag, description;
    List<String> aliases = new ArrayList<>();
    int range = 0;

    @Override
    public String getAlias() {
        return getAliases().get(0);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission("chat.channel." + getKey()) || player.hasPermission("chat.channel.*");
    }

    @Override
    public boolean hasPermission(UUID player) {
        return ChatPlugin.getInstance().hasPermission(player, "chat.channel." + getKey()) || ChatPlugin.getInstance().hasPermission(player, "chat.channel.*");
    }

    @Override
    public void setFocusChannel(UUID player) {
        SQLSetting.set(player, null, "FocusChannel", getKey());
    }

    @Override
    public void joinChannel(UUID player) {
        SQLSetting.set(player, getKey(), "Joined", true);
    }

    @Override
    public void leaveChannel(UUID player) {
        SQLSetting.set(player, getKey(), "Joined", false);
    }
    
    @Override
    public boolean isJoined(UUID player) {
        return SQLSetting.getBoolean(player, getKey(), "Joined", true);
    }

    @Override
    public Channel getChannel() {
        return this;
    }

    @Override
    public List<Option> getOptions() {
        return Arrays.asList(
            Option.booleanOption("Joined", "Listening", "Join or leave the channel.", "1"),
            Option.colorOption("ChannelColor", "Channel Color", "Main channel color", "white"),
            Option.colorOption("TextColor", "Text Color", "Color of chat messages", "white"),
            Option.colorOption("SenderColor", "Player Color", "Color of player names", "white"),
            Option.colorOption("BracketColor", "Bracket Color", "Color of brackets and puncutation", "white"),

            Option.soundOption("SoundCueChat", "Chat Cue", "Sound played when you receive a message", "off"),
            Option.intOption("SoundCueChatVolume", "Chat Cue Volume", "Sound played when you receive a message", "10", 1, 10),

            Option.soundOption("SoundCueName", "Name Cue", "Sound played when your named is mentioned in chat", "off"),
            Option.intOption("SoundCueNameVolume", "Name Cue Volume", "Sound played when your name is mentioned in chat", "10", 1, 10),

            Option.bracketOption("BracketType", "Brackets", "Appearance of brackets", "angle"),
            Option.booleanOption("ShowChannelTag", "Show Channel Tag", "Show the channel tag at the beginning of every message", "0"),
            Option.booleanOption("ShowPlayerTitle", "Show Player Title", "Show a player's current title in every message", "1"),
            Option.booleanOption("ShowServer", "Show Server", "Show a player's server in every message", "0"),
            Option.booleanOption("LanguageFilter", "Language Filter", "Filter out foul language", "1")
            );
    }

    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        playerDidUseCommand(context);
    }

    @Override
    public void announce(String sender, Object msg) {
        Message message;
        if (msg instanceof String) {
            message = makeMessage(null, (String)msg);
        } else {
            List<Object> json;
            if (msg instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> tmpson = (List<Object>)msg;
                json = tmpson;
            } else {
                json = new ArrayList<>();
                json.add(msg);
            }
            String str = Msg.jsonToString(msg);
            message = makeMessage(null, str);
            message.json = json;
            message.languageFilterJson = json;
        }
        message.senderName = sender;
        message.special = "announcement";
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    void fillMessage(Message message) {
        if (message.senderTitle == null) {
            ChatPlugin.getInstance().loadTitle(message);
        }
        if (message.json == null || message.languageFilterJson == null) {
            MessageFilter filter = new MessageFilter(message.sender, message.message);
            filter.process();
            message.json = filter.getJson();
            message.languageFilterJson = filter.getLanguageFilterJson();
            message.languageFilterMessage = filter.toString();
        }
    }

    Message makeMessage(Player player, String text) {
        Message message = new Message();
        message.channel = getKey();
        if (player != null) {
            message.sender = player.getUniqueId();
            message.senderName = player.getName();
            message.location = player.getLocation();
        }
        message.senderServer = ChatPlugin.getInstance().getServerName();
        message.senderServerDisplayName = ChatPlugin.getInstance().getServerDisplayName();
        message.message = text;
        fillMessage(message);
        return message;
    }

    Object channelTag(ChatColor channelColor, ChatColor bracketColor, BracketType bracketType) {
        return Msg.button(channelColor,
                          bracketColor + bracketType.opening + channelColor + getTag() + bracketColor + bracketType.closing,
                          getTitle() + "\n&5&o" + getDescription(),
                          "/" + getKey() + " ");
    }

    Object serverTag(Message message, ChatColor serverColor, ChatColor bracketColor, BracketType bracketType) {
        String name;
        if (message.senderServerDisplayName != null) {
            name = message.senderServerDisplayName;
        } else if (message.senderServer != null) {
            name = message.senderServer;
        } else {
            return "";
        }
        return Msg.button(serverColor,
                          bracketColor + bracketType.opening + serverColor + name + bracketColor + bracketType.closing,
                          null,
                          null);
    }

    Object senderTitleTag(Message message, ChatColor bracketColor, BracketType bracketType) {
        if (message.senderTitle == null) return "";
        return Msg.button(
            bracketColor,
            bracketColor + bracketType.opening + Msg.format(message.senderTitle) + bracketColor + bracketType.closing,
            Msg.format(message.senderTitle) +
            (message.senderTitleDescription != null ? "\n&5&o" + message.senderTitleDescription : ""),
            null);
    }

    Object senderTag(Message message, ChatColor senderColor, ChatColor bracketColor, BracketType bracketType, boolean useBrackets) {
        if (message.sender == null) {
            return Msg.button(senderColor, useBrackets ? bracketColor + bracketType.opening + senderColor + message.senderName + bracketColor + bracketType.closing : message.senderName, null, null);
        }
        return Msg.button(senderColor,
                          useBrackets ? bracketColor + bracketType.opening + senderColor + message.senderName + bracketColor + bracketType.closing : message.senderName,
                          message.senderName +
                          (message.senderTitle != null ? "\n&5&oTitle&r " + Msg.format(message.senderTitle) : "") +
                          (message.senderServerDisplayName != null ? "\n&5&oServer&r " + message.senderServerDisplayName : "") +
                          "\n&5&oChannel&r " + getTitle(),
                          "/tell " + message.senderName + " ");
    }

    void appendMessage(List<Object> json, Message message, ChatColor textColor, boolean languageFilter) {
        List<Object> sourceList = languageFilter ? message.languageFilterJson : message.json;
        for (Object o: sourceList) {
            if (o instanceof Map) {
                @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>)o;
                map = new HashMap<>(map);
                map.put("color", textColor.name().toLowerCase());
                json.add(map);
            } else {
                json.add(o);
            }
        }
    }

    static boolean shouldIgnore(UUID player, Message message) {
        if (message.sender != null && SQLIgnore.doesIgnore(player, message.sender)) return true;
        return false;
    }


    public List<Chatter> getOnlineMembers() {
        List<Chatter> result = new ArrayList<>();
        for (Chatter chatter: ChatPlugin.getInstance().getOnlinePlayers()) {
            if (!hasPermission(chatter.getUuid())) continue;
            if (!isJoined(chatter.getUuid())) continue;
            result.add(chatter);
        }
        return result;
    }

    public List<Player> getLocalMembers() {
        List<Player> result = new ArrayList<>();
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            result.add(player);
        }
        return result;
    }

    public boolean playSoundCue(Player player, String skey) {
        SoundCue soundCue = SoundCue.of(SQLSetting.getString(player.getUniqueId(), getKey(), "SoundCue"+skey, "off"));
        if (soundCue == null) return false;
        int volume = SQLSetting.getInt(player.getUniqueId(), getKey(), "SoundCue"+skey+"Volume", 10);
        float vol = (float)volume / 10.0f;
        player.playSound(player.getEyeLocation(), soundCue.sound, vol, 1.0f);
        return true;
    }
}
