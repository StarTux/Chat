package com.winthier.chat;

import com.cavetale.core.command.CommandNode;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

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
        plugin.loadChannels();
        Msg.info(sender, Component.text("Configs reloaded", NamedTextColor.YELLOW));
        return true;
    }

    boolean debug(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        boolean v = !plugin.isDebugMode();
        plugin.setDebugMode(v);
        Msg.info(sender, Component.text("Debug mode " + (v ? "enabled" : "disabled"), NamedTextColor.YELLOW));
        return true;
    }

    boolean listDefaults(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        String channelArg = args[0];
        Channel channel = plugin.findChannel(channelArg);
        if (channel == null) {
            Msg.warn(sender, Component.text("Channel not found: " + channelArg, NamedTextColor.RED));
            return true;
        }
        List<Component> lines = new ArrayList<>();
        for (SQLSetting sett: SQLSetting.getDefaultSettings().getMap().values()) {
            if (!channel.getKey().equals(sett.getChannel())) continue;
            lines.add(TextComponent.ofChildren(Component.text(sett.getChannel(), NamedTextColor.GRAY),
                                               Component.text(".", NamedTextColor.DARK_GRAY),
                                               Component.text(sett.getSettingKey(), NamedTextColor.GRAY),
                                               Component.text(" = ", NamedTextColor.DARK_GRAY),
                                               Component.text(sett.getSettingValue(), NamedTextColor.YELLOW)));
        }
        sender.sendMessage(Component.join(Component.text("\n"), lines));
        return true;
    }

    boolean setDefault(CommandSender sender, String[] args) {
        if (args.length != 3) return false;
        String channelArg = args[0];
        String key = args[1];
        String value = args[2];
        Channel channel = plugin.findChannel(channelArg);
        if (channel == null) {
            Msg.warn(sender, Component.text("Channel not found: " + channelArg, NamedTextColor.RED));
            return true;
        }
        SQLSetting.set(null, channel.getKey(), key, value);
        Msg.info(sender, Component.text("Did set " + channel.getKey() + "." + key + " = " + value, NamedTextColor.YELLOW));
        return true;
    }

    boolean announce(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        String channelArg = args[0];
        Channel channel = plugin.findChannel(channelArg);
        if (channel == null) {
            Msg.warn(sender, Component.text("Channel not found: " + channelArg, NamedTextColor.RED));
            return true;
        }
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        channel.announce(Component.text(msg));
        Msg.info(sender, Component.text("Announcement sent to " + channel.getTitle(), NamedTextColor.YELLOW));
        return true;
    }

    boolean initDb(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.initializeDatabase();
        Msg.info(sender, Component.text("Database initialized", NamedTextColor.YELLOW));
        return true;
    }
}
