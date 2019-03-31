package com.winthier.chat.channel;

import cn.nukkit.utils.TextFormat;
import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Option {
    public final String key;
    public final String displayName;
    public final String description;
    public final String defaultValue;
    @AllArgsConstructor
    public static class State {
        public final String value;
        public final String displayName;
        public final String description;
        public final TextFormat color;
        public final TextFormat activeColor;
    }
    public final List<State> states;

    static Option colorOption(String key, String displayName, String description, String defaultValue) {
        List<State> states = new ArrayList<>();
        for (TextFormat c: TextFormat.values()) {
            if (!c.isColor()) continue;
            states.add(new State(c.name().toLowerCase(), "" + c.getChar(), displayName + " " + Msg.camelCase(c.name()), c, c));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }

    static Option booleanOption(String key, String displayName, String description, String defaultValue) {
        return new Option(key, displayName, description, defaultValue, Arrays.asList(
                              new State("1", "On", displayName + " On", TextFormat.DARK_GRAY, TextFormat.GREEN),
                              new State("0", "Off", displayName + " Off", TextFormat.DARK_GRAY, TextFormat.DARK_RED)));
    }

    static Option intOption(String key, String displayName, String description, String defaultValue, int min, int max) {
        List<State> states = new ArrayList<>();
        for (int i = min; i <= max; ++i) {
            String str = "" + i;
            states.add(new State(str, str, displayName + " " + str, TextFormat.DARK_GRAY, TextFormat.GREEN));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }

    static Option bracketOption(String key, String displayName, String description, String defaultValue) {
        List<State> states = new ArrayList<>();
        for (BracketType b: BracketType.values()) {
            states.add(new State(b.name().toLowerCase(), b.opening + b.closing, displayName + " " + b.opening + " " + b.closing, TextFormat.DARK_GRAY, TextFormat.GREEN));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }

    static Option soundOption(String key, String displayName, String description, String defaultValue) {
        List<State> states = new ArrayList<>();
        states.add(new State("off", "Off", "Off", TextFormat.DARK_GRAY, TextFormat.GREEN));
        for (SoundCue b: SoundCue.values()) {
            states.add(new State(b.name().toLowerCase(), b.name().substring(0, 1), Msg.camelCase(b.name()), TextFormat.DARK_GRAY, TextFormat.GREEN));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }
}
