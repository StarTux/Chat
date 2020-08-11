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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
            Option.intOption("SoundCueChatVolume", "Chat Cue Volume", "Sound played when you receive a message", "10", 1, 10)
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
        }
        message.local = local;
        ChatPlugin.getInstance().didCreateMessage(this, message);
        handleMessage(message);
    }

    final void fillMessage(Message message) {
        if (message.senderTitle == null && message.sender != null) {
            message.senderTitle = ChatPlugin.getInstance()
                .getTitle(message.sender);
        }
    }

    final Message makeMessage(Player player, String text) {
        Message message = new Message();
        message.channel = getKey();
        if (player != null) {
            message.sender = player.getUniqueId();
            message.senderName = player.getName();
            message.senderDisplayName = player.getDisplayName();
            message.location = player.getLocation();
        }
        message.senderServer = ChatPlugin.getInstance().getServerName();
        message.senderServerDisplayName = ChatPlugin.getInstance().getServerDisplayName();
        message.message = text;
        fillMessage(message);
        return message;
    }

    final Object channelTag(ChatColor channelColor, ChatColor bracketColor, BracketType bracketType) {
        return Msg.button(channelColor,
                          bracketColor + bracketType.opening + channelColor + getTag() + bracketColor + bracketType.closing,
                          getTitle() + "\n&5&o" + getDescription(),
                          "/" + getAlias() + " ");
    }

    final Object serverTag(Message message, ChatColor serverColor, ChatColor bracketColor, BracketType bracketType) {
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

    final Object senderTitleTag(Message message, ChatColor bracketColor, BracketType bracketType) {
        if (message.senderTitle == null) return "";
        return Msg.button(
            bracketColor,
            /*bracketColor + bracketType.opening +*/ Msg.format(message.senderTitle) /*+ bracketColor + bracketType.closing*/,
            Msg.format(message.senderTitle)
            + (message.senderTitleDescription != null ? "\n&5&o" + message.senderTitleDescription : ""),
            null);
    }

    final Object senderTag(Message message, ChatColor senderColor, ChatColor bracketColor, BracketType bracketType, boolean useBrackets) {
        if (message.senderName == null) return "";
        if (message.sender == null) {
            return Msg.button(senderColor, useBrackets ? bracketColor + bracketType.opening + senderColor + message.senderName + ChatColor.RESET + bracketColor + bracketType.closing : message.senderName, null, null);
        }
        final ChatPlugin plugin = ChatPlugin.getInstance();
        String certs;
        Player player = message.sender != null
            ? Bukkit.getPlayer(message.sender)
            : null;
        if (player != null) {
            final String horseCertificates = "%horse_certificates%";
            certs = PlaceholderAPI.setPlaceholders(player, horseCertificates);
            if (certs.equals(horseCertificates)) certs = null;
        } else {
            certs = null;
        }
        return Msg
            .button(senderColor,
                    (useBrackets
                     ? bracketColor + bracketType.opening + senderColor + message.senderDisplayName + bracketColor + bracketType.closing
                     : message.senderDisplayName),
                    message.senderName,
                    message.senderName
                    + (message.senderTitle != null ? "\n&5&oTitle&r " + Msg.format(message.senderTitle) : "")
                    + (message.senderServerDisplayName != null ? "\n&5&oServer&r " + message.senderServerDisplayName : "")
                    + "\n&5&oRank&r " + plugin.getVault()
                    .groupOf(message.sender)
                    + "\n&5&oJobs&r " + plugin.getVault()
                    .groupSuffixes(message.sender).stream()
                    .collect(Collectors.joining(" "))
                    + "\n&5&oChannel&r " + getTitle()
                    + "\n&5&oTime&r " + timeFormat.format(new Date())
                    + (certs != null
                       ? "\n&5&oCertificates&r " + certs
                       : ""),
                    "/msg " + message.senderName + " ");
    }

    final void appendMessage(List<Object> json, Message message, ChatColor textColor, Player player) {
        List<Object> sourceList;
        if (message.json != null) {
            sourceList = message.json;
        } else {
            MessageFilter messageFilter = new MessageFilter(message.sender, message.message);
            messageFilter.setRecipient(player);
            messageFilter.process();
            if (messageFilter.isPinging()) {
                player.playSound(player.getEyeLocation(), SoundCue.DING.sound, 1.0f, 1.0f);
            }
            sourceList = messageFilter.getJson();
        }
        Map<String, Object> map = new HashMap<>();
        List<Object> extra = new ArrayList<>(sourceList);
        String colorValue = null;
        boolean bold = false;
        boolean italic = false;
        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;
        for (int i = 0; i < message.message.length() - 1; i += 2) {
            if (message.message.charAt(i) != ChatColor.COLOR_CHAR) break;
            ChatColor color = ChatColor
                .getByChar(message.message.charAt(i + 1));
            if (color == null) break;
            if (color.isColor()) colorValue = color.name().toLowerCase();
            if (color == ChatColor.BOLD) bold = true;
            if (color == ChatColor.ITALIC) italic = true;
            if (color == ChatColor.UNDERLINE) underlined = true;
            if (color == ChatColor.STRIKETHROUGH) strikethrough = true;
            if (color == ChatColor.MAGIC) obfuscated = true;
        }
        map.put("text", "");
        if (colorValue != null) map.put("color", colorValue);
        if (italic) map.put("italic", true);
        if (bold) map.put("bold", true);
        if (underlined) map.put("underlined", true);
        if (strikethrough) map.put("strikethrough", true);
        if (obfuscated) map.put("obfuscated", true);
        map.put("extra", extra);
        map.put("insertion", message.message);
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
        for (Player player: Bukkit.getServer().getOnlinePlayers()) {
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
        player.playSound(player.getEyeLocation(), soundCue.sound, vol, 1.0f);
        return true;
    }
}
