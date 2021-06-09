package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class PublicChannel extends AbstractChannel {
    @Override
    public void playerDidUseCommand(PlayerCommandContext c) {
        if (SQLSetting.getBoolean(null, getKey(), "MutePlayers", false)) return;
        if (!isJoined(c.player.getUniqueId())) {
            joinChannel(c.player.getUniqueId());
        }
        if (c.message == null || c.message.isEmpty()) {
            setFocusChannel(c.player.getUniqueId());
            Msg.info(c.player, "Now focusing %s&r.", getTitle());
        } else {
            SQLLog.store(c.player, this, null, c.message);
            Message message = makeMessage(c.player, c.message);
            if (message.shouldCancel) return;
            ChatPlugin.getInstance().didCreateMessage(this, message);
            handleMessage(message);
        }
    }

    @Override
    public void consoleDidUseCommand(String msg) {
        Message message = makeMessage(null, msg);
        message.senderName = "Console";
        SQLLog.store("Console", this, null, msg);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    public void handleMessage(Message message) {
        fillMessage(message);
        if (message.shouldCancel && message.sender != null) return;
        String log = String.format("[%s][%s]%s: %s",
                                   getTag(), message.senderServer, message.senderName + ChatColor.RESET, message.message);
        ChatPlugin.getInstance().getLogger().info(log);
        Set<UUID> delivered = new HashSet<>();
        int range = getRange();
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (ChatPlugin.getInstance().isChatPaused(player)) continue;
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (shouldIgnore(player.getUniqueId(), message)) continue;
            if (range != 0 && message.location != null) {
                if (!message.location.getWorld().equals(player.getWorld())) continue;
                if (message.location.distanceSquared(player.getLocation()) > range * range) continue;
            }
            send(message, player);
            delivered.add(player.getUniqueId());
        }
        // Chat Spy
        if (range != 0 && message.location != null) {
            for (UUID uuid : ChatPlugin.getInstance().getChatSpies()) {
                if (delivered.contains(uuid)) continue;
                final Player spy = Bukkit.getPlayer(uuid);
                if (spy == null) continue;
                if (!spy.hasPermission("chat.admin")) continue;
                spy.sendMessage(ChatColor.YELLOW
                                + "[Spy] "
                                + message.senderName
                                + ": " + message.message);
            }
        }
    }

    @Override
    public void exampleOutput(Player player) {
        Message message = makeMessage(player, "Hello World");
        message.prefix = (Msg.format(" &7&oPreview&r "));
        send(message, player);
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
        if (message.prefix != null) json.add(message.prefix);
        // Channel Tag
        if (SQLSetting.getBoolean(uuid, key, "ShowChannelTag", false)) {
            json.add(channelTag(channelColor, bracketColor, bracketType));
        }
        // Server Tag
        if (message.senderServer != null && SQLSetting.getBoolean(uuid, key, "ShowServer", false)) {
            json.add(serverTag(message, channelColor, bracketColor, bracketType));
        }
        // Player Title
        if (SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", true)) {
            json.add(senderTitleTag(message, bracketColor, bracketType));
        }
        // Player Name
        json.add(senderTag(message, senderColor, bracketColor, bracketType, tagPlayerName));
        if (!tagPlayerName && message.senderName != null) json.add(Msg.button(bracketColor, ":", null, null));
        json.add(" ");
        // Message
        appendMessage(json, message, textColor, player);
        Msg.raw(player, json);
        // Sound Cue
        playSoundCue(player);
    }
}
