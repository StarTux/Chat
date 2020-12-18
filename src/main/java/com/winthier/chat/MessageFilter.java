package com.winthier.chat;

import com.cavetale.dirty.Dirty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.winthier.chat.sql.SQLPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter
public final class MessageFilter {
    private final UUID sender;
    private final String message;
    private final List<Component> components = new ArrayList<>();
    // Output
    private List<Object> json = null;
    private List<Object> languageFilterJson = null;
    private boolean shouldCancel = false;
    // Helper
    static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public MessageFilter(final UUID sender, final String message) {
        this.sender = sender;
        this.message = message;
        components.add(new Component(message));
    }

    public void process() {
        findURLs();
        findItems();
        colorize();
        filterSpam();
        json = build();
        filterLanguage();
        languageFilterJson = build();
    }

    public void findURLs() {
        if (ChatPlugin.getInstance().hasPermission(sender, "chat.url")) {
        OUTER: while (true) {
                for (Component component: new ArrayList<>(components)) {
                    if (component.findURL()) {
                        continue OUTER;
                    }
                }
                break OUTER;
            }
        }
    }

    public void findItems() {
        if (ChatPlugin.getInstance().hasPermission(sender, "chat.item")) {
        OUTER: while (true) {
                for (Component component: new ArrayList<>(components)) {
                    if (component.findItem()) {
                        continue OUTER;
                    }
                }
                break OUTER;
            }
        }
    }

    public void colorize() {
        if (ChatPlugin.getInstance().hasPermission(sender, "chat.color")) {
            for (Component component: components) {
                component.colorize();
            }
        }
    }

    public void filterSpam() {
        for (Component component: components) {
            component.filterSpam();
        }
    }

    public void filterLanguage() {
        for (Component component: components) {
            component.filterLanguage();
        }
    }

    public List<Object> build() {
        List<Object> result = new ArrayList<>();
        for (Component component: components) {
            result.add(component.toJson());
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Component component: components) {
            sb.append(component.text);
        }
        return sb.toString();
    }

    @AllArgsConstructor
    private class Component {
        protected String text;
        static final String URL_VALID = ""
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyz"
                + "0123456789-._~:/?#[]@!$&'()*+,;=";
        static final String URL_SPECIAL = "~:/?#[]@!$&'()*+,;=";

        void colorize() {
            text = ChatColor.translateAlternateColorCodes('&', text);
        }

        boolean findURL() {
            int start = text.indexOf("http://");
            int end = start + 7;
            if (start < 0) {
                start = text.indexOf("https://");
                end = start + 8;
            }
            if (start < 0) {
                start = text.indexOf("www.");
                end = start + 4;
            }
            if (start < 0) return false;
            int slash = 0;
            int dots = 0;
            while (true) {
                if (end >= text.length() - 1) break;
                char c = text.charAt(end + 1);
                if (c == '.') dots += 1;
                if (c == '/') slash += 1;
                if (URL_SPECIAL.indexOf(c) >= 0) {
                    if (dots < 1 || slash < 1) return false;
                }
                if (URL_VALID.indexOf(c) < 0) break;
                end += 1;
            }
            if (dots < 1) return false;
            if (text.charAt(end) == '.') end -= 1;
            String url = text.substring(start, end + 1);
            int index = components.indexOf(this);
            components.add(index, new URLComponent(url));
            components.add(index, new Component(text.substring(0, start)));
            text = text.substring(end + 1);
            return true;
        }

        boolean findItem() {
            for (SQLPattern pat: SQLPattern.find("item")) {
                Matcher matcher = pat.getMatcher(text);
                if (matcher.find()) {
                    int index = components.indexOf(this);
                    Map<String, Object> raw = new HashMap<>();
                    raw.put("text", "item");
                    Map<String, Object> hoverEvent = new HashMap<>();
                    raw.put("hoverEvent", hoverEvent);
                    hoverEvent.put("action", "show_item");
                    Map<String, Object> value = new HashMap<>();
                    Player player = Bukkit.getPlayer(sender);
                    if (player != null) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item != null && item.getAmount() > 0) {
                            value.put("id", item.getType().getKey().toString());
                            value.put("Count", item.getAmount());
                            Map<String, Object> tag = Dirty.getItemTag(item);
                            boolean hasDisplayName = false;
                            if (tag != null) {
                                value.put("tag", tag);
                                Object o = tag.get("display");
                                if (o instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> displayTag = (Map<String, Object>) o;
                                    Object nameTag = displayTag.get("Name");
                                    if (nameTag != null) {
                                        if (nameTag instanceof String) {
                                            nameTag = GSON.fromJson((String) nameTag, Object.class);
                                        }
                                        if (nameTag instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> extraMap = (Map<String, Object>) nameTag;
                                            Object extraTag = extraMap.get("extra");
                                            if (extraTag != null) nameTag = extraTag;
                                        }
                                        raw.put("text", "");
                                        raw.put("extra", nameTag);
                                        hasDisplayName = true;
                                    }
                                }
                            }
                            if (!hasDisplayName) {
                                raw.put("text", item.getI18NDisplayName());
                            }
                        }
                    }
                    hoverEvent.put("value", GSON.toJson(value));
                    components.add(index, new RawComponent("]", ChatColor.RESET + "]"));
                    components.add(index, new RawComponent("[item]", raw));
                    components.add(index, new RawComponent("[", "["));
                    components.add(index, new Component(text.substring(0, matcher.start())));
                    text = text.substring(matcher.end());
                    return true;
                }
            }
            return false;
        }

        void filterLanguage() {
            for (SQLPattern pat: SQLPattern.find("Language")) {
                text = pat.replaceWithAsterisks(text);
            }
        }

        void filterSpam() {
            for (SQLPattern pat: SQLPattern.find("Spam")) {
                if (pat.getReplacement().isEmpty()) {
                    if (pat.getMatcher(text).find()) {
                        shouldCancel = true;
                    }
                } else {
                    text = pat.replaceAll(text);
                }
            }
        }

        Object toJson() {
            Map<String, Object> result = new HashMap<>();
            result.put("text", text);
            return result;
        }
    }

    private final class URLComponent extends Component {
        private final String url;

        URLComponent(final String text, final String url) {
            super(text);
            this.url = url;
        }

        URLComponent(final String url) {
            this(url, url);
        }

        @Override
        void colorize() { }

        @Override boolean findItem() {
            return false;
        }

        @Override boolean findURL() {
            return false;
        }

        @Override
        Object toJson() {
            Map<String, Object> result = new HashMap<>();
            result.put("text", text);
            Map<String, Object> clickEvent = new HashMap<>();
            result.put("clickEvent", clickEvent);
            clickEvent.put("action", "open_url");
            if (!url.startsWith("http://") && (!url.startsWith("https://"))) {
                clickEvent.put("value", "http://" + url);
            } else {
                clickEvent.put("value", url);
            }
            Map<String, Object> hoverEvent = new HashMap<>();
            result.put("hoverEvent", hoverEvent);
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", "url");
            return result;
        }
    }

    private final class RawComponent extends Component {
        private final Object raw;

        RawComponent(final String text, final Object raw) {
            super(text);
            this.raw = raw;
        }

        @Override boolean findItem() {
            return false;
        }

        @Override boolean findURL() {
            return false;
        }

        @Override void colorize() { }

        @Override Object toJson() {
            return raw;
        }
    }

    public boolean shouldCancel() {
        return shouldCancel;
    }

    /**
     * Helper function for Component.
     */
    static String filterLanguage(String in) {
        for (SQLPattern pat: SQLPattern.find("Language")) {
            in = pat.replaceWithAsterisks(in);
        }
        return in;
    }

    /**
     * Helper function for Component::findItem.
     */
    static String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String name = meta.getDisplayName();
                if (name != null && !name.isEmpty()) {
                    return filterLanguage(name);
                }
            }
        }
        String name = item.getI18NDisplayName();
        if (name != null) {
            return name;
        }
        return null;
    }
}
