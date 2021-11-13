package com.winthier.chat.channel;

import com.cavetale.core.event.player.PlayerTeamQuery.Team;
import com.cavetale.core.event.player.PlayerTeamQuery;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import com.winthier.perm.Perm;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TeamChannel extends AbstractChannel {
    @Getter private final String permission = "chat.channel.team";
    private final Team exampleTeam = new Team("chat:example", Component.text("Example"));
    private final Component usage = Component.join(JoinConfiguration.noSeparators(), new Component[] {
            Component.text("Usage", NamedTextColor.GRAY, TextDecoration.ITALIC),
            Component.text("/t ", NamedTextColor.GREEN),
            Component.text("<message>", NamedTextColor.GREEN, TextDecoration.ITALIC),
            Component.text(" - ", NamedTextColor.DARK_GRAY),
            Component.text("Send a message", NamedTextColor.WHITE),
            Component.newline(),
            Component.text("/t ", NamedTextColor.GREEN),
            Component.text(" - ", NamedTextColor.DARK_GRAY),
            Component.text("Focus team chat", NamedTextColor.WHITE),
        });

    public TeamChannel(final ChatPlugin plugin, final SQLChannel row) {
        super(plugin, row);
    }

    @Override
    public boolean canJoin(UUID player) {
        return Perm.has(player, permission) || Perm.has(player, "chat.channel.*");
    }

    @Override
    public boolean canTalk(UUID player) {
        return Perm.has(player, permission) || Perm.has(player, "chat.channel.*");
    }

    @Override
    public void playerDidUseCommand(PlayerCommandContext c) {
        if (getRange() < 0) return;
        Player player = c.getPlayer();
        String msg = c.getMessage();
        if (!isJoined(player.getUniqueId())) {
            joinChannel(player.getUniqueId());
        }
        PlayerTeamQuery playerTeams = new PlayerTeamQuery();
        playerTeams.callEvent();
        Team playerTeam = playerTeams.getTeam(player);
        if (playerTeam == null) {
            Msg.warn(player, Component.text("You're not in a team!", NamedTextColor.RED));
            return;
        }
        if (msg == null || msg.isEmpty()) {
            setFocusChannel(player);
            Msg.info(player, Component.text("Now focusing team ", NamedTextColor.WHITE)
                     .append(playerTeam.displayName));
            return;
        }
        SQLLog.store(player, this, playerTeam.key, msg);
        Message message = new Message().init(this).player(player, msg);
        message.setTargetName(playerTeam.key);
        plugin.didCreateMessage(this, message);
        handleMessage(message);
        PluginPlayerEvent.Name.USE_CHAT_TEAM.ultimate(plugin, player)
            .detail(Detail.NAME, playerTeam.key)
            .call();
    }

    @Override
    public void consoleDidUseCommand(String msg) {
        plugin.getLogger().warning("[chat:team] player expected");
    }

    @Override
    public void handleMessage(Message message) {
        String log = String.format("[%s][%s][%s]%s: %s",
                                   getTag(), message.getTargetName(),
                                   message.getSenderServer(), message.getSenderName(),
                                   message.getMessage());
        plugin.getLogger().info(log);
        PlayerTeamQuery playerTeams = new PlayerTeamQuery();
        playerTeams.callEvent();
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            if (shouldIgnore(player.getUniqueId(), message)) continue;
            Team playerTeam = playerTeams.getTeam(player);
            if (playerTeam == null || !playerTeam.key.equals(message.getTargetName())) continue;
            send(message, player, playerTeam);
        }
    }

    protected void send(Message message, Player player, Team team) {
        Component component = makeOutput(message, player, team);
        player.sendMessage(component);
        playSoundCue(player);
    }

    @Override
    public Component makeExampleOutput(Player player) {
        Message message = new Message().init(this).player(player, "Hello World");
        message.setTargetName(exampleTeam.key);
        return makeOutput(message, player);
    }

    @Override
    public Component makeOutput(Message message, Player player) {
        PlayerTeamQuery playerTeams = new PlayerTeamQuery();
        playerTeams.callEvent();
        Team team = playerTeams.getTeam(player);
        if (team == null) {
            team = exampleTeam;
        }
        return makeOutput(message, player, team);
    }

    public Component makeOutput(Message message, Player player, Team team) {
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
        cb.append(makeTeamTag(team, bracketType, bracketColor, channelColor));
        if (!message.isHideSenderTags()) {
            // Server Tag
            if (showServer) {
                cb.append(makeServerTag(message, channelColor, bracketColor, bracketType));
            }
            // Player Title
            if (showPlayerTitle) {
                cb.append(makeTitleTag(message, bracketColor, bracketType));
            }
            // Player Name
            Component senderTag = makeSenderTag(message, senderColor, bracketColor, bracketType, tagPlayerName);
            if (!Objects.equals(senderTag, Component.empty())) {
                cb.append(senderTag);
                if (!tagPlayerName) {
                    cb.append(Component.text(":", bracketColor));
                }
            }
        }
        cb.append(Component.text(" "));
        cb.append(makeMessageComponent(message, player, textColor, bracketType, bracketColor, languageFilter));
        return cb.build();
    }

    protected Component makeTeamTag(Team team, BracketType bracketType, TextColor bracketColor, TextColor channelColor) {
        return Component.text()
            .append(Component.text(bracketType.opening, bracketColor))
            .append(team.displayName)
            .append(Component.text(bracketType.closing, bracketColor))
            .clickEvent(ClickEvent.suggestCommand("/t "))
            .hoverEvent(HoverEvent.showText(Component.join(JoinConfiguration.noSeparators(),
                                                           Component.text("Team ", NamedTextColor.GRAY),
                                                           team.displayName)))
            .color(channelColor)
            .build();
    }

    protected void usage(Player player) {
        Msg.info(player, usage);
    }
}
