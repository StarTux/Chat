package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
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
import org.bukkit.Bukkit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PublicChannel extends AbstractChannel {
    @Override
    public void playerDidUseCommand(PlayerCommandContext c) {
        if (range < 0) return;
        if (!isJoined(c.player.getUniqueId())) {
            joinChannel(c.player.getUniqueId());
        }
        if (c.message == null || c.message.isEmpty()) {
            setFocusChannel(c.player.getUniqueId());
            Msg.info(c.player, "Now focusing %s&r", getTitle());
        } else {
            SQLLog.store(c.player, this, null, c.message);
            Message message = makeMessage(c.player, c.message);
            if (range <= 0) ChatPlugin.getInstance().didCreateMessage(message);
            handleMessage(message);
        }        
    }

    public void handleMessage(Message message) {
        fillMessage(message);
        Location senderLocation;
        if (range > 0) {
            Player sender = Bukkit.getServer().getPlayer(message.sender);
            if (sender == null) return;
            senderLocation = sender.getLocation();
        } else {
            senderLocation = null;
        }
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (SQLIgnore.doesIgnore(player.getUniqueId(), message.sender)) continue;
            if (range > 0) {
                if (!senderLocation.getWorld().equals(player.getWorld())) continue;
                if (senderLocation.distanceSquared(player.getLocation()) > range*range) continue;
            }
            send(message, player);
        }
    }

    void send(Message message, Player player) {
        UUID uuid = player.getUniqueId();
        String key = getKey();
        List<Object> json = new ArrayList<>();
        ChatColor channelColor = SQLSetting.getChatColor(uuid, key, "ChannelColor", ChatColor.WHITE);
        ChatColor textColor = SQLSetting.getChatColor(uuid, key, "TextColor", ChatColor.WHITE);
        ChatColor senderColor = SQLSetting.getChatColor(uuid, key, "SenderColor", ChatColor.WHITE);
        ChatColor bracketColor = SQLSetting.getChatColor(uuid, key, "BracketColor", ChatColor.WHITE);
        boolean tagPlayerName = SQLSetting.getBoolean(uuid, key, "TagPlayerName", false);
        BracketType bracketType = BracketType.of(SQLSetting.getString(uuid, key, "BracketType", "angle"));
        json.add("");
        // Channel Tag
        if (SQLSetting.getBoolean(uuid, key, "ShowChannelTag", true)) {
            json.add(channelTag(channelColor, bracketColor, bracketType));
        }
        // Server Tag
        if (message.senderServer != null && SQLSetting.getBoolean(uuid, key, "ShowServer", true)) {
            json.add(serverTag(message, channelColor, bracketColor, bracketType));
        }
        // Player Title
        if (SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", true)) {
            json.add(senderTitleTag(message, bracketColor, bracketType));
        }
        // Player Name
        json.add(senderTag(message, senderColor, bracketColor, bracketType, tagPlayerName));
        json.add(Msg.button(bracketColor, ":", null, null));
        json.add(" ");
        // Message
        appendMessage(json, message, textColor, SQLSetting.getBoolean(uuid, key, "LanguageFilter", true));
        Msg.raw(player, json);
    }
}
