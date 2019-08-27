package com.winthier.chat;

import com.cavetale.dirty.Dirty;
import com.winthier.chat.sql.SQLPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
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
    private boolean shouldCancel = false;
    static final Pattern PING_PATTERN = Pattern
        .compile("@[0-9a-zA-Z_]{1,16}\\b");
    @Setter private boolean staff = false;
    @Setter private Player recipient = null;
    private boolean pinging = false;

    public MessageFilter(UUID sender, String message) {
        this.sender = sender;
        this.message = message;
        components.add(new Component(message));
    }

    public void process() {
        findURLs();
        findItems();
        findPings();
        colorize();
        json = build();
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

    public void findPings() {
        if (!ChatPlugin.getInstance().hasPermission(sender, "chat.ping")) {
            return;
        }
        OUTER: while (true) {
            for (Component component: new ArrayList<>(components)) {
                if (component.findPing()) {
                    continue OUTER;
                }
            }
            break OUTER;
        }
    }

    public void colorize() {
        if (ChatPlugin.getInstance().hasPermission(sender, "chat.color")) {
            for (Component component: components) {
                component.colorize();
            }
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

        boolean findPing() {
            Matcher matcher = PING_PATTERN.matcher(message);
            while (matcher.find()) {
                String group = matcher.group();
                String target = group.substring(1);
                if (target.equals("staff")) {
                    if (!ChatPlugin.getInstance()
                        .hasPermission(sender, "chat.ping.staff")) {
                        continue;
                    }
                } else {
                    if (!ChatPlugin.getInstance()
                        .hasPermission(sender, "chat.ping.player")) {
                        continue;
                    }
                }
                int index = components.indexOf(this);
                components.add(index, new PingComponent(group, target));
                components.add(index, new Component(message.substring(0, matcher.start())));
                message = message.substring(matcher.end());
                return true;
            }
            return false;
        }

        boolean findItem() {
            for (SQLPattern pat: SQLPattern.find("item")) {
                Matcher matcher = pat.getMatcher(message);
                if (matcher.find()) {
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

        @Override boolean findPing() {
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

    private final class PingComponent extends Component {
        private final String target;
        PingComponent(final String message, final String target) {
            super(message);
            this.target = target;
        }

        @Override boolean findItem() {
            return false;
        }

        @Override boolean findURL() {
            return false;
        }

        @Override boolean findPing() {
            return false;
        }

        @Override void colorize() { }

        @Override Map<String, Object> toJson() {
            Map<String, Object> json = new HashMap<>();
            json.put("text", "@" + target);
            boolean bolden = false;
            if (target.equals("staff")) {
                if (recipient != null
                    && recipient.hasPermission("chat.staff")) {
                    bolden = true;
                }
            } else {
                if (recipient != null
                    && recipient.getName().equals(target)) {
                    bolden = true;
                }
            }
            if (bolden) {
                pinging = true;
                json.put("bold", true);
            }
            return json;
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

        @Override boolean findPing() {
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
