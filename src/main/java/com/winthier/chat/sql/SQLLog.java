package com.winthier.chat.sql;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.channel.Channel;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "logs")
@Getter @Setter @NoArgsConstructor
public final class SQLLog {
    @Id private Integer id;
    private UUID player;
    @Column(nullable = false, length = 16) private String sender;
    @Column(nullable = false) private Date time;
    @Column(nullable = false, length = 16) private String server;
    private String world;
    private Integer x, y, z;
    @Column(nullable = false, length = 16) private String channel;
    private String target;
    @Column(nullable = false, length = 511) private String message;

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
        setWorld(loc.getLevel().getName());
        setX((int) Math.floor(loc.getX()));
        setY((int) Math.floor(loc.getY()));
        setZ((int) Math.floor(loc.getZ()));
        setChannel(channel.getKey());
        setTarget(target);
        setMessage(message);
    }

    public static void store(String sender, Channel channel, String target, String message) {
        SQLDB.get().saveAsync(new SQLLog(sender, channel, target, message), null);
    }

    public static void store(Player player, Channel channel, String target, String message) {
        SQLDB.get().saveAsync(new SQLLog(player, channel, target, message), null);
    }
}
