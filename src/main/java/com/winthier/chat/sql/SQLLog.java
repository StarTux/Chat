package com.winthier.chat.sql;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.channel.Channel;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Table(name = "logs")
@Getter @Setter @NoArgsConstructor
public final class SQLLog implements SQLRow {
    @Id private Integer id;
    private UUID player;
    @Column(nullable = false, length = 16) private String sender;
    @Column(nullable = false) private Date time;
    @Column(nullable = false, length = 16) private String server;
    private String world;
    private Integer x;
    private Integer y;
    private Integer z;
    @Column(nullable = false, length = 16) private String channel;
    private String target;
    @Column(nullable = false, length = 511) private String message;

    private SQLLog(final String sender, final Channel channel, final String target, final String message) {
        setTime(new Date());
        setServer(ChatPlugin.getInstance().getServerName());
        setSender(sender);
        setChannel(channel.getKey());
        setTarget(target);
        setMessage(message);
    }

    private SQLLog(final Player player, final Channel channel, final String target, final String message) {
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
        SQLDB.get().saveAsync(new SQLLog(sender, channel, target, message), null);
    }

    public static void store(Player player, Channel channel, String target, String message) {
        SQLDB.get().saveAsync(new SQLLog(player, channel, target, message), null);
    }
}
