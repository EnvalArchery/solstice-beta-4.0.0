package me.nullrush.solstice.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.nullrush.solstice.events.Event;

@Getter
@AllArgsConstructor
public class MouseInputEvent extends Event {
    private final int button;
}