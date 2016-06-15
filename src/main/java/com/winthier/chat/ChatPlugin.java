package com.winthier.chat;

import com.winthier.chat.channel.*;
import com.winthier.chat.connect.ConnectListener;
import com.winthier.chat.sql.SQLChannel;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLPattern;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.title.TitleHandler;
import com.winthier.chat.vault.VaultHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ChatPlugin extends JavaPlugin {
    @Getter static ChatPlugin instance;
    final List<CommandResponder> commandResponders = new ArrayList<>();
    final List<Channel> channels = new ArrayList<>();
    ConnectListener connectListener = null;
    VaultHandler vaultHandler = null;
    TitleHandler titleHandler = null;
    ChatListener chatListener = new ChatListener();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        if (!SQLDB.probe()) {
            getLogger().info("Installing Chat database due to first time usage");
            installDDL();
            initializeDatabase();
        }
        loadChannels();
        if (getServer().getPluginManager().getPlugin("Connect") != null) {
            connectListener = new ConnectListener();
            getServer().getPluginManager().registerEvents(connectListener, this);
            getLogger().info("Connect plugin found!");
        } else {
            getLogger().warning("Connect plugin NOT found!");
        }
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultHandler = new VaultHandler();
            getLogger().info("Vault plugin found!");
        } else {
            getLogger().warning("Vault plugin NOT found!");
        }
        if (getServer().getPluginManager().getPlugin("Title") != null) {
            titleHandler = new TitleHandler();
            getLogger().warning("Title plugin found!");
        } else {
            getLogger().warning("Title plugin found!");
        }
        getServer().getPluginManager().registerEvents(chatListener, this);
        getCommand("chatadmin").setExecutor(new AdminCommand());
        getCommand("chat").setExecutor(new ChatCommand());
    }

    void loadChannels() {
        commandResponders.clear();
        channels.clear();
        for (SQLChannel chan: SQLChannel.fetch()) {
            CommandResponder cmd;
            if ("pm".equals(chan.getChannelKey())) {
                cmd = new PrivateChannel();
            } else if ("reply".equals(chan.getChannelKey())) {
                cmd = new ReplyCommand();
            } else {
                cmd = new PublicChannel();
            }
            Set<String> aliases = new HashSet<>();
            aliases.add(chan.getChannelKey().toLowerCase());
            aliases.add(chan.getTag().toLowerCase());
            for (String ali: chan.getAliases().split(",")) {
                aliases.add(ali.toLowerCase());
            }
            cmd.getAliases().addAll(aliases);
            if (cmd instanceof AbstractChannel) {
                AbstractChannel channel = (AbstractChannel)cmd;
                channel.setTag(chan.getTag());
                channel.setKey(chan.getChannelKey());
                channel.setTitle(chan.getTitle());
            }
            commandResponders.add(cmd);
        }
        for (CommandResponder cmd: commandResponders) {
            if (cmd instanceof Channel) {
                channels.add((Channel)cmd);
            }
        }
    }

    @SuppressWarnings("unchecked")
    void initializeDatabase() {
        ConfigurationSection patternSection = getConfig().getConfigurationSection("patterns");
        if (patternSection != null) {
            for (String category: patternSection.getKeys(false)) {
                for (Object o: patternSection.getList(category)) {
                    if (o instanceof List) {
                        List<Object> list = (List<Object>)o;
                        if (list.size() >= 1) {
                            SQLPattern pat = new SQLPattern();
                            pat.setCategory(category);
                            pat.setRegex(list.get(0).toString());
                            if (list.size() >= 2) {
                                pat.setReplacement(list.get(1).toString());
                            } else {
                                pat.setReplacement("");
                            }
                            getDatabase().save(pat);
                        }
                    } else if (o instanceof String) {
                        SQLPattern pat = new SQLPattern();
                        pat.setCategory(category);
                        pat.setRegex((String)o);
                        pat.setReplacement("");
                        getDatabase().save(pat);
                    } else {
                    }
                }
            }
        }
        ConfigurationSection channelsSection = getConfig().getConfigurationSection("channels");
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
                getDatabase().save(chan);
            }
        }
        for (Map<?, ?> tmpMap: getConfig().getMapList("settings")) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>)tmpMap;
            SQLSetting st = new SQLSetting((UUID)null, map.get("channel"), map.get("key"), (Object)map.get("value"));
            getDatabase().save(st);
        }
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        return SQLDB.getDatabaseClasses();
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
        if (vaultHandler != null) return vaultHandler.hasPermission(uuid, permission);
        return false;
    }

    public Channel getFocusChannel(UUID uuid) {
        String channelName = SQLSetting.getString(uuid, null, "FocusChannel", "g");
        Channel channel = findChannel(channelName);
        return null;
    }

    public String getServerName() {
        if (connectListener != null) return connectListener.getServerName();
        return getConfig().getString("ServerName", "winthier");
    }

    public void loadTitle(Message message) {
        if (titleHandler != null) {
            titleHandler.loadTitle(message);
        } else if (vaultHandler != null) {
            message.senderTitle = vaultHandler.getPlayerTitle(message.sender);
        }
    }

    public void didCreateMessage(Message message) {
        if (connectListener != null) {
            connectListener.broadcastMessage(message);
        }
    }

    public void didReceiveMessage(Message message) {
        Channel channel = findChannel(message.channel);
        if (channel != null) {
            channel.handleMessage(message);
        }
    }
}
