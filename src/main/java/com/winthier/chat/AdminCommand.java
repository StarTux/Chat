package com.winthier.chat;

import com.winthier.chat.channel.Channel;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.UUID;
import java.util.regex.Matcher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AdminCommand extends AbstractChatCommand {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("reload") && args.length == 1) {
            SQLDB.reload();
            ChatPlugin.getInstance().reloadConfig();
            ChatPlugin.getInstance().loadChannels();
            Msg.info(sender, "Configs reloaded");
        } else if (firstArg.equals("debug") && args.length == 1) {
            boolean v = !ChatPlugin.getInstance().isDebugMode();
            ChatPlugin.getInstance().setDebugMode(v);
            Msg.info(sender, "Debug mode %s", (v ? "enabled" : "disabled"));
        } else if (firstArg.equals("testfilter") && args.length >= 2) {
            StringBuilder sb = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; ++i) {
                sb.append(" ").append(args[i]);
            }
            MessageFilter filter = new MessageFilter(player == null ? null : player.getUniqueId(), sb.toString());
            Msg.send(sender, "&8Input &r%s", filter.toString());
            filter.findURLs();
            Msg.send(sender, "&8URLs &r%s", filter.toString());
            filter.colorize();
            Msg.send(sender, "&8Color &r%s", filter.toString());
        } else if (firstArg.equals("listdefaults") && args.length == 2) {
            String channelArg = args[1];
            Channel channel = ChatPlugin.getInstance().findChannel(channelArg);
            if (channel == null) {
                Msg.warn(sender, "Channel not found: %s", channelArg);
                return true;
            }
            for (SQLSetting sett: SQLSetting.getDefaultSettings().getMap().values()) {
                if (!channel.getKey().equals(sett.getChannel())) continue;
                Msg.send(sender, "%s/%s = %s", sett.getChannel(), sett.getSettingKey(), sett.getSettingValue());
            }
        } else if (firstArg.equals("setdefault") && args.length == 4) {
            String channelArg = args[1];
            String key = args[2];
            String value = args[3];
            Channel channel = ChatPlugin.getInstance().findChannel(channelArg);
            if (channel == null) {
                Msg.warn(sender, "Channel not found: %s", channelArg);
                return true;
            }
            SQLSetting.set(null, channel.getKey(), key, value);
            Msg.info(sender, "Did set %s/%s = '%s'", channel.getKey(), key, value);
        } else if (firstArg.equals("announce") && args.length >= 3) {
            String channelArg = args[1];
            Channel channel = ChatPlugin.getInstance().findChannel(channelArg);
            if (channel == null) {
                Msg.warn(sender, "Channel not found: %s", channelArg);
                return true;
            }
            StringBuilder sb = new StringBuilder(args[2]);
            for (int i = 3; i < args.length; ++i) {
                sb.append(" ").append(args[i]);
            }
            channel.announce(sb.toString());
            Msg.info(sender, "Announcement sent to %s", channel.getTitle());
        } else if (firstArg.equals("initdb") && args.length == 1) {
            if (player != null) {
                player.sendMessage("Only for console!");
                return true;
            }
            ChatPlugin.getInstance().initializeDatabase();
            Msg.info(sender, "Database initialized.");
        } else if (firstArg.equals("spy")) {
            if (player == null) {
                sender.sendMessage("Player expected");
                return true;
            }
            final UUID uuid = player.getUniqueId();
            final ChatPlugin plugin = ChatPlugin.getInstance();
            if (plugin.getChatSpies().contains(uuid)) {
                plugin.getChatSpies().remove(uuid);
                Msg.send(player, "&eChat spy disabled.");
            } else {
                plugin.getChatSpies().add(uuid);
                Msg.send(player, "&eChat spy enabled.");
            }
        } else if (firstArg.equals("prefix")) {
            if (args.length < 2) return false;
            Chatter target = ChatPlugin.getInstance().findOfflinePlayer(args[1]);
            if (target == null) {
                Msg.send(player, "&cPlayer not found: " + args[1]);
                return true;
            }
            if (args.length == 2) {
                SQLSetting setting = SQLSetting.find(target.getUuid(), null, "prefix");
                Msg.send(sender, "Prefix of " + target.getName() + ": " + setting.getSettingValue());
                return true;
            }
            if (args.length == 3 && args[2].equals("-reset")) {
                SQLSetting.set(target.getUuid(), null, "prefix", null);
                Msg.send(sender, "Prefix of " + target.getName() + " reset.");
                return true;
            }
            StringBuilder sb = new StringBuilder(args[2]);
            for (int i = 3; i < args.length; i += 1) {
                sb.append(" ").append(args[i]);
            }
            String prefix = sb.toString();
            SQLSetting.set(target.getUuid(), null, "prefix", prefix);
            Msg.send(sender, "Prefix of " + target.getName() + "changed to " + prefix);
            return true;
        }
        return true;
    }

    void usage(CommandSender sender) {
        Msg.send(sender, "&e/chadm Reload &7- &rReload configs and databases");
        Msg.send(sender, "&e/chadm Debug &7- &rToggle debug mode");
        Msg.send(sender, "&e/chadm TestFilter &o<Message> &7- &rTest all chat filters");
        Msg.send(sender, "&e/chadm TestPattern &o<PatternName> <Message> &7- &rTest a pattern");
        Msg.send(sender, "&e/chadm ListDefaults &o<Channel>&7- &rView channel default settings");
        Msg.send(sender, "&e/chadm SetDefault &o<Channel> <Key> <Value> &7- &rChange channel default setting");
        Msg.send(sender, "&e/chadm Announce &o<Channel> <Message> &7- &rMake an announcement");
        Msg.send(sender, "&e/chadm InitDB &7- &rInitialize the Database");
        Msg.send(sender, "&e/chadm Spy &7- &rToggle chat spy");
        Msg.send(sender, "&e/chadm Prefix &o<Player> &e-reset&o|[Prefix] &7- &rChange or reset player prefix");
    }
}
