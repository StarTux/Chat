package com.winthier.chat;

import com.cavetale.core.chat.ChannelChatEvent;
import com.cavetale.core.chat.ChatHandler;
import com.cavetale.core.connect.NetworkServer;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLLog;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import static com.winthier.chat.ChatPlugin.plugin;

public final class CoreChat implements ChatHandler {
    @Override
    public void sendAndLog(Player player, Component message) {
        Backlog.backlog().send(player, message);
    }

    @Override
    public void sendNoLog(Player player, Component message) {
        Backlog.backlog().sendNoLog(player, message);
    }

    @Override
    public boolean doesIgnore(UUID ignorer, UUID ignoree) {
        return SQLIgnore.doesIgnore(ignorer, ignoree);
    }

    @Override
    public void getChannelLog(String channelName, Instant from, Instant to, Consumer<List<ChannelChatEvent>> callback) {
        final Channel channel = plugin().findChannel(channelName);
        if (channel == null) {
            callback.accept(List.of());
            return;
        }
        plugin().getDb().find(SQLLog.class)
            .eq("channel", channelName)
            .gte("time", Date.from(from))
            .lte("time", Date.from(to))
            .orderByAscending("time")
            .findListAsync(list -> {
                    final List<ChannelChatEvent> result = new ArrayList<>(list.size());
                    for (SQLLog it : list) {
                        result.add(new ChannelChatEvent(it.getTime().toInstant(),
                                                        it.getPlayer(),
                                                        it.getTarget(),
                                                        NetworkServer.of(it.getServer()),
                                                        it.getChannel(),
                                                        it.getMessage(),
                                                        channel.makeMessageComponent(it.toMessage())));
                    }
                    callback.accept(result);
                });
    }

    @Override
    public void getChannelLog(String channelName, Instant since, Consumer<List<ChannelChatEvent>> callback) {
        final Channel channel = plugin().findChannel(channelName);
        if (channel == null) {
            callback.accept(List.of());
            return;
        }
        plugin().getDb().find(SQLLog.class)
            .eq("channel", channelName)
            .gte("time", Date.from(since))
            .orderByAscending("time")
            .findListAsync(list -> {
                    final List<ChannelChatEvent> result = new ArrayList<>(list.size());
                    for (SQLLog it : list) {
                        result.add(new ChannelChatEvent(it.getTime().toInstant(),
                                                        it.getPlayer(),
                                                        it.getTarget(),
                                                        NetworkServer.of(it.getServer()),
                                                        it.getChannel(),
                                                        it.getMessage(),
                                                        channel.makeMessageComponent(it.toMessage())));
                    }
                    callback.accept(result);
                });
    }

    @Override
    public ChatPlugin getPlugin() {
        return plugin();
    }
}
