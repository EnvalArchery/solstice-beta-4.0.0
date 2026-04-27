package me.nullrush.solstice.modules.impl.movement;

import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.TickEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;

@RegisterModule(name = "AutoWalk", description = "Automatically walks at all times.", category = Module.Category.MOVEMENT)
public class AutoWalkModule extends Module {
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        mc.options.forwardKey.setPressed(true);
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.world == null) return;
        mc.options.forwardKey.setPressed(false);
    }
}
