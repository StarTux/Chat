package com.winthier.chat.channel;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.TextFormat;
import com.winthier.chat.ChatPlugin;
import com.winthier.chat.Chatter;
import com.winthier.chat.Message;
import com.winthier.chat.MessageFilter;
import com.winthier.chat.sql.SQLIgnore;
import com.winthier.chat.sql.SQLSetting;
import com.winthier.chat.util.Msg;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class AbstractChannel implements Channel {
    private String title, key, tag, description;
    private int range = 0;
    private final List<String> aliases = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public final String getAlias() {
        return getAliases().get(0);
    }

    /**
     * Override if channel has a special permission.
     */
    @Override
    public boolean hasPermission(Player player) {
        return player.hasPermission("chat.channel." + getKey()) || player.hasPermission("chat.channel.*");
    }

    /**
     * Override if channel has a special permission.
     */
    @Override
    public boolean hasPermission(UUID player) {
        return ChatPlugin.getInstance().hasPermission(player, "chat.channel." + getKey()) || ChatPlugin.getInstance().hasPermission(player, "chat.channel.*");
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

    @Override
    public final List<Option> getOptions() {
        return Arrays.asList(
            Option.booleanOption("ShowChannelTag", "Show Channel Tag", "Show the channel tag at the beginning of every message", "0"),
            Option.booleanOption("ShowPlayerTitle", "Show Player Title", "Show a player's current title in every message", "1"),
            Option.booleanOption("ShowServer", "Show Server", "Show a player's server in every message", "0"),

            Option.colorOption("ChannelColor", "Channel Color", "Main channel color", "white"),
            Option.colorOption("TextColor", "Text Color", "Color of chat messages", "white"),
            Option.colorOption("SenderColor", "Player Color", "Color of player names", "white"),
            Option.colorOption("BracketColor", "Bracket Color", "Color of brackets and puncutation", "white"),

            Option.bracketOption("BracketType", "Brackets", "Appearance of brackets", "angle"),

            Option.soundOption("SoundCueChat", "Chat Cue", "Sound played when you receive a message", "off"),
            Option.intOption("SoundCueChatVolume", "Chat Cue Volume", "Sound played when you receive a message", "10", 1, 10),

            Option.booleanOption("LanguageFilter", "Language Filter", "Filter out foul language", "1")
            );
    }

    /**
     * Override if channel command syntax is more specific.
     */
    @Override
    public void playerDidUseChat(PlayerCommandContext context) {
        playerDidUseCommand(context);
    }

    @Override
    public void announce(Object msg) {
        announce(msg, false);
    }

    @Override
    public void announceLocal(Object msg) {
        announce(msg, true);
    }

    private void announce(Object msg, boolean local) {
        Message message;
        if (msg instanceof String) {
            message = makeMessage(null, (String)msg);
        } else {
            List<Object> json;
            if (msg instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> tmpson = (List<Object>)msg;
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
        if (message.senderTitle == null) {
            ChatPlugin.getInstance().loadTitle(message);
        }
        if (message.json == null || message.languageFilterJson == null) {
            MessageFilter filter = new MessageFilter(message.sender, message.message);
            filter.process();
            message.json = filter.getJson();
            message.languageFilterJson = filter.getLanguageFilterJson();
            message.languageFilterMessage = filter.toString();
            message.shouldCancel = filter.shouldCancel();
        }
    }

    final Message makeMessage(Player player, String text) {
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

    final Object channelTag(TextFormat channelColor, TextFormat bracketColor, BracketType bracketType) {
        return Msg.button(channelColor,
                          bracketColor + bracketType.opening + channelColor + getTag() + bracketColor + bracketType.closing,
                          getTitle() + "\n&5&o" + getDescription(),
                          "/" + getAlias() + " ");
    }

    final Object serverTag(Message message, TextFormat serverColor, TextFormat bracketColor, BracketType bracketType) {
        String name;
        if (message.senderServerDisplayName != null) {
            name = message.senderServerDisplayName;
        } else if (message.senderServer != null) {
            name = message.senderServer;
        } else {
            return "";
        }
        return Msg.button(serverColor,
                          bracketColor + bracketType.opening + serverColor + name + bracketColor + bracketType.closing,
                          null,
                          null);
    }

    final Object senderTitleTag(Message message, TextFormat bracketColor, BracketType bracketType) {
        if (message.senderTitle == null) return "";
        return Msg.button(
            bracketColor,
            bracketColor + bracketType.opening + Msg.format(message.senderTitle) + bracketColor + bracketType.closing,
            Msg.format(message.senderTitle)
            + (message.senderTitleDescription != null ? "\n&5&o" + message.senderTitleDescription : ""),
            null);
    }

    final Object senderTag(Message message, TextFormat senderColor, TextFormat bracketColor, BracketType bracketType, boolean useBrackets) {
        if (message.senderName == null) return "";
        if (message.sender == null) {
            return Msg.button(senderColor, useBrackets ? bracketColor + bracketType.opening + senderColor + message.senderName + bracketColor + bracketType.closing : message.senderName, null, null);
        }
        return Msg.button(senderColor,
                          useBrackets ? bracketColor + bracketType.opening + senderColor + message.senderName + bracketColor + bracketType.closing : message.senderName,
                          message.senderName,
                          message.senderName
                          + (message.senderTitle != null ? "\n&5&oTitle&r " + Msg.format(message.senderTitle) : "")
                          + (message.senderServerDisplayName != null ? "\n&5&oServer&r " + message.senderServerDisplayName : "")
                          + "\n&5&oChannel&r " + getTitle()
                          + "\n&5&oTime&r " + timeFormat.format(new Date()),
                          "/msg " + message.senderName + " ");
    }

    final void appendMessage(List<Object> json, Message message, TextFormat textColor, boolean languageFilter) {
        List<Object> sourceList = languageFilter ? message.languageFilterJson : message.json;
        Map<String, Object> map = new HashMap<>();
        List<Object> extra = new ArrayList<>(sourceList);
        map.put("text", "");
        map.put("color", textColor.name().toLowerCase());
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
            if (!hasPermission(chatter.getUuid())) continue;
            if (!isJoined(chatter.getUuid())) continue;
            result.add(chatter);
        }
        return result;
    }

    public final List<Player> getLocalMembers() {
        List<Player> result = new ArrayList<>();
        for (Player player: Server.getInstance().getOnlinePlayers().values()) {
            if (!hasPermission(player)) continue;
            if (!isJoined(player.getUniqueId())) continue;
            result.add(player);
        }
        return result;
    }

    public final boolean playSoundCue(Player player) {
        SoundCue soundCue = SoundCue.of(SQLSetting.getString(player.getUniqueId(), getKey(), "SoundCueChat", "off"));
        if (soundCue == null) return false;
        int volume = SQLSetting.getInt(player.getUniqueId(), getKey(), "SoundCueChatVolume", 10);
        float vol = (float)volume / 10.0f;
        /* Zombiefied for Nukkit port */
        // player.playSound(player.getLocation().add(0, player.getEyeHeight(), 0), soundCue.sound, vol, 1.0f);
        return true;
    }
}
