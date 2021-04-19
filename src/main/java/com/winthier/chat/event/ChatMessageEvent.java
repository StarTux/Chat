package com.winthier.chat.event;

import com.winthier.chat.Message;
import com.winthier.chat.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter @RequiredArgsConstructor
public final class ChatMessageEvent extends Event implements Cancellable {
    private static HandlerList handlers = new HandlerList();
    private final Channel channel;
    private final Message message;
    @Setter private boolean cancelled = false;

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static boolean call(Channel channel, Message message) {
        ChatMessageEvent event = new ChatMessageEvent(channel, message);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return (!event.isCancelled());
    }
}
