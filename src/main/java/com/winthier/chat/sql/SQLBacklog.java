package com.winthier.chat.sql;

import com.winthier.chat.util.Msg;
import com.winthier.sql.SQLRow.Name;
import com.winthier.sql.SQLRow.NotNull;
import com.winthier.sql.SQLRow;
import java.util.Date;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Data @NotNull @Name("backlog")
public final class SQLBacklog implements SQLRow {
    @Id private Integer id;
    private UUID player;
    @Keyed private Date time;
    @Text private String messageJson;

    public SQLBacklog() { }

    public SQLBacklog(final Player player, final Component message) {
        this.player = player.getUniqueId();
        this.time = new Date();
        this.messageJson = Msg.toJson(message);
    }

    public Component getMessageComponent() {
        return Msg.parseComponent(messageJson);
    }
}
