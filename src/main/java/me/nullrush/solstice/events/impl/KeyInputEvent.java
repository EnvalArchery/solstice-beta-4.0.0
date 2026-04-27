package me.nullrush.solstice.events.impl;

import lombok.*;
import me.nullrush.solstice.events.Event;

@EqualsAndHashCode(callSuper = true) @Data
public class KeyInputEvent extends Event {
    private final int key, modifiers;
}
