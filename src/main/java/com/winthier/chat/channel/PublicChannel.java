package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
    public void playerDidUseCommand(PlayerCommandContext context) {
        Player player = context.getPlayer();
        String msg = context.getMessage();
        if (SQLSetting.getBoolean(null, getKey(), "MutePlayers", false)) return;
        if (!isJoined(player.getUniqueId())) {
            joinChannel(player.getUniqueId());
        }
        if (msg == null || msg.isEmpty()) {
            setFocusChannel(player.getUniqueId());
            Msg.info(player, Component.text("Now focusing " + getTitle(), NamedTextColor.WHITE));
            return;
        }
        SQLLog.store(player, this, null, msg);
        Message message = new Message().init(this).player(player, msg);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    public void consoleDidUseCommand(String msg) {
        Message message = new Message().init(this).console(msg);
        SQLLog.store("Console", this, null, msg);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    public void handleMessage(Message message) {
        String log = String.format("[%s][%s]%s: %s", getTag(), message.getSenderServer(),
                                   message.getSenderName(), message.getMessage());
        ChatPlugin.getInstance().getLogger().info(log);
        Location location = message.getLocation();
        final boolean ranged = range > 0 && location != null;
        long maxDistance = ranged ? (long) range * range : 0L;
        Player sender = message.getSender() != null ? Bukkit.getPlayer(message.getSender()) : null;
        int seenCount = 0;
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!canJoin(player.getUniqueId())) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (shouldIgnore(player.getUniqueId(), message)) continue;
            if (ranged) {
                if (!location.getWorld().equals(player.getWorld())) continue;
                double dist = location.distanceSquared(player.getLocation());
                if ((long) dist > maxDistance) continue;
            }
            send(message, player);
            // Being in GM3 only doesn't count you if you also have
            // the chat.invisible permission. Otherwise, legitimate
            // spectators wouldn't count.
            if (ranged && sender != null && !sender.equals(player) && sender.canSee(player)) {
                boolean invisible = !sender.canSee(player)
                    || (player.getGameMode() == GameMode.SPECTATOR && player.hasPermission("chat.invisible"));
                if (!invisible) seenCount += 1;
            }
        }
        if (!message.isPassive() && ranged && sender != null && seenCount == 0) {
            sender.sendMessage(TextComponent.ofChildren(Component.text("[Chat] ", NamedTextColor.WHITE),
                                                        Component.text("Nobody is in range to hear you", NamedTextColor.YELLOW)));
        }
    }

    @Override
    public Component makeExampleOutput(Player player) {
        Message message = new Message().init(this).player(player, "Hello World");
        return makeOutput(message, player);
    }

    @Override
    public Component makeOutput(Message message, Player player) {
        UUID uuid = player.getUniqueId();
        String key = getKey();
        final TextColor white = NamedTextColor.WHITE;
        TextColor channelColor = SQLSetting.getTextColor(uuid, key, "ChannelColor", white);
        TextColor textColor = SQLSetting.getTextColor(uuid, key, "TextColor", white);
        TextColor senderColor = SQLSetting.getTextColor(uuid, key, "SenderColor", white);
        TextColor bracketColor = SQLSetting.getTextColor(uuid, key, "BracketColor", white);
        boolean tagPlayerName = SQLSetting.getBoolean(uuid, key, "TagPlayerName", false);
        boolean languageFilter = SQLSetting.getBoolean(uuid, null, "LanguageFilter", true);
        boolean showServer = SQLSetting.getBoolean(uuid, key, "ShowServer", false);
        boolean showPlayerTitle = SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", true);
        boolean showChannelTag = SQLSetting.getBoolean(uuid, key, "ShowChannelTag", true);
        String tmp = SQLSetting.getString(uuid, key, "BracketType", null);
        BracketType bracketType = tmp != null ? BracketType.of(tmp) : BracketType.ANGLE;
        TextComponent.Builder cb = Component.text();
        if (showChannelTag) {
            cb.append(makeChannelTag(channelColor, bracketColor, bracketType));
        }
        if (!message.isHideSenderTags()) {
            // Server Tag
            if (showServer) {
                cb = cb.append(makeServerTag(message, channelColor, bracketColor, bracketType));
            }
            // Player Title
            if (showPlayerTitle) {
                cb = cb.append(makeTitleTag(message, bracketColor, bracketType));
            }
            // Player Name
            Component senderTag = makeSenderTag(message, senderColor, bracketColor, bracketType, tagPlayerName);
            if (!Objects.equals(senderTag, Component.empty())) {
                cb = cb.append(senderTag);
                if (!tagPlayerName) {
                    cb = cb.append(Component.text(":", bracketColor));
                }
            }
        }
        cb.append(Component.text(" "));
        cb = cb.append(makeMessageComponent(message, player, textColor, languageFilter));
        return cb.build();
    }
}
