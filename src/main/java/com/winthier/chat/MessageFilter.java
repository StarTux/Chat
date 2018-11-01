package com.winthier.chat;

import com.cavetale.dirty.Dirty;
import com.winthier.chat.sql.SQLPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONValue;

@Getter
public final class MessageFilter {
    private final UUID sender;
    private final String message;
    private final List<Component> components = new ArrayList<>();
    // Output
    private List<Object> json = null;
    private List<Object> languageFilterJson = null;
    private boolean shouldCancel = false;

    public MessageFilter(UUID sender, String message) {
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
            sb.append(component.message);
        }
        return sb.toString();
    }

    @AllArgsConstructor
    private class Component {
        private String message;

        void colorize() {
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        boolean findURL() {
            for (SQLPattern pat: SQLPattern.find("URL")) {
                Matcher matcher = pat.getMatcher(message);
                if (matcher.find()) {
                    int index = components.indexOf(this);
                    components.add(index, new URLComponent(matcher.group()));
                    components.add(index, new Component(message.substring(0, matcher.start())));
                    message = message.substring(matcher.end());
                    return true;
                }
            }
            return false;
        }

        boolean findItem() {
            for (SQLPattern pat: SQLPattern.find("item")) {
                Matcher matcher = pat.getMatcher(message);
                if (matcher.find()) {
                    System.out.println("matcher find");
                    int index = components.indexOf(this);
                    Map<String, Object> raw = new HashMap<>();
                    raw.put("text", "[item]");
                    Map<String, Object> hoverEvent = new HashMap<>();
                    raw.put("hoverEvent", hoverEvent);
                    hoverEvent.put("action", "show_item");
                    Map<String, Object> value = new HashMap<>();
                    Player player = Bukkit.getPlayer(sender);
                    if (player != null) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (item != null) {
                            value.put("id", "minecraft:" + item.getType().name().toLowerCase());
                            value.put("Count", item.getAmount());
                            value.put("tag", Dirty.getItemTag(item));
                        }
                    }
                    hoverEvent.put("value", JSONValue.toJSONString(value));
                    components.add(index, new RawComponent("[item]", raw));
                    components.add(index, new Component(message.substring(0, matcher.start())));
                    message = message.substring(matcher.end());
                    return true;
                }
            }
            return false;
        }

        void filterLanguage() {
            for (SQLPattern pat: SQLPattern.find("Language")) {
                message = pat.replaceWithAsterisks(message);
            }
        }

        void filterSpam() {
            for (SQLPattern pat: SQLPattern.find("Spam")) {
                if (pat.getReplacement().isEmpty()) {
                    if (pat.getMatcher(message).find()) {
                        shouldCancel = true;
                    }
                } else {
                    message = pat.replaceAll(message);
                }
            }
        }

        Map<String, Object> toJson() {
            Map<String, Object> result = new HashMap<>();
            result.put("text", message);
            return result;
        }
    }

    private final class URLComponent extends Component {
        private final String url;

        URLComponent(String message, String url) {
            super(message);
            this.url = url;
        }

        URLComponent(String message) {
            this(message, message);
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
        Map<String, Object> toJson() {
            Map<String, Object> result = super.toJson();
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
            return result;
        }
    }

    private final class RawComponent extends Component {
        private final Map<String, Object> raw;

        RawComponent(String message, Map<String, Object> raw) {
            super(message);
            this.raw = raw;
        }

        @Override boolean findItem() {
            return false;
        }
        @Override boolean findURL() {
            return false;
        }

        @Override void colorize() { }

        @Override Map<String, Object> toJson() {
            return this.raw;
        }
    }

    public boolean shouldCancel() {
        return shouldCancel;
    }
}
