package me.nullrush.solstice.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.nullrush.solstice.events.Event;
import net.minecraft.item.ItemStack;

@Getter @AllArgsConstructor
public class ConsumeItemEvent extends Event {
    private final ItemStack stack;
}
