package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PublicChannel extends AbstractChannel {
    @Override
    public boolean canJoin(UUID player) {
        String perm = "chat.channel." + key;
        return ChatPlugin.getInstance().hasPermission(player, perm)
            || ChatPlugin.getInstance().hasPermission(player, perm + ".join")
            || ChatPlugin.getInstance().hasPermission(player, "chat.channel.*");
    }

    @Override
    public boolean canTalk(UUID player) {
        String perm = "chat.channel." + key;
        return ChatPlugin.getInstance().hasPermission(player, perm)
            || ChatPlugin.getInstance().hasPermission(player, perm + ".talk")
            || ChatPlugin.getInstance().hasPermission(player, "chat.channel.*");
    }

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
        String log = "[" + getTag() + "]"
            + "[" + message.senderServer + "]"
            + (message.senderName != null
               ? message.senderName + ": "
               : "")
            + message.message;
        ChatPlugin.getInstance().getLogger().info(log);
        long maxDistance;
        if (range > 0 && message.location != null) {
            maxDistance = (long) range * range;
        } else {
            maxDistance = 0L;
        }
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!canJoin(player.getUniqueId())) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (shouldIgnore(player.getUniqueId(), message)) continue;
            if (maxDistance > 0L) {
                if (!message.location.getWorld().equals(player.getWorld())) continue;
                double dist = message.location.distanceSquared(player.getLocation());
                if ((long) dist > maxDistance) continue;
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
        final ChatColor white = ChatColor.WHITE;
        ChatColor channelColor = SQLSetting.getChatColor(uuid, key, "ChannelColor", white);
        ChatColor textColor = SQLSetting.getChatColor(uuid, key, "TextColor", white);
        ChatColor senderColor = SQLSetting.getChatColor(uuid, key, "SenderColor", white);
        ChatColor bracketColor = SQLSetting.getChatColor(uuid, key, "BracketColor", white);
        boolean tagPlayerName = SQLSetting.getBoolean(uuid, key, "TagPlayerName", false);
        String tmp = SQLSetting.getString(uuid, key, "BracketType", null);
        BracketType bracketType = tmp != null
            ? BracketType.of(tmp)
            : BracketType.ANGLE;
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
        if (!tagPlayerName && message.senderName != null) {
            json.add(Msg.button(bracketColor, ":", null, null));
        }
        json.add(" ");
        // Message
        boolean languageFilter = SQLSetting.getBoolean(uuid, key, "LanguageFilter", true);
        appendMessage(json, message, textColor, languageFilter);
        Msg.raw(player, json);
        // Sound Cue
        playSoundCue(player);
    }
}
