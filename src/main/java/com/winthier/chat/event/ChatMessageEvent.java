package com.winthier.chat.event;

import cn.nukkit.Server;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import com.winthier.chat.Message;
import com.winthier.chat.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @RequiredArgsConstructor
public class ChatMessageEvent extends Event implements Cancellable {
    // Event Stuff

    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    @Setter private boolean cancelled = false;

    // Chat Stuff
    private final Channel channel;
    private final Message message;

    public static boolean call(Channel channel, Message message) {
        ChatMessageEvent event = new ChatMessageEvent(channel, message);
        Server.getInstance().getPluginManager().callEvent(event);
        return (!event.isCancelled());
    }
}
