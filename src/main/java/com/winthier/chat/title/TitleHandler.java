package com.winthier.chat.title;

import com.winthier.chat.Message;
import com.winthier.title.Title;
import com.winthier.title.TitlePlugin;

public final class TitleHandler {
    public void loadTitle(Message message) {
        if (message.sender == null) return;
        Title title = TitlePlugin.getInstance().getPlayerTitle(message.sender);
        if (title == null) return;
        message.senderTitle = title.getTitle();
        message.senderTitleDescription = title.getDescription();
    }
}
