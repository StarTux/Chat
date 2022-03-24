package com.winthier.chat;

import com.winthier.chat.channel.AbstractChannel;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.sql.SQLDB;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class ChatListener implements Listener {
    private final ChatPlugin plugin;

    @EventHandler(ignoreCancelled = true)
    public void onAsyncChat(final AsyncChatEvent event) {
        event.setCancelled(true);
        final Player player = event.getPlayer();
        if (event.originalMessage() instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) event.originalMessage();
            Bukkit.getScheduler().runTask(plugin, () -> {
                    onPlayerChat(player, textComponent.content());
                });
        }
    }

    void onPlayerChat(Player player, String message) {
        if (!player.isValid()) return;
        Channel channel = plugin.getFocusChannel(player.getUniqueId());
        if (channel == null) return;
        if (!channel.canTalk(player.getUniqueId())) return;
        ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(player, channel, message);
        if (!event.call()) return;
        channel.playerDidUseChat(new PlayerCommandContext(player, null, event.getMessage()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SQLDB.load(event.getPlayer().getUniqueId());
        event.joinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SQLDB.clear(event.getPlayer().getUniqueId());
        event.quitMessage(null);
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
