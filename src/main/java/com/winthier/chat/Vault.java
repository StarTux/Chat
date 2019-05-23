package com.winthier.chat;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
final class Vault {
    private final JavaPlugin plugin;
    private Permission permission = null;
    private Chat chat = null;

    void setup() {
        RegisteredServiceProvider<Permission> provider =
            plugin.getServer()
            .getServicesManager()
            .getRegistration(Permission.class);
        if (provider != null) {
            permission = provider.getProvider();
        }
        RegisteredServiceProvider<Chat> provider2 =
            plugin.getServer()
            .getServicesManager()
            .getRegistration(Chat.class);
        if (provider2 != null) {
            chat = provider2.getProvider();
        }
    }

    boolean has(UUID uuid, String perm) {
        if (permission == null) return false;
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(uuid);
        if (off == null) return false;
        return permission.playerHas((String)null, off, perm);
    }

    String prefix(UUID uuid) {
        if (chat == null) return null;
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(uuid);
        if (off == null) return null;
        return chat.getPlayerPrefix((String)null, off);
    }
}
