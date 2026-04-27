package me.nullrush.solstice.modules.impl.movement;

import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.NumberSetting;

@RegisterModule(name = "NoJumpDelay", description = "Removes the delay that slows down your jumping.", category = Module.Category.MOVEMENT)
public class NoJumpDelayModule extends Module {
    public NumberSetting ticks = new NumberSetting("Ticks", "The amount of ticks that have to be waited for before jumping again.", 1, 0, 20);

    @Override
    public String getMetaData() {
        return String.valueOf(ticks.getValue());
    }
}
