package com.winthier.chat;

import com.cavetale.core.chat.ChatHandler;
import com.winthier.chat.sql.SQLIgnore;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class CoreChat implements ChatHandler {
    @Override
    public void sendAndLog(Player player, Component message) {
        Backlog.backlog().send(player, message);
    }

    @Override
    public void sendNoLog(Player player, Component message) {
        Backlog.backlog().sendNoLog(player, message);
    }

    @Override
    public boolean doesIgnore(UUID ignorer, UUID ignoree) {
        return SQLIgnore.doesIgnore(ignorer, ignoree);
    }

    @Override
    public ChatPlugin getPlugin() {
        return ChatPlugin.plugin();
    }
}
