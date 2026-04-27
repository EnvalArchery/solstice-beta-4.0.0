package me.nullrush.solstice.modules.impl.miscellaneous;

import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.NumberSetting;

@RegisterModule(name = "UnfocusedFPS", description = "Limits your FPS when the game window is out of focus.", category = Module.Category.MISCELLANEOUS)
public class UnfocusedFPSModule extends Module {
    public NumberSetting limit = new NumberSetting("Limit", "The FPS limit that will be enforced once the game is out of focus.", 30, 10, 60);

    @Override
    public String getMetaData() {
        return String.valueOf(limit.getValue().intValue());
    }
}
