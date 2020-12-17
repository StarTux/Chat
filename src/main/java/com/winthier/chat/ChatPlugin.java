package com.winthier.chat;

import com.winthier.chat.channel.AbstractChannel;
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
import com.winthier.chat.sql.SQLPattern;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.title.TitleHandler;
import com.winthier.generic_events.GenericEvents;
import com.winthier.sql.SQLDatabase;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ChatPlugin extends JavaPlugin {
    @Getter private static ChatPlugin instance;
    private final List<CommandResponder> commandResponders = new ArrayList<>();
    private final List<Channel> channels = new ArrayList<>();
    private ConnectListener connectListener = null;
    private TitleHandler titleHandler = null;
    private ChatListener chatListener = new ChatListener();
    private PrivateChannel privateChannel = null;
    private PartyChannel partyChannel = null;
    private PlayerCacheHandler playerCacheHandler = null;
    private DynmapHandler dynmapHandler = null;
    private ChatCommand chatCommand = new ChatCommand();
    @Setter private boolean debugMode = false;
    private SQLDatabase db;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
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
        if (getServer().getPluginManager().isPluginEnabled("Title")) {
            titleHandler = new TitleHandler();
            getLogger().info("Title plugin found!");
        } else {
            getLogger().warning("Title plugin NOT found!");
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
        getCommand("chatadmin").setExecutor(new AdminCommand());
        getCommand("chat").setExecutor(chatCommand);
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
        SQLDB.clear();
        instance = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandResponder cmd = findCommand(label);
        if (cmd == null) return false;
        Player player = sender instanceof Player ? (Player) sender : null;
        if (player != null && !cmd.hasPermission(player)) return true;
        String msg;
        if (args.length == 0) {
            msg = null;
        } else {
            StringBuilder sb = new StringBuilder(args[0]);
            for (int i = 1; i < args.length; i += 1) sb.append(" ").append(args[i]);
            msg = sb.toString();
        }
        if (player == null) {
            cmd.consoleDidUseCommand(msg);
        } else {
            if (!ChatPlayerTalkEvent.call(player, cmd.getChannel(), msg)) return true;
            cmd.playerDidUseCommand(new PlayerCommandContext(player, label, msg));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 0) return null;
        String arg = args[args.length - 1];
        return completePlayerName(arg);
    }

    public List<String> completePlayerName(String name) {
        List<String> result = new ArrayList<>();
        name = name.toLowerCase();
        for (Chatter chatter: getOnlinePlayers()) {
            if (name.isEmpty()) {
                result.add(chatter.getName());
            } else {
                String name2 = chatter.getName().toLowerCase();
                if (name2.startsWith(name)) {
                    result.add(chatter.getName());
                }
            }
        }
        return result;
    }

    void loadChannels() {
        commandResponders.clear();
        channels.clear();
        for (SQLChannel chan : SQLChannel.fetch()) {
            CommandResponder cmd;
            if ("pm".equals(chan.getChannelKey())) {
                commandResponders.add(new ReplyCommand());
                privateChannel = new PrivateChannel();
                cmd = privateChannel;
            } else if ("party".equals(chan.getChannelKey())) {
                commandResponders.add(new PartyCommand());
                partyChannel = new PartyChannel();
                cmd = partyChannel;
            } else if ("reply".equals(chan.getChannelKey())) {
                cmd = new ReplyCommand();
            } else {
                cmd = new PublicChannel();
            }
            for (String ali: chan.getAliases().split(",")) {
                cmd.getAliases().add(ali.toLowerCase());
            }
            if (cmd instanceof AbstractChannel) {
                AbstractChannel channel = (AbstractChannel) cmd;
                channel.setTag(chan.getTag());
                channel.setKey(chan.getChannelKey());
                channel.setTitle(chan.getTitle());
                channel.setDescription(chan.getDescription());
                if (chan.getLocalRange() != null) {
                    channel.setRange(chan.getLocalRange());
                }
            }
            commandResponders.add(cmd);
            if (cmd instanceof Channel) {
                channels.add((Channel) cmd);
            }
        }
    }

    @SuppressWarnings("unchecked")
    void initializeDatabase() {
        YamlConfiguration config;
        InputStreamReader isr = new InputStreamReader(getResource("database.yml"));
        config = YamlConfiguration.loadConfiguration(isr);
        ConfigurationSection patternSection = config.getConfigurationSection("patterns");
        if (patternSection != null) {
            for (String category: patternSection.getKeys(false)) {
                for (Object o: patternSection.getList(category)) {
                    if (o instanceof List) {
                        List<Object> list = (List<Object>) o;
                        if (list.size() >= 1) {
                            SQLPattern pat = new SQLPattern();
                            pat.setCategory(category);
                            pat.setRegex(list.get(0).toString());
                            if (list.size() >= 2) {
                                pat.setReplacement(list.get(1).toString());
                            } else {
                                pat.setReplacement("");
                            }
                            getDb().save(pat);
                        }
                    } else if (o instanceof String) {
                        SQLPattern pat = new SQLPattern();
                        pat.setCategory(category);
                        pat.setRegex((String) o);
                        pat.setReplacement("");
                        getDb().save(pat);
                    }
                }
            }
        }
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

    public boolean hasPermission(UUID uuid, String permission) {
        if (uuid == null) return true;
        Player player = getServer().getPlayer(uuid);
        if (player != null) return player.hasPermission(permission);
        return GenericEvents.playerHasPermission(uuid, permission);
    }

    public Channel getFocusChannel(UUID uuid) {
        String forcedFocusChannelName = getConfig().getString("ForcedFocusChannel", null);
        if (forcedFocusChannelName != null && !forcedFocusChannelName.isEmpty()) {
            Channel channel = findChannel(forcedFocusChannelName);
            if (channel != null) return channel;
        }
        String channelName = SQLSetting.getString(uuid, null, "FocusChannel", "g");
        return findChannel(channelName);
    }

    public String getServerName() {
        if (connectListener != null) return connectListener.getServerName();
        return getConfig().getString("ServerName", "N/A");
    }

    public String getServerDisplayName() {
        if (connectListener != null) return connectListener.getServerDisplayName();
        return getConfig().getString("ServerDisplayName", "N/A");
    }

    public void loadTitle(Message message) {
        if (titleHandler != null) {
            titleHandler.loadTitle(message);
        }
    }

    public void didCreateMessage(Channel channel, Message message) {
        if (!ChatMessageEvent.call(channel, message)) {
            return;
        }
        if (!message.local && connectListener != null && channel.getRange() == 0) {
            connectListener.broadcastMessage(message);
        }
        if (dynmapHandler != null
            && SQLSetting.getBoolean(null, message.channel, "PostToDynmap", false)) {
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
        Channel channel = findChannel(message.channel);
        if (channel == null) {
            getLogger().warning("Could not find message channel: '" + message.channel + "'");
        } else {
            channel.handleMessage(message);
        }
    }

    public Chatter getOnlinePlayer(String name) {
        Player player = getServer().getPlayer(name);
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

    public boolean announce(String channel, Object message) {
        Channel ch = findChannel(channel);
        if (ch == null) return false;
        ch.announce(message);
        return true;
    }

    public boolean announceLocal(String channel, Object message) {
        Channel ch = findChannel(channel);
        if (ch == null) return false;
        ch.announceLocal(message);
        return true;
    }

    public boolean doesIgnore(UUID player, UUID ignoree) {
        return SQLIgnore.doesIgnore(player, ignoree);
    }

    public void onBungeeJoin(UUID uuid, String name, String server) {
        if (!server.equals(getServerName())) return;
        if (GenericEvents.playerHasPermission(uuid, "chat.joinmessage")) {
            getServer().getScheduler().runTask(this, () -> {
                    announce("info", ChatColor.GREEN + name + " joined");
                });
        }
    }

    public void onBungeeQuit(UUID uuid, String name, String server) {
        if (!server.equals(getServerName())) return;
        if (GenericEvents.playerHasPermission(uuid, "chat.joinmessage")) {
            getServer().getScheduler().runTask(this, () -> {
                    announce("info", ChatColor.AQUA + name + " disconnected");
                });
        }
    }
}
