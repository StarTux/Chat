// package com.winthier.chat.sql;

// import com.avaje.ebean.validation.Length;
// import com.avaje.ebean.validation.NotEmpty;
// import com.avaje.ebean.validation.NotNull;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.UUID;
// import javax.persistence.Entity;
// import javax.persistence.Id;
// import javax.persistence.Table;
// import javax.persistence.UniqueConstraint;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.NonNull;
// import lombok.Setter;

// @Entity
// @Table(name = "players",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"uuid"}))
// @Getter
// @Setter
// @NoArgsConstructor
// public class SQLPlayer
// {
//     // Cache
//     final static Map<UUID, SQLPlayer> cache = new HashMap<>();
//     // Content
//     @Id Integer id;
//     @NotNull UUID uuid;

//     private SQLPlayer(UUID uuid)
//     {
// 	setUuid(uuid);
//     }

//     public static SQLPlayer of(UUID uuid)
//     {
// 	SQLPlayer result = cache.get(uuid);
// 	if (result == null) {
// 	    result = SQLDB.get().find(SQLPlayer.class).where().eq("uuid", uuid).findUnique();
// 	    if (result == null) {
// 		result = new SQLPlayer(uuid);
// 		SQLDB.get().save(result);
// 	    }
// 	    cache.put(uuid, result);
// 	}
// 	return result;
//     }
// }
