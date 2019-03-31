package com.winthier.chat.channel;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.TextFormat;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        ChatPlugin.getInstance().getLogger().info(String.format("[%s][%s]%s: %s", getTag(), message.senderServer, message.senderName, message.message));
        for (Player player: Server.getInstance().getOnlinePlayers().values()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (shouldIgnore(player.getUniqueId(), message)) continue;
            int range = getRange();
            if (range != 0 && message.location != null) {
                if (!message.location.getLevel().equals(player.getLevel())) continue;
                if (message.location.distanceSquared(player.getLocation()) > range * range) continue;
            }
            send(message, player);
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
        TextFormat channelColor = SQLSetting.getChatColor(uuid, key, "ChannelColor", TextFormat.WHITE);
        TextFormat textColor = SQLSetting.getChatColor(uuid, key, "TextColor", TextFormat.WHITE);
        TextFormat senderColor = SQLSetting.getChatColor(uuid, key, "SenderColor", TextFormat.WHITE);
        TextFormat bracketColor = SQLSetting.getChatColor(uuid, key, "BracketColor", TextFormat.WHITE);
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
        appendMessage(json, message, textColor, SQLSetting.getBoolean(uuid, key, "LanguageFilter", true));
        // .raw(player, json);
        player.sendMessage(channelColor + bracketType.opening + this.getTag() + bracketType.closing + senderColor + message.senderName + ": " + textColor + message.message);
        // Sound Cue
        playSoundCue(player);
    }
}
