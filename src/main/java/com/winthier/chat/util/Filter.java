package com.winthier.chat.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;

public final class Filter {
    static final String URL_VALID = ""
        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        + "abcdefghijklmnopqrstuvwxyz"
        + "0123456789-._~:/?#[]@!$&'()*+,;=";
    static final String URL_SPECIAL = "~:/?#[]@!$&'()*+,;=%";

    private Filter() { }

    public static String filterCaps(String msg) {
        if (msg.length() <= 3) return msg;
        final int len = msg.length();
        for (int i = 0; i < len; i += 1) {
            if (Character.isLowerCase(msg.charAt(i))) {
                return msg;
            }
        }
        return msg.toLowerCase();
    }

    public static String filterUnicode(String msg) {
        StringBuilder sb = null;
        final int len = msg.length();
        for (int i = 0; i < len; i += 1) {
            char c = msg.charAt(i);
            if ((int) c >= 0x0200) {
                if (sb == null) sb = new StringBuilder(msg);
                sb.setCharAt(i, '?');
            }
        }
        return sb != null ? sb.toString() : msg;
    }

    public static String filterLegacyColors(String msg, boolean color, boolean format, boolean obfuscate) {
        StringBuilder sb = null;
        final int len = msg.length() - 1;
        for (int i = 0; i < len; i += 1) {
            char c = msg.charAt(i);
            if (c != '&') continue;
            char d = msg.charAt(i + 1);
            ChatColor chatColor = ChatColor.getByChar(d);
            if (chatColor == null) continue;
            if (chatColor == ChatColor.MAGIC) {
                if (!obfuscate) continue;
            } else if (chatColor == ChatColor.RESET) {
                if (!color) continue;
            } else if (chatColor.isFormat()) {
                if (!format) continue;
            } else if (chatColor.isColor()) {
                if (!color) continue;
            } else {
                continue;
            }
            if (sb == null) sb = new StringBuilder(msg);
            sb.setCharAt(i, ChatColor.COLOR_CHAR);
            i += 1;
        }
        return sb != null ? sb.toString() : msg;
    }

    public static String findUrl(String text) {
        int http = text.indexOf("http://");
        int https = text.indexOf("https://");
        int www = text.indexOf("www.");
        if (http < 0 && https < 0 && www < 0) return null;
        int start = Integer.MAX_VALUE;
        int end = 0;
        if (http >= 0) {
            start = http;
            end = start + 7;
        }
        if (https >= 0 && https < start) {
            start = https;
            end = start + 8;
        }
        if (www >= 0 && www < start) {
            start = www;
            end = start + 4;
        }
        if (start < 0) return null;
        int slash = 0;
        int dots = 0;
        while (true) {
            if (end >= text.length() - 1) break;
            char c = text.charAt(end + 1);
            if (c == '.') dots += 1;
            if (c == '/') slash += 1;
            if (URL_SPECIAL.indexOf(c) >= 0) {
                if (dots < 1 || slash < 1) return null;
            }
            if (URL_VALID.indexOf(c) < 0) break;
            end += 1;
        }
        if (dots < 1) return null;
        if (text.charAt(end) == '.') end -= 1;
        return text.substring(start, end + 1);
    }

    public static List<String> findUrls(String text) {
        List<String> urls = null;
        String url;
        while (true) {
            url = findUrl(text);
            if (url == null) return urls;
            if (urls == null) {
                urls = new ArrayList<>();
            }
            urls.add(url);
            text = text.substring(text.indexOf(url) + url.length());
        }
    }
}
