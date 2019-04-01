package com.winthier.chat;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerKickEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.ServerCommandEvent;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.CommandResponder;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.sql.SQLDB;
import com.winthier.connect.Connect;
import java.util.HashMap;

public final class ChatListener implements Listener {
    void onPlayerChat(Player player, String message) {
        if (!player.isValid()) return;
        Channel channel = ChatPlugin.getInstance().getFocusChannel(player.getUniqueId());
        if (channel == null) return;
        if (!channel.hasPermission(player)) return;
        if (!ChatPlayerTalkEvent.call(player, channel, message)) return;
        channel.playerDidUseChat(new PlayerCommandContext(player, null, message));
    }

    @EventHandler
    public void onPlayerChat(final PlayerChatEvent event) {
        event.setCancelled(true);
        onPlayerChat(event.getPlayer(), event.getMessage());
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        final String[] arr = event.getMessage().split("\\s+", 2);
        if (arr.length < 1) return;
        String firstArg = arr[0];
        if (firstArg.startsWith("/")) firstArg = firstArg.substring(1);
        CommandResponder cmd = ChatPlugin.getInstance().findCommand(firstArg);
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
        if (arr.length < 2) return;
        String firstArg = arr[0];
        if (firstArg.startsWith("/")) firstArg = firstArg.substring(1);
        CommandResponder cmd = ChatPlugin.getInstance().findCommand(firstArg);
        if (cmd == null) return;
        event.setCancelled(true);
        cmd.consoleDidUseCommand(arr[1]);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SQLDB.clear(event.getPlayer().getUniqueId());
        event.setJoinMessage((String)null);
        HashMap<String, Object> map = new HashMap<>();
        map.put("uuid", event.getPlayer().getUniqueId().toString());
        map.put("name", event.getPlayer().getName());
        Connect.getInstance().broadcastAll("BUNGEE_PLAYER_JOIN", map);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SQLDB.clear(event.getPlayer().getUniqueId());
        event.setQuitMessage((String)null);
        HashMap<String, Object> map = new HashMap<>();
        map.put("uuid", event.getPlayer().getUniqueId().toString());
        map.put("name", event.getPlayer().getName());
        Connect.getInstance().broadcastAll("BUNGEE_PLAYER_QUIT", map);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        event.setQuitMessage((String)null);
    }
}
