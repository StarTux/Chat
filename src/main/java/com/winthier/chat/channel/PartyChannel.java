package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PartyChannel extends AbstractChannel {
    private final Component usage = TextComponent
        .ofChildren(Component.text("Usage", NamedTextColor.GRAY, TextDecoration.ITALIC),
                    Component.text("\n"),
                    Component.text("/party ", NamedTextColor.GREEN),
                    Component.text("<name>", NamedTextColor.GREEN, TextDecoration.ITALIC),
                    Component.text(" - ", NamedTextColor.DARK_GRAY),
                    Component.text("Join a party", NamedTextColor.WHITE),
                    Component.text("\n"),
                    Component.text("/party ", NamedTextColor.GREEN),
                    Component.text("quit", NamedTextColor.GREEN, TextDecoration.ITALIC),
                    Component.text(" - ", NamedTextColor.DARK_GRAY),
                    Component.text("Quit any party", NamedTextColor.WHITE),
                    Component.text("\n"),
                    Component.text("/p ", NamedTextColor.GREEN),
                    Component.text("<message>", NamedTextColor.GREEN, TextDecoration.ITALIC),
                    Component.text(" - ", NamedTextColor.DARK_GRAY),
                    Component.text("Send a message", NamedTextColor.WHITE),
                    Component.text("\n"),
                    Component.text("/p ", NamedTextColor.GREEN),
                    Component.text(" - ", NamedTextColor.DARK_GRAY),
                    Component.text("Focus party chat", NamedTextColor.WHITE));

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
        Player player = c.getPlayer();
        String msg = c.getMessage();
        if (!isJoined(player.getUniqueId())) {
            joinChannel(player.getUniqueId());
        }
        String partyName = getPartyName(player.getUniqueId());
        if (partyName == null) {
            Msg.warn(player, Component.text("You're not in a party", NamedTextColor.RED));
            return;
        }
        if (msg == null || msg.isEmpty()) {
            setFocusChannel(player.getUniqueId());
            Msg.info(player, Component.text("Now focusing party " + partyName, NamedTextColor.WHITE));
            return;
        }
        SQLLog.store(player, this, partyName, msg);
        Message message = new Message().init(this).player(player, msg);
        message.setTargetName(partyName);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    public void consoleDidUseCommand(String msg) {
        String[] arr = msg.split("\\s+", 2);
        if (arr.length != 2) return;
        String target = arr[0];
        msg = arr[1];
        Message message = new Message().init(this).console(msg);
        SQLLog.store("Console", this, target, msg);
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    @Override
    public void handleMessage(Message message) {
        String log = String.format("[%s][%s][%s]%s: %s",
                                   getTag(), message.getTargetName(),
                                   message.getSenderServer(), message.getSenderName(),
                                   message.getMessage());
        ChatPlugin.getInstance().getLogger().info(log);
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (shouldIgnore(player.getUniqueId(), message)) continue;
            String playerParty = getPartyName(player.getUniqueId());
            if (!Objects.equals(message.getTargetName(), playerParty)) continue;
            send(message, player);
        }
    }

    @Override
    public Component makeExampleOutput(Player player) {
        Message message = new Message().init(this).player(player, "Hello World");
        message.setTargetName("Example");
        return makeOutput(message, player);
    }

    @Override
    public Component makeOutput(Message message, Player player) {
        UUID uuid = player.getUniqueId();
        String key = getKey();
        String partyName = message.getTargetName();
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
            cb = cb.append(makeChannelTag(channelColor, bracketColor, bracketType));
        }
        cb = cb.append(makePartyTag(partyName, bracketType, bracketColor, channelColor));
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

    Component makePartyTag(String partyName, BracketType bracketType, TextColor bracketColor, TextColor channelColor) {
        Component tooltip = Component.text()
            .append(Component.text("Party ", NamedTextColor.GRAY)).append(Component.text(partyName, NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false)
            .build();
        return Component.text()
            .append(Component.text(bracketType.opening, bracketColor))
            .append(Component.text(partyName, channelColor))
            .append(Component.text(bracketType.closing, bracketColor))
            .clickEvent(ClickEvent.suggestCommand("/p "))
            .hoverEvent(HoverEvent.showText(tooltip))
            .build();
    }

    String getPartyName(UUID uuid) {
        return SQLSetting.getString(uuid, getKey(), "Party", null);
    }

    void setPartyName(UUID uuid, String name) {
        SQLSetting.set(uuid, getKey(), "Party", name);
    }

    void partyCommand(PlayerCommandContext context) {
        String msg = context.getMessage();
        Player player = context.getPlayer();
        final String[] args = msg == null ? new String[0] : msg.split("\\s+");
        UUID uuid = player.getUniqueId();
        String partyName = getPartyName(uuid);
        if (args.length == 0) {
            if (partyName == null) {
                usage(player);
            } else {
                listPlayers(player, partyName);
            }
        } else if (args.length == 1) {
            String arg = args[0];
            if ("quit".equalsIgnoreCase(arg) || "q".equalsIgnoreCase(arg)) {
                if (partyName == null) {
                    Msg.warn(player, Component.text("You're not in a party.", NamedTextColor.RED));
                } else {
                    setPartyName(uuid, null);
                    Msg.info(player, Component.text("You left party " + partyName, NamedTextColor.YELLOW));
                }
            } else {
                setPartyName(uuid, arg);
                Msg.info(player, Component.text("You joined party " + arg, NamedTextColor.WHITE));
            }
        } else {
            usage(player);
        }
    }

    void usage(Player player) {
        Msg.info(player, usage);
    }

    void listPlayers(Player player, String partyName) {
        TextColor senderColor = SQLSetting.getTextColor(player.getUniqueId(), getKey(), "SenderColor", NamedTextColor.WHITE);
        TextColor channelColor = SQLSetting.getTextColor(player.getUniqueId(), getKey(), "ChannelColor", NamedTextColor.WHITE);
        List<Component> chatters = new ArrayList<>();
        for (Chatter chatter: ChatPlugin.getInstance().getOnlinePlayers()) {
            String otherPartyName = getPartyName(chatter.getUuid());
            if (Objects.equals(partyName, otherPartyName)) {
                chatters.add(Component.text(chatter.getName(), senderColor));
            }
        }
        TextComponent.Builder cb = Component.text();
        cb = cb.append(Component.text("Party " + partyName + "(" + chatters.size() + "): ", channelColor));
        cb = cb.append(Component.join(Component.text(", ", NamedTextColor.DARK_GRAY), chatters));
        player.sendMessage(cb.build());
    }
}
