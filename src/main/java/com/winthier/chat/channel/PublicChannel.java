package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PublicChannel extends AbstractChannel {
    @Override
    public void playerDidUseCommand(PlayerCommandContext c) {
        if (!hasPermission(c.player)) return;
        if (!isJoined(c.player.getUniqueId())) {
            joinChannel(c.player.getUniqueId());
        }
        if (c.message == null || c.message.isEmpty()) {
            setFocusChannel(c.player.getUniqueId());
            Msg.info(c.player, "Now focusing %s&r", getTitle());
        } else {
            SQLLog.store(c.player, this, c.message);
            Message message = new Message();
            message.channel = getKey();
            message.sender = c.player.getUniqueId();
            message.senderName = c.player.getName();
            message.message = c.message;
            fillMessage(message);
            ChatPlugin.getInstance().didCreateMessage(message);
            handleMessage(message);
        }        
    }

    public void handleMessage(Message message) {
        fillMessage(message);
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            send(message, player);
        }
    }


    public List<Option> getOptions() {
        return Arrays.asList(
            Option.colorOption("ChannelColor", "Channel Color", "white"),
            Option.colorOption("TextColor", "Text Color", "white"),
            Option.colorOption("SenderColor", "Sender Color", "white"),
            Option.colorOption("BracketColor", "Bracket Color", "white"),
            Option.bracketOption("BracketType", "Brackets", "angle"),
            Option.booleanOption("ShowChannelTag", "Show Channel Tag", "1"),
            Option.booleanOption("ShowPlayerTitle", "Show Player Title", "1"),
            Option.booleanOption("LanguageFilter", "Language Filter", "1")
            );
    }

    void send(Message message, Player player) {
        UUID uuid = player.getUniqueId();
        String key = getKey();
        List<Object> json = new ArrayList<>();
        ChatColor channelColor = SQLSetting.getChatColor(uuid, key, "ChannelColor", ChatColor.WHITE);
        ChatColor textColor = SQLSetting.getChatColor(uuid, key, "TextColor", ChatColor.WHITE);
        ChatColor senderColor = SQLSetting.getChatColor(uuid, key, "SenderColor", ChatColor.WHITE);
        ChatColor bracketColor = SQLSetting.getChatColor(uuid, key, "BracketColor", ChatColor.WHITE);
        BracketType bracketType = BracketType.of(SQLSetting.getString(uuid, key, "BracketType", "angle"));
        if (SQLSetting.getBoolean(uuid, key, "ShowChannelTag", true)) {
            json.add(bracketColor+bracketType.opening+channelColor+getTag()+bracketColor+bracketType.closing);
        }
        if (message.senderTitle != null && SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", true)) {
            json.add(bracketColor+bracketType.opening+Msg.format(message.senderTitle)+bracketColor+bracketType.closing);
        }
        json.add(senderColor+message.senderName);
        json.add(bracketColor+":");
        json.add(" ");
        List<Object> sourceList;
        if (SQLSetting.getBoolean(uuid, key, "LanguageFilter", true)) {
            sourceList = message.languageFilterJson;
        } else {
            sourceList = message.json;
        }
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
        Msg.raw(player, json);
    }
}
