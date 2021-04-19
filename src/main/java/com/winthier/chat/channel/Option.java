package com.winthier.chat.channel;

import com.winthier.chat.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Value;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;

@Value
public final class Option {
    private final String key;
    private final String displayName;
    private final String description;
    private final String defaultValue;

    @Value
    public static final class State {
        private final String value;
        private final String displayName;
        private final String description;
        private final TextColor color;
        private final TextColor activeColor;
    }
    private final List<State> states;

    public static Option colorOption(String key, String displayName, String description, String defaultValue) {
        List<State> states = new ArrayList<>();
        for (ChatColor c: ChatColor.values()) {
            if (!c.isColor()) continue;
            TextColor c2 = NamedTextColor.NAMES.value(c.name().toLowerCase());
            if (c2 == null) c2 = NamedTextColor.WHITE;
            states.add(new State(c.name().toLowerCase(), "" + c.getChar(), displayName + " " + Msg.camelCase(c.name()), c2, c2));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }

    public static Option booleanOption(String key, String displayName, String description, String defaultValue) {
        List<State> states = Arrays.asList(new State("1", "On", displayName + " On", NamedTextColor.DARK_GRAY, NamedTextColor.GREEN),
                                           new State("0", "Off", displayName + " Off", NamedTextColor.DARK_GRAY, NamedTextColor.DARK_RED));
        return new Option(key, displayName, description, defaultValue, states);
    }

    public static Option intOption(String key, String displayName, String description, String defaultValue, int min, int max) {
        List<State> states = new ArrayList<>();
        for (int i = min; i <= max; ++i) {
            String str = "" + i;
            states.add(new State(str, str, displayName + " " + str, NamedTextColor.DARK_GRAY, NamedTextColor.GREEN));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }

    public static Option bracketOption(String key, String displayName, String description, String defaultValue) {
        List<State> states = new ArrayList<>();
        for (BracketType b: BracketType.values()) {
            states.add(new State(b.name().toLowerCase(),
                                 b.opening + b.closing,
                                 displayName + " " + b.opening + " " + b.closing,
                                 NamedTextColor.DARK_GRAY, NamedTextColor.GREEN));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }

    public static Option soundOption(String key, String displayName, String description, String defaultValue) {
        List<State> states = new ArrayList<>();
        states.add(new State("off", "Off", "Off", NamedTextColor.DARK_GRAY, NamedTextColor.GREEN));
        for (SoundCue b: SoundCue.values()) {
            states.add(new State(b.name().toLowerCase(), b.name().substring(0, 1), Msg.camelCase(b.name()), NamedTextColor.DARK_GRAY, NamedTextColor.GREEN));
        }
        return new Option(key, displayName, description, defaultValue, states);
    }
}
