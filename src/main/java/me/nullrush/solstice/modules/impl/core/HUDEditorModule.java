package me.nullrush.solstice.modules.impl.core;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;

@RegisterModule(name = "HUDEditor", description = "Opens a dedicated editor focused on the HUD module.", category = Module.Category.CORE, drawn = false)
public class HUDEditorModule extends Module {
    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }

        Solstice.CLICK_GUI.setHudEditorMode(true);
        mc.setScreen(Solstice.CLICK_GUI);
    }

    @Override
    public void onDisable() {
        Solstice.CLICK_GUI.setHudEditorMode(false);
        if (mc.currentScreen == Solstice.CLICK_GUI) {
            mc.setScreen(null);
        }
    }
}
