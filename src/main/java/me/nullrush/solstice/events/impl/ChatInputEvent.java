package me.nullrush.solstice.events.impl;

import lombok.*;
import me.nullrush.solstice.events.Event;

@Getter @Setter @AllArgsConstructor
public class ChatInputEvent extends Event {
    private String message;
}
