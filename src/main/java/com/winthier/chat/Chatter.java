package com.winthier.chat;

import java.util.UUID;
import lombok.Value;

@Value
public final class Chatter {
    private final UUID uuid;
    private final String name;
}
