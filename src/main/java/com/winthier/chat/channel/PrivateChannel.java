package com.winthier.chat.channel;

import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import com.winthier.perm.Perm;
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

public final class PrivateChannel extends AbstractChannel {
    @Getter private final String permission = "chat.pm";

    public PrivateChannel(final ChatPlugin plugin, final SQLChannel row) {
        super(plugin, row);
    }

    @Override
    public boolean canJoin(UUID player) {
        return Perm.has(player, permission);
    }

    @Override
    public boolean canTalk(UUID player) {
        return Perm.has(player, permission);
    }

    public void handleMessage(Message message) {
        String special = message.getSpecial();
        if (special == null) {
            String log = String.format("[%s][%s]%s->%s: %s",
                                       getTag(), message.getSenderServer(),
                                       message.getSenderName(), message.getTargetName(),
                                       message.getMessage());
            plugin.getLogger().info(log);
        }
        Player player = Bukkit.getPlayer(message.getTarget());
        if (player == null) return;
        if (special == null) {
            if (!hasPermission(player)) return;
            if (!isJoined(player.getUniqueId())) return;
        }
        if (special != null) {
            send(message, player);
        }
        if (special == null && !shouldIgnore(player.getUniqueId(), message)) {
            SQLSetting.set(player.getUniqueId(), getKey(), "ReplyName", message.getSenderName());
            send(message, player);
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
        Chatter target = plugin.getOnlinePlayer(targetName);
        if (target == null) {
            plugin.getLogger().warning("Player not found: " + targetName + ".");
            return;
        }
        msg = arr[1];
        Message message = new Message().init(this).console(msg);
        message.setTarget(target.uuid);
        message.setTargetName(target.name);
        SQLLog.store(Chatter.CONSOLE.name, this, target.name, msg);
        plugin.didCreateMessage(this, message);
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
        return cb.build();
    }

    private void sendAck(Message old, Player player) {
        Message ack = new Message().player(player).init(this).ack(old);
        plugin.didCreateMessage(this, ack);
        handleMessage(ack);
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
        Chatter target = plugin.getOnlinePlayer(targetName);
        if (target == null) {
            Msg.warn(player, Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return;
        }
        if (target.uuid.equals(uuid)) {
            Msg.warn(player, Component.text("You cannot message yourself", NamedTextColor.RED));
            return;
        }
        if (arr.length == 1) {
            setFocusChannel(player);
            SQLSetting.set(uuid, getKey(), "FocusName", target.name);
            Msg.info(player, Component.text("Now focusing " + target.name, NamedTextColor.WHITE));
            PluginPlayerEvent.Name.FOCUS_PRIVATE_CHAT.ultimate(plugin, player)
                .detail(Detail.TARGET, target.uuid)
                .detail(Detail.NAME, target.name)
                .call();
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
        Chatter target = plugin.getOnlinePlayer(focusName);
        if (target == null) {
            Msg.warn(player, Component.text("Player not found: " + focusName, NamedTextColor.RED));
            return;
        }
        talk(player, target, msg);
    }

    protected void reply(PlayerCommandContext context) {
        Player player = context.getPlayer();
        String msg = context.getMessage();
        String replyName = SQLSetting.getString(player.getUniqueId(), getKey(), "ReplyName", null);
        if (replyName == null) return;
        Chatter target = plugin.getOnlinePlayer(replyName);
        if (target == null) {
            Msg.warn(player, Component.text("Player not found: " + replyName, NamedTextColor.RED));
            return;
        }
        if (msg == null || msg.isEmpty()) {
            UUID uuid = player.getUniqueId();
            setFocusChannel(player);
            SQLSetting.set(uuid, getKey(), "FocusName", target.name);
            Msg.info(player, Component.text("Now focusing " + target.name, NamedTextColor.WHITE));
            PluginPlayerEvent.Name.FOCUS_PRIVATE_CHAT.ultimate(plugin, player)
                .detail(Detail.TARGET, target.uuid)
                .detail(Detail.NAME, target.name)
                .call();
        } else {
            talk(player, target, msg);
            PluginPlayerEvent.Name.USE_PRIVATE_CHAT_REPLY.ultimate(plugin, player)
                .detail(Detail.TARGET, target.uuid)
                .detail(Detail.NAME, target.name)
                .call();
        }
    }

    private void talk(Player player, Chatter target, String msg) {
        SQLLog.store(player, this, target.uuid.toString(), msg);
        Message message = new Message().init(this).player(player, msg);
        message.setTarget(target.uuid);
        message.setTargetName(target.name);
        plugin.didCreateMessage(this, message);
        handleMessage(message);
        if (target.isConsole()) {
            send(new Message().init(this).console().ack(message), player);
        }
        PluginPlayerEvent.Name.USE_PRIVATE_CHAT.ultimate(plugin, player)
            .detail(Detail.TARGET, target.uuid)
            .detail(Detail.NAME, target.name)
            .call();
    }
}
