package com.winthier.chat.event;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import com.winthier.chat.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter @RequiredArgsConstructor
public final class ChatPlayerTalkEvent extends Event implements Cancellable {
    // Event Stuff

    private static HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    @Setter private boolean cancelled = false;

    // Chat Stuff

    private final Player player;
    private final Channel channel;
    private final String message;

    public static boolean call(Player player, Channel channel, String msg) {
        ChatPlayerTalkEvent event = new ChatPlayerTalkEvent(player, channel, msg);
        Server.getInstance().getPluginManager().callEvent(event);
        return (!event.isCancelled());
    }
}
