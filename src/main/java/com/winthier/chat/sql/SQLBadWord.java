package com.winthier.chat.sql;

import com.winthier.chat.ChatPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import net.kyori.adventure.text.TextReplacementConfig;

@Data @Table(name = "bad_words")
public final class SQLBadWord {
    @Id private Integer id;
    @Column(nullable = false, unique = true) String word;

    public SQLBadWord() { }

    public SQLBadWord(final String world) {
        this.word = word;
    }

    public static void loadAllAsync() {
        SQLDB.get().find(SQLBadWord.class).findListAsync(rows -> {
                List<String> words = new ArrayList<>(rows.size());
                for (SQLBadWord row : rows) {
                    String word = row.getWord();
                    words.add(word);
                    if (word.contains("i")) words.add(word.replace("i", "1"));
                    if (word.contains("o")) words.add(word.replace("o", "0"));
                }
                List<TextReplacementConfig> configs = new ArrayList<>(words.size());
                for (String word : words) {
                    Pattern pattern;
                    try {
                        pattern = Pattern.compile(word, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
                    } catch (PatternSyntaxException pse) {
                        pse.printStackTrace();
                        continue;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < word.length(); i += 1) sb.append("*");
                    TextReplacementConfig config = TextReplacementConfig.builder()
                        .match(pattern)
                        .replacement(sb.toString())
                        .build();
                    configs.add(config);
                }
                ChatPlugin.getInstance().getBadWords().clear();
                ChatPlugin.getInstance().getBadWords().addAll(configs);
            });
    }
}
