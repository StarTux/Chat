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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class PartyChannel extends AbstractChannel {
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
        if (getRange() < 0) return;
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
            if (message.shouldCancel) return;
            message.targetName = partyName;
            ChatPlugin.getInstance().didCreateMessage(this, message);
            handleMessage(message);
        }
    }

    @Override
    public void consoleDidUseCommand(String msg) {
        String[] arr = msg.split("\\s+", 2);
        if (arr.length != 2) return;
        String target = arr[0];
        msg = arr[1];
        Message message = makeMessage(null, msg);
        message.senderName = "Console";
        message.targetName = target;
        SQLLog.store("Console", this, target, msg);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    public void handleMessage(Message message) {
        fillMessage(message);
        if (message.shouldCancel && message.sender != null) return;
        String log = String.format("[%s][%s][%s]%s: %s",
                                   getTag(), message.targetName,
                                   message.senderServer, message.senderName,
                                   message.message);
        ChatPlugin.getInstance().getLogger().info(log);
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (shouldIgnore(player.getUniqueId(), message)) continue;
            if (!message.targetName.equals(getPartyName(player.getUniqueId()))) continue;
            send(message, player);
        }
    }

    @Override
    public void exampleOutput(Player player) {
        Message message = makeMessage(player, "Hello World");
        message.targetName = "Example";
        message.prefix = (Msg.format(" &7&oPreview&r "));
        send(message, player);
    }

    void send(Message message, Player player) {
        UUID uuid = player.getUniqueId();
        String key = getKey();
        String partyName = message.targetName;
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
        // Party Name Tag
        String text = ""
            + bracketColor + bracketType.opening
            + channelColor + partyName
            + bracketColor + bracketType.closing;
        json.add(Msg.button(channelColor, text, null, "/p "));
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

    String getPartyName(UUID uuid) {
        return SQLSetting.getString(uuid, getKey(), "Party", null);
    }

    void setPartyName(UUID uuid, String name) {
        SQLSetting.set(uuid, getKey(), "Party", name);
    }

    void partyCommand(PlayerCommandContext context) {
        final String[] args = context.message == null
            ? new String[0]
            : context.message.split("\\s+");
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
        json.add(Msg.format("&oParty &a%s&r:", partyName));
        ChatColor senderColor = SQLSetting
            .getChatColor(player.getUniqueId(), getKey(), "SenderColor", ChatColor.WHITE);
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
