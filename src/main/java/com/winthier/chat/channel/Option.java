package com.winthier.chat.channel;

import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import org.bukkit.ChatColor;

@AllArgsConstructor
public class Option {
    public final String key;
    public final String displayName;
    public final String defaultValue;
    @AllArgsConstructor
    public static class State {
        public final String value;
        public final String displayName;
        public final String description;
        public final ChatColor color;
        public final ChatColor activeColor;
    }
    public final List<State> states;

    static Option colorOption(String key, String displayName, String defaultValue) {
        List<State> states = new ArrayList<>();
        for (ChatColor c: ChatColor.values()) {
            if (!c.isColor()) continue;
            states.add(new State(c.name().toLowerCase(), ""+c.getChar(), displayName+" "+Msg.camelCase(c.name()), c, c));
        }
        return new Option(key, displayName, defaultValue, states);
    }

    static Option booleanOption(String key, String displayName, String defaultValue) {
        return new Option(key, displayName, defaultValue, Arrays.asList(
                              new State("1", "On", displayName+" On", ChatColor.DARK_GRAY, ChatColor.GREEN),
                              new State("0", "Off", displayName+" Off", ChatColor.DARK_GRAY, ChatColor.DARK_RED)));
    }

    static Option bracketOption(String key, String displayName, String defaultValue) {
        List<State> states = new ArrayList<>();
        for (AbstractChannel.BracketType b: AbstractChannel.BracketType.values()) {
            states.add(new State(b.name().toLowerCase(), b.opening+b.closing, b.opening+" "+b.closing, ChatColor.DARK_GRAY, ChatColor.GREEN));
        }
        return new Option(key, displayName, defaultValue, states);
    }
}
