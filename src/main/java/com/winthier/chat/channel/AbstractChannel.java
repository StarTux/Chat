package com.winthier.chat.channel;

import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.MessageFilter;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter @Setter
public abstract class AbstractChannel implements Channel {
    protected String title;
    protected String key;
    protected String tag;
    protected String description;
    protected int range = 0;
    protected final List<String> aliases = new ArrayList<>();
    protected final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    protected final List<Option> options = new ArrayList<>();

    AbstractChannel() {
        options.add(Option
                    .booleanOption("ShowChannelTag", "Show Channel Tag",
                                   "Show the channel tag at the beginning of every message",
                                   "0"));
        options.add(Option
                    .booleanOption("ShowPlayerTitle", "Show Player Title",
                                   "Show a player's current title in every message",
                                   "1"));
        options.add(Option
                    .booleanOption("ShowServer", "Show Server",
                                   "Show a player's server in every message",
                                   "0"));
        options.add(Option
                    .colorOption("ChannelColor", "Channel Color",
                                 "Main channel color",
                                 "white"));
        options.add(Option
                    .colorOption("TextColor", "Text Color",
                                 "Color of chat messages",
                                 "white"));
        options.add(Option
                    .colorOption("SenderColor", "Player Color",
                                 "Color of player names",
                                 "white"));
        options.add(Option
                    .colorOption("BracketColor", "Bracket Color",
                                 "Color of brackets and punctuation",
                                 "white"));
        options.add(Option
                    .bracketOption("BracketType", "Brackets",
                                   "Appearance of brackets",
                                   "angle"));
        options.add(Option
                    .soundOption("SoundCueChat", "Chat Cue",
                                 "Sound played when you receive a message",
                                 "off"));
        options.add(Option
                    .intOption("SoundCueChatVolume", "Chat Cue Volume",
                               "Sound played when you receive a message",
                               "10", 1, 10));
        options.add(Option
                    .booleanOption("LanguageFilter", "Language Filter",
                                   "Filter out foul language",
                                   "1"));
    }

    @Override
    public final String getAlias() {
        return getAliases().get(0);
    }

    @Override
    public final boolean hasPermission(Player player) {
        return canTalk(player.getUniqueId());
    }

    @Override
    public final void setFocusChannel(UUID player) {
        SQLSetting.set(player, null, "FocusChannel", getKey());
    }

    @Override
    public final void joinChannel(UUID player) {
        SQLSetting.set(player, getKey(), "Joined", true);
    }

    @Override
    public final void leaveChannel(UUID player) {
        SQLSetting.set(player, getKey(), "Joined", false);
    }

    @Override
    public final boolean isJoined(UUID player) {
        return SQLSetting.getBoolean(player, getKey(), "Joined", true);
    }

    @Override
    public final Channel getChannel() {
        return this;
    }

    /**
     * Override if channel command syntax is more specific.
     */
    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        playerDidUseCommand(context);
    }

    @Override
    public final void announce(Object msg) {
        announce(msg, false);
    }

    @Override
    public final void announceLocal(Object msg) {
        announce(msg, true);
    }

    private void announce(Object msg, boolean local) {
        Message message;
        if (msg instanceof String) {
            message = makeMessage(null, (String) msg);
        } else {
            List<Object> json;
            if (msg instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> tmpson = (List<Object>) msg;
                json = tmpson;
            } else {
                json = new ArrayList<>();
                json.add(msg);
            }
            String str = Msg.jsonToString(msg);
            message = makeMessage(null, str);
            message.json = json;
            message.languageFilterJson = json;
        }
        message.local = local;
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    final void fillMessage(Message message) {
        if (message.senderTitle == null && message.senderTitleJson == null) {
            ChatPlugin.getInstance().loadTitle(message);
        }
        if (message.message != null && (message.json == null || message.languageFilterJson == null)) {
            MessageFilter filter = new MessageFilter(message.sender, message.message);
            filter.process();
            if (message.json == null) {
                message.json = filter.getJson();
            }
            if (message.languageFilterJson == null) {
                message.languageFilterJson = filter.getLanguageFilterJson();
            }
            message.languageFilterMessage = filter.toString();
            message.shouldCancel = filter.shouldCancel();
        }
    }

    public final Message makeMessage(Player player, String text) {
        Message message = new Message();
        message.channel = getKey();
        if (player != null) {
            message.sender = player.getUniqueId();
            message.senderName = player.getName();
            message.location = player.getLocation();
        }
        message.senderServer = ChatPlugin.getInstance().getServerName();
        message.senderServerDisplayName = ChatPlugin.getInstance().getServerDisplayName();
        message.message = text;
        fillMessage(message);
        return message;
    }

    final Object channelTag(ChatColor channelColor, ChatColor bracketColor,
                            BracketType bracketType) {
        String text = bracketColor + bracketType.opening
            + channelColor + getTag()
            + bracketColor + bracketType.closing;
        String tooltip = getTitle() + "\n&5&o" + getDescription();
        String cmd = "/" + getAlias() + " ";
        return Msg.button(channelColor, text, tooltip, cmd);
    }

    final Object serverTag(Message message, ChatColor serverColor,
                           ChatColor bracketColor, BracketType bracketType) {
        String name;
        if (message.senderServerDisplayName != null) {
            name = message.senderServerDisplayName;
        } else if (message.senderServer != null) {
            name = message.senderServer;
        } else {
            return "";
        }
        String text = bracketColor + bracketType.opening
            + serverColor + name
            + bracketColor + bracketType.closing;
        return Msg.button(serverColor, text, null, null);
    }

    final Object senderTitleTag(Message message, ChatColor bracketColor, BracketType bracketType) {
         if (message.senderTitleJson != null) {
            List<Object> list = (List<Object>) Msg.GSON.fromJson(message.senderTitleJson, List.class);
            if (list == null) return "";
            List<Object> extra = new ArrayList<>();
            extra.add(Msg.button(bracketColor, bracketType.opening, null, null));
            extra.addAll(list);
            extra.add(Msg.button(bracketColor, bracketType.closing, null, null));
            List<Object> tooltip = new ArrayList<>();
            tooltip.add(Msg.extra(list));
            if (message.senderTitleDescription != null) {
                Map<String, Object> tooltip2 = new HashMap<>();
                tooltip2.put("text", "\n" + Msg.format(message.senderTitleDescription));
                tooltip.add(tooltip2);
            }
            Map<String, Object> hoverEvent = new HashMap<>();
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", tooltip);
            Map<String, Object> result = new HashMap<>();
            result.put("color", bracketColor.getName().toLowerCase());
            result.put("text", "");
            result.put("extra", extra);
            result.put("hoverEvent", hoverEvent);
            return result;
         } else if (message.senderTitle != null) {
            String text = bracketColor + bracketType.opening
                + Msg.format(message.senderTitle)
                + bracketColor + bracketType.closing;
            String tooltip = Msg.format(message.senderTitle)
                + (message.senderTitleDescription != null
                   ? "\n&f" + message.senderTitleDescription
                   : "");
            return Msg.button(bracketColor, text, tooltip, null);
        } else {
            return "";
        }
    }

    final Object senderTag(Message message, ChatColor senderColor, ChatColor bracketColor,
                           BracketType bracketType, boolean useBrackets) {
        if (message.senderName == null) return "";
        String text = useBrackets
            ? (bracketColor + bracketType.opening
               + senderColor + message.senderName
               + bracketColor + bracketType.closing)
            : message.senderName;
        if (message.sender == null) {
            return Msg.button(senderColor, text, null, null);
        }
        String titleLine = message.senderTitle != null
            ? "\n&5&oTitle&r " + Msg.format(message.senderTitle)
            : "";
        String serverLine = message.senderServerDisplayName != null
            ? "\n&5&oServer&r "
            + message.senderServerDisplayName : "";
        String tooltip = message.senderName
            + titleLine
            + serverLine
            + "\n&5&oChannel&r " + getTitle()
            + "\n&5&oTime&r " + timeFormat.format(new Date());
        String cmd = "/msg " + message.senderName + " ";
        return Msg.button(senderColor, text, message.senderName, tooltip, cmd);
    }

    final void appendMessage(List<Object> json, Message message, ChatColor textColor,
                             boolean languageFilter) {
        List<Object> sourceList = languageFilter ? message.languageFilterJson : message.json;
        Map<String, Object> map = new HashMap<>();
        List<Object> extra = new ArrayList<>(sourceList);
        map.put("text", "");
        map.put("color", textColor.getName().toLowerCase());
        map.put("extra", extra);
        map.put("insertion", languageFilter ? message.languageFilterMessage : message.message);
        json.add(map);
    }

    static boolean shouldIgnore(UUID player, Message message) {
        if (message.sender != null && SQLIgnore.doesIgnore(player, message.sender)) return true;
        return false;
    }


    public final List<Chatter> getOnlineMembers() {
        List<Chatter> result = new ArrayList<>();
        for (Chatter chatter: ChatPlugin.getInstance().getOnlinePlayers()) {
            if (!canJoin(chatter.getUuid())) continue;
            if (!isJoined(chatter.getUuid())) continue;
            result.add(chatter);
        }
        return result;
    }

    public final List<Player> getLocalMembers() {
        List<Player> result = new ArrayList<>();
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
            if (!canJoin(player.getUniqueId())) continue;
            if (!isJoined(player.getUniqueId())) continue;
            result.add(player);
        }
        return result;
    }

    public final boolean playSoundCue(Player player) {
        String tmp = SQLSetting.getString(player.getUniqueId(), getKey(), "SoundCueChat", "off");
        SoundCue soundCue = tmp != null
            ? SoundCue.of(tmp)
            : null;
        if (soundCue == null) return false;
        int volume = SQLSetting.getInt(player.getUniqueId(), getKey(), "SoundCueChatVolume", 10);
        float vol = (float) volume / 10.0f;
        player.playSound(player.getLocation(), soundCue.sound, vol, 1.0f);
        return true;
    }
}
