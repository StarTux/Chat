package com.winthier.chat;

import com.cavetale.core.connect.NetworkServer;
import com.cavetale.core.event.perm.PlayerPermissionUpdateEvent;
import com.cavetale.core.font.Emoji;
import com.winthier.chat.channel.AbstractChannel;
import com.winthier.chat.channel.Channel;
import com.winthier.chat.channel.PlayerCommandContext;
import com.winthier.chat.event.ChatPlayerTalkEvent;
import com.winthier.chat.sql.SQLDB;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class ChatListener implements Listener {
    private static final String PERM_EMOJI = "chat.emoji";
    private final ChatPlugin plugin;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        List<String> completions = getEmojiCompletions();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(PERM_EMOJI)) {
                player.addCustomChatCompletions(completions);
            } else {
                player.removeCustomChatCompletions(completions);
            }
        }
    }

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

    private void onPlayerChat(Player player, String message) {
        if (!player.isValid()) return;
        Channel channel = plugin.getFocusChannel(player.getUniqueId());
        if (channel == null) return;
        if (!channel.canTalk(player.getUniqueId())) return;
        ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(player, channel, message);
        if (!event.call()) return;
        channel.playerDidUseChat(new PlayerCommandContext(player, null, event.getMessage()));
    }

    private static List<String> getEmojiCompletions() {
        List<Emoji> all = Emoji.all();
        List<String> result = new ArrayList<>(all.size());
        for (Emoji emoji : all) {
            if (emoji.isPublic()) {
                result.add(":" + emoji.name + ":");
            }
        }
        return result;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        SQLSetting.loadSettingsAsync(uuid);
        event.joinMessage(null);
        if (event.getPlayer().hasPermission(PERM_EMOJI)) {
            event.getPlayer().addCustomChatCompletions(getEmojiCompletions());
        }
        if (NetworkServer.current().isSurvival()) {
            SQLIgnore.loadIgnoresAsync(uuid, list -> {
                    if (!player.isOnline()) return;
                    for (SQLIgnore row : list) {
                        Player target = Bukkit.getPlayer(row.getIgnoree());
                        if (target != null) player.hidePlayer(plugin, target);
                    }
                });
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (player.equals(other)) continue;
                if (SQLIgnore.doesIgnore(other.getUniqueId(), player.getUniqueId())) {
                    other.hidePlayer(plugin, player);
                }
            }
        }
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

    @EventHandler
    private void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Component msg = event.message();
        if (msg == null) return;
        event.message(null);
        Channel channel = plugin.findChannel("info");
        if (channel == null) return;
        Player player = event.getPlayer();
        if (!player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS)) return;
        Message message = new Message().init(channel).message(msg);
        message.setSender(player.getUniqueId());
        message.setSenderName(player.getName());
        message.setPassive(true);
        message.setHideSenderTags(true);
        plugin.didCreateMessage(channel, message);
        channel.handleMessage(message);
    }

    @EventHandler
    public void onPlayerPermissionUpdate(PlayerPermissionUpdateEvent event) {
        Boolean ch = event.getPermissionChange(PERM_EMOJI);
        if (ch == null) {
            return;
        } else if (ch) {
            event.getPlayer().addCustomChatCompletions(getEmojiCompletions());
        } else {
            event.getPlayer().removeCustomChatCompletions(getEmojiCompletions());
        }
    }
}
