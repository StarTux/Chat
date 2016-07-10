package com.winthier.chat;

import com.winthier.chat.sql.SQLPattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import lombok.Getter;
import org.bukkit.ChatColor;

@Getter
public class MessageFilter {
    final UUID sender;
    final String message;
    final List<Component> components = new ArrayList<>();
    // Output
    List<Object> json = null;
    List<Object> languageFilterJson = null;
    boolean shouldCancel = false;

    public MessageFilter(UUID sender, String message) {
        this.sender = sender;
        this.message = message;
        components.add(new Component(message));
    }

    public void process() {
        findURLs();
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
        } else {
            for (Component component: components) {
                for (SQLPattern pat: SQLPattern.find("URL")) {
                    component.message = pat.replaceAll(message);
                }
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

    class Component {
        String message;

        Component(String message) {
            this.message = message;
        }

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
            String[] arr = message.split(" ");
            if (arr.length > 0) {
                int capsCount = 0;
                for (int i = 0; i < arr.length; ++i) {
                    String word = arr[i];
                    if (word.length() >= 4) {
                        int caps = 0;
                        for (int j = 0; j < word.length(); ++j) {
                            if (Character.isUpperCase(word.charAt(j))) caps += 1;
                        }
                        if (caps * 2 >= word.length()) {
                            arr[i] = word.toLowerCase();
                            capsCount += 1;
                        }
                    }
                }
                if (capsCount * 2 >= arr.length) {
                    message = message.toLowerCase();
                } else if (capsCount > 0) {
                    StringBuilder sb = new StringBuilder(arr[0]);
                    for (int i = 1; i < arr.length; ++i) sb.append(" ").append(arr[i]);
                    message = sb.toString();
                }
            }
        }

        Map<String, Object> toJson() {
            Map<String, Object> result = new HashMap<>();
            result.put("text", message);
            return result;
        }
    }

    class URLComponent extends Component {
        String url;

        URLComponent(String message, String url) {
            super(message);
            this.url = url;
        }

        URLComponent(String message) {
            this(message, message);
        }

        @Override void colorize() {}
        @Override boolean findURL() { return false; }

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

    public boolean shouldCancel() {
        return shouldCancel;
    }
}
