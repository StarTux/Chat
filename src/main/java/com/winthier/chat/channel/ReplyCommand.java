package com.winthier.chat.channel;

import lombok.Value;
import java.util.List;
import java.util.Arrays;

@Value
public class ReplyCommand implements CommandResponder {
    String name = "reply";
    List<String> aliases = Arrays.<String>asList("r", "xreply", "xr");

    @Override
    public void playerDidUseCommand(PlayerCommandContext context) {
    }
}
