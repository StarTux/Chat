package com.winthier.chat.channel;

import org.bukkit.Sound;

public enum SoundCue {
    DING(Sound.ENTITY_ARROW_HIT_PLAYER),
    CLICK(Sound.UI_BUTTON_CLICK),
    ANVIL(Sound.BLOCK_ANVIL_LAND),
    BASEDRUM(Sound.BLOCK_NOTE_BASEDRUM),
    BASS(Sound.BLOCK_NOTE_BASS),
    HARP(Sound.BLOCK_NOTE_HARP),
    HAT(Sound.BLOCK_NOTE_HAT),
    PLING(Sound.BLOCK_NOTE_PLING),
    SNARE(Sound.BLOCK_NOTE_SNARE),
    CAT(Sound.ENTITY_CAT_AMBIENT),
    DOG(Sound.ENTITY_WOLF_AMBIENT),
    CHICKEN(Sound.ENTITY_CHICKEN_HURT),
    COW(Sound.ENTITY_COW_HURT),
    DONKEY(Sound.ENTITY_DONKEY_AMBIENT),
    VILLAGER(Sound.ENTITY_VILLAGER_TRADING);

    SoundCue(Sound sound) {
        this.sound = sound;
    }

    final Sound sound;

    static SoundCue of(String val) {
        if (val == null) return null;
        try {
            return valueOf(val.toUpperCase());
        } catch (IllegalArgumentException ile) {
            return null;
        }
    }
}
