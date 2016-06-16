package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PartyChannel extends AbstractChannel {
    @Override
    public void playerDidUseCommand(PlayerCommandContext c) {
        if (range < 0) return;
        if (!isJoined(c.player.getUniqueId())) {
            joinChannel(c.player.getUniqueId());
        }
        String partyName = getPartyName(c.player.getUniqueId());
        if (partyName == null) {
            Msg.warn(c.player, "Join a party first.");
            return;
        }
        if (c.message == null || c.message.isEmpty()) {
            setFocusChannel(c.player.getUniqueId());
            Msg.info(c.player, "Now focusing party %s&r", partyName);
        } else {
            SQLLog.store(c.player, this, partyName, c.message);
            Message message = makeMessage(c.player, c.message);
            message.targetName = partyName;
            if (range <= 0) ChatPlugin.getInstance().didCreateMessage(message);
            handleMessage(message);
        }        
    }

    public void handleMessage(Message message) {
        fillMessage(message);
        ChatPlugin.getInstance().getLogger().info(String.format("[%s][%s][%s]%s: %s", getTag(), message.targetName, message.senderServer, message.senderName, message.message));
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (SQLIgnore.doesIgnore(player.getUniqueId(), message.sender)) continue;
            if (!message.targetName.equals(getPartyName(player.getUniqueId()))) continue;
            send(message, player);
        }
    }

    void send(Message message, Player player) {
        UUID uuid = player.getUniqueId();
        String key = getKey();
        String partyName = message.targetName;
        List<Object> json = new ArrayList<>();
        ChatColor channelColor = SQLSetting.getChatColor(uuid, key, "ChannelColor", ChatColor.WHITE);
        ChatColor textColor = SQLSetting.getChatColor(uuid, key, "TextColor", ChatColor.WHITE);
        ChatColor senderColor = SQLSetting.getChatColor(uuid, key, "SenderColor", ChatColor.WHITE);
        ChatColor bracketColor = SQLSetting.getChatColor(uuid, key, "BracketColor", ChatColor.WHITE);
        boolean tagPlayerName = SQLSetting.getBoolean(uuid, key, "TagPlayerName", false);
        BracketType bracketType = BracketType.of(SQLSetting.getString(uuid, key, "BracketType", "angle"));
        json.add("");
        // Channel Tag
        if (SQLSetting.getBoolean(uuid, key, "ShowChannelTag", false)) {
            json.add(channelTag(channelColor, bracketColor, bracketType));
        }
        // Party Name Tag
        json.add(Msg.button(channelColor,
                            bracketColor + bracketType.opening + channelColor + partyName + bracketColor + bracketType.closing,
                            null,
                            "/p "));
        // Server Tag
        if (message.senderServer != null && SQLSetting.getBoolean(uuid, key, "ShowServer", false)) {
            json.add(serverTag(message, channelColor, bracketColor, bracketType));
        }
        // Player Title
        if (SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", false)) {
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

    String getPartyName(UUID uuid) {
        return SQLSetting.getString(uuid, getKey(), "Party", null);
    }

    void setPartyName(UUID uuid, String name) {
        SQLSetting.set(uuid, getKey(), "Party", name);
    }

    void partyCommand(PlayerCommandContext context) {
        final String[] args = context.message == null ? new String[0] : context.message.split("\\s+");
        UUID uuid = context.player.getUniqueId();
        String partyName = getPartyName(uuid);
        if (args.length == 0) {
            if (partyName == null) {
                usage(context.player);
            } else {
                listPlayers(context.player, partyName);
            }
        } else if (args.length == 1) {
            String arg = args[0];
            if ("quit".equalsIgnoreCase(arg) || "q".equalsIgnoreCase(arg)) {
                if (partyName == null) {
                    Msg.warn(context.player, "You are not in a party.");
                } else {
                    setPartyName(uuid, null);
                    Msg.info(context.player, "Quit party %s.", partyName);
                }
            } else {
                setPartyName(uuid, arg);
                Msg.info(context.player, "Joined party %s.", arg);
            }
        } else {
            usage(context.player);
        }
    }

    void usage(Player player) {
        Msg.send(player, "&oUsage:&r &a/Party <&oName&a>&r|&aQuit");
    }

    void listPlayers(Player player, String partyName) {
        List<Object> json = new ArrayList<>();
        json.add(Msg.format("&oParty &a%s&r: ", partyName));
        ChatColor senderColor = SQLSetting.getChatColor(player.getUniqueId(), key, "SenderColor", ChatColor.WHITE);
        for (Chatter chatter: ChatPlugin.getInstance().getOnlinePlayers()) {
            String otherPartyName = getPartyName(chatter.getUuid());
            if (otherPartyName != null && otherPartyName.equals(partyName)) {
                json.add(" ");
                json.add(Msg.button(senderColor, chatter.getName(), null, null));
            }
        }
        Msg.raw(player, json);
    }
}
