package com.winthier.chat;

import java.util.UUID;
import lombok.Value;

@Value
public class Chatter {
    UUID uuid;
    String name;
}
