package me.nullrush.solstice.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.nullrush.solstice.events.Event;

@Getter @AllArgsConstructor
public class ChangeYawEvent extends Event {
    private final float yaw;
}
