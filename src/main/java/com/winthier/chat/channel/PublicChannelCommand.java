package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import java.util.List;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;

public final class PublicChannelCommand extends Command implements PluginIdentifiableCommand {
    @Getter private final ChatPlugin plugin;
    private final PublicChannel channel;

    protected PublicChannelCommand(final PublicChannel channel) {
        super(channel.getAlias(),
              channel.getTitle() + " Chat Channel", // description
              "Usage: /" + channel.getAlias() + " <message>", // usageMessage
              channel.getAliases()); // aliases
        this.channel = channel;
        this.plugin = channel.getPlugin();
        setPermission(channel.permission);
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        String message = String.join(" ", args);
        if (sender instanceof Player) {
            Player player = (Player) sender;
            ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(player, channel, message);
            if (!event.call()) return true;
            channel.playerDidUseCommand(new PlayerCommandContext(player, alias, event.getMessage()));
            return true;
        } else if (sender instanceof ConsoleCommandSender) {
            channel.consoleDidUseCommand(message);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 0) return null;
        return plugin.completeChatArg(sender, args[args.length - 1]);
    }

    @Override
    public boolean testPermissionSilent(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        if (sender instanceof Player) {
            Player player = (Player) sender;
            return channel.canTalk(player.getUniqueId());
        }
        return false;
    }
}
