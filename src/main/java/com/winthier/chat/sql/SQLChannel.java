package com.winthier.chat.sql;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.util.List;
import java.util.UUID;
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
    @NotNull String channelKey;
    @NotNull String tag;
    @NotNull String title;
    @NotNull String aliases;
    @NotNull String description;
    Integer localRange;
    
    public static List<SQLChannel> fetch() {
        return SQLDB.get().find(SQLChannel.class).findList();
    }
}
