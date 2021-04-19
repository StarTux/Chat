package com.winthier.chat.dynmap;

import com.winthier.chat.Message;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;

public final class DynmapHandler implements Listener {
    DynmapAPI getDynmap() {
        Plugin pl = Bukkit.getServer().getPluginManager().getPlugin("dynmap");
        if (pl != null && pl instanceof DynmapAPI) return (DynmapAPI) pl;
        return null;
    }

    public void postPlayerMessage(Message message) {
        String msg = message.getMessage();
        if (msg == null) return;
        String sender = message.getSenderName();
        if (sender == null) sender = "Announcement";
        getDynmap().postPlayerMessageToWeb(sender, sender, msg);
    }
}
