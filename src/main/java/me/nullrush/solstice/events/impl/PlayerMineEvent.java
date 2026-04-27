package me.nullrush.solstice.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.nullrush.solstice.events.Event;
import net.minecraft.util.math.BlockPos;

@Getter @AllArgsConstructor
public class PlayerMineEvent extends Event {
    private final int actorID;
    private final BlockPos position;
}
