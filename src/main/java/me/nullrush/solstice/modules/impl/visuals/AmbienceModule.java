package me.nullrush.solstice.modules.impl.visuals;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.CategorySetting;
import me.nullrush.solstice.settings.impl.ColorSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.color.ColorUtils;

import java.awt.*;

@RegisterModule(name = "Ambience", description = "Modifies the world's ambience, such as time and fog color.", category = Module.Category.VISUALS)
public class AmbienceModule extends Module {
    public BooleanSetting modifyTime = new BooleanSetting("ModifyTime", "Modifies the world's time.", true);
    public NumberSetting time = new NumberSetting("Time", "The time that the world will be set to.", new BooleanSetting.Visibility(modifyTime, true), 200, -200, 200);
    public BooleanSetting modifyFog = new BooleanSetting("ModifyFog", "Modifies certain things about the world's fog.", false);
    public NumberSetting fogStart = new NumberSetting("FogStart", "The start value of the world's fog.", new BooleanSetting.Visibility(modifyFog, true), 50, 0, 300);
    public NumberSetting fogEnd = new NumberSetting("FogEnd", "The end value of the world's fog.", new BooleanSetting.Visibility(modifyFog, true), 150, 0, 300);
    public ColorSetting fogColor = new ColorSetting("FogColor", "Modifies the color of the world's fog.", new BooleanSetting.Visibility(modifyFog, true), ColorUtils.getDefaultOutlineColor());
    public CategorySetting skyCategory = new CategorySetting("Sky", "Sky-related ambience options.");
    public BooleanSetting modifySky = new BooleanSetting("ModifySky", "Enabled", "Modifies the world's sky color.", new CategorySetting.Visibility(skyCategory), false);
    public ModeSetting skyMode = new ModeSetting("SkyMode", "Mode", "Controls how the custom sky color is generated.", new BooleanSetting.Visibility(modifySky, true), "Static", new String[]{"Static", "Phobos", "BlackHole", "Nebula"});
    public ColorSetting skyColor = new ColorSetting("SkyColor", "Sky Color", "The static color used by the custom sky.", new ModeSetting.Visibility(skyMode, "Static"), new ColorSetting.Color(new Color(92, 144, 255, 255), false, false));
    public NumberSetting skySpeed = new NumberSetting("SkySpeed", "Speed", "The speed used by the Phobos-inspired animated sky.", new ModeSetting.Visibility(skyMode, "Phobos"), 1.0f, 0.1f, 4.0f);
    public NumberSetting skyIntensity = new NumberSetting("SkyIntensity", "Intensity", "The brightness multiplier used by the Phobos-inspired animated sky.", new ModeSetting.Visibility(skyMode, "Phobos"), 1.0f, 0.3f, 2.0f);
    public ColorSetting skyShaderColor = new ColorSetting("SkyShaderColor", "Shader Color", "The tint used by ambience sky shaders.", new ModeSetting.Visibility(skyMode, "BlackHole", "Nebula"), new ColorSetting.Color(new Color(132, 92, 255, 255), false, false));
    public NumberSetting skyShaderOpacity = new NumberSetting("SkyShaderOpacity", "Opacity", "The opacity used by ambience sky shaders.", new ModeSetting.Visibility(skyMode, "BlackHole", "Nebula"), 0.82f, 0.1f, 1.0f);
    public NumberSetting skyShaderGlow = new NumberSetting("SkyShaderGlow", "Glow", "The glow intensity used by ambience sky shaders.", new ModeSetting.Visibility(skyMode, "BlackHole", "Nebula"), 1.0f, 0.2f, 3.0f);
    public BooleanSetting syncFogToSky = new BooleanSetting("SyncFogToSky", "Sync Fog", "Uses the sky color as the fog color while both custom sky and fog are enabled.", new BooleanSetting.Visibility(modifySky, true), false);

    public void renderSkyShader() {
        if (mc.player == null || mc.world == null) return;
        if (!isToggled() || !modifySky.getValue() || !hasSkyShader()) return;

        Solstice.AMBIENCE_SHADER_MANAGER.render(getSkyShaderName(), skyShaderColor.getColor(), skyShaderOpacity.getValue().floatValue(), skyShaderGlow.getValue().floatValue());
    }

    public Color getRuntimeSkyColor() {
        if (!modifySky.getValue()) {
            return skyColor.getColor();
        }

        if ("Phobos".equalsIgnoreCase(skyMode.getValue())) {
            float t = ((System.currentTimeMillis() % 600000L) / 1000.0f) * skySpeed.getValue().floatValue() * 0.12f;
            float intensity = skyIntensity.getValue().floatValue();

            float red = 0.30f
                    + 0.12f * (float) Math.cos(6.28318f * (t + 0.00f))
                    + 0.11f * (float) Math.cos(6.28318f * (t * 1.7f + 0.30f))
                    + 0.10f * (float) Math.cos(6.28318f * (t * 2.8f + 0.10f));
            float green = 0.40f
                    + 0.12f * (float) Math.cos(6.28318f * (t + 0.80f))
                    + 0.11f * (float) Math.cos(6.28318f * (t * 1.7f + 0.40f))
                    + 0.10f * (float) Math.cos(6.28318f * (t * 2.8f + 0.70f));
            float blue = 0.52f
                    + 0.12f * (float) Math.cos(6.28318f * (t + 1.10f))
                    + 0.11f * (float) Math.cos(6.28318f * (t * 1.7f + 0.10f))
                    + 0.12f * (float) Math.cos(6.28318f * (t * 2.8f + 1.10f));

            return new Color(scale(red, intensity), scale(green, intensity), scale(blue, intensity), 255);
        }

        if ("BlackHole".equalsIgnoreCase(skyMode.getValue())) {
            Color tint = skyShaderColor.getColor();
            return new Color(scale(tint.getRed() / 255.0f, 0.42f), scale(tint.getGreen() / 255.0f, 0.32f), scale(tint.getBlue() / 255.0f, 0.55f), 255);
        }

        if ("Nebula".equalsIgnoreCase(skyMode.getValue())) {
            Color tint = skyShaderColor.getColor();
            return new Color(scale(tint.getRed() / 255.0f, 0.70f), scale(tint.getGreen() / 255.0f, 0.52f), scale(tint.getBlue() / 255.0f, 0.85f), 255);
        }

        return skyColor.getColor();
    }

    public Color getRuntimeFogColor() {
        if (modifySky.getValue() && syncFogToSky.getValue()) {
            return getRuntimeSkyColor();
        }

        return fogColor.getColor();
    }

    private int scale(float value, float intensity) {
        return Math.clamp((int) (Math.clamp(value * intensity, 0.0f, 1.0f) * 255.0f), 0, 255);
    }

    public boolean hasSkyShader() {
        return "BlackHole".equalsIgnoreCase(skyMode.getValue()) || "Nebula".equalsIgnoreCase(skyMode.getValue());
    }

    public String getSkyShaderName() {
        return "BlackHole".equalsIgnoreCase(skyMode.getValue()) ? "black_hole" : "nebula";
    }
}
