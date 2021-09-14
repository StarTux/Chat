package com.winthier.chat;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.CommandResponder;
import com.winthier.chat.channel.Option;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.channel.PrivateChannel;
import com.winthier.chat.connect.ConnectListener;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class ChatCommand extends AbstractChatCommand {
    private final ChatPlugin plugin;
    private List<Option> globalOptions = Arrays.asList(Option.booleanOption("LanguageFilter", "Language Filter",
                                                                            "Filter out foul language", "1"));
    CommandNode rootNode;

    public ChatCommand enable() {
        plugin.getCommand("chat").setExecutor(this);
        rootNode = new CommandNode("chat").description("Chat menu")
            .playerCaller(this::chat);
        rootNode.addChild("set").denyTabCompletion()
            .arguments("[channel]")
            .description("Chat settings")
            .playerCaller(this::set);
        rootNode.addChild("list").denyTabCompletion()
            .description("List channels")
            .senderCaller(this::list);
        rootNode.addChild("join").denyTabCompletion()
            .arguments("<channel>")
            .description("Join a channel")
            .playerCaller(this::join);
        rootNode.addChild("leave").denyTabCompletion()
            .arguments("<channel>")
            .description("Leave a channel")
            .playerCaller(this::leave);
        rootNode.addChild("ignore")
            .arguments("[player]")
            .description("(Un)ignore players")
            .playerCaller(this::ignore);
        rootNode.addChild("who").denyTabCompletion()
            .arguments("<channel>")
            .description("List channel players")
            .senderCaller(this::who);
        rootNode.addChild("say").denyTabCompletion()
            .arguments("<channel> <message...>")
            .description("Speak in chat")
            .senderCaller(this::say);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player != null && args.length == 1) {
            Channel channel = plugin.findChannel(args[0]);
            if (channel != null) {
                if (!channel.canTalk(player.getUniqueId())) return false;
                channel.joinChannel(player.getUniqueId());
                channel.setFocusChannel(player.getUniqueId());
                Msg.info(player, Component.text("Now focusing " + channel.getTitle(), NamedTextColor.WHITE));
                return true;
            }
        }
        return rootNode.call(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = rootNode.complete(sender, command, label, args);
        return list != null ? list : super.onTabComplete(sender, command, label, args);
    }

    boolean chat(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        showMenu(sender);
        return true;
    }

    boolean set(Player player, String[] args) {
        if (player == null) return false;
        if (args.length == 0) {
            listChannelsForSettings(player);
        }
        setOption(player, args);
        return true;
    }

    boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        listChannels(sender);
        return true;
    }

    boolean join(Player player, String[] args) {
        if (args.length != 1) return false;
        Channel channel = plugin.findChannel(args[0]);
        if (channel == null || !channel.canJoin(player.getUniqueId())) return false;
        channel.joinChannel(player.getUniqueId());
        listChannels(player);
        return true;
    }

    boolean leave(Player player, String[] args) {
        if (args.length != 1) return false;
        Channel channel = plugin.findChannel(args[0]);
        if (channel == null || !channel.canJoin(player.getUniqueId())) return false;
        channel.leaveChannel(player.getUniqueId());
        listChannels(player);
        return true;
    }

    boolean ignore(Player player, String[] args) {
        if (args.length == 0) {
            listIgnores(player);
            return true;
        } else if (args.length == 1) {
            toggleIgnore(player, args[0]);
            return true;
        } else {
            return false;
        }
    }

    boolean who(CommandSender sender, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        Channel channel;
        if (args.length == 0) {
            if (player == null) return false;
            channel = plugin.getFocusChannel(player.getUniqueId());
            if (channel == null) {
                throw new CommandWarn("Join a channel first!");
            }
        } else if (args.length == 1) {
            String channelName = args[0];
            channel = plugin.findChannel(channelName);
            if (channel == null) {
                throw new CommandWarn("Channel not found: " + channelName);
            }
        } else {
            return false;
        }
        if (player != null && !channel.canJoin(player.getUniqueId())) {
            throw new CommandWarn("Channel not found!");
        }
        StringBuilder sb = new StringBuilder();
        List<Component> chatters = new ArrayList<>();
        for (Chatter chatter : channel.getOnlineMembers()) {
            chatters.add(Component.text(chatter.getName(), NamedTextColor.WHITE));
        }
        Msg.info(sender, TextComponent.ofChildren(new Component[] {
                    Component.text("Channel " + channel.getTitle() + " (" + chatters.size() + "): ", NamedTextColor.YELLOW),
                    Component.join(Component.text(", ", NamedTextColor.DARK_GRAY), chatters),
                }));
        return true;
    }

    boolean say(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        CommandResponder cmd = plugin.findCommand(args[0]);
        if (cmd == null) return false;
        if (player != null && !cmd.hasPermission(player)) return false;
        StringBuilder sb = new StringBuilder(args[1]);
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (player != null && !ChatPlayerTalkEvent.call(player, cmd.getChannel(), msg)) return false;
        if (player != null) {
            cmd.playerDidUseCommand(new PlayerCommandContext(player, args[0], msg));
        } else {
            cmd.consoleDidUseCommand(msg);
        }
        return true;
    }

    private static boolean isKeyValuePairValid(List<Option> options, String key, String value) {
        for (Option option: options) {
            if (key.equals(option.getKey())) {
                for (Option.State state: option.getStates()) {
                    if (value.equals(state.getValue())) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    void setOption(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        if (args.length == 0) return;
        Channel channel;
        List<Option> options;
        String chkey;
        if (args[0].equals("_")) {
            channel = null;
            options = globalOptions;
            chkey = null;
        } else {
            channel = plugin.findChannel(args[0]);
            if (channel == null || !channel.canJoin(player.getUniqueId())) return;
            options = channel.getOptions();
            chkey = channel.getKey();
        }
        if (args.length == 2 && args[1].equals("reset")) {
            if (channel == null) return;
            for (Option option : options) {
                SQLSetting.set(uuid, chkey, option.getKey(), null);
            }
            Msg.info(player, Component.text("Settings reset to default", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
        } else if (args.length == 3) {
            String key = args[1];
            String value = args[2];
            if (!isKeyValuePairValid(options, key, value)) return;
            SQLSetting.set(uuid, chkey, key, value);
            Msg.info(player, Component.text("Settings updated", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
        }
        if (channel == null) {
            listChannelsForSettings(player);
        } else {
            showSettingsMenu(player, channel);
            PluginPlayerEvent.Name.OPEN_CHAT_SETTINGS.ultimate(plugin, player)
                .detail(Detail.NAME, channel.getKey())
                .call();
        }
    }

    void showMenu(CommandSender sender) {
        Msg.info(sender, Component.text("Menu", NamedTextColor.GRAY));
        Player human = sender instanceof Player ? (Player) sender : null;
        TextComponent.Builder cb = Component.text();
        cb.append(Component.text("Channels", NamedTextColor.WHITE, TextDecoration.ITALIC));
        for (Channel channel : plugin.getChannels()) {
            if (human != null && !channel.canJoin(human.getUniqueId())) continue;
            if (SQLSetting.getBoolean(null, channel.getKey(), "MutePlayers", false)) continue;
            cb.append(Component.space());
            TextColor channelColor = SQLSetting.getTextColor(human != null ? human.getUniqueId() : null,
                                                             channel.getKey(), "ChannelColor", NamedTextColor.WHITE);
            Component tooltip;
            if (channel instanceof PrivateChannel) {
                tooltip = TextComponent.ofChildren(Component.text("/msg [user] [message]", channelColor),
                                                   Component.text("\nWhisper someone", NamedTextColor.GRAY));
            } else {
                tooltip = TextComponent.ofChildren(Component.text("/" + channel.getAlias() + " [message]", channelColor),
                                                   Component.text("\nTalk in " + channel.getTitle(), NamedTextColor.GRAY));
            }
            cb.append(Component.text()
                      .content("[" + channel.getTag() + "]").color(channelColor)
                      .clickEvent(ClickEvent.suggestCommand("/" + channel.getTag().toLowerCase() + " "))
                      .hoverEvent(HoverEvent.showText(tooltip))
                      .build());
        }
        TextColor clickColor = NamedTextColor.GREEN;
        TextColor titleColor = NamedTextColor.WHITE;
        TextColor descColor = NamedTextColor.GRAY;
        cb.append(Component.newline());
        cb.append(Component.join(Component.space(),
                                 Component.text("Community", NamedTextColor.GRAY, TextDecoration.ITALIC),
                                 Component.text().content("[List]").color(clickColor)
                                 .hoverEvent(Component.join(Component.newline(),
                                                            Component.text("/ch list", titleColor),
                                                            Component.text("Channel List", descColor))
                                             .asHoverEvent())
                                 .clickEvent(ClickEvent.runCommand("/ch list")).build(),
                                 Component.text().content("[Who]").color(clickColor)
                                 .hoverEvent(Component.join(Component.newline(),
                                                            Component.text("/ch who <channel>", titleColor),
                                                            Component.text("Channel User List", descColor))
                                             .asHoverEvent())
                                 .clickEvent(ClickEvent.suggestCommand("/ch who ")).build(),
                                 Component.text().content("[Reply]").color(clickColor)
                                 .hoverEvent(Component.join(Component.newline(),
                                                            Component.text("/r [message]", titleColor),
                                                            Component.text("Reply to a private message", descColor),
                                                            Component.text("or focus on reply", descColor))
                                             .asHoverEvent())
                                 .clickEvent(ClickEvent.suggestCommand("/r ")).build(),
                                 Component.text().content("[Party]").color(clickColor)
                                 .hoverEvent(Component.join(Component.newline(),
                                                            Component.text("/party [name]", titleColor),
                                                            Component.text("Select a named party", descColor),
                                                            Component.text("or show current party", descColor))
                                             .asHoverEvent())
                                 .clickEvent(ClickEvent.suggestCommand("/party "))));
        clickColor = NamedTextColor.YELLOW;
        cb.append(Component.newline());
        cb.append(Component.join(Component.space(),
                                 Component.text("Settings", NamedTextColor.GRAY, TextDecoration.ITALIC),
                                 Component.text().content("[Set]").color(clickColor)
                                 .hoverEvent(Component.join(Component.newline(),
                                                            Component.text("/ch set <channel>", titleColor),
                                                            Component.text("Channel Settings", descColor))
                                             .asHoverEvent())
                                 .clickEvent(ClickEvent.runCommand("/ch set")).build(),
                                 Component.text().content("[Ignore]").color(clickColor)
                                 .hoverEvent(Component.join(Component.newline(),
                                                            Component.text("/ignore [player]", titleColor),
                                                            Component.text("(Un)ignore a player", descColor),
                                                            Component.text("or list ignored players", descColor))
                                             .asHoverEvent())
                                 .clickEvent(ClickEvent.suggestCommand("/ignore ")).build()));
        sender.sendMessage(cb.build());
    }

    /**
     * Create one line per option.
     */
    Component makeOptionComponent(Player player, Channel channel, Option option) {
        UUID uuid = player.getUniqueId();
        String alias = channel != null ? channel.getAlias() : "_";
        String chkey = channel != null ? channel.getKey() : null;
        TextComponent.Builder cb = Component.text();
        cb.append(Component.text().content(option.getDisplayName()).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
                  .hoverEvent(TextComponent.ofChildren(Component.text(option.getDisplayName(), NamedTextColor.WHITE),
                                                       Component.text("\n" + option.getDescription(), NamedTextColor.GRAY))));
        String current = SQLSetting.getString(uuid, chkey, option.getKey(), option.getDefaultValue());
        for (Option.State state: option.getStates()) {
            cb.append(Component.space());
            boolean active = Objects.equals(current, state.getValue());
            Component c;
            if (active) {
                Component tooltip = TextComponent.ofChildren(Component.text(state.getDisplayName(), state.getActiveColor()),
                                                             Component.text("\n" + state.getDescription(), NamedTextColor.WHITE));
                c = Component.text().content("[" + state.getDisplayName() + "]").color(state.getActiveColor())
                    .hoverEvent(HoverEvent.showText(tooltip))
                    .clickEvent(ClickEvent.runCommand("/ch set " + alias + " " + option.getKey() + " " + state.getValue()))
                    .build();
            } else {
                Component tooltip = TextComponent.ofChildren(Component.text(state.getDisplayName(), state.getColor()),
                                                             Component.text("\n" + state.getDescription(), NamedTextColor.WHITE));
                c = Component.text().content(state.getDisplayName()).color(state.getColor())
                    .hoverEvent(HoverEvent.showText(tooltip))
                    .clickEvent(ClickEvent.runCommand("/ch set " + alias + " " + option.getKey() + " " + state.getValue()))
                    .build();
            }
            cb.append(c);
        }
        return cb.build();
    }

    void listChannelsForSettings(Player player) {
        if (player == null) return;
        Msg.info(player, Component.text("Settings Menu", NamedTextColor.WHITE));
        List<Component> lines = new ArrayList<>();
        for (Option option : globalOptions) {
            lines.add(makeOptionComponent(player, null, option));
        }
        TextComponent.Builder cb = Component.text();
        cb.append(Component.text("Channel Settings", NamedTextColor.WHITE, TextDecoration.ITALIC));
        for (Channel channel: plugin.getChannels()) {
            if (!channel.canJoin(player.getUniqueId())) continue;
            cb.append(Component.space());
            TextColor channelColor = SQLSetting.getTextColor(player.getUniqueId(), channel.getKey(), "ChannelColor", NamedTextColor.WHITE);
            cb.append(Component.text().content("[" + channel.getTag() + "]").color(channelColor)
                      .hoverEvent(HoverEvent.showText(Component.text(channel.getTitle(), channelColor)))
                      .clickEvent(ClickEvent.runCommand("/ch set " + channel.getAlias()))
                      .build());
        }
        lines.add(cb.build());
        player.sendMessage(Component.join(Component.newline(), lines));
    }

    void showSettingsMenu(Player player, Channel channel) {
        UUID uuid = player.getUniqueId();
        String key = channel.getKey();
        Msg.info(player, Component.text("Settings", NamedTextColor.WHITE));
        List<Component> lines = new ArrayList<>();
        final TextColor kcolor = TextColor.color(0xA0A0A0);
        for (Option option: channel.getOptions()) {
            lines.add(makeOptionComponent(player, channel, option));
        }
        List<Component> components = new ArrayList<>();
        components.add(Component.text("Server Default", kcolor, TextDecoration.ITALIC));
        components.add(Component.text().content("[Reset]").color(NamedTextColor.DARK_RED)
                       .hoverEvent(HoverEvent.showText(Component.text("Reset to channel defaults", NamedTextColor.DARK_RED)))
                       .clickEvent(ClickEvent.runCommand("/ch set " + channel.getAlias() + " reset"))
                       .build());
        if (player.hasPermission("chat.admin")) {
            TextColor color = TextColor.color(0xFF0000);
            components.add(Component.text().content("[Defaults]").color(color)
                           .hoverEvent(HoverEvent.showText(Component.text("Overwrite all public channel\ndefaultswith your settings", color)))
                           .clickEvent(ClickEvent.runCommand("/chadm setdefaults " + channel.getAlias()))
                           .build());
        }
        lines.add(Component.join(Component.space(), components));
        lines.add(Component.join(Component.space(),
                                 Component.text("Example", kcolor, TextDecoration.ITALIC),
                                 channel.makeExampleOutput(player)));
        channel.playSoundCue(player);
        player.sendMessage(Component.join(Component.newline(), lines));
    }

    void listChannels(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        Msg.info(sender, Component.text("Channel List", NamedTextColor.WHITE));
        List<Component> lines = new ArrayList<>();
        for (Channel channel: plugin.getChannels()) {
            List<Object> json = new ArrayList<>();
            if (player != null && !channel.canJoin(player.getUniqueId())) continue;
            TextComponent.Builder cb = Component.text().content(" ");
            if (player == null || channel.isJoined(player.getUniqueId())) {
                cb.append(Component.text().content("\u2612").color(NamedTextColor.GREEN)
                          .hoverEvent(HoverEvent.showText(Component.text("Leave " + channel.getTitle(), NamedTextColor.GREEN)))
                          .clickEvent(ClickEvent.runCommand("/ch leave " + channel.getAlias()))
                          .build());
            } else {
                cb.append(Component.text().content("\u2610").color(NamedTextColor.RED)
                          .hoverEvent(HoverEvent.showText(Component.text("Join " + channel.getTitle(), NamedTextColor.RED)))
                          .clickEvent(ClickEvent.runCommand("/ch join " + channel.getAlias()))
                          .build());
            }
            cb.append(Component.space());
            cb.append(Component.text().content("\u2699").color(NamedTextColor.YELLOW)
                      .hoverEvent(HoverEvent.showText(Component.join(Component.newline(),
                                                                     Component.text("/ch set " + channel.getAlias(), NamedTextColor.YELLOW),
                                                                     Component.text("Open channel settings", NamedTextColor.GRAY))))
                      .clickEvent(ClickEvent.runCommand("/ch set " + channel.getAlias()))
                      .build());
            cb.append(Component.space());
            TextColor channelColor = player != null
                ? SQLSetting.getTextColor(player.getUniqueId(), channel.getKey(), "ChannelColor", NamedTextColor.WHITE)
                : SQLSetting.getTextColor(null, channel.getKey(), "ChannelColor", NamedTextColor.WHITE);
            Component tooltip = TextComponent.ofChildren(Component.text("/" + channel.getTag().toLowerCase() + " [message]", channelColor),
                                                         Component.text("\nFocus " + channel.getTitle(), NamedTextColor.GRAY));
            cb.append(Component.text().content(channel.getTitle()).color(channelColor)
                      .hoverEvent(HoverEvent.showText(tooltip))
                      .clickEvent(ClickEvent.runCommand("/" + channel.getTag().toLowerCase()))
                      .build());
            if (player != null && channel.equals(plugin.getFocusChannel(player.getUniqueId()))) {
                cb.append(Component.text().content("*").color(channelColor)
                          .hoverEvent(HoverEvent.showText(Component.text("You are focusing " + channel.getTitle(), channelColor)))
                          .build());
            }
            cb.append(Component.text(" - ", NamedTextColor.DARK_GRAY));
            cb.append(Component.text(channel.getDescription(), NamedTextColor.GRAY));
            lines.add(cb.build());
        }
        sender.sendMessage(Component.join(Component.newline(), lines));
        if (sender instanceof Player) {
            PluginPlayerEvent.Name.LIST_CHAT_CHANNELS.call(plugin, player);
        }
    }

    void listIgnores(Player player) {
        UUID uuid = player.getUniqueId();
        TextComponent.Builder cb = Component.text();
        List<Component> ignores = new ArrayList<>();
        for (UUID ign: SQLIgnore.listIgnores(uuid)) {
            Chatter chatter = plugin.findOfflinePlayer(ign);
            if (chatter == null) continue;
            ignores.add(Component.text().content(chatter.getName()).color(NamedTextColor.RED)
                        .hoverEvent(HoverEvent.showText(Component.text("Click to unignore " + chatter.getName(), NamedTextColor.RED)))
                        .clickEvent(ClickEvent.runCommand("/ch ignore " + chatter.getName()))
                        .build());
        }
        if (ignores.isEmpty()) {
            Msg.warn(player, Component.text("Not ignoring anyone", NamedTextColor.RED));
            return;
        }
        player.sendMessage(TextComponent.ofChildren(Component.text("Ignoring " + ignores.size() + " players: ", NamedTextColor.WHITE, TextDecoration.ITALIC),
                                                    Component.join(Component.text(", ", NamedTextColor.DARK_GRAY), ignores)));
    }

    void toggleIgnore(Player player, String name) {
        Chatter ignoree = plugin.findOfflinePlayer(name);
        if (ignoree == null) {
            Msg.warn(player, Component.text("Player not found: " + name, NamedTextColor.RED));
            return;
        }
        if (SQLIgnore.doesIgnore(player.getUniqueId(), ignoree.getUuid())) {
            SQLIgnore.ignore(player.getUniqueId(), ignoree.getUuid(), false);
            Msg.info(player, Component.text("No longer ignoring " + ignoree.getName(), NamedTextColor.WHITE));
            plugin.getConnectListener().broadcastMeta(ConnectListener.META_IGNORE, player.getUniqueId());
        } else {
            SQLIgnore.ignore(player.getUniqueId(), ignoree.getUuid(), true);
            Msg.info(player, Component.text("Ignoring " + ignoree.getName(), NamedTextColor.YELLOW));
            plugin.getConnectListener().broadcastMeta(ConnectListener.META_IGNORE, player.getUniqueId());
        }
    }
}
