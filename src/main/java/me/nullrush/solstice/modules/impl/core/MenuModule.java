package me.nullrush.solstice.modules.impl.core;

import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.CategorySetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;

@RegisterModule(name = "Menu", description = "Replaces the default title screen with the client's custom main menu screen.", category = Module.Category.CORE, persistent = true, drawn = false)
public class MenuModule extends Module {
    public static final String[] MENU_SHADERS = new String[]{"BlueHole", "Borealis", "Cube", "CubicPulse", "Desert", "DNA", "GammaRays", "Gas", "Lines", "Liquid", "Mandelbrot", "Minecraft", "Missing", "Nebula", "Paper", "Pixels", "RainbowWave", "RusherHack", "Sea", "Steam", "Sun", "PhobosFluid", "PhobosGlow", "PhobosSnow", "PhobosTruchet"};

    public CategorySetting mainMenuCategory = new CategorySetting("MainMenu", "The category for settings related to the main menu.");
    public BooleanSetting mainMenu = new BooleanSetting("MainMenu", "Enabled", "Replaces Minecraft's default main menu with a customizable one.", new CategorySetting.Visibility(mainMenuCategory), true);
    public ModeSetting backgroundMode = new ModeSetting("BackgroundMode", "Background", "Controls which background style is rendered behind the custom main menu.", new CategorySetting.Visibility(mainMenuCategory), "Solstice", new String[]{"Solstice", "Shader", "Blend", "Backdrop"});

    public CategorySetting shaderCategory = new CategorySetting("Shaders", "The category for the imported main menu shader backgrounds.", new ModeSetting.Visibility(backgroundMode, "Shader", "Blend"));
    public ModeSetting shader = new ModeSetting("Shader", "Shader", "Selects which imported shader background should render on the main menu.", new CategorySetting.Visibility(shaderCategory), "Borealis", MENU_SHADERS);
    public NumberSetting shaderSpeed = new NumberSetting("ShaderSpeed", "Speed", "Controls the playback speed of the imported main menu shader.", new CategorySetting.Visibility(shaderCategory), 10.0f, 1.0f, 20.0f);
    public NumberSetting shaderOpacity = new NumberSetting("ShaderOpacity", "Opacity", "Controls how strong the imported shader layer appears when blended with the Solstice menu.", new ModeSetting.Visibility(backgroundMode, "Blend"), 72, 0, 100);
    public NumberSetting shaderShift = new NumberSetting("ShaderShift", "Shift", "Controls the phase used by compatible imported shaders.", new CategorySetting.Visibility(shaderCategory), 1.6f, 0.0f, 4.0f);
}
