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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
        String special = message.getSpecial();
        if (special == null) {
            String log = String.format("[%s][%s]%s->%s: %s",
                                       getTag(), message.getSenderServer(),
                                       message.getSenderName(), message.getTargetName(),
                                       message.getMessage());
            ChatPlugin.getInstance().getLogger().info(log);
        }
        Player player = Bukkit.getServer().getPlayer(message.getTarget());
        if (player == null) return;
        if (special == null) {
            if (!hasPermission(player)) return;
            if (!isJoined(player.getUniqueId())) return;
        }
        if (special != null || !shouldIgnore(player.getUniqueId(), message)) {
            send(message, player);
        }
        if (special == null) {
            SQLSetting.set(player.getUniqueId(), getKey(), "ReplyName", message.getSenderName());
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
        Message message = new Message().init(this).console(msg);
        message.setTarget(target.getUuid());
        message.setTargetName(target.getName());
        SQLLog.store("Console", this, target.getName(), msg);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    protected void send(Message message, Player player) {
        Component component = makeOutput(message, player);
        player.sendMessage(component);
        if (message.getSpecial() == null) {
            playSoundCue(player);
        }
    }

    @Override
    public Component makeExampleOutput(Player player) {
        Message message = new Message().init(this).player(player, "Hello World");
        message.setTarget(player.getUniqueId());
        message.setTargetName(player.getName());
        return makeOutput(message, player);
    }

    @Override
    public Component makeOutput(Message message, Player target) {
        boolean ack = message.getSpecial() != null;
        UUID uuid = target.getUniqueId();
        String key = getKey();
        List<Object> json = new ArrayList<>();
        final TextColor white = NamedTextColor.WHITE;
        TextColor channelColor = SQLSetting.getTextColor(uuid, key, "ChannelColor", white);
        TextColor textColor = SQLSetting.getTextColor(uuid, key, "TextColor", white);
        TextColor senderColor = SQLSetting.getTextColor(uuid, key, "SenderColor", white);
        TextColor bracketColor = SQLSetting.getTextColor(uuid, key, "BracketColor", white);
        boolean tagPlayerName = SQLSetting.getBoolean(uuid, key, "TagPlayerName", false);
        boolean languageFilter = SQLSetting.getBoolean(uuid, null, "LanguageFilter", true);
        boolean showServer = SQLSetting.getBoolean(uuid, key, "ShowServer", false);
        boolean showPlayerTitle = SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", false);
        boolean showChannelTag = SQLSetting.getBoolean(uuid, key, "ShowChannelTag", false);
        String tmp = SQLSetting.getString(uuid, key, "BracketType", null);
        BracketType bracketType = tmp != null ? BracketType.of(tmp) : BracketType.ANGLE;
        TextComponent.Builder cb = Component.text();
        if (showChannelTag) {
            cb.append(makeChannelTag(channelColor, bracketColor, bracketType));
        }
        if (showServer) {
            cb.append(makeServerTag(message, channelColor, bracketColor, bracketType));
        }
        // From/To
        cb.append(Component.text((ack ? "To " : "From "), senderColor));
        if (showPlayerTitle) {
            cb.append(makeTitleTag(message, bracketColor, bracketType));
        }
        cb.append(makeSenderTag(message, senderColor, bracketColor, bracketType, tagPlayerName));
        if (!tagPlayerName) {
            cb.append(Component.text(":", bracketColor));
        }
        cb.append(Component.text(" "));
        Component messageComponent = makeMessageComponent(message, target, textColor, bracketType, bracketColor, languageFilter);
        cb.append(messageComponent);
        message.message(messageComponent);
        return cb.build();
    }

    void sendAck(Message old, Player player) {
        Message message = new Message().init(this).player(player);
        message.setSpecial("Ack");
        message.setTarget(old.getSender());
        message.setTargetName(old.getSenderName());
        message.setMessageJson(old.getMessageJson());
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
        Player player = context.getPlayer();
        String msg = context.getMessage();
        UUID uuid = player.getUniqueId();
        if (msg == null) return;
        final String[] arr = msg.split("\\s+", 2);
        if (arr.length == 0) {
            return;
        }
        String targetName = arr[0];
        Chatter target = ChatPlugin.getInstance().getOnlinePlayer(targetName);
        if (target == null) {
            Msg.warn(player, Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }
        if (target.getUuid().equals(uuid)) {
            Msg.warn(player, Component.text("You cannot message yourself", NamedTextColor.RED));
            return;
        }
        if (arr.length == 1) {
            setFocusChannel(uuid);
            SQLSetting.set(uuid, getKey(), "FocusName", target.getName());
            Msg.info(player, Component.text("Now focusing " + target.getName(), NamedTextColor.WHITE));
        } else if (arr.length == 2) {
            talk(player, target, arr[1]);
        }
    }

    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        Player player = context.getPlayer();
        String msg = context.getMessage();
        String focusName = SQLSetting.getString(player.getUniqueId(), getKey(), "FocusName", null);
        if (focusName == null) return;
        Chatter target = ChatPlugin.getInstance().getOnlinePlayer(focusName);
        if (target == null) {
            Msg.warn(player, Component.text("Player not found: " + focusName, NamedTextColor.RED));
            return;
        }
        talk(player, target, msg);
    }

    void reply(PlayerCommandContext context) {
        Player player = context.getPlayer();
        String msg = context.getMessage();
        String replyName = SQLSetting.getString(player.getUniqueId(), getKey(), "ReplyName", null);
        if (replyName == null) return;
        Chatter target = ChatPlugin.getInstance().getOnlinePlayer(replyName);
        if (target == null) {
            Msg.warn(player, Component.text("Player not found: " + replyName, NamedTextColor.RED));
            return;
        }
        if (msg == null || msg.isEmpty()) {
            UUID uuid = player.getUniqueId();
            setFocusChannel(uuid);
            SQLSetting.set(uuid, getKey(), "FocusName", target.getName());
            Msg.info(player, Component.text("Now focusing " + target.getName(), NamedTextColor.WHITE));
        } else {
            talk(player, target, msg);
        }
    }

    void talk(Player player, Chatter target, String msg) {
        SQLLog.store(player, this, target.getUuid().toString(), msg);
        Message message = new Message().init(this).player(player, msg);
        message.setTarget(target.getUuid());
        message.setTargetName(target.getName());
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }
}
