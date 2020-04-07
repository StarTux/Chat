package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@Getter
public final class PrivateChannel extends AbstractChannel {
    @Override
    public boolean canJoin(UUID player) {
        return ChatPlugin.getInstance().hasPermission(player, "chat.pm");
    }

    @Override
    public boolean canTalk(UUID player) {
        return ChatPlugin.getInstance().hasPermission(player, "chat.pm");
    }

    public void handleMessage(Message message) {
        if (message.special == null) {
            String log = String.format("[%s][%s]%s->%s: %s",
                                       getTag(), message.senderServer,
                                       message.senderName, message.targetName,
                                       message.message);
            ChatPlugin.getInstance().getLogger().info(log);
        }
        Player player = Bukkit.getServer().getPlayer(message.target);
        if (player == null) return;
        if (message.special == null) {
            if (!hasPermission(player)) return;
            if (!isJoined(player.getUniqueId())) return;
        }
        fillMessage(message);
        if (message.shouldCancel && message.sender != null) return;
        if (message.special != null || !shouldIgnore(player.getUniqueId(), message)) {
            send(message, player);
        }
        if (message.special == null) {
            sendAck(message, player);
        }
    }

    @Override
    public void consoleDidUseCommand(String msg) {
        final String[] arr = msg.split("\\s+", 2);
        if (arr.length != 2) {
            return;
        }
        String targetName = arr[0];
        Chatter target = ChatPlugin.getInstance().getOnlinePlayer(targetName);
        if (target == null) {
            ChatPlugin.getInstance().getLogger().warning("Player not found: " + targetName + ".");
            return;
        }
        msg = arr[1];
        Message message = makeMessage(null, msg);
        message.senderName = "Console";
        message.target = target.getUuid();
        message.targetName = target.getName();
        SQLLog.store("Console", this, target.getName(), msg);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    public void exampleOutput(Player player) {
        Message message = makeMessage(player, "Hello World");
        message.target = player.getUniqueId();
        message.targetName = player.getName();
        message.prefix = (Msg.format(" &7&oPreview&r "));
        send(message, player);
    }

    void send(Message message, Player player) {
        boolean ack = message.special != null;
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
        // Server Name
        if (message.senderServer != null && SQLSetting.getBoolean(uuid, key, "ShowServer", false)) {
            json.add(serverTag(message, channelColor, bracketColor, bracketType));
        }
        // From/To
        json.add(Msg.button(senderColor, ack ? "To" : "From", null, null));
        json.add(" ");
        // Player Title
        if (SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", false)) {
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
        if (!ack) {
            playSoundCue(player);
        }
        // Reply
        SQLSetting.set(uuid, key, "ReplyName", message.senderName);
    }

    void sendAck(Message message, Player player) {
        message.special = "Ack";
        message.target = message.sender;
        message.targetName = message.senderName;
        message.sender = player.getUniqueId();
        message.senderName = player.getName();
        message.senderTitle = null;
        message.senderTitleDescription = null;
        message.senderServer = ChatPlugin.getInstance().getServerName();
        message.senderServerDisplayName = ChatPlugin.getInstance().getServerDisplayName();
        fillMessage(message);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        UUID uuid = context.player.getUniqueId();
        if (context.message == null) return;
        final String[] arr = context.message.split("\\s+", 2);
        if (arr.length == 0) {
            return;
        }
        String targetName = arr[0];
        Chatter target = ChatPlugin.getInstance().getOnlinePlayer(targetName);
        if (target == null) {
            Msg.warn(context.player, "Player not found: %s.", targetName);
            return;
        }
        if (target.getUuid().equals(uuid)) {
            Msg.warn(context.player, "You cannot message yourself.");
            return;
        }
        if (arr.length == 1) {
            setFocusChannel(uuid);
            SQLSetting.set(uuid, getKey(), "FocusName", target.getName());
            Msg.info(context.player, "Now focusing %s", target.getName());
        } else if (arr.length == 2) {
            talk(context.player, target, arr[1]);
        }
    }

    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        String focusName = SQLSetting
            .getString(context.player.getUniqueId(), getKey(), "FocusName", null);
        if (focusName == null) return;
        Chatter target = ChatPlugin.getInstance().getOnlinePlayer(focusName);
        if (target == null) {
            Msg.send(context.player, "&cPlayer not found: %s", focusName);
            return;
        }
        talk(context.player, target, context.message);
    }

    void reply(PlayerCommandContext context) {
        String replyName = SQLSetting
            .getString(context.player.getUniqueId(), getKey(), "ReplyName", null);
        if (replyName == null) return;
        Chatter target = ChatPlugin.getInstance().getOnlinePlayer(replyName);
        if (target == null) {
            Msg.send(context.player, "&cPlayer not found: %s", replyName);
            return;
        }
        if (context.message == null || context.message.isEmpty()) {
            UUID uuid = context.player.getUniqueId();
            setFocusChannel(uuid);
            SQLSetting.set(uuid, getKey(), "FocusName", target.getName());
            Msg.info(context.player, "Now focusing %s", target.getName());
        } else {
            talk(context.player, target, context.message);
        }
    }

    void talk(Player player, Chatter target, String msg) {
        SQLLog.store(player, this, target.getUuid().toString(), msg);
        Message message = makeMessage(player, msg);
        if (message.shouldCancel) return;
        message.target = target.getUuid();
        message.targetName = target.getName();
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }
}
