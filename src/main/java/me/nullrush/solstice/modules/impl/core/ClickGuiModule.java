package me.nullrush.solstice.modules.impl.core;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.CategorySetting;
import me.nullrush.solstice.settings.impl.ColorSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@RegisterModule(name = "ClickGui", description = "Allows you to change and interact with the client's modules and settings through a GUI.", category = Module.Category.CORE, drawn = false, bind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGuiModule extends Module {
    public BooleanSetting sounds = new BooleanSetting("Sounds", "Plays Minecraft UI sounds when interacting with the client's GUI.", true);
    public BooleanSetting blur = new BooleanSetting("Blur", "Whether or not to blur the background behind the GUI.", true);
    public NumberSetting guiScale = new NumberSetting("GuiScale", "Scale", "The scale that will be used for the ClickGUI.", 1.0f, 0.7f, 1.65f);
    public NumberSetting scrollSpeed = new NumberSetting("ScrollSpeed", "The speed at which the scrolling of the frames will be at.", 15, 1, 50);
    public NumberSetting frameAlpha = new NumberSetting("FrameAlpha", "Frame Alpha", "The opacity used for category headers and larger surfaces.", 140, 40, 220);
    public NumberSetting moduleAlpha = new NumberSetting("ModuleAlpha", "Module Alpha", "The opacity used for module rows and setting rows.", 46, 18, 150);
    public CategorySetting backgroundShaderCategory = new CategorySetting("BackgroundShaders", "BackgroundShaders", "The settings used for the fullscreen shader layer that sits behind the ClickGUI.");
    public BooleanSetting backgroundShaders = new BooleanSetting("BackgroundShadersEnabled", "Enabled", "Whether or not the background shader layer should render.", new CategorySetting.Visibility(backgroundShaderCategory), true);
    public ModeSetting backgroundShaderType = new ModeSetting("BackgroundShaderType", "Type", "The fullscreen shader style rendered behind the ClickGUI.", new CategorySetting.Visibility(backgroundShaderCategory), "Hologram", new String[]{"None", "Hologram", "Rainbow", "Cyberpunk", "Plasma", "Liquid", "Matrix", "Aurora", "Nebula", "Scan"});
    public NumberSetting backgroundShaderOpacity = new NumberSetting("BackgroundShaderOpacity", "Opacity", "How visible the background shader layer should be.", new CategorySetting.Visibility(backgroundShaderCategory), 92, 0, 200);
    public NumberSetting backgroundShaderSpeed = new NumberSetting("BackgroundShaderSpeed", "Speed", "How quickly the background shader layer animates.", new CategorySetting.Visibility(backgroundShaderCategory), 12L, 1L, 30L);
    public NumberSetting backgroundShaderIntensity = new NumberSetting("BackgroundShaderIntensity", "Intensity", "How strong the fullscreen shader shapes should be.", new CategorySetting.Visibility(backgroundShaderCategory), 72, 10, 140);
    public ColorSetting backgroundShaderColor = new ColorSetting("BackgroundShaderColor", "Main Color", "The main color used by the fullscreen shader layer.", new CategorySetting.Visibility(backgroundShaderCategory), new ColorSetting.Color(new Color(255, 50, 168), false, false));
    public ColorSetting backgroundShaderAccent = new ColorSetting("BackgroundShaderAccent", "Accent Color", "The accent color used by the fullscreen shader layer.", new CategorySetting.Visibility(backgroundShaderCategory), new ColorSetting.Color(new Color(154, 32, 255), false, false));
    public CategorySetting panelShaderCategory = new CategorySetting("PanelShaders", "PanelShaders", "The settings used for category headers, modules, and setting rows.");
    public BooleanSetting categoryShaders = new BooleanSetting("CategoryShaders", "Category Shaders", "Whether shader accents should render on category headers and larger ClickGUI panels.", new CategorySetting.Visibility(panelShaderCategory), true);
    public BooleanSetting moduleShaders = new BooleanSetting("ModuleShaders", "Module Shaders", "Whether shader accents should render on module and setting rows.", new CategorySetting.Visibility(panelShaderCategory), true);
    public ModeSetting panelShaderType = new ModeSetting("PanelShaderType", "Type", "The shader style rendered inside ClickGUI panels.", new CategorySetting.Visibility(panelShaderCategory), "Liquid", new String[]{"None", "Hologram", "Rainbow", "Cyberpunk", "Plasma", "Liquid", "Prism", "Scanline"});
    public NumberSetting panelShaderOpacity = new NumberSetting("PanelShaderOpacity", "Opacity", "The opacity of the panel shader streaks.", new CategorySetting.Visibility(panelShaderCategory), 62, 0, 180);
    public NumberSetting panelShaderSpeed = new NumberSetting("PanelShaderSpeed", "Speed", "The speed of the panel shader animation.", new CategorySetting.Visibility(panelShaderCategory), 3L, 0L, 24L);
    public NumberSetting glowWidth = new NumberSetting("GlowWidth", "Glow Width", "How thick the highlight bands and edge glows should be.", new CategorySetting.Visibility(panelShaderCategory), 5, 1, 12);
    public NumberSetting glowIntensity = new NumberSetting("GlowIntensity", "Glow Intensity", "How intense the panel glow should be.", new CategorySetting.Visibility(panelShaderCategory), 1.15, 0.5, 4.0);
    public ColorSetting panelShaderColor = new ColorSetting("PanelShaderColor", "Main Color", "The main color used by the ClickGUI panel shaders.", new CategorySetting.Visibility(panelShaderCategory), new ColorSetting.Color(new Color(255, 50, 168), false, false));
    public ColorSetting panelShaderAccent = new ColorSetting("PanelShaderAccent", "Accent Color", "The accent color used by the ClickGUI panel shaders.", new CategorySetting.Visibility(panelShaderCategory), new ColorSetting.Color(new Color(154, 32, 255), false, false));
    public ColorSetting color = new ColorSetting("Color", "The color that will be used in the GUI.", new ColorSetting.Color(new Color(130, 202, 255), true, false));

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setToggled(false);
            return;
        }

        Solstice.CLICK_GUI.setHudEditorMode(false);
        mc.setScreen(Solstice.CLICK_GUI);
    }

    @Override
    public void onDisable() {
        mc.setScreen(null);
    }

    public boolean isRainbow() {
        if(color.isSync()) return Solstice.MODULE_MANAGER.getModule(ColorModule.class).color.isRainbow();
        return color.isRainbow();
    }
}
