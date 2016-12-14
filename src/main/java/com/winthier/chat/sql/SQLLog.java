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
    UUID player;
    @NotNull String sender;
    @NotNull Date time;
    @NotNull String server;
    String world;
    Integer x, y, z;
    @NotNull String channel;
    String target;
    @NotNull @Length(max=511) String message;

    private SQLLog(String sender, Channel channel, String target, String message) {
        setTime(new Date());
        setServer(ChatPlugin.getInstance().getServerName());
        setSender(sender);
        setChannel(channel.getKey());
        setTarget(target);
        setMessage(message);
    }

    private SQLLog(Player player, Channel channel, String target, String message) {
        setTime(new Date());
        setServer(ChatPlugin.getInstance().getServerName());
        setPlayer(player.getUniqueId());
        setSender(player.getName());
        Location loc = player.getLocation();
        setWorld(loc.getWorld().getName());
        setX(loc.getBlockX());
        setY(loc.getBlockY());
        setZ(loc.getBlockZ());
        setChannel(channel.getKey());
        setTarget(target);
        setMessage(message);
    }

    public static void store(String sender, Channel channel, String target, String message) {
        SQLDB.get().save(new SQLLog(sender, channel, target, message));
    }

    public static void store(Player player, Channel channel, String target, String message) {
        SQLDB.get().save(new SQLLog(player, channel, target, message));
    }
}
