package com.winthier.chat;

import com.cavetale.core.connect.NetworkServer;
import com.winthier.chat.sql.SQLBacklog;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import static com.winthier.chat.ChatPlugin.plugin;

/**
 * A feature that resends the last few lines of chat when the player
 * joins, thanks to the Mojang client clearing all chat when switching
 * servers in a network.
 *
 * Messages coming in while we are loading are stored until we are
 * done loading.
 */
public final class Backlog implements Listener {
    private final Map<UUID, List<Component>> storedMessages = new HashMap<>();

    public void send(Player player, Component message) {
        List<Component> stored = storedMessages.get(player.getUniqueId());
        if (stored != null) {
            stored.add(message);
        } else {
            player.sendMessage(message);
        }
        plugin().getDb().insertAsync(new SQLBacklog(player, message), null);
    }

    public void sendNoLog(Player player, Component message) {
        List<Component> stored = storedMessages.get(player.getUniqueId());
        if (stored != null) {
            stored.add(message);
        } else {
            player.sendMessage(message);
        }
    }

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin());
        final Date then = new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L);
        plugin().getDb().find(SQLBacklog.class)
            .lt("time", then)
            .deleteAsync(null);
    }

    public static Backlog backlog() {
        return plugin().getBacklog();
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (NetworkServer.current() == NetworkServer.VOID) {
            if (!event.getPlayer().hasPermission("chat.backlog.onlogin")) {
                return;
            }
        } else {
            if (!event.getPlayer().hasPermission("chat.backlog.onjoin")) {
                return;
            }
        }
        sendBacklog(event.getPlayer());
    }

    public void sendBacklog(Player player) {
        storedMessages.put(player.getUniqueId(), new ArrayList<>());
        plugin().getDb().find(SQLBacklog.class)
            .eq("player", player.getUniqueId())
            .orderByDescending("time")
            .limit(100)
            .findListAsync(backlogs -> {
                    List<Component> stored = storedMessages.remove(player.getUniqueId());
                    if (!player.isOnline()) return;
                    for (int i = backlogs.size() - 1; i >= 0; i -= 1) {
                        player.sendMessage(backlogs.get(i).getMessageComponent());
                    }
                    if (stored != null) {
                        for (Component it : stored) {
                            player.sendMessage(it);
                        }
                    }
                });
    }
}
