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

@Getter
@RequiredArgsConstructor
public class ChatMessageEvent extends Event implements Cancellable {
    // Event Stuff

    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Setter boolean cancelled = false;

    // Chat Stuff
    final Channel channel;
    final Message message;

    public static boolean call(Channel channel, Message message) {
        ChatMessageEvent event = new ChatMessageEvent(channel, message);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return (!event.isCancelled());
    }
}
