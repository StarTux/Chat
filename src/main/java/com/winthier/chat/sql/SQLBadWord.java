package com.winthier.chat.sql;

import com.winthier.chat.ChatPlugin;
import com.winthier.sql.SQLRow;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;

@Data @Table(name = "bad_words")
public final class SQLBadWord implements SQLRow {
    @Id private Integer id;
    @Column(nullable = false, unique = true) String word;

    public SQLBadWord() { }

    public SQLBadWord(final String world) {
        this.word = word;
    }

    protected static ComponentLike replace(MatchResult matchResult, TextComponent.Builder builder) {
        final String stars = "********";
        final String result = stars.substring(0, Math.min(stars.length(), matchResult.end() - matchResult.start()));
        return Component.text(result);
    }

    public static void loadBadWordsAsync() {
        SQLDB.get().find(SQLBadWord.class).findListAsync(SQLBadWord::acceptSync);
    }

    public static void loadBadWords() {
        acceptSync(SQLDB.get().find(SQLBadWord.class).findList());
    }

    private static void acceptSync(List<SQLBadWord> rows) {
        List<String> words = new ArrayList<>(rows.size());
        for (SQLBadWord row : rows) {
            String word = row.getWord()
                .replace("i", "[i1]")
                .replace("o", "[o0]");
            words.add(word);
        }
        List<TextReplacementConfig> configList = new ArrayList<>(words.size());
        List<Pattern> patternList = new ArrayList<>(words.size());
        for (String word : words) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(word, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException pse) {
                ChatPlugin.getInstance().getLogger().log(Level.SEVERE, "Pattern " + word, pse);
                continue;
            }
            patternList.add(pattern);
            TextReplacementConfig config = TextReplacementConfig.builder()
                .match(pattern)
                .replacement(SQLBadWord::replace)
                .build();
            configList.add(config);
        }
        ChatPlugin.getInstance().setBadWordConfigList(List.copyOf(configList));
        ChatPlugin.getInstance().setBadWordList(List.copyOf(patternList));
    }
}
