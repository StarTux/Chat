package com.winthier.chat.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patterns")
@Getter
@Setter
@NoArgsConstructor
public final class SQLPattern {
    private static final String ASTERISKS = "*********************************************************************************************************************************************************************************************************************************************************************************************************************************************************************";
    // Cache
    private static Map<String, List<SQLPattern>> cache = null;

    // Content
    @Id private Integer id;
    @Column(nullable = false, length = 16) private String category;
    @Column(nullable = false, length = 255) private String regex;
    @Column(nullable = false, length = 255) private String replacement;
    private transient Pattern pattern = null;

    public Pattern getPattern() {
        if (pattern == null) {
            pattern = Pattern.compile(getRegex(), Pattern.CASE_INSENSITIVE);
        }
        return pattern;
    }

    public Matcher getMatcher(String msg) {
        return getPattern().matcher(msg);
    }

    public String replaceAll(String msg) {
        return getMatcher(msg).replaceAll(getReplacement());
    }

    private static String asterisks(int size) {
        return ASTERISKS.substring(0, size);
    }

    public String replaceWithAsterisks(String msg) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = getMatcher(msg);
        while (matcher.find()) {
            matcher.appendReplacement(sb, asterisks(matcher.end() - matcher.start()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static List<SQLPattern> find(String category) {
        if (cache == null) {
            cache = new HashMap<>();
            for (SQLPattern entry: SQLDB.get().find(SQLPattern.class).findList()) {
                List<SQLPattern> list = cache.get(entry.getCategory());
                if (list == null) {
                    list = new ArrayList<>();
                    cache.put(entry.getCategory(), list);
                }
                list.add(entry);
            }
        }
        List<SQLPattern> result = cache.get(category);
        if (result != null) return result;
        return new ArrayList<>();
    }

    static void clearCache() {
        if (cache == null) return;
        cache.clear();
        cache = null;
    }
}
