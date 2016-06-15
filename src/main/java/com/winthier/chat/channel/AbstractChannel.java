package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.MessageFilter;
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
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Getter @Setter
public abstract class AbstractChannel implements Channel {
    String title, key, tag, description;
    List<String> aliases = new ArrayList<>();
    int range = 0;
    public static enum BracketType {
        PAREN("(", ")"),
        BRACKETS("[", "]"),
        CURLY("{", "}"),
        ANGLE("<", ">")
        ;
        final String opening;
        final String closing;
        BracketType(String opening, String closing) {
            this.opening = opening;
            this.closing = closing;
        }
        static BracketType of(String val) {
            if (val == null) return BracketType.BRACKETS;
            try {
                return valueOf(val.toUpperCase());
            } catch (IllegalArgumentException ile) {
                return BracketType.BRACKETS;
            }
        }
    }

    @Override
    public String getAlias() {
        return getAliases().get(0);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission("chat.channel." + getKey()) || player.hasPermission("chat.channel.*");
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
    public List<Option> getOptions() {
        return Arrays.asList(
            Option.booleanOption("Joined", "Listening", "1"),
            Option.colorOption("ChannelColor", "Channel Color", "white"),
            Option.colorOption("TextColor", "Text Color", "white"),
            Option.colorOption("SenderColor", "Player Color", "white"),
            Option.colorOption("BracketColor", "Bracket Color", "white"),
            Option.bracketOption("BracketType", "Brackets", "angle"),
            Option.booleanOption("ShowChannelTag", "Show Channel Tag", "1"),
            Option.booleanOption("ShowPlayerTitle", "Show Player Title", "1"),
            Option.booleanOption("ShowServer", "Show Server", "1"),
            Option.booleanOption("LanguageFilter", "Language Filter", "1")
            );
    }

    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        playerDidUseCommand(context);
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
        }
    }

    Message makeMessage(Player player, String text) {
        Message message = new Message();
        message.channel = getKey();
        message.sender = player.getUniqueId();
        message.senderName = player.getName();
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
        return Msg.button(serverColor,
                          bracketColor + bracketType.opening + serverColor + message.senderServerDisplayName + bracketColor + bracketType.closing,
                          null,
                          null);
    }

    Object senderTitleTag(Message message, ChatColor bracketColor, BracketType bracketType) {
        return Msg.button(
            bracketColor,
            bracketColor + bracketType.opening + Msg.format(message.senderTitle) + bracketColor + bracketType.closing,
            Msg.format(message.senderTitle) +
            (message.senderTitleDescription != null ? "\n&5&o" + message.senderTitleDescription : ""),
            null);
    }

    Object senderTag(Message message, ChatColor senderColor, ChatColor bracketColor, BracketType bracketType, boolean useBrackets) {
        return Msg.button(senderColor,
                          useBrackets ? bracketColor + bracketType.opening + senderColor + message.senderName + bracketColor + bracketType.closing : message.senderName,
                          message.senderName +
                          "\n&5&oTitle&r " + Msg.format(message.senderTitle) +
                          "\n&5&oServer&r " + message.senderServerDisplayName,
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
}
