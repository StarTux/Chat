package com.winthier.chat;

import com.cavetale.core.font.Emoji;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.CommandResponder;
import com.winthier.chat.channel.PartyChannel;
import com.winthier.chat.channel.PartyCommand;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.channel.PrivateChannel;
import com.winthier.chat.channel.PublicChannel;
import com.winthier.chat.channel.ReplyCommand;
import com.winthier.chat.connect.ConnectListener;
import com.winthier.chat.dynmap.DynmapHandler;
import com.winthier.chat.event.ChatMessageEvent;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.playercache.PlayerCacheHandler;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.perm.Perm;
import com.winthier.sql.SQLDatabase;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
    private ConnectListener connectListener = null;
    private ChatListener chatListener = new ChatListener(this);
    private PrivateChannel privateChannel = null;
    private PartyChannel partyChannel = null;
    private PlayerCacheHandler playerCacheHandler = null;
    private DynmapHandler dynmapHandler = null;
    private ChatCommand chatCommand = new ChatCommand(this);
    @Setter private boolean debugMode = false;
    private SQLDatabase db;
    private final List<TextReplacementConfig> badWords = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;
        db = new SQLDatabase(this);
        for (Class<?> clazz: SQLDB.getDatabaseClasses()) db.registerTable(clazz);
        db.createAllTables();
        loadChannels();
        if (getServer().getPluginManager().isPluginEnabled("Connect")) {
            connectListener = new ConnectListener();
            getServer().getPluginManager().registerEvents(connectListener, this);
            getLogger().info("Connect plugin found!");
        } else {
            getLogger().warning("Connect plugin NOT found!");
        }
        if (getServer().getPluginManager().isPluginEnabled("PlayerCache")) {
            playerCacheHandler = new PlayerCacheHandler();
            getLogger().info("PlayerCache plugin found!");
        } else {
            getLogger().warning("PlayerCache plugin NOT found!");
        }
        if (getServer().getPluginManager().isPluginEnabled("dynmap")) {
            dynmapHandler = new DynmapHandler();
            getServer().getPluginManager().registerEvents(dynmapHandler, this);
            getLogger().info("Dynmap plugin found!");
        } else {
            getLogger().warning("Dynmap plugin NOT found!");
        }
        getServer().getPluginManager().registerEvents(chatListener, this);
        new AdminCommand(this).enable();
        chatCommand.enable();
        getCommand("join").setExecutor(new JoinLeaveCommand(true));
        getCommand("leave").setExecutor(new JoinLeaveCommand(false));
        getCommand("ignore").setExecutor(new IgnoreCommand());
        SQLDB.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            SQLDB.load(player.getUniqueId());
        }
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
            if (!ChatPlayerTalkEvent.call(player, cmd.getChannel(), msg)) return true;
            cmd.playerDidUseCommand(new PlayerCommandContext(player, label, msg));
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

    public String getServerName() {
        if (connectListener != null) return connectListener.getServerName();
        return "minecraft";
    }

    public String getServerDisplayName() {
        if (connectListener != null) return connectListener.getServerDisplayName();
        return "Minecraft";
    }

    public void didCreateMessage(Channel channel, Message message) {
        if (!ChatMessageEvent.call(channel, message)) {
            return;
        }
        if (!message.isLocal() && connectListener != null && channel.getRange() == 0) {
            connectListener.broadcastMessage(message);
        }
        if (dynmapHandler != null && SQLSetting.getBoolean(null, message.getChannel(), "PostToDynmap", false)) {
            try {
                dynmapHandler.postPlayerMessage(message);
            } catch (Exception e) {
                dynmapHandler = null;
                getLogger().warning("dynmap error");
                e.printStackTrace();
            }
        }
    }

    public void didReceiveMessage(Message message) {
        Channel channel = findChannel(message.getChannel());
        if (channel == null) {
            getLogger().warning("Could not find message channel: '" + message.getChannel() + "'");
        } else {
            channel.handleMessage(message);
        }
    }

    public Chatter getOnlinePlayer(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) {
            return new Chatter(player.getUniqueId(), player.getName());
        }
        if (connectListener != null) {
            Chatter chatter = connectListener.findPlayer(name);
            if (chatter != null) return chatter;
        }
        return null;
    }

    public List<Chatter> getOnlinePlayers() {
        if (connectListener != null) {
            return connectListener.getOnlinePlayers();
        } else {
            List<Chatter> result = new ArrayList<>();
            for (Player player: getServer().getOnlinePlayers()) {
                result.add(new Chatter(player.getUniqueId(), player.getName()));
            }
            return result;
        }
    }

    public Chatter findOfflinePlayer(UUID uuid) {
        if (playerCacheHandler != null) {
            String name = playerCacheHandler.nameForUuid(uuid);
            if (name == null) return null;
            return new Chatter(uuid, name);
        }
        OfflinePlayer op = getServer().getOfflinePlayer(uuid);
        if (op == null) return null;
        return new Chatter(uuid, op.getName());
    }

    public Chatter findOfflinePlayer(String name) {
        if (playerCacheHandler != null) {
            UUID uuid = playerCacheHandler.uuidForName(name);
            if (uuid == null) return null;
            String name2 = playerCacheHandler.nameForUuid(uuid);
            if (name2 != null) name = name2;
            return new Chatter(uuid, name);
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer op = getServer().getOfflinePlayer(name);
        if (op == null) return null;
        return new Chatter(op.getUniqueId(), op.getName());
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

    public boolean doesIgnore(UUID player, UUID ignoree) {
        return SQLIgnore.doesIgnore(player, ignoree);
    }

    public void onBungeeJoin(UUID uuid, String name, String server, long timestamp) {
        if (!Perm.has(uuid, "chat.joinmessage")) return;
        Channel channel = findChannel("info");
        if (channel == null) return;
        Message message = new Message().init(channel)
            .message(Component.text(name + " joined", NamedTextColor.GREEN, TextDecoration.ITALIC));
        message.setSender(uuid);
        message.setSenderName(name);
        message.setLocal(true);
        message.setPassive(true);
        message.setHideSenderTags(true);
        channel.handleMessage(message);
    }

    public void onBungeeQuit(UUID uuid, String name, String server, long timestamp) {
        if (!Perm.has(uuid, "chat.joinmessage")) return;
        Channel channel = findChannel("info");
        if (channel == null) return;
        Message message = new Message().init(channel)
            .message(Component.text(name + " disconnected", NamedTextColor.AQUA, TextDecoration.ITALIC));
        message.setSender(uuid);
        message.setSenderName(name);
        message.setLocal(true);
        message.setPassive(true);
        message.setHideSenderTags(true);
        channel.handleMessage(message);
    }
}
