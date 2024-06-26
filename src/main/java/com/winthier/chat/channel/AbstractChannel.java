package com.winthier.chat.channel;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.font.Emoji;
import com.cavetale.core.font.GlyphPolicy;
import com.cavetale.core.perm.ExtraRank;
import com.cavetale.core.perm.Perm;
import com.cavetale.core.perm.StaffRank;
import com.cavetale.core.text.LineWrap;
import com.cavetale.mytems.item.font.Glyph;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.title.Title;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import static com.cavetale.core.font.Unicode.tiny;
import static com.winthier.chat.Backlog.backlog;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.openUrl;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

/**
 * Implements common methods and fields of all channels.
 */
@Getter @Setter
public abstract class AbstractChannel implements Channel {
    protected final ChatPlugin plugin;
    protected final String title;
    protected final String key;
    protected final String tag;
    protected final String description;
    protected final int range;
    protected final List<String> aliases;
    protected final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    protected final ZoneId timezone = ZoneId.of("UTC-11");
    protected final List<Option> options = new ArrayList<>();

    AbstractChannel(final ChatPlugin plugin, final SQLChannel row) {
        this.plugin = plugin;
        this.tag = row.getTag();
        this.key = row.getChannelKey();
        this.title = row.getTitle();
        this.description = row.getDescription();
        this.range = row.getLocalRange();
        this.aliases = new ArrayList<>(List.of(row.getAliases().split(",")));
        Option[] opts = {
            Option.booleanOption("ShowChannelTag", "Show Channel Tag",
                                 "Show the channel tag at the beginning of every message",
                                 "0"),
            Option.booleanOption("ShowPlayerTitle", "Show Player Title",
                                 "Show a player's current title in every message",
                                 "1"),
            Option.booleanOption("ShowServer", "Show Server",
                                 "Show a player's server in every message",
                                 "0"),
            Option.colorOption("ChannelColor", "Channel Color",
                               "Main channel color",
                               "white"),
            Option.colorOption("TextColor", "Text Color",
                               "Color of chat messages",
                               "white"),
            Option.colorOption("SenderColor", "Player Color",
                               "Color of player names",
                               "white"),
            Option.colorOption("BracketColor", "Bracket Color",
                               "Color of brackets and punctuation",
                               "white"),
            Option.bracketOption("BracketType", "Brackets",
                                 "Appearance of brackets",
                                 "angle"),
            Option.soundOption("SoundCueChat", "Chat Cue",
                               "Sound played when you receive a message",
                               "off"),
            Option.intOption("SoundCueChatVolume", "Chat Cue Volume",
                             "Sound played when you receive a message",
                             "10", 1, 10)
        };
        for (Option option : opts) options.add(option);
    }

    @Override
    public final String getAlias() {
        return getAliases().get(0);
    }

    @Override
    public final boolean hasPermission(Player player) {
        return canTalk(player.getUniqueId());
    }

    @Override
    public final void setFocusChannel(Player player) {
        setFocusChannel(player.getUniqueId());
        PluginPlayerEvent.Name.FOCUS_CHAT_CHANNEL.make(plugin, player)
            .detail(Detail.NAME, getKey())
            .callEvent();
    }

    @Override
    public final void setFocusChannel(UUID player) {
        SQLSetting.set(player, null, "FocusChannel", getKey());
    }

    @Override
    public final void joinChannel(UUID player) {
        SQLSetting.set(player, getKey(), "Joined", true);
    }

    @Override
    public final void leaveChannel(UUID player) {
        SQLSetting.set(player, getKey(), "Joined", false);
    }

    @Override
    public final boolean isJoined(UUID player) {
        return SQLSetting.getBoolean(player, getKey(), "Joined", true);
    }

    @Override
    public final Channel getChannel() {
        return this;
    }

    /**
     * Override if channel command syntax is more specific.
     */
    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        playerDidUseCommand(context);
    }

    @Override
    public final void announce(Component msg) {
        announce(null, msg, false);
    }

    @Override
    public final void announceLocal(Component msg) {
        announce(null, msg, true);
    }

    @Override
    public final void announce(UUID sender, Component msg) {
        announce(sender, msg, false);
    }

    @Override
    public final void announceLocal(UUID sender, Component msg) {
        announce(sender, msg, true);
    }

    private void announce(UUID sender, Component msg, boolean local) {
        Message message = new Message().init(this).message(msg);
        message.setPassive(true);
        message.setSender(sender);
        message.setLocal(local);
        plugin.didCreateMessage(this, message);
        handleMessage(message);
    }

    /**
     * Default implementation. Applies to all channels without side
     * effects. Meaning, all except PM.
     */
    protected void send(Message message, Player player) {
        Component component = makeOutput(message, player);
        backlog().send(player, component);
        if (!message.isPassive()) {
            playSoundCue(player);
        }
    }

    protected final Component makeChannelTag(TextColor channelColor, TextColor bracketColor, BracketType bracketType) {
        Component tooltip = join(separator(newline()), text(getTitle()), text(getDescription(), GRAY));
        return text()
            .append(text(bracketType.opening, bracketColor))
            .append(text(getTag(), channelColor))
            .append(text(bracketType.closing, bracketColor))
            .hoverEvent(showText(tooltip))
            .clickEvent(suggestCommand("/" + getAlias()))
            .build();
    }

    protected final Component makeServerTag(Message message, TextColor serverColor, TextColor bracketColor, BracketType bracketType) {
        String name;
        if (message.getSenderServerDisplayName() != null) {
            name = message.getSenderServerDisplayName();
        } else if (message.getSenderServer() != null) {
            name = message.getSenderServer();
        } else {
            return empty();
        }
        return text()
            .append(text(bracketType.opening, bracketColor))
            .append(text(name, serverColor))
            .append(text(bracketType.closing, bracketColor))
            .hoverEvent(showText(text(name, serverColor)))
            .clickEvent(suggestCommand("/" + message.getSenderServer()))
            .build();
    }

    protected final Component makeTitleTag(Message message, TextColor bracketColor, BracketType bracketType) {
        Title theTitle = message.getTitle();
        if (theTitle == null || theTitle.isPrefix() || theTitle.isEmptyTitle()) return empty();
        return text()
            .append(text(bracketType.opening, bracketColor))
            .append(theTitle.getTitleComponent(message.getSender()))
            .append(text(bracketType.closing, bracketColor))
            .hoverEvent(showText(theTitle.getTooltip(message.getSender())))
            .clickEvent(suggestCommand("/title " + theTitle.getName()))
            .build();
    }

    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), timezone).format(timeFormat);
    }

    protected final Component makeSenderTag(Message message, TextColor senderColor, TextColor bracketColor,
                                            BracketType bracketType, boolean useBrackets, boolean languageFilter) {
        final Component senderName;
        Component senderDisplayName = message.getSenderDisplayName();
        if (senderDisplayName != null) {
            senderName = senderDisplayName;
        } else if (message.getSenderName() != null) {
            senderName = text(message.getSenderName());
        } else {
            return empty();
        }
        final String serverName;
        if (message.getSenderServerDisplayName() != null) {
            serverName = message.getSenderServerDisplayName();
        } else if (message.getSenderServer() != null) {
            serverName = message.getSenderServer();
        } else {
            serverName = "";
        }
        TextColor vcolor = TextColor.color(0xFFFFFF);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(senderName);
        if (message.getStatusMessage() != null) {
            String statusMessage = languageFilter
                ? plugin.filterBadWords(message.getStatusMessage())
                : message.getStatusMessage();
            tooltip.addAll(new LineWrap()
                           .emoji(message.isEmoji())
                           .glyphPolicy(GlyphPolicy.PUBLIC)
                           .tooltip(false)
                           .componentMaker(str -> text(str, GRAY))
                           .wrap(statusMessage));
            tooltip.add(empty());
        }
        if (message.getSender() != null) {
            int level = Perm.get().getLevel(message.getSender());
            if (level > 0) {
                tooltip.add(textOfChildren(text(tiny("tier "), GRAY), Glyph.toComponent("" + level)));
            }
            StaffRank staffRank = StaffRank.ofPlayer(message.getSender());
            if (staffRank != null) {
                tooltip.add(textOfChildren(text(tiny("staff "), GRAY), staffRank.asComponent()));
            }
            Set<ExtraRank> extraRanks = ExtraRank.ofPlayer(message.getSender());
            if (!extraRanks.isEmpty()) {
                List<Component> rankComponents = new ArrayList<>(extraRanks.size());
                for (ExtraRank rank : extraRanks) {
                    rankComponents.add(rank.asComponent());
                }
                tooltip.add(textOfChildren(text(tiny("extra "), GRAY),
                                           join(separator(space()), rankComponents)));
            }
        }
        tooltip.add(textOfChildren(text(tiny("server "), GRAY), text(serverName, vcolor)));
        tooltip.add(textOfChildren(text(tiny("channel "), GRAY), text(getTitle(), vcolor)));
        tooltip.add(textOfChildren(text(tiny("time "), GRAY), text(formatTimestamp(message.getTime()), vcolor)));
        TextComponent.Builder cb = text().color(senderColor)
            .insertion(message.getSenderName());
        if (useBrackets) {
            cb.append(text(bracketType.opening, bracketColor));
        }
        cb.append(senderName);
        if (useBrackets) {
            cb.append(text(bracketType.closing, bracketColor));
        }
        cb = cb.hoverEvent(showText(join(separator(newline()), tooltip)));
        if (message.getSenderName() != null) {
            cb.clickEvent(suggestCommand("/msg " + message.getSenderName()));
        }
        return cb.build();
    }

    public final Component makeMessageComponent(Message message,
                                                TextColor textColor,
                                                BracketType brackets,
                                                TextColor bracketColor,
                                                boolean languageFilter) {
        Component messageComponent = message.getMessageComponent();
        if (messageComponent != null) {
            if (languageFilter) {
                messageComponent = plugin.filterBadWords(messageComponent);
            }
            return messageComponent;
        }
        String raw = message.getMessage();
        if (raw == null) return empty();
        Component component;
        if (message.isEmoji()) {
            component = Emoji.replaceText(raw, GlyphPolicy.PUBLIC, true).asComponent().color(textColor);
        } else {
            component = text(raw, textColor);
        }
        Component itemComponent = message.getItemComponent();
        if (itemComponent != null) {
            Component itemTag = text().color(textColor)
                .append(text(brackets.opening, bracketColor))
                .append(itemComponent)
                .append(text(brackets.closing, bracketColor))
                .build();
            TextReplacementConfig textReplacementConfig = TextReplacementConfig.builder()
                .once()
                .matchLiteral("[item]")
                .replacement(itemTag)
                .build();
            component = component.replaceText(textReplacementConfig);
        }
        List<String> urls = message.getUrls();
        if (urls != null) {
            for (String url : urls) {
                TextColor linkColor = TextColor.color(0x4040EE);
                Component urlComponent = text()
                    .content(url).color(TextColor.color(0xC0C0FF))
                    .hoverEvent(showText(text(url, linkColor, TextDecoration.UNDERLINED)))
                    .clickEvent(openUrl(url))
                    .build();
                TextReplacementConfig textReplacementConfig = TextReplacementConfig.builder()
                    .once()
                    .matchLiteral(url)
                    .replacement(urlComponent)
                    .build();
                component = component.replaceText(textReplacementConfig);
            }
        }
        if (languageFilter) {
            component = plugin.filterBadWords(component)
                .insertion(plugin.filterBadWords("" + message.getRawMessage()));
        } else {
            component = component.insertion(message.getRawMessage());
        }
        return component;
    }

    public final Component makeMessageComponent(Message message) {
        return makeMessageComponent(message, WHITE, BracketType.BRACKETS, WHITE, false);
    }

    static boolean shouldIgnore(UUID player, Message message) {
        if (message.getSender() != null && SQLIgnore.doesIgnore(player, message.getSender())) return true;
        return false;
    }

    public final List<Chatter> getOnlineMembers() {
        List<Chatter> result = new ArrayList<>();
        for (Chatter chatter : plugin.getOnlinePlayers()) {
            if (!canJoin(chatter.getUuid())) continue;
            if (!isJoined(chatter.getUuid())) continue;
            result.add(chatter);
        }
        return result;
    }

    public final List<Player> getLocalMembers() {
        List<Player> result = new ArrayList<>();
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!canJoin(player.getUniqueId())) continue;
            if (!isJoined(player.getUniqueId())) continue;
            result.add(player);
        }
        return result;
    }

    @Override
    public final boolean playSoundCue(Player player) {
        String tmp = SQLSetting.getString(player.getUniqueId(), getKey(), "SoundCueChat", "off");
        SoundCue soundCue = tmp != null
            ? SoundCue.of(tmp)
            : null;
        if (soundCue == null) return false;
        int volume = SQLSetting.getInt(player.getUniqueId(), getKey(), "SoundCueChatVolume", 10);
        float vol = (float) volume / 10.0f;
        player.playSound(player.getLocation(), soundCue.sound, vol, 1.0f);
        return true;
    }

    /**
     * Most commands are handled by the plugin. The plugin's onCommand
     * will find the channel in question.
     */
    @Override
    public void registerCommand() { }

    @Override
    public void unregisterCommand() { }

    private Component makeJoinLeaveTooltip(UUID uuid, String name, long timestamp) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(text(name, WHITE));
        int level = Perm.get().getLevel(uuid);
        if (level > 0) {
            tooltip.add(textOfChildren(text(tiny("tier "), GRAY), Glyph.toComponent("" + level)));
        }
        StaffRank staffRank = StaffRank.ofPlayer(uuid);
        if (staffRank != null) {
            tooltip.add(textOfChildren(text(tiny("staff "), GRAY), staffRank.asComponent()));
        }
        Set<ExtraRank> extraRanks = ExtraRank.ofPlayer(uuid);
        if (!extraRanks.isEmpty()) {
            List<Component> rankComponents = new ArrayList<>(extraRanks.size());
            for (ExtraRank rank : extraRanks) {
                rankComponents.add(rank.asComponent());
            }
            tooltip.add(textOfChildren(text(tiny("extra "), GRAY),
                                       join(separator(space()), rankComponents)));
        }
        tooltip.add(textOfChildren(text(tiny("time "), GRAY), text(formatTimestamp(timestamp), WHITE)));
        return join(separator(newline()), tooltip);
    }

    /**
     * Send player join message to this channel.
     */
    public void onBungeeJoin(UUID uuid, String name, long timestamp) {
        if (plugin.containsBadWord(name)) {
            plugin.getLogger().info("Skipping name containing bad world: " + name);
            return;
        }
        Message message = new Message().init(this)
            .message(text(name + " joined", GREEN, ITALIC)
                     .hoverEvent(showText(makeJoinLeaveTooltip(uuid, name, timestamp)))
                     .clickEvent(suggestCommand("/msg " + name)));
        message.setSender(uuid);
        message.setSenderName(name);
        message.setLocal(true);
        message.setPassive(true);
        message.setHideSenderTags(true);
        plugin.didCreateMessage(this, message);
        handleMessage(message);
    }

    /**
     * Send player quit message to this channel.
     */
    public void onBungeeQuit(UUID uuid, String name, long timestamp) {
        if (plugin.containsBadWord(name)) {
            plugin.getLogger().info("Skipping name containing bad world: " + name);
            return;
        }
        Message message = new Message().init(this)
            .message(text(name + " disconnected", AQUA, ITALIC)
                     .hoverEvent(showText(makeJoinLeaveTooltip(uuid, name, timestamp)))
                     .clickEvent(suggestCommand("/msg " + name)));
        message.setSender(uuid);
        message.setSenderName(name);
        message.setLocal(true);
        message.setPassive(true);
        message.setHideSenderTags(true);
        plugin.didCreateMessage(this, message);
        handleMessage(message);
    }
}
