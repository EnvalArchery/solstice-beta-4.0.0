package me.nullrush.solstice.modules.impl.player;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.TickEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.NumberSetting;

@RegisterModule(name = "Timer", description = "Makes your game run at a faster tick speed.", category = Module.Category.PLAYER)
public class TimerModule extends Module {
    public NumberSetting multiplier = new NumberSetting("Multiplier", "The multiplier that will be added to the game's speed.", 1.0f, 0.0f, 20.0f);

    @SubscribeEvent(priority = Integer.MIN_VALUE)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        Solstice.WORLD_MANAGER.setTimerMultiplier(multiplier.getValue().floatValue());
    }

    @Override
    public void onEnable() {
        Solstice.WORLD_MANAGER.setTimerMultiplier(multiplier.getValue().floatValue());
    }

    @Override
    public void onDisable() {
        Solstice.WORLD_MANAGER.setTimerMultiplier(1.0f);
    }

    @Override
    public String getMetaData() {
        return String.valueOf(multiplier.getValue().floatValue());
    }
}
