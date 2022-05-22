package com.winthier.chat.sql;

import com.winthier.sql.SQLRow;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;

@Table(name = "channels",
       uniqueConstraints = @UniqueConstraint(columnNames = {"channel_key"}))
@Data
public final class SQLChannel implements SQLRow {
    // Content
    @Id private Integer id;
    @Column(nullable = false, length = 16) private String channelKey;
    @Column(nullable = false, length = 8) private String tag;
    @Column(nullable = false, length = 16) private String title;
    @Column(nullable = false, length = 64) private String aliases;
    @Column(nullable = false, length = 255) private String description;
    @Column(nullable = false) private int localRange;

    /**
     * Called through ChatPlugin::onEnable() and AdminCommand /reload.
     */
    public static List<SQLChannel> fetch() {
        return SQLDB.get().find(SQLChannel.class).findList();
    }
}
