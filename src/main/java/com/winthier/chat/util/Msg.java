package com.winthier.chat.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Msg {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private Msg() { }

    public static void info(CommandSender to, Component content) {
        to.sendMessage(TextComponent.ofChildren(Component.text().content("[Chat]").color(NamedTextColor.DARK_AQUA)
                                                .clickEvent(ClickEvent.suggestCommand("/ch"))
                                                .hoverEvent(HoverEvent.showText(Component.text("/ch", NamedTextColor.DARK_AQUA)))
                                                .build(),
                                                Component.text(" "),
                                                content));
    }

    public static void warn(CommandSender to, Component content) {
        to.sendMessage(TextComponent.ofChildren(Component.text().content("[Chat]").color(NamedTextColor.DARK_RED)
                                                .clickEvent(ClickEvent.suggestCommand("/ch"))
                                                .hoverEvent(HoverEvent.showText(Component.text("/ch", NamedTextColor.DARK_RED)))
                                                .build(),
                                                Component.text(" "),
                                                content));
    }

    public static String camelCase(String msg) {
        StringBuilder sb = new StringBuilder();
        for (String tok: msg.split("_")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tok.substring(0, 1).toUpperCase());
            sb.append(tok.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String toJson(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    public static Component parseComponent(String in) {
        return GsonComponentSerializer.gson().deserialize(in);
    }
}
