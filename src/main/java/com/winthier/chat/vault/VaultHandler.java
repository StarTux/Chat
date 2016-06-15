package com.winthier.chat.vault;

import java.util.UUID;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHandler {
    Permission permission = null;
    Chat chat = null;

    public Permission getPermission() {
        if (permission == null) {
            RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if (permissionProvider != null) permission = permissionProvider.getProvider();
        }
        return permission;
    }

    public Chat getChat() {
        if (chat == null) {
            RegisteredServiceProvider<Chat> chatProvider = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
            if (chatProvider != null) chat = chatProvider.getProvider();
        }
        return chat;
    }
    
    public boolean hasPermission(UUID uuid, String permission) {
        return getPermission().playerHas((String)null, Bukkit.getServer().getOfflinePlayer(uuid), permission);
    }

    public String getPlayerTitle(UUID uuid) {
        return getChat().getPlayerPrefix((String)null, Bukkit.getServer().getOfflinePlayer(uuid));
    }
}
