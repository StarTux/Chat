package com.winthier.chat.sql;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "channels",
       uniqueConstraints = @UniqueConstraint(columnNames = {"channel_key"}))
@Getter @Setter @NoArgsConstructor
public final class SQLChannel {
    // Content
    @Id private Integer id;
    @Column(nullable = false) private String channelKey;
    @Column(nullable = false) private String tag;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String aliases;
    @Column(nullable = false) private String description;
    private Integer localRange;

    public static List<SQLChannel> fetch() {
        return SQLDB.get().find(SQLChannel.class).findList();
    }
}
