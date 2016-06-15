package com.winthier.chat.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.channel.Channel;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
public class SQLLog {
    @Id Integer id;
    @NotNull UUID player;
    @NotNull Date time;
    @NotNull String server;
    @NotNull String world;
    @NotNull Integer x, y, z;
    @NotNull String channel;
    @NotNull String message;

    private SQLLog(Player player, Channel channel, String message) {
        setPlayer(player.getUniqueId());
        setTime(new Date());
        setServer(ChatPlugin.getInstance().getServerName());
        Location loc = player.getLocation();
        setWorld(loc.getWorld().getName());
        setX(loc.getBlockX());
        setY(loc.getBlockY());
        setZ(loc.getBlockZ());
        setChannel(channel.getKey());
        setMessage(message);
    }

    public static void store(Player player, Channel channel, String message) {
        SQLLog log = new SQLLog(player, channel, message);
        SQLDB.get().save(log);
    }
}
