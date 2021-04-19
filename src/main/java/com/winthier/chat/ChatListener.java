package com.winthier.chat;

import com.winthier.chat.channel.AbstractChannel;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.CommandResponder;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.sql.SQLDB;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitRunnable;

@RequiredArgsConstructor
public final class ChatListener implements Listener {
    private final ChatPlugin plugin;

    @EventHandler
    public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        new BukkitRunnable() {
            @Override public void run() {
                onPlayerChat(event.getPlayer(), event.getMessage());
            }
        }.runTask(plugin);
    }

    void onPlayerChat(Player player, String message) {
        if (!player.isValid()) return;
        Channel channel = plugin.getFocusChannel(player.getUniqueId());
        if (channel == null) return;
        if (!channel.canTalk(player.getUniqueId())) return;
        if (!ChatPlayerTalkEvent.call(player, channel, message)) return;
        channel.playerDidUseChat(new PlayerCommandContext(player, null, message));
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        final String[] arr = event.getMessage().split("\\s+", 2);
        if (arr.length < 1) return;
        String firstArg = arr[0];
        if (firstArg.startsWith("/")) firstArg = firstArg.substring(1);
        CommandResponder cmd = plugin.findCommand(firstArg);
        if (cmd == null) return;
        event.setCancelled(true);
        if (!cmd.hasPermission(event.getPlayer())) return;
        String msg = arr.length >= 2 ? arr[1] : null;
        if (!ChatPlayerTalkEvent.call(event.getPlayer(), cmd.getChannel(), msg)) return;
        cmd.playerDidUseCommand(new PlayerCommandContext(event.getPlayer(), firstArg, msg));
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        final String[] arr = event.getCommand().split("\\s+", 2);
        if (arr.length == 0) return;
        String firstArg = arr[0];
        if (firstArg.startsWith("/")) firstArg = firstArg.substring(1);
        CommandResponder cmd = plugin.findCommand(firstArg);
        if (cmd == null) return;
        event.setCancelled(true);
        if (arr.length < 2) return;
        cmd.consoleDidUseCommand(arr[1]);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SQLDB.load(event.getPlayer().getUniqueId());
        event.setJoinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SQLDB.clear(event.getPlayer().getUniqueId());
        event.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        event.leaveMessage(Component.empty());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Component deathMessage = event.deathMessage();
        if (deathMessage == null) return;
        Player player = event.getEntity();
        event.deathMessage(Component.empty());
        if (!player.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES)) return;
        Channel localChannel = plugin.findChannel("local");
        if (localChannel != null && localChannel instanceof AbstractChannel) {
            AbstractChannel channel = (AbstractChannel) localChannel;
            Message message = new Message().init(localChannel).player(player, deathMessage);
            message.setHideSenderTags(true);
            message.setLocal(true);
            message.setLocation(player.getLocation());
            message.setPassive(true);
            channel.handleMessage(message);
        }
    }
}
