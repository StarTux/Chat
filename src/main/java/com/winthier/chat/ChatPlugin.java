package com.winthier.chat;

import com.cavetale.core.chat.ChannelChatEvent;
import com.cavetale.core.command.RemotePlayer;
import com.cavetale.core.connect.Connect;
import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.font.Emoji;
import com.cavetale.core.perm.Perm;
import com.cavetale.core.playercache.PlayerCache;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.CommandResponder;
import com.winthier.chat.channel.PartyChannel;
import com.winthier.chat.channel.PartyCommand;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.channel.PrivateChannel;
import com.winthier.chat.channel.PublicChannel;
import com.winthier.chat.channel.ReplyCommand;
import com.winthier.chat.channel.TeamChannel;
import com.winthier.chat.connect.ConnectListener;
import com.winthier.chat.event.ChatMessageEvent;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.sql.SQLDatabase;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ChatPlugin extends JavaPlugin {
    @Getter private static ChatPlugin instance;
    private final List<CommandResponder> commandResponders = new ArrayList<>();
    private final List<Channel> channels = new ArrayList<>();
    private final ConnectListener connectListener = new ConnectListener();
    private ChatListener chatListener = new ChatListener(this);
    private PrivateChannel privateChannel = null;
    private PartyChannel partyChannel = null;
    private TeamChannel teamChannel = null;
    private ChatCommand chatCommand = new ChatCommand(this);
    @Setter private boolean debugMode = false;
    private SQLDatabase db;
    @Setter private List<TextReplacementConfig> badWordConfigList = List.of();
    @Setter private List<Pattern> badWordList = List.of();
    private final Backlog backlog = new Backlog();

    @Override
    public void onLoad() {
        new CoreChat().register();
    }

    @Override
    public void onEnable() {
        instance = this;
        db = new SQLDatabase(this);
        db.registerTables(SQLDB.getDatabaseClasses());
        db.createAllTables();
        loadChannels();
        chatListener.enable();
        connectListener.enable();
        new AdminCommand(this).enable();
        chatCommand.enable();
        getCommand("join").setExecutor(new JoinLeaveCommand(true));
        getCommand("leave").setExecutor(new JoinLeaveCommand(false));
        getCommand("ignore").setExecutor(new IgnoreCommand());
        new ClearScreenCommand().enable();
        SQLDB.load();
        backlog.enable();
    }

    @Override
    public void onDisable() {
        unloadChannels();
        SQLDB.clear();
        instance = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandResponder cmd = findCommand(label);
        if (cmd == null) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player != null && !cmd.hasPermission(player)) return true;
        String msg = args.length != 0 ? String.join(" ", args) : null;
        if (player == null) {
            cmd.consoleDidUseCommand(msg);
        } else {
            ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(player, cmd.getChannel(), msg);
            if (!event.call()) return true;
            cmd.playerDidUseCommand(new PlayerCommandContext(player, label, event.getMessage()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        return completeChatArg(sender, arg);
    }

    public List<String> completePlayerName(String name) {
        List<String> result = new ArrayList<>();
        name = name.toLowerCase();
        for (Chatter chatter : getOnlinePlayers()) {
            if (name.isEmpty()) {
                result.add(chatter.getName());
            } else {
                String name2 = chatter.getName().toLowerCase();
                if (name2.contains(name)) {
                    result.add(chatter.getName());
                }
            }
        }
        if (Chatter.CONSOLE.name.contains(name)) {
            result.add(Chatter.CONSOLE.name);
        }
        return result;
    }

    public List<String> completeChatArg(CommandSender sender, String arg) {
        if (arg.isEmpty()) {
            return null;
        } else if (arg.startsWith(":") && sender.hasPermission("chat.emoji")) {
            return Emoji.tabComplete(arg);
        } else if (arg.startsWith("[") && sender.hasPermission("chat.item")) {
            return "item]".contains(arg.substring(1))
                ? Arrays.asList("[item]")
                : null;
        } else {
            return completePlayerName(arg);
        }
    }

    protected void loadChannels() {
        List<SQLChannel> rows = SQLChannel.fetch();
        Collections.sort(rows, (a, b) -> Integer.compare(a.getId(), b.getId()));
        for (SQLChannel row : rows) {
            Channel cmd;
            if ("pm".equals(row.getChannelKey())) {
                commandResponders.add(new ReplyCommand(this));
                privateChannel = new PrivateChannel(this, row);
                cmd = privateChannel;
            } else if ("party".equals(row.getChannelKey())) {
                commandResponders.add(new PartyCommand(this));
                partyChannel = new PartyChannel(this, row);
                cmd = partyChannel;
            } else if ("team".equals(row.getChannelKey())) {
                teamChannel = new TeamChannel(this, row);
                cmd = teamChannel;
            } else {
                cmd = new PublicChannel(this, row);
            }
            commandResponders.add(cmd);
            channels.add(cmd);
            cmd.registerCommand();
        }
        // Register the wildcard permission
        Map<String, Boolean> children = new LinkedHashMap<>();
        for (Channel channel : channels) {
            children.put(channel.getPermission(), true);
        }
        Permission perm = new Permission("chat.channel.*", "Access any channel", PermissionDefault.FALSE, children);
        Bukkit.getPluginManager().removePermission(perm.getName());
        Bukkit.getPluginManager().addPermission(perm);
    }

    protected void unloadChannels() {
        for (CommandResponder commandResponder : commandResponders) {
            commandResponder.unregisterCommand();
        }
        Bukkit.getPluginManager().removePermission("chat.channel.*");
        commandResponders.clear();
        channels.clear();
    }

    @SuppressWarnings("unchecked")
    protected void initializeDatabase() {
        YamlConfiguration config;
        InputStreamReader isr = new InputStreamReader(getResource("database.yml"));
        config = YamlConfiguration.loadConfiguration(isr);
        ConfigurationSection channelsSection = config.getConfigurationSection("channels");
        if (channelsSection != null) {
            for (String key: channelsSection.getKeys(false)) {
                ConfigurationSection channelSection = channelsSection.getConfigurationSection(key);
                SQLChannel chan = new SQLChannel();
                chan.setChannelKey(key);
                chan.setTag(channelSection.getString("tag"));
                chan.setTitle(channelSection.getString("title"));
                chan.setDescription(channelSection.getString("description"));
                List<String> aliases = channelSection.getStringList("aliases");
                StringBuilder sb = new StringBuilder(aliases.get(0));
                for (int i = 1; i < aliases.size(); ++i) {
                    sb.append(",").append(aliases.get(i));
                }
                chan.setAliases(sb.toString());
                if (channelSection.isSet("range")) {
                    chan.setLocalRange(channelSection.getInt("range"));
                }
                getDb().save(chan);
            }
        }
        for (Map<?, ?> tmpMap: config.getMapList("settings")) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) tmpMap;
            SQLSetting st = new SQLSetting((UUID) null, map.get("channel"),
                                           map.get("key"), (Object) map.get("value"));
            getDb().save(st);
        }
    }

    public CommandResponder findCommand(String nameOrAlias) {
        for (CommandResponder cmd: commandResponders) {
            for (String alias: cmd.getAliases()) {
                if (alias.equalsIgnoreCase(nameOrAlias)) {
                    return cmd;
                }
            }
        }
        return null;
    }

    public Channel findChannel(String nameOrAlias) {
        for (Channel channel: channels) {
            if (channel.getKey().equalsIgnoreCase(nameOrAlias)) return channel;
            for (String alias: channel.getAliases()) {
                if (alias.equalsIgnoreCase(nameOrAlias)) {
                    return channel;
                }
            }
        }
        return null;
    }

    public Channel getFocusChannel(UUID uuid) {
        String focusChannelName = SQLSetting.getString(uuid, null, "FocusChannel", "g");
        Channel focusChannel = findChannel(focusChannelName);
        if (focusChannel != null && focusChannel.canTalk(uuid)) {
            return focusChannel;
        }
        for (Channel channel : channels) {
            if (channel instanceof PublicChannel && channel.canTalk(uuid)) {
                channel.setFocusChannel(uuid);
                return channel;
            }
        }
        return null;
    }

    public void didCreateMessage(Channel channel, Message message) {
        if (!ChatMessageEvent.call(channel, message)) {
            return;
        }
        if (!message.isLocal() && channel.getRange() == 0) {
            connectListener.broadcastMessage(message);
        }
        new ChannelChatEvent(message.getSender(), message.getTarget(),
                             NetworkServer.current(),
                             channel.getKey(),
                             message.getMessage(),
                             channel.makeMessageComponent(message)).callEvent();
    }

    public void didReceiveMessage(Message message) {
        Channel channel = findChannel(message.getChannel());
        if (channel == null) {
            getLogger().warning("Could not find message channel: '" + message.getChannel() + "'");
            return;
        }
        channel.handleMessage(message);
        new ChannelChatEvent(message.getSender(), message.getTarget(),
                             NetworkServer.of(message.getSenderServer()),
                             channel.getKey(),
                             message.getMessage(),
                             channel.makeMessageComponent(message)).callEvent();
    }

    /**
     * Find a Chatter by name. It is possible that the Console chatter
     * is returned.
     * This is only used by PrivateCommand.
     */
    public Chatter getOnlinePlayer(String name) {
        if (name.equalsIgnoreCase(Chatter.CONSOLE.name)) {
            return Chatter.CONSOLE;
        }
        RemotePlayer player = Connect.get().getRemotePlayer(name);
        return player != null
            ? new Chatter(player.getUniqueId(), player.getName())
            : null;
    }

    public List<Chatter> getOnlinePlayers() {
        List<Chatter> result = new ArrayList<>();
        for (RemotePlayer player : Connect.get().getRemotePlayers()) {
            result.add(new Chatter(player.getUniqueId(), player.getName()));
        }
        return result;
    }

    public Chatter findOfflinePlayer(UUID uuid) {
        PlayerCache player = PlayerCache.forUuid(uuid);
        return player != null
            ? new Chatter(player.uuid, player.name)
            : null;
    }

    public Chatter findOfflinePlayer(String name) {
        PlayerCache player = PlayerCache.forName(name);
        return player != null
            ? new Chatter(player.uuid, player.name)
            : null;
    }

    public boolean announce(String channel, Component message) {
        Channel ch = findChannel(channel);
        if (ch == null) return false;
        ch.announce(message);
        return true;
    }

    public boolean announceLocal(String channel, Component message) {
        Channel ch = findChannel(channel);
        if (ch == null) return false;
        ch.announceLocal(message);
        return true;
    }

    public boolean announce(String channel, UUID sender, Component message) {
        Channel ch = findChannel(channel);
        if (ch == null) return false;
        ch.announce(sender, message);
        return true;
    }

    public boolean announceLocal(String channel, UUID sender, Component message) {
        Channel ch = findChannel(channel);
        if (ch == null) return false;
        ch.announceLocal(sender, message);
        return true;
    }

    public boolean doesIgnore(UUID player, UUID ignoree) {
        return SQLIgnore.doesIgnore(player, ignoree);
    }

    public Component filterBadWords(Component in) {
        for (TextReplacementConfig config : badWordConfigList) {
            in = in.replaceText(config);
        }
        return in;
    }

    public String filterBadWords(String in) {
        for (Pattern pattern : badWordList) {
            in = pattern.matcher(in).replaceAll("***");
        }
        return in;
    }

    public boolean containsBadWord(String in) {
        for (Pattern pattern : badWordList) {
            if (pattern.matcher(in).find()) return true;
        }
        return false;
    }

    public void onBungeeJoin(UUID uuid, String name, long timestamp) {
        if (!Perm.get().has(uuid, "chat.joinmessage")) return;
        Channel channel = findChannel("info");
        if (channel != null) channel.onBungeeJoin(uuid, name, timestamp);
    }

    public void onBungeeQuit(UUID uuid, String name, long timestamp) {
        if (!Perm.get().has(uuid, "chat.joinmessage")) return;
        Channel channel = findChannel("info");
        if (channel != null) channel.onBungeeQuit(uuid, name, timestamp);
    }

    public static ChatPlugin plugin() {
        return instance;
    }
}

