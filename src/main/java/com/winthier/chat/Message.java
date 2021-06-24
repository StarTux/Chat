package com.winthier.chat;

import com.winthier.chat.channel.Channel;
import com.winthier.chat.util.Filter;
import com.winthier.chat.util.Msg;
import com.winthier.title.Title;
import com.winthier.title.TitlePlugin;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Data
public final class Message {
    private long time;
    private UUID sender;
    private String senderName;
    private String senderDisplayNameJson;
    private String channel;
    private String special;
    private UUID target; // PM
    private String targetName; // PM, Party
    private String senderServer;
    private String senderServerDisplayName;
    private Title title;

    private String message;
    private String messageJson;
    private List<String> urls;

    private boolean hideSenderTags;
    private boolean passive;
    private boolean emoji;

    private String itemJson;

    private transient Location location;
    private transient boolean local;

    public String serialize() {
        return Msg.GSON.toJson(this);
    }

    public static Message deserialize(String in) {
        return Msg.GSON.fromJson(in, Message.class);
    }

    public Message init(Channel theChannel) {
        this.channel = theChannel.getKey();
        this.time = System.currentTimeMillis();
        this.senderServer = ChatPlugin.getInstance().getServerName();
        this.senderServerDisplayName = ChatPlugin.getInstance().getServerDisplayName();
        return this;
    }

    public Message player(final Player player) {
        this.sender = player.getUniqueId();
        this.senderName = player.getName();
        Component displayName = player.displayName();
        if (displayName != null) this.senderDisplayNameJson = Msg.toJson(displayName);
        this.location = player.getLocation();
        this.title = TitlePlugin.getInstance().getPlayerTitle(player);
        this.emoji = player.hasPermission("chat.emoji");
        return this;
    }

    public Message player(final Player player, String msg) {
        player(player);
        if (!player.hasPermission("chat.caps")) {
            msg = Filter.filterCaps(msg);
        }
        if (!player.hasPermission("chat.unicode")) {
            msg = Filter.filterUnicode(msg);
        }
        boolean color = player.hasPermission("chat.color");
        boolean format = player.hasPermission("chat.format");
        boolean obfuscate = player.hasPermission("chat.obfuscate");
        if (color || format || obfuscate) {
            msg = Filter.filterLegacyColors(msg, color, format, obfuscate);
        }
        if (player.hasPermission("chat.item") && msg.contains("[item]")) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack != null) {
                Component itemName;
                if (itemStack.hasItemMeta()) {
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemName = itemMeta.hasDisplayName()
                        ? itemMeta.displayName()
                        : Component.text(itemStack.getI18NDisplayName());
                } else {
                    itemName = Component.text(itemStack.getI18NDisplayName());
                }
                Component itemComponent = itemName.hoverEvent(itemStack.asHoverEvent());
                itemJson = Msg.toJson(itemComponent);
            }
        }
        if (player.hasPermission("chat.url")) {
            this.urls = Filter.findUrls(msg);
        }
        this.message = msg;
        return this;
    }

    public Message player(final Player player, final Component msg) {
        player(player);
        this.messageJson = Msg.toJson(msg);
        this.message = Msg.plain(msg);
        return this;
    }

    public Message console(final String msg) {
        this.sender = null;
        this.senderName = "Console";
        this.message = msg;
        this.urls = Filter.findUrls(msg);
        this.emoji = true;
        return this;
    }

    public Message message(Component component) {
        this.messageJson = Msg.toJson(component);
        this.message = Msg.plain(component);
        return this;
    }

    public Component getSenderDisplayName() {
        if (senderDisplayNameJson == null) return null;
        return Msg.parseComponent(senderDisplayNameJson);
    }

    public Component getMessageComponent() {
        if (messageJson == null) return null;
        return Msg.parseComponent(messageJson);
    }

    public Component getItemComponent() {
        if (itemJson == null) return null;
        return Msg.parseComponent(itemJson);
    }
}
