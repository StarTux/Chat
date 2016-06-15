package com.winthier.chat;

import com.winthier.chat.channel.*;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ChatListener implements Listener {
    @EventHandler
    public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        new BukkitRunnable() {
            @Override public void run() {
                onPlayerChat(event.getPlayer(), event.getMessage());
            }
        }.runTask(ChatPlugin.getInstance());
    }

    @EventHandler
    void onPlayerChat(Player player, String message) {
        Channel channel = ChatPlugin.getInstance().getFocusChannel(player.getUniqueId());
        if (channel == null) return;
        channel.playerDidUseCommand(new PlayerCommandContext(player, null, message));
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        System.out.println("cmd " + event.getPlayer().getName() + " " + event.getMessage());
        final String[] arr = event.getMessage().split("\\s+", 2);
        if (arr.length < 1) return;
        String firstArg = arr[0];
        if (firstArg.startsWith("/")) firstArg = firstArg.substring(1);
        CommandResponder cmd = ChatPlugin.getInstance().findCommand(firstArg);
        if (cmd == null) return;
        String msg = arr.length >= 2 ? arr[1] : null;
        event.setCancelled(true);
        cmd.playerDidUseCommand(new PlayerCommandContext(event.getPlayer(), firstArg, msg));
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
    }
}
