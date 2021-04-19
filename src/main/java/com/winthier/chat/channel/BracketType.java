package com.winthier.chat.channel;

public enum BracketType {
    PAREN("(", ")"),
    BRACKETS("[", "]"),
    CURLY("{", "}"),
    ANGLE("<", ">");

    public final String opening;
    public final String closing;

    BracketType(final String opening, final String closing) {
        this.opening = opening;
        this.closing = closing;
    }

    static BracketType of(String val) {
        if (val == null) return BracketType.BRACKETS;
        try {
            return valueOf(val.toUpperCase());
        } catch (IllegalArgumentException ile) {
            return BracketType.BRACKETS;
        }
    }
}
