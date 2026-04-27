package me.nullrush.solstice.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.nullrush.solstice.events.Event;
import net.minecraft.util.math.Vec3d;

@Getter @Setter @RequiredArgsConstructor
public class UpdateVelocityEvent extends Event {
    private final Vec3d movementInput;
    private final float speed;

    private Vec3d velocity;
}
