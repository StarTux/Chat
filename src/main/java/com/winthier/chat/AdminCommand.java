package com.winthier.chat;

import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.Option;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static com.winthier.chat.Backlog.backlog;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class AdminCommand extends AbstractChatCommand {
    private final ChatPlugin plugin;
    private CommandNode rootNode;

    public AdminCommand enable() {
        rootNode = new CommandNode("chadm");
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload configs and databases")
            .senderCaller(this::reload);
        rootNode.addChild("debug").denyTabCompletion()
            .description("Toggle debug mode")
            .senderCaller(this::debug);
        rootNode.addChild("listdefaults").arguments("<channel>")
            .description("View channel default settings")
            .senderCaller(this::listDefaults);
        rootNode.addChild("setdefault").arguments("<channel> <key> <value>")
            .description("Set channel default settings")
            .senderCaller(this::setDefault);
        rootNode.addChild("announce").arguments("<channel> <message...>")
            .description("Make an announcement")
            .senderCaller(this::announce);
        rootNode.addChild("initdb").denyTabCompletion()
            .description("Initialize the database")
            .senderCaller(this::initDb);
        rootNode.addChild("setdefaults").arguments("<channel>")
            .description("Set all channel defaults")
            .playerCaller(this::setDefaults);
        rootNode.addChild("badword").arguments("<expression>")
            .description("Check expression against bad word filter")
            .senderCaller(this::badWord);
        rootNode.addChild("sendbacklog").arguments("<player>")
            .description("Send someone their backlog")
            .completers(CommandArgCompleter.NULL)
            .senderCaller(this::sendBacklog);
        plugin.getCommand("chatadmin").setExecutor(this);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return rootNode.call(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> list = rootNode.complete(sender, command, label, args);
        return list != null ? list : super.onTabComplete(sender, command, label, args);
    }

    boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        SQLDB.reload();
        plugin.reloadConfig();
        plugin.unloadChannels();
        plugin.loadChannels();
        Msg.info(sender, text("Configs reloaded", YELLOW));
        return true;
    }

    boolean debug(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        boolean v = !plugin.isDebugMode();
        plugin.setDebugMode(v);
        Msg.info(sender, text("Debug mode " + (v ? "enabled" : "disabled"), YELLOW));
        return true;
    }

    boolean listDefaults(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String channelArg = args[0];
        Channel channel = plugin.findChannel(channelArg);
        if (channel == null) {
            Msg.warn(sender, text("Channel not found: " + channelArg, RED));
            return true;
        }
        List<Component> lines = new ArrayList<>();
        for (SQLSetting sett: SQLSetting.getDefaultSettings().getMap().values()) {
            if (!channel.getKey().equals(sett.getChannel())) continue;
            lines.add(join(separator(newline()), text(sett.getChannel(), GRAY),
                           text(".", DARK_GRAY),
                           text(sett.getSettingKey(), GRAY),
                           text(" = ", DARK_GRAY),
                           text(sett.getSettingValue(), YELLOW)));
        }
        sender.sendMessage(join(separator(newline()), lines));
        return true;
    }

    boolean setDefault(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        String channelArg = args[0];
        String key = args[1];
        String value = args[2];
        Channel channel = plugin.findChannel(channelArg);
        if (channel == null) {
            Msg.warn(sender, text("Channel not found: " + channelArg, RED));
            return true;
        }
        SQLSetting.set(null, channel.getKey(), key, value);
        Msg.info(sender, text("Did set " + channel.getKey() + "." + key + " = " + value, YELLOW));
        return true;
    }

    boolean setDefaults(Player player, String[] args) {
        if (args.length != 1) return false;
        Channel channel = plugin.findChannel(args[0]);
        if (channel == null) throw new CommandWarn("Channel not found: " + args[0]);
        UUID uuid = player.getUniqueId();
        for (Option option : channel.getOptions()) {
            SQLSetting s = SQLSetting.find(uuid, channel.getKey(), option.getKey());
            if (s == null || s.getSettingValue() == null) {
                s = SQLSetting.find(null, channel.getKey(), option.getKey());
            }
            String value = s != null && s.getSettingValue() != null
                ? s.getSettingValue()
                : option.getDefaultValue();
            SQLSetting.set(null, channel.getKey(), option.getKey(), value);
        }
        Msg.info(player, text("Channel defaults updated: " + channel.getTitle(), GREEN));
        return true;
    }

    boolean announce(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        String channelArg = args[0];
        Channel channel = plugin.findChannel(channelArg);
        if (channel == null) {
            Msg.warn(sender, text("Channel not found: " + channelArg, RED));
            return true;
        }
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        channel.announce(text(msg));
        Msg.info(sender, text("Announcement sent to " + channel.getTitle(), YELLOW));
        return true;
    }

    boolean initDb(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.initializeDatabase();
        Msg.info(sender, text("Database initialized", YELLOW));
        return true;
    }

    protected boolean badWord(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        String expression = String.join(" ", args);
        int count = 0;
        for (var pattern : plugin.getBadWordList()) {
            if (pattern.matcher(expression).find()) {
                sender.sendMessage(text("Match: " + pattern.toString(), YELLOW));
                count += 1;
            }
        }
        sender.sendMessage(text("Total matches: " + count + ", Filtered: ", YELLOW)
                           .append(plugin.filterBadWords(text(expression))));
        return true;
    }

    private boolean sendBacklog(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final Player target = CommandArgCompleter.requirePlayer(args[0]);
        sender.sendMessage(text("Sending " + target.getName() + " their backlog...", YELLOW));
        backlog().sendBacklog(target);
        return true;
    }
}
