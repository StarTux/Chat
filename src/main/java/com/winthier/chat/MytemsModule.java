package com.winthier.chat;

import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

@RequiredArgsConstructor
final class MytemsModule {
    private final ChatPlugin plugin;

    public void enable() {
        for (Mytems mytems : Mytems.values()) {
            if (mytems.component != null && !mytems.component.equals(Component.empty())) {
                String key = mytems.name().toLowerCase();
                Component component = mytems.component
                    .hoverEvent(HoverEvent.showText(mytems.getMytem().getDisplayName()));
                plugin.getEmoji().put(key, component);
            }
        }
    }
}
