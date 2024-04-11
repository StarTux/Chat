package com.winthier.chat;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.mytems.Mytems;
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
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

@RequiredArgsConstructor
public final class ChatCommand extends AbstractChatCommand {
    private final ChatPlugin plugin;
    private List<Option> globalOptions = Arrays.asList(Option.booleanOption("LanguageFilter", "Language Filter",
                                                                            "Filter out foul language", "1"));
    private CommandNode rootNode;

    public ChatCommand enable() {
        plugin.getCommand("chat").setExecutor(this);
        rootNode = new CommandNode("chat").description("Chat menu")
            .senderCaller(this::chat);
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
            .playerCaller(this::joinChannel);
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
                Msg.info(player, text("Now focusing " + channel.getTitle(), WHITE));
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

    private boolean chat(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        showMenu(sender);
        return true;
    }

    private boolean set(Player player, String[] args) {
        if (player == null) return false;
        if (args.length == 0) {
            listChannelsForSettings(player);
        }
        setOption(player, args);
        return true;
    }

    private boolean list(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        listChannels(sender);
        return true;
    }

    private boolean joinChannel(Player player, String[] args) {
        if (args.length != 1) return false;
        Channel channel = plugin.findChannel(args[0]);
        if (channel == null || !channel.canJoin(player.getUniqueId())) return false;
        channel.joinChannel(player.getUniqueId());
        listChannels(player);
        return true;
    }

    private boolean leave(Player player, String[] args) {
        if (args.length != 1) return false;
        Channel channel = plugin.findChannel(args[0]);
        if (channel == null || !channel.canJoin(player.getUniqueId())) return false;
        channel.leaveChannel(player.getUniqueId());
        listChannels(player);
        return true;
    }

    private boolean ignore(Player player, String[] args) {
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

    private boolean who(CommandSender sender, String[] args) {
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
            chatters.add(text(chatter.getName(), WHITE));
        }
        Msg.info(sender, join(noSeparators(), new Component[] {
                    text("Channel " + channel.getTitle() + " (" + chatters.size() + "): ", YELLOW),
                    join(separator(text(", ", DARK_GRAY)), chatters),
                }));
        return true;
    }

    private boolean say(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        CommandResponder cmd = plugin.findCommand(args[0]);
        if (cmd == null) return false;
        if (player != null && !cmd.hasPermission(player)) return false;
        StringBuilder sb = new StringBuilder(args[1]);
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (player != null) {
            ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(player, cmd.getChannel(), msg);
            if (!event.call()) return false;
            cmd.playerDidUseCommand(new PlayerCommandContext(player, args[0], event.getMessage()));
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

    private void setOption(Player player, String[] args) {
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
            Msg.info(player, text("Settings reset to default", GREEN));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
        } else if (args.length == 3) {
            String key = args[1];
            String value = args[2];
            if (!isKeyValuePairValid(options, key, value)) return;
            SQLSetting.set(uuid, chkey, key, value);
            Msg.info(player, text("Settings updated", GREEN));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
        }
        if (channel == null) {
            listChannelsForSettings(player);
        } else {
            showSettingsMenu(player, channel);
            PluginPlayerEvent.Name.OPEN_CHAT_SETTINGS.make(plugin, player)
                .detail(Detail.NAME, channel.getKey())
                .callEvent();
        }
    }

    private void showMenu(CommandSender sender) {
        Msg.info(sender, text("Menu", GRAY));
        Player human = sender instanceof Player ? (Player) sender : null;
        TextComponent.Builder cb = text();
        cb.append(text("Channels", WHITE, ITALIC));
        for (Channel channel : plugin.getChannels()) {
            if (human != null && !channel.canJoin(human.getUniqueId())) continue;
            if (SQLSetting.getBoolean(null, channel.getKey(), "MutePlayers", false)) continue;
            cb.append(space());
            TextColor channelColor = SQLSetting.getTextColor(human != null ? human.getUniqueId() : null,
                                                             channel.getKey(), "ChannelColor", WHITE);
            Component tooltip;
            if (channel instanceof PrivateChannel) {
                tooltip = join(separator(newline()), new Component[] {
                        text("/msg [user] [message]", channelColor),
                        text("Whisper someone", GRAY),
                    });
            } else {
                tooltip = join(separator(newline()), new Component[] {
                        text("/" + channel.getAlias() + " [message]", channelColor),
                        text("Talk in " + channel.getTitle(), GRAY),
                    });
            }
            cb.append(text()
                      .content("[" + channel.getTag() + "]").color(channelColor)
                      .clickEvent(suggestCommand("/" + channel.getTag().toLowerCase() + " "))
                      .hoverEvent(showText(tooltip))
                      .build());
        }
        TextColor clickColor = GREEN;
        TextColor titleColor = WHITE;
        TextColor descColor = GRAY;
        cb.append(newline());
        cb.append(join(separator(space()), new Component[] {
                    text("Community", GRAY, ITALIC),
                    text().content("[List]").color(clickColor)
                    .hoverEvent(join(separator(newline()),
                                     text("/ch list", titleColor),
                                     text("Channel List", descColor))
                                .asHoverEvent())
                    .clickEvent(runCommand("/ch list")).build(),
                    text().content("[Who]").color(clickColor)
                    .hoverEvent(join(separator(newline()),
                                     text("/ch who <channel>", titleColor),
                                     text("Channel User List", descColor))
                                .asHoverEvent())
                    .clickEvent(suggestCommand("/ch who ")).build(),
                    text().content("[Reply]").color(clickColor)
                    .hoverEvent(join(separator(newline()),
                                     text("/r [message]", titleColor),
                                     text("Reply to a private message", descColor),
                                     text("or focus on reply", descColor))
                                .asHoverEvent())
                    .clickEvent(suggestCommand("/r ")).build(),
                    text().content("[Party]").color(clickColor)
                    .hoverEvent(join(separator(newline()),
                                     text("/party [name]", titleColor),
                                     text("Select a named party", descColor),
                                     text("or show current party", descColor))
                                .asHoverEvent())
                    .clickEvent(suggestCommand("/party ")).build(),
                }));
        clickColor = YELLOW;
        cb.append(newline());
        cb.append(join(separator(space()), new Component[] {
                    text("Settings", GRAY, ITALIC),
                    text().content("[Set]").color(clickColor)
                    .hoverEvent(join(separator(newline()),
                                     text("/ch set <channel>", titleColor),
                                     text("Channel Settings", descColor))
                                .asHoverEvent())
                    .clickEvent(runCommand("/ch set")).build(),
                    text().content("[Ignore]").color(clickColor)
                    .hoverEvent(join(separator(newline()),
                                     text("/ignore [player]", titleColor),
                                     text("(Un)ignore a player", descColor),
                                     text("or list ignored players", descColor))
                                .asHoverEvent())
                    .clickEvent(suggestCommand("/ignore ")).build(),
                }));
        sender.sendMessage(cb.build());
    }

    /**
     * Create one line per option.
     */
    Component makeOptionComponent(Player player, Channel channel, Option option) {
        UUID uuid = player.getUniqueId();
        String alias = channel != null ? channel.getAlias() : "_";
        String chkey = channel != null ? channel.getKey() : null;
        TextComponent.Builder cb = text();
        cb.append(text().content(option.getDisplayName()).color(GRAY).decorate(ITALIC)
                  .hoverEvent(join(separator(newline()), new Component[] {
                              text(option.getDisplayName(), WHITE),
                              text(option.getDescription(), GRAY),
                          })));
        String current = SQLSetting.getString(uuid, chkey, option.getKey(), option.getDefaultValue());
        for (Option.State state: option.getStates()) {
            cb.append(space());
            boolean active = Objects.equals(current, state.getValue());
            Component c;
            if (active) {
                Component tooltip = join(separator(newline()), new Component[] {
                        text(state.getDisplayName(), state.getActiveColor()),
                        text(state.getDescription(), WHITE),
                    });
                c = text().content("[" + state.getDisplayName() + "]").color(state.getActiveColor())
                    .hoverEvent(showText(tooltip))
                    .clickEvent(runCommand("/ch set " + alias + " " + option.getKey() + " " + state.getValue()))
                    .build();
            } else {
                Component tooltip = join(separator(newline()), new Component[] {
                        text(state.getDisplayName(), state.getColor()),
                        text(state.getDescription(), WHITE),
                    });
                c = text().content(state.getDisplayName()).color(state.getColor())
                    .hoverEvent(showText(tooltip))
                    .clickEvent(runCommand("/ch set " + alias + " " + option.getKey() + " " + state.getValue()))
                    .build();
            }
            cb.append(c);
        }
        return cb.build();
    }

    private void listChannelsForSettings(Player player) {
        if (player == null) return;
        Msg.info(player, text("Settings Menu", WHITE));
        List<Component> lines = new ArrayList<>();
        for (Option option : globalOptions) {
            lines.add(makeOptionComponent(player, null, option));
        }
        TextComponent.Builder cb = text();
        cb.append(text("Channel Settings", WHITE, ITALIC));
        for (Channel channel: plugin.getChannels()) {
            if (!channel.canJoin(player.getUniqueId())) continue;
            cb.append(space());
            TextColor channelColor = SQLSetting.getTextColor(player.getUniqueId(), channel.getKey(), "ChannelColor", WHITE);
            cb.append(text().content("[" + channel.getTag() + "]").color(channelColor)
                      .hoverEvent(showText(text(channel.getTitle(), channelColor)))
                      .clickEvent(runCommand("/ch set " + channel.getAlias()))
                      .build());
        }
        lines.add(cb.build());
        player.sendMessage(join(separator(newline()), lines));
    }

    private void showSettingsMenu(Player player, Channel channel) {
        UUID uuid = player.getUniqueId();
        String key = channel.getKey();
        Msg.info(player, text("Settings", WHITE));
        List<Component> lines = new ArrayList<>();
        final TextColor kcolor = TextColor.color(0xA0A0A0);
        for (Option option: channel.getOptions()) {
            lines.add(makeOptionComponent(player, channel, option));
        }
        List<Component> components = new ArrayList<>();
        components.add(text("Server Default", kcolor, ITALIC));
        components.add(text().content("[Reset]").color(DARK_RED)
                       .hoverEvent(showText(text("Reset to channel defaults", DARK_RED)))
                       .clickEvent(runCommand("/ch set " + channel.getAlias() + " reset"))
                       .build());
        if (player.hasPermission("chat.admin")) {
            TextColor color = TextColor.color(0xFF0000);
            components.add(text().content("[Defaults]").color(color)
                           .hoverEvent(showText(join(separator(newline()), new Component[] {
                                           text("Overwrite all public"),
                                           text("channel defaults with"),
                                           text("your settings"),
                                       }).color(color)))
                           .clickEvent(runCommand("/chadm setdefaults " + channel.getAlias()))
                           .build());
        }
        lines.add(join(separator(space()), components));
        lines.add(join(separator(space()),
                       text("Example", kcolor, ITALIC),
                       channel.makeExampleOutput(player)));
        channel.playSoundCue(player);
        for (Component line : lines) {
            player.sendMessage(line);
        }
    }

    private void listChannels(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        Msg.info(sender, text("Channel List", WHITE));
        List<Component> lines = new ArrayList<>();
        for (Channel channel: plugin.getChannels()) {
            List<Object> json = new ArrayList<>();
            if (player != null && !channel.canJoin(player.getUniqueId())) continue;
            TextComponent.Builder cb = text().content(" ");
            if (player == null || channel.isJoined(player.getUniqueId())) {
                cb.append(Mytems.ON.asComponent()
                          .hoverEvent(showText(text("Leave " + channel.getTitle(), GREEN)))
                          .clickEvent(runCommand("/ch leave " + channel.getAlias())));
            } else {
                cb.append(Mytems.OFF.asComponent()
                          .hoverEvent(showText(text("Join " + channel.getTitle(), RED)))
                          .clickEvent(runCommand("/ch join " + channel.getAlias())));
            }
            cb.append(space());
            cb.append(Mytems.MONKEY_WRENCH.asComponent()
                      .hoverEvent(showText(join(separator(newline()),
                                                text("/ch set " + channel.getAlias(), YELLOW),
                                                text("Open channel settings", GRAY))))
                      .clickEvent(runCommand("/ch set " + channel.getAlias())));
            cb.append(space());
            TextColor channelColor = player != null
                ? SQLSetting.getTextColor(player.getUniqueId(), channel.getKey(), "ChannelColor", WHITE)
                : SQLSetting.getTextColor(null, channel.getKey(), "ChannelColor", WHITE);
            Component tooltip = join(separator(newline()), new Component[] {
                    text("/" + channel.getTag().toLowerCase() + " [message]", channelColor),
                    text("Focus " + channel.getTitle(), GRAY),
                });
            cb.append(text().content(channel.getTitle()).color(channelColor)
                      .hoverEvent(showText(tooltip))
                      .clickEvent(runCommand("/" + channel.getTag().toLowerCase()))
                      .build());
            if (player != null && channel.equals(plugin.getFocusChannel(player.getUniqueId()))) {
                cb.append(text().content("*").color(channelColor)
                          .hoverEvent(showText(text("You are focusing " + channel.getTitle(), channelColor)))
                          .build());
            }
            cb.append(text(" - ", DARK_GRAY));
            cb.append(text(channel.getDescription(), GRAY));
            lines.add(cb.build());
        }
        sender.sendMessage(join(separator(newline()), lines));
        if (sender instanceof Player) {
            PluginPlayerEvent.Name.LIST_CHAT_CHANNELS.call(plugin, player);
        }
    }

    public void listIgnores(Player player) {
        UUID uuid = player.getUniqueId();
        TextComponent.Builder cb = text();
        List<Component> ignores = new ArrayList<>();
        for (UUID ign: SQLIgnore.listIgnores(uuid)) {
            Chatter chatter = plugin.findOfflinePlayer(ign);
            if (chatter == null) continue;
            ignores.add(text().content(chatter.getName()).color(RED)
                        .hoverEvent(showText(text("Click to unignore " + chatter.getName(), RED)))
                        .clickEvent(runCommand("/ch ignore " + chatter.getName()))
                        .build());
        }
        if (ignores.isEmpty()) {
            Msg.warn(player, text("Not ignoring anyone", RED));
            return;
        }
        player.sendMessage(join(separator(newline()), new Component[] {
                    text("Ignoring " + ignores.size() + " players: ", WHITE, ITALIC),
                    join(separator(text(", ", DARK_GRAY)), ignores),
                }));
    }

    public void toggleIgnore(Player player, String name) {
        final Chatter ignoree = plugin.findOfflinePlayer(name);
        if (ignoree == null) {
            Msg.warn(player, text("Player not found: " + name, RED));
            return;
        }
        final Player target = Bukkit.getPlayer(ignoree.getUuid());
        if (SQLIgnore.doesIgnore(player.getUniqueId(), ignoree.getUuid())) {
            SQLIgnore.ignore(player.getUniqueId(), ignoree.getUuid(), false);
            Msg.info(player, text("No longer ignoring " + ignoree.getName(), WHITE));
            plugin.getConnectListener().broadcastMeta(ConnectListener.META_IGNORE, player.getUniqueId());
            if (target != null) {
                player.showPlayer(plugin, target);
            }
        } else {
            SQLIgnore.ignore(player.getUniqueId(), ignoree.getUuid(), true);
            Msg.info(player, text("Ignoring " + ignoree.getName(), YELLOW));
            plugin.getConnectListener().broadcastMeta(ConnectListener.META_IGNORE, player.getUniqueId());
            if (target != null) {
                player.hidePlayer(plugin, target);
            }
        }
    }
}
