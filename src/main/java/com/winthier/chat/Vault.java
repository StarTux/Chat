package com.winthier.chat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public final class Vault {
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
        return permission.playerHas((String) null, off, perm);
    }

    String prefix(UUID uuid) {
        if (chat == null) return null;
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(uuid);
        if (off == null) return null;
        return chat.getPlayerPrefix((String) null, off);
    }

    String suffix(UUID uuid) {
        if (chat == null) return null;
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(uuid);
        if (off == null) return null;
        return chat.getPlayerSuffix((String) null, off);
    }

    public String groupOf(UUID uuid) {
        if (permission == null) return null;
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(uuid);
        if (off == null) return null;
        return permission.getPrimaryGroup((String) null, off);
    }

    public List<String> groupsOf(UUID uuid) {
        if (permission == null) return Collections.emptyList();
        OfflinePlayer off = plugin.getServer().getOfflinePlayer(uuid);
        if (off == null) return null;
        String[] arr = permission.getPlayerGroups((String) null, off);
        return Arrays.asList(arr);
    }

    public String groupSuffix(String group) {
        if (chat == null) return null;
        return chat.getGroupSuffix((String) null, group);
    }

    public List<String> groupSuffixes(UUID uuid) {
        return groupsOf(uuid).stream()
            .map(this::groupSuffix)
            .filter(Objects::nonNull)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
