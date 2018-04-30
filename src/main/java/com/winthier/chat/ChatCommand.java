package com.winthier.chat;

import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.CommandResponder;
import com.winthier.chat.channel.Option;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.channel.PrivateChannel;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ChatCommand extends AbstractChatCommand {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 0) {
            showMenu(player);
            return true;
        }
        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("set")) {
            if (player == null) return false;
            if (args.length == 1) {
                listChannelsForSettings(player);
                return true;
            }
            setOption(player, Arrays.copyOfRange(args, 1, args.length));
        } else if (firstArg.equals("list") && args.length == 1) {
            listChannels(player);
        } else if (firstArg.equals("join") && args.length == 2) {
            Channel channel = ChatPlugin.getInstance().findChannel(args[1]);
            if (channel == null || !channel.hasPermission(player)) return false;
            channel.joinChannel(player.getUniqueId());
            listChannels(player);
        } else if (firstArg.equals("leave") && args.length == 2) {
            Channel channel = ChatPlugin.getInstance().findChannel(args[1]);
            if (channel == null || !channel.hasPermission(player)) return false;
            channel.leaveChannel(player.getUniqueId());
            listChannels(player);
        } else if (firstArg.equals("ignore")) {
            if (args.length == 1) {
                listIgnores(player);
            } else if (args.length == 2) {
                toggleIgnore(player, args[1]);
            } else {
                return false;
            }
        } else if (firstArg.equals("who")) {
            Channel channel;
            if (args.length == 1) {
                if (player == null) return false;
                channel = ChatPlugin.getInstance().getFocusChannel(player.getUniqueId());
            } else if (args.length == 2) {
                channel = ChatPlugin.getInstance().findChannel(args[1]);
            } else {
                return false;
            }
            if (channel == null || !channel.hasPermission(player)) return true;
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Chatter chatter: channel.getOnlineMembers()) {
                sb.append(" ").append(chatter.getName());
                count += 1;
            }
            Msg.send(sender, "&oChannel &a%s &r(%d):%s", channel.getTitle(), count, sb.toString());
        } else if (firstArg.equals("say")) {
            if (args.length < 3) return false;
            CommandResponder cmd = ChatPlugin.getInstance().findCommand(args[1]);
            if (cmd == null) return false;
            if (player != null && !cmd.hasPermission(player)) return false;
            StringBuilder sb = new StringBuilder(args[2]);
            for (int i = 3; i < args.length; i += 1) sb.append(" ").append(args[i]);
            String msg = sb.toString();
            if (player != null && !ChatPlayerTalkEvent.call(player, cmd.getChannel(), msg)) return false;
            cmd.playerDidUseCommand(new PlayerCommandContext(player, args[1], msg));
        } else if (args.length == 1) {
            if (player == null) return false;
            Channel channel = ChatPlugin.getInstance().findChannel(firstArg);
            if (channel == null || !channel.hasPermission(player)) return false;
            channel.joinChannel(player.getUniqueId());
            channel.setFocusChannel(player.getUniqueId());
            Msg.info(player, "Now focusing %s&r.", channel.getTitle());
            listChannels(player);
        }
        return true;
    }

    private boolean isKeyValuePairValid(List<Option> options, String key, String value) {
        for (Option option: options) {
            if (key.equals(option.key)) {
                for (Option.State state: option.states) {
                    if (value.equals(state.value)) {
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
        Channel channel = ChatPlugin.getInstance().findChannel(args[0]);
        if (channel == null || !channel.hasPermission(player)) return;
        if (args.length == 2 && args[1].equals("reset")) {
            for (Option option: channel.getOptions()) {
                SQLSetting.set(uuid, channel.getKey(), option.key, null);
            }
            Msg.info(player, "&aSettings reset to default");
        } else if (args.length == 3) {
            String key = args[1];
            String value = args[2];
            if (!isKeyValuePairValid(channel.getOptions(), key, value)) return;
            SQLSetting.set(uuid, channel.getKey(), key, value);
            Msg.info(player, "&aSettings updated");
        }
        showSettingsMenu(player, channel);
    }

    void showMenu(Player player) {
        if (player == null) return;
        Msg.info(player, "&3Menu");
        List<Object> json = new ArrayList<>();
        json.add(Msg.format("&oChannels"));
        for (Channel channel: ChatPlugin.getInstance().getChannels()) {
            if (!channel.hasPermission(player)) continue;
            json.add(" ");
            ChatColor channelColor = SQLSetting.getChatColor(player.getUniqueId(), channel.getKey(), "ChannelColor", ChatColor.WHITE);
            if (channel instanceof PrivateChannel) {
                json.add(Msg.button(channelColor, "[" + channel.getTag() + "]", "/msg [user] [message]\n&5&oWhisper someone.", "/" + channel.getTag().toLowerCase() + " "));
            } else {
                json.add(Msg.button(channelColor, "[" + channel.getTag() + "]", "/" + channel.getTag().toLowerCase() + " [message]\n&5&oTalk in " + channel.getTitle(), "/" + channel.getTag().toLowerCase() + " "));
            }
        }
        Msg.raw(player, json);
        json.clear();
        json.add(Msg.format("&oCommands "));
        json.add(Msg.button(ChatColor.GOLD, "&6[List]", "/ch list\n&5&oChannel List.", "/ch list"));
        json.add(" ");
        json.add(Msg.button(ChatColor.BLUE, "&9[Who]", "/ch who [channel]\n&5&oUser List", "/ch who "));
        json.add(" ");
        json.add(Msg.button(ChatColor.GRAY, "&7[Set]", "/ch set\n&5&oChange channel preferences.", "/ch set"));
        json.add(" ");
        json.add(Msg.button(ChatColor.LIGHT_PURPLE, "&d[Reply]", "/reply <msg>\n&5&oReply to private messages.", "/reply "));
        json.add(" ");
        json.add(Msg.button(ChatColor.GREEN, "&a[Party]", "/party [name]\n&5&oSelect a named party,\n&5&oshow current party.", "/party "));
        json.add(" ");
        json.add(Msg.button(ChatColor.RED, "&c[Ignore]", "/ignore [user]\n&5&oIgnore, unignore, or\n&5&olist ignored users.", "/ignore "));
        Msg.raw(player, json);
    }

    void listChannelsForSettings(Player player) {
        if (player == null) return;
        Msg.info(player, "&3Menu");
        List<Object> json = new ArrayList<>();
        json.add(Msg.format("&oChannel Settings"));
        for (Channel channel: ChatPlugin.getInstance().getChannels()) {
            if (!channel.hasPermission(player)) continue;
            json.add(" ");
            json.add(Msg.button("&r[" + SQLSetting.getChatColor(player.getUniqueId(), channel.getKey(), "ChannelColor", ChatColor.WHITE) + channel.getTag() + "&r]", channel.getTitle(), "/ch set " + channel.getAlias()));
        }
        Msg.raw(player, json);
    }

    void showSettingsMenu(Player player, Channel channel) {
        UUID uuid = player.getUniqueId();
        Msg.info(player, SQLSetting.getChatColor(uuid, channel.getKey(), "ChannelColor", ChatColor.WHITE) + channel.getTitle() + " Settings");
        List<Object> json = new ArrayList<>();
        for (Option option: channel.getOptions()) {
            json.clear();
            json.add(" ");
            json.add(Msg.button(ChatColor.WHITE, "&o" + option.displayName, option.displayName + "\n&5" + option.description, null));
            for (Option.State state: option.states) {
                json.add(" ");
                String current = SQLSetting.getString(uuid, channel.getKey(), option.key, option.defaultValue);
                boolean active = false;
                if (current != null && current.equals(state.value)) active = true;
                if (active) {
                    json.add(Msg.button("&r[" + state.activeColor + state.displayName + "&r]", state.description, "/ch set " + channel.getAlias() + " " + option.key + " " + state.value));
                } else {
                    json.add(Msg.button(state.color + state.displayName, state.description, "/ch set " + channel.getAlias() + " " + option.key + " " + state.value));
                }
            }
            Msg.raw(player, json);
        }
        channel.exampleOutput(player);
        json.clear();
        json.add(" ");
        json.add(Msg.button(ChatColor.DARK_RED, "&r[&4Reset&r]", "&4Reset to channel defaults.", "/ch set " + channel.getAlias() + " reset"));
        Msg.raw(player, json);
    }

    void listChannels(Player player) {
        if (player == null) return;
        Msg.info(player, "Channel List");
        for (Channel channel: ChatPlugin.getInstance().getChannels()) {
            List<Object> json = new ArrayList<>();
            if (!channel.hasPermission(player)) continue;
            json.add(" ");
            if (channel.isJoined(player.getUniqueId())) {
                json.add(Msg.button(ChatColor.GREEN, "x", "Leave " + channel.getTitle(), "/ch leave " + channel.getAlias()));
            } else {
                json.add(Msg.button(ChatColor.RED, "o", "Join " + channel.getTitle(), "/ch join " + channel.getAlias()));
            }
            json.add(" ");
            ChatColor channelColor = SQLSetting.getChatColor(player.getUniqueId(), channel.getKey(), "ChannelColor", ChatColor.WHITE);
            if (channel instanceof PrivateChannel) {
                json.add(Msg.button(channelColor, channel.getTitle(), "/msg [user] [message]\n&5&oWhisper someone.", "/" + channel.getTag().toLowerCase() + " "));
            } else {
                json.add(Msg.button(channelColor, channel.getTitle(), "/" + channel.getTag().toLowerCase() + " [message]\n&5&oTalk in " + channel.getTitle(), "/" + channel.getTag().toLowerCase() + " "));
            }
            if (channel.equals(ChatPlugin.getInstance().getFocusChannel(player.getUniqueId()))) {
                json.add(Msg.button(channelColor, "*", "You are focusing " + channel.getTitle(), null));
            }
            json.add(Msg.button(ChatColor.DARK_GRAY, " - ", null, null));
            json.add(Msg.button(ChatColor.GRAY, channel.getDescription(), null, null));
            Msg.raw(player, json);
        }
    }

    void listIgnores(Player player) {
        UUID uuid = player.getUniqueId();
        List<Object> json = new ArrayList<>();
        json.add(Msg.format("&oIgnoring"));
        int count = 0;
        for (UUID ign: SQLIgnore.listIgnores(uuid)) {
            Chatter chatter = ChatPlugin.getInstance().findOfflinePlayer(ign);
            if (chatter == null) continue;
            count += 1;
            json.add(" ");
            json.add(Msg.button(ChatColor.RED, chatter.getName(), "Click to unignore " + chatter.getName(), "/ch ignore " + chatter.getName()));
        }
        if (count == 0) {
            json.add(Msg.format(" &anobody"));
        }
        Msg.raw(player, json);
    }

    void toggleIgnore(Player player, String name) {
        Chatter ignoree = ChatPlugin.getInstance().findOfflinePlayer(name);
        if (ignoree == null) {
            Msg.warn(player, "Player not found: %s.", name);
            return;
        }
        if (SQLIgnore.doesIgnore(player.getUniqueId(), ignoree.getUuid())) {
            SQLIgnore.ignore(player.getUniqueId(), ignoree.getUuid(), false);
            Msg.info(player, "No longer ignoring %s.", ignoree.getName());
        } else {
            SQLIgnore.ignore(player.getUniqueId(), ignoree.getUuid(), true);
            Msg.info(player, "Ignoring %s.", ignoree.getName());
        }
        listIgnores(player);
    }
}
