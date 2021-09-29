package com.winthier.chat.event;

import com.winthier.chat.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when an online player on this server talks in a chat
 * channel.  It is not called if the player to talk in said channel is
 * missing.
 */
@Getter
public final class ChatPlayerTalkEvent extends Event implements Cancellable {
    @Getter private static HandlerList handlerList = new HandlerList();
    private final Player player;
    private final Channel channel;
    @Setter private String message;
    @Setter private boolean cancelled = false;

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public ChatPlayerTalkEvent(final Player player, final Channel channel, final String message) {
        this.player = player;
        this.channel = channel;
        this.message = message;
    }

    public static boolean call(Player thePlayer, Channel theChannel, String theMessage) {
        ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(thePlayer, theChannel, theMessage);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return (!event.isCancelled());
    }
}
