package com.winthier.chat.channel;

import com.cavetale.core.event.player.PlayerTeamQuery.Team;
import com.cavetale.core.event.player.PlayerTeamQuery;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.perm.Perm;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLLog;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class TeamChannel extends AbstractChannel {
    @Getter private final String permission = "chat.channel.team";
    private final Team exampleTeam = new Team("chat:example", text("Example", BLUE), BLUE);
    private final Component usage = textOfChildren(text("Usage", GRAY, ITALIC),
                                                   text("/t ", GREEN),
                                                   text("<message>", GREEN, ITALIC),
                                                   text(" - ", DARK_GRAY),
                                                   text("Send a message", WHITE),
                                                   newline(),
                                                   text("/t ", GREEN),
                                                   text(" - ", DARK_GRAY),
                                                   text("Focus team chat", WHITE));

    public TeamChannel(final ChatPlugin plugin, final SQLChannel row) {
        super(plugin, row);
    }

    @Override
    public boolean canJoin(UUID player) {
        return Perm.get().has(player, permission) || Perm.get().has(player, "chat.channel.*");
    }

    @Override
    public boolean canTalk(UUID player) {
        return Perm.get().has(player, permission) || Perm.get().has(player, "chat.channel.*");
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
            Msg.warn(player, text("You're not in a team!", RED));
            return;
        }
        if (msg == null || msg.isEmpty()) {
            setFocusChannel(player);
            Msg.info(player, text("Now focusing team ", WHITE)
                     .append(playerTeam.displayName));
            return;
        }
        SQLLog.store(player, this, playerTeam.key, msg);
        Message message = new Message().init(this).player(player, msg);
        message.setTargetName(playerTeam.key);
        plugin.didCreateMessage(this, message);
        handleMessage(message);
        PluginPlayerEvent.Name.USE_CHAT_TEAM.make(plugin, player)
            .detail(Detail.NAME, playerTeam.key)
            .callEvent();
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
        final TextColor white = WHITE;
        TextColor channelColor = SQLSetting.getTextColor(uuid, key, "ChannelColor", white);
        TextColor textColor = team.getTextColor() != null
            ? team.getTextColor()
            : SQLSetting.getTextColor(uuid, key, "TextColor", white);
        TextColor senderColor = SQLSetting.getTextColor(uuid, key, "SenderColor", white);
        TextColor bracketColor = SQLSetting.getTextColor(uuid, key, "BracketColor", white);
        boolean tagPlayerName = SQLSetting.getBoolean(uuid, key, "TagPlayerName", false);
        boolean languageFilter = SQLSetting.getBoolean(uuid, null, "LanguageFilter", true);
        boolean showServer = SQLSetting.getBoolean(uuid, key, "ShowServer", false);
        boolean showPlayerTitle = SQLSetting.getBoolean(uuid, key, "ShowPlayerTitle", true);
        boolean showChannelTag = SQLSetting.getBoolean(uuid, key, "ShowChannelTag", true);
        String tmp = SQLSetting.getString(uuid, key, "BracketType", null);
        BracketType bracketType = tmp != null ? BracketType.of(tmp) : BracketType.ANGLE;
        TextComponent.Builder cb = text();
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
            Component senderTag = makeSenderTag(message, senderColor, bracketColor, bracketType, tagPlayerName, languageFilter);
            if (!Objects.equals(senderTag, empty())) {
                cb.append(senderTag);
                if (!tagPlayerName) {
                    cb.append(text(":", bracketColor));
                }
            }
        }
        cb.append(text(" "));
        cb.append(makeMessageComponent(message, textColor, bracketType, bracketColor, languageFilter));
        return cb.build();
    }

    protected Component makeTeamTag(Team team, BracketType bracketType, TextColor bracketColor, TextColor channelColor) {
        return text()
            .append(text(bracketType.opening, bracketColor))
            .append(team.displayName)
            .append(text(bracketType.closing, bracketColor))
            .clickEvent(suggestCommand("/t "))
            .hoverEvent(showText(textOfChildren(text("Team ", GRAY), team.displayName)))
            .color(channelColor)
            .build();
    }

    protected void usage(Player player) {
        Msg.info(player, usage);
    }
}
