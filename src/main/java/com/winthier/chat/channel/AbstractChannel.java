package com.winthier.chat.channel;

import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.Emoji;
import com.cavetale.core.font.GlyphPolicy;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Filter;
import com.winthier.chat.util.Msg;
import com.winthier.perm.rank.ExtraRank;
import com.winthier.perm.rank.PlayerRank;
import com.winthier.perm.rank.StaffRank;
import com.winthier.title.Title;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
    protected final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
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
        PluginPlayerEvent.Name.FOCUS_CHAT_CHANNEL.ultimate(plugin, player)
            .detail(Detail.NAME, getKey())
            .call();
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
        announce(msg, false);
    }

    @Override
    public final void announceLocal(Component msg) {
        announce(msg, true);
    }

    private void announce(Component msg, boolean local) {
        Message message = new Message().init(this).message(msg);
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
        player.sendMessage(component);
        playSoundCue(player);
    }

    protected final Component makeChannelTag(TextColor channelColor, TextColor bracketColor, BracketType bracketType) {
        Component tooltip = Component.join(JoinConfiguration.separator(Component.newline()),
                                           Component.text(getTitle()),
                                           Component.text(getDescription(), NamedTextColor.GRAY));
        return Component.text()
            .append(Component.text(bracketType.opening, bracketColor))
            .append(Component.text(getTag(), channelColor))
            .append(Component.text(bracketType.closing, bracketColor))
            .hoverEvent(HoverEvent.showText(tooltip))
            .clickEvent(ClickEvent.suggestCommand("/" + getAlias()))
            .build();
    }

    protected final Component makeServerTag(Message message, TextColor serverColor, TextColor bracketColor, BracketType bracketType) {
        String name;
        if (message.getSenderServerDisplayName() != null) {
            name = message.getSenderServerDisplayName();
        } else if (message.getSenderServer() != null) {
            name = message.getSenderServer();
        } else {
            return Component.empty();
        }
        return Component.text()
            .append(Component.text(bracketType.opening, bracketColor))
            .append(Component.text(name, serverColor))
            .append(Component.text(bracketType.closing, bracketColor))
            .hoverEvent(HoverEvent.showText(Component.text(name, serverColor)))
            .clickEvent(ClickEvent.suggestCommand("/" + message.getSenderServer()))
            .build();
    }

    protected final Component makeTitleTag(Message message, TextColor bracketColor, BracketType bracketType) {
        Title theTitle = message.getTitle();
        if (theTitle == null || theTitle.isPrefix() || theTitle.isEmptyTitle()) return Component.empty();
        return Component.text()
            .append(Component.text(bracketType.opening, bracketColor))
            .append(theTitle.getTitleComponent())
            .append(Component.text(bracketType.closing, bracketColor))
            .hoverEvent(HoverEvent.showText(theTitle.getTooltip()))
            .clickEvent(ClickEvent.suggestCommand("/title " + theTitle.getName()))
            .build();
    }

    protected final Component makeSenderTag(Message message, TextColor senderColor, TextColor bracketColor, BracketType bracketType, boolean useBrackets) {
        final Component senderName;
        Component senderDisplayName = message.getSenderDisplayName();
        if (senderDisplayName != null) {
            senderName = senderDisplayName;
        } else if (message.getSenderName() != null) {
            senderName = Component.text(message.getSenderName());
        } else {
            return Component.empty();
        }
        final String serverName;
        if (message.getSenderServerDisplayName() != null) {
            serverName = message.getSenderServerDisplayName();
        } else if (message.getSenderServer() != null) {
            serverName = message.getSenderServer();
        } else {
            serverName = "";
        }
        TextColor kcolor = TextColor.color(0xA0A0A0);
        TextColor vcolor = TextColor.color(0xFFFFFF);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(senderName);
        if (message.getSender() != null) {
            StaffRank staffRank = StaffRank.ofPlayer(message.getSender());
            if (staffRank != null) {
                tooltip.add(Component.text("Staff ", kcolor).append(Component.text(staffRank.getDisplayName(), vcolor)));
            }
            PlayerRank playerRank = PlayerRank.ofPlayer(message.getSender());
            if (playerRank != null) {
                tooltip.add(Component.text("Rank ", kcolor).append(Component.text(playerRank.getDisplayName(), vcolor)));
            }
            Set<ExtraRank> extraRanks = ExtraRank.ofPlayer(message.getSender());
            if (!extraRanks.isEmpty()) {
                String extraString = extraRanks.stream()
                    .map(ExtraRank::getDisplayName)
                    .collect(Collectors.joining(" "));
                tooltip.add(Component.text("Extra ", kcolor).append(Component.text(extraString, vcolor)));
            }
        }
        tooltip.add(Component.text("Server ", kcolor).append(Component.text(serverName, vcolor)));
        tooltip.add(Component.text("Channel ", kcolor).append(Component.text(getTitle(), vcolor)));
        tooltip.add(Component.text("Time ", kcolor).append(Component.text(timeFormat.format(new Date(message.getTime())), vcolor)));
        TextComponent.Builder cb = Component.text().color(senderColor)
            .insertion(message.getSenderName());
        if (useBrackets) {
            cb.append(Component.text(bracketType.opening, bracketColor));
        }
        cb.append(senderName);
        if (useBrackets) {
            cb.append(Component.text(bracketType.closing, bracketColor));
        }
        cb = cb.hoverEvent(HoverEvent.showText(Component.join(JoinConfiguration.separator(Component.newline()),
                                                              tooltip)));
        if (message.getSenderName() != null) {
            cb.clickEvent(ClickEvent.suggestCommand("/msg " + message.getSenderName()));
        }
        return cb.build();
    }

    protected final Component makeMessageComponent(Message message, Player target, TextColor textColor,
                                                   BracketType brackets, TextColor bracketColor, boolean languageFilter) {
        Component messageComponent = message.getMessageComponent();
        if (messageComponent != null) {
            if (languageFilter) {
                messageComponent = Filter.filterBadWords(messageComponent);
            }
            return messageComponent;
        }
        String raw = message.getMessage();
        if (raw == null) return Component.empty();
        Component component;
        if (message.isEmoji()) {
            component = Emoji.replaceText(raw, GlyphPolicy.PUBLIC, true).asComponent().color(textColor);
        } else {
            component = Component.text(raw, textColor);
        }
        Component itemComponent = message.getItemComponent();
        if (itemComponent != null) {
            Component itemTag = Component.text().color(textColor)
                .append(Component.text(brackets.opening, bracketColor))
                .append(itemComponent)
                .append(Component.text(brackets.closing, bracketColor))
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
                Component urlComponent = Component.text()
                    .content(url).color(TextColor.color(0xC0C0FF))
                    .hoverEvent(HoverEvent.showText(Component.text(url, linkColor, TextDecoration.UNDERLINED)))
                    .clickEvent(ClickEvent.openUrl(url))
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
            component = Filter.filterBadWords(component);
        }
        component = component.insertion(Msg.plain(component));
        return component;
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
}
