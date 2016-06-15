package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.MessageFilter;
import com.winthier.chat.sql.SQLSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter @Setter
public abstract class AbstractChannel implements Channel {
    String title, key, tag;
    List<String> aliases = new ArrayList<>();
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
    public boolean hasPermission(Player player) {
        return player.hasPermission("chat.channel." + getKey());
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
        return SQLSetting.getBoolean(player, getKey(), "Joined", false);
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

    public List<Option> getOptions() {
        return new ArrayList<>();
    }
}
