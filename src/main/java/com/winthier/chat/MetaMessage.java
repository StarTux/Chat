package com.winthier.chat;

import lombok.Data;
import java.util.UUID;

/**
 * A package which is sent by ConnectMessage in the ChatMeta
 * channel. This mostly exists so other servers can be notified about
 * updates to the database so they can reload accordingly.
 */
@Data
public final class MetaMessage {
    private String meta;
    private UUID uuid;

    public MetaMessage() { }

    public MetaMessage(final String meta, final UUID uuid) {
        this.meta = meta;
        this.uuid = uuid;
    }
}
