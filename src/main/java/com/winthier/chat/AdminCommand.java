package com.winthier.chat;

import com.winthier.chat.MessageFilter;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLPattern;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.regex.Matcher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand extends AbstractChatCommand {
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
            boolean v = !ChatPlugin.getInstance().debugMode;
            ChatPlugin.getInstance().debugMode = v;
            Msg.info(sender, "Debug mode %s", (v ? "enabled": "disabled"));
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
            filter.filterSpam();
            Msg.send(sender, "&8Spam &r%s", filter.toString());
            filter.filterLanguage();
            Msg.send(sender, "&8Language &r%s", filter.toString());
            if (player != null) {
                MessageFilter filter2 = new MessageFilter(player.getUniqueId(), sb.toString());
                filter2.process();
                Msg.raw(player, filter2.getJson());
                Msg.raw(player, filter2.getLanguageFilterJson());
            }
        } else if (firstArg.equals("testpattern") && args.length >= 3) {
            StringBuilder sb = new StringBuilder(args[2]);
            for (int i = 3; i < args.length; ++i) {
                sb.append(" ").append(args[i]);
            }
            String category = args[1];
            int matches = 0, count = 0;
            for (SQLPattern pat: SQLPattern.find(category)) {
                Matcher mat = pat.getMatcher(sb.toString());
                count += 1;
                if (mat.find()) {
                    Msg.send(sender, "&8Id(&r%d&8), Regex(&r%s&8), Match(&r%s&8)", pat.getId(), pat.getRegex(), mat.group());
                    matches += 1;
                }
            }
            Msg.send(sender, "%d&8/&r%d&8 Matches", matches, count);
        } else if (firstArg.equals("initdb")) {
            ChatPlugin.getInstance().initializeDatabase();
            Msg.send(sender, "Database initialized");
        } else if (firstArg.equals("listdefaults") && args.length == 2) {
            String channelArg = args[1];
            Channel channel = ChatPlugin.getInstance().findChannel(channelArg);
            if (channel == null) {
                Msg.warn(sender, "Channel not found: %s", channelArg);
                return true;
            }
            for (SQLSetting sett: SQLSetting.getDefaultSettings().map.values()) {
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
            channel.announce(sender.getName(), sb.toString());
            Msg.info(sender, "Announcement sent to %s", channel.getTitle());
        }
        return true;
    }

    void usage(CommandSender sender) {
        Msg.send(sender, "&e/chadm Debug &7- &rReload configs and databases");
        Msg.send(sender, "&e/chadm TestFilter &o<Message> &7- &rTest all chat filters");
        Msg.send(sender, "&e/chadm TestPattern &o<PatternName> <Message> &7- &rTest a pattern");
        Msg.send(sender, "&e/chadm InitDB &7- &rInitialize databases");
        Msg.send(sender, "&e/chadm ListDefaults &o<Channel>&7- &rView channel default settings");
        Msg.send(sender, "&e/chadm SetDefault &o<Channel> <Key> <Value> &7- &rChange channel default setting");
        Msg.send(sender, "&e/chadm Announce &o<Channel> <Message> &7- &rMake an announcement");
    }
}
