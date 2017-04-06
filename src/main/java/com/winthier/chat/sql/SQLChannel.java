package com.winthier.chat.sql;

import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Table(name = "channels",
       uniqueConstraints = @UniqueConstraint(columnNames = {"channel_key"}))
@Getter
@Setter
@NoArgsConstructor
public class SQLChannel {
    // Content
    @Id Integer id;
    @Column(nullable = false) String channelKey;
    @Column(nullable = false) String tag;
    @Column(nullable = false) String title;
    @Column(nullable = false) String aliases;
    @Column(nullable = false) String description;
    Integer localRange;
    
    public static List<SQLChannel> fetch() {
        return SQLDB.get().find(SQLChannel.class).findList();
    }
}
