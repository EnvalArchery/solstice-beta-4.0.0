package me.nullrush.solstice.gui.api;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.ClickGuiScreen;
import me.nullrush.solstice.modules.impl.core.ClickGuiModule;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public final class GuiVisuals {
    private static final Color FALLBACK_ACCENT = new Color(130, 202, 255);
    private static final Color FALLBACK_SECONDARY = new Color(96, 146, 255);
    private static final Color HOT_PINK = new Color(255, 50, 168);
    private static final Color RICH_PURPLE = new Color(154, 32, 255);
    private static final Color CYAN_GLOW = new Color(90, 236, 255);

    private GuiVisuals() {
    }

    public static float getTime() {
        return (System.currentTimeMillis() % 600000L) / 1000.0f;
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color accent(long offset, int alpha) {
        try {
            if (Solstice.MODULE_MANAGER != null && Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class) != null) {
                return ClickGuiScreen.getButtonColor((int) offset, alpha);
            }
        } catch (Exception ignored) {
        }

        return withAlpha(FALLBACK_ACCENT, alpha);
    }

    public static Color secondary(long offset, int alpha) {
        Color accent = accent(offset, 255);
        Color mixed = mix(accent, FALLBACK_SECONDARY, 0.35f);
        return withAlpha(mixed, alpha);
    }

    public static Color mix(Color first, Color second, float amount) {
        float inverse = 1.0f - amount;
        return new Color(
                Math.min(255, Math.max(0, (int) (first.getRed() * inverse + second.getRed() * amount))),
                Math.min(255, Math.max(0, (int) (first.getGreen() * inverse + second.getGreen() * amount))),
                Math.min(255, Math.max(0, (int) (first.getBlue() * inverse + second.getBlue() * amount))),
                Math.min(255, Math.max(0, (int) (first.getAlpha() * inverse + second.getAlpha() * amount)))
        );
    }

    public static void renderBackdrop(DrawContext context, float width, float height, float time) {
        Renderer2D.renderGradient(context.getMatrices(), 0, 0, width, height, new Color(6, 10, 18, 255), new Color(3, 5, 11, 255));
        Renderer2D.renderSidewaysGradient(context.getMatrices(), 0, 0, width, height, new Color(10, 18, 32, 110), new Color(2, 8, 18, 40));

        renderGrid(context, width, height);
        renderPulseLine(context, -40, height * 0.28f, width + 40, 10.0f, accent(10, 85), time * 1.25f, 0.0f);
        renderPulseLine(context, -60, height * 0.48f, width + 60, 16.0f, accent(120, 115), time * 1.1f, 0.21f);
        renderPulseLine(context, -30, height * 0.72f, width + 30, 12.0f, secondary(220, 85), time * 0.9f, 0.43f);
    }

    public static void renderOverlay(DrawContext context, float width, float height, float time) {
        ClickGuiModule module = getClickGuiModule();
        String shader = module == null ? "Hologram" : module.backgroundShaderType.getValue();
        boolean enabled = module == null || module.backgroundShaders.getValue();
        int opacity = module == null ? 92 : module.backgroundShaderOpacity.getValue().intValue();
        float speed = module == null ? 1.2f : module.backgroundShaderSpeed.getValue().floatValue() / 10.0f;
        float intensity = module == null ? 0.72f : module.backgroundShaderIntensity.getValue().floatValue() / 100.0f;
        float scaledTime = time * speed;
        Color backgroundMain = getBackgroundMainColor(module);
        Color backgroundAccent = getBackgroundAccentColor(module);
        Color backgroundHighlight = mix(backgroundMain, Color.WHITE, 0.14f);

        if (!enabled || "None".equals(shader)) {
            return;
        }

        switch (shader) {
            case "Hologram" -> {
                renderGrid(context, width, height, opacity / 10 + 3, opacity / 12 + 3);
                renderWideBands(context, 0, 0, width, height, scaledTime, opacity, intensity, backgroundMain, backgroundAccent, backgroundHighlight, false);
                renderPulseLine(context, -40, height * 0.32f, width + 40, 11.0f + 8.0f * intensity, withAlpha(mix(backgroundMain, accent(24, 255), 0.4f), opacity + 26), scaledTime * 0.84f, 0.14f);
                renderPulseLine(context, -50, height * 0.58f, width + 50, 15.0f + 10.0f * intensity, withAlpha(mix(backgroundAccent, secondary(120, 255), 0.35f), opacity + 18), scaledTime * 0.72f, 0.39f);
            }
            case "Rainbow" -> {
                renderGrid(context, width, height, opacity / 12 + 2, opacity / 14 + 2);
                renderSpectrumBands(context, 0, 0, width, height, scaledTime, 0.18f, opacity, intensity, backgroundMain, backgroundAccent, backgroundHighlight);
                renderPulseLine(context, -30, height * 0.45f, width + 30, 8.0f + 6.0f * intensity, withAlpha(backgroundHighlight, opacity / 2 + 32), scaledTime, 0.23f);
            }
            case "Cyberpunk" -> {
                renderGrid(context, width, height, opacity / 12 + 2, opacity / 13 + 2);
                renderWideBands(context, 0, 0, width, height, scaledTime * 0.9f, opacity, intensity, backgroundMain, backgroundAccent, backgroundHighlight, true);
                for (int i = -((int) height); i < width + height; i += 36) {
                    Renderer2D.renderLine(context.getMatrices(), i, 0, i - height * 0.55f, height, withAlpha(mix(backgroundHighlight, accent(i, 255), 0.25f), opacity / 3 + 18));
                }
                renderPulseLine(context, -20, height * 0.62f, width + 20, 6.0f + 6.0f * intensity, withAlpha(backgroundHighlight, opacity / 2 + 24), scaledTime * 1.2f, 0.31f);
            }
            case "Plasma" -> {
                renderGrid(context, width, height, opacity / 12 + 2, opacity / 12 + 2);
                renderWideBands(context, 0, 0, width, height, scaledTime * 1.08f, opacity, intensity, backgroundMain, backgroundAccent, backgroundHighlight, false);
                for (int i = 0; i < 7; i++) {
                    float centerY = height * (0.13f + i * 0.11f);
                    renderPulseLine(context, -40, centerY, width + 40, 7.0f + i * 1.35f + intensity * 7.0f, withAlpha(mix(backgroundMain, accent(i * 31L, 255), 0.3f), opacity / 2 + 22), scaledTime * (0.74f + i * 0.05f), 0.12f * i);
                }
            }
            case "Liquid" -> {
                renderGrid(context, width, height, opacity / 14 + 2, opacity / 16 + 2);
                renderLiquidBands(context, 0, 0, width, height, scaledTime, 0.14f, opacity, intensity, backgroundMain, backgroundAccent, backgroundHighlight);
                renderPulseLine(context, -40, height * 0.76f, width + 40, 10.0f + 6.0f * intensity, withAlpha(mix(backgroundAccent, backgroundHighlight, 0.25f), opacity / 2 + 18), scaledTime * 0.9f, 0.48f);
            }
            case "Matrix" -> {
                renderGrid(context, width, height, opacity / 10 + 1, opacity / 12 + 1);
                for (int x = 18; x < width; x += 34) {
                    float drop = (scaledTime * (24.0f + (x % 5) * 4.0f) + x * 0.8f) % (height + 70.0f) - 70.0f;
                    Renderer2D.renderQuad(context.getMatrices(), x, drop, x + 2, Math.min(height, drop + 34.0f + intensity * 18.0f), withAlpha(mix(backgroundAccent, accent(x, 255), 0.35f), opacity / 2 + 18));
                    Renderer2D.renderQuad(context.getMatrices(), x - 1, drop + 28.0f, x + 3, Math.min(height, drop + 34.0f + intensity * 18.0f), withAlpha(backgroundMain, opacity / 2 + 28));
                }
            }
            case "Aurora" -> {
                Renderer2D.renderGradient(context.getMatrices(), 0, 0, width, height * 0.48f, new Color(0, 0, 0, 0), withAlpha(mix(backgroundAccent, secondary(20, 255), 0.36f), opacity / 2 + 24));
                Renderer2D.renderGradient(context.getMatrices(), 0, height * 0.1f, width, height * 0.72f, new Color(0, 0, 0, 0), withAlpha(mix(backgroundMain, accent(120, 255), 0.34f), opacity / 2 + 18));
                renderPulseLine(context, -30, height * 0.35f, width + 30, 16.0f + intensity * 10.0f, withAlpha(backgroundMain, opacity / 2 + 20), scaledTime * 0.75f, 0.22f);
                renderPulseLine(context, -30, height * 0.55f, width + 30, 20.0f + intensity * 10.0f, withAlpha(backgroundAccent, opacity / 2 + 18), scaledTime * 0.68f, 0.48f);
            }
            case "Nebula" -> {
                Renderer2D.renderGradient(context.getMatrices(), 0, 0, width, height, new Color(0, 0, 0, 0), withAlpha(mix(backgroundMain, accent(50, 255), 0.4f), opacity / 3 + 18));
                for (int i = 0; i < 7; i++) {
                    float orbX = (float) ((Math.sin(scaledTime * 0.38f + i * 1.6f) * 0.5f + 0.5f) * width);
                    float orbY = (float) ((Math.cos(scaledTime * 0.31f + i * 1.1f) * 0.5f + 0.5f) * height);
                    float radius = 42.0f + i * 12.0f + intensity * 28.0f;
                    Renderer2D.renderCircle(context.getMatrices(), orbX, orbY, radius, withAlpha(i % 2 == 0 ? mix(backgroundMain, accent(i * 40L, 255), 0.45f) : mix(backgroundAccent, secondary(i * 60L, 255), 0.45f), opacity / 4 + 12));
                }
            }
            case "Scan" -> {
                renderGrid(context, width, height, opacity / 8 + 3, opacity / 10 + 2);
                float lineY = (scaledTime * 36.0f) % (height + 90.0f) - 45.0f;
                Renderer2D.renderGradient(context.getMatrices(), 0, lineY - 26.0f, width, lineY + 26.0f, new Color(0, 0, 0, 0), withAlpha(backgroundMain, opacity / 2 + 28));
                renderPulseLine(context, -20, lineY, width + 20, 6.0f + intensity * 6.0f, withAlpha(mix(backgroundMain, accent(90, 255), 0.45f), opacity / 2 + 32), scaledTime, 0.14f);
            }
            default -> renderWideBands(context, 0, 0, width, height, scaledTime, opacity, intensity, backgroundMain, backgroundAccent, backgroundHighlight, false);
        }
    }

    public static void renderPanel(DrawContext context, float left, float top, float right, float bottom, Color accent, boolean active, boolean hovered) {
        Color base = hovered ? new Color(18, 24, 36, 225) : new Color(12, 18, 29, 210);
        Color shadow = active ? withAlpha(accent, 36) : new Color(0, 0, 0, 50);

        Renderer2D.renderQuad(context.getMatrices(), left, top, right, bottom, shadow);
        Renderer2D.renderQuad(context.getMatrices(), left + 1, top + 1, right - 1, bottom - 1, base);
        Renderer2D.renderGradient(context.getMatrices(), left + 1, top + 1, right - 1, top + 13, withAlpha(accent, active ? 48 : 26), new Color(0, 0, 0, 0));
        Renderer2D.renderOutline(context.getMatrices(), left, top, right, bottom, hovered ? withAlpha(accent, 145) : withAlpha(accent, active ? 110 : 70));
        Renderer2D.renderQuad(context.getMatrices(), left + 1, bottom - 2, right - 1, bottom - 1, withAlpha(accent, active ? 120 : 55));
    }

    public static void renderClickGuiPanel(DrawContext context, float left, float top, float right, float bottom, Color accent, boolean active, boolean hovered, boolean categorySurface) {
        ClickGuiModule module = getClickGuiModule();
        float time = getTime();
        float panelDrift = module == null ? 0.0f : module.panelShaderSpeed.getValue().floatValue() / 48.0f;
        Color primary = mix(accent, getPanelMainColor(module), 0.55f);
        Color deep = mix(primary, getPanelAccentColor(module), 0.42f);
        Color bright = mix(primary, Color.WHITE, 0.14f);
        int baseAlpha = module == null ? 132 : categorySurface ? module.frameAlpha.getValue().intValue() : module.moduleAlpha.getValue().intValue();
        int fillAlpha = Math.min(230, baseAlpha + (active ? 22 : 0) + (hovered ? 18 : 0));
        Color shadow = withAlpha(Color.BLACK, Math.min(150, baseAlpha / 2 + 30));
        Color outer = withAlpha(mix(primary, deep, 0.34f), Math.min(255, baseAlpha + 34));
        Color inner = withAlpha(mix(new Color(24, 6, 36), deep, active ? 0.62f : hovered ? 0.54f : 0.48f), fillAlpha);
        Color topGlow = withAlpha(mix(primary, bright, 0.25f), Math.min(255, baseAlpha + 52));

        Renderer2D.renderQuad(context.getMatrices(), left, top, right, bottom, shadow);
        Renderer2D.renderQuad(context.getMatrices(), left + 1, top + 1, right - 1, bottom - 1, inner);
        Renderer2D.renderGradient(context.getMatrices(), left + 1, top + 1, right - 1, top + Math.min(16, bottom - top - 1), topGlow, new Color(0, 0, 0, 0));

        if (shouldRenderPanelShader(module, categorySurface)) {
            renderPanelShader(context, left + 1, top + 1, right - 1, bottom - 1, primary, deep, time, panelDrift, module.panelShaderType.getValue(), module.panelShaderOpacity.getValue().intValue(), module.glowIntensity.getValue().floatValue(), module.glowWidth.getValue().intValue());
        }

        Renderer2D.renderOutline(context.getMatrices(), left, top, right, bottom, hovered ? withAlpha(primary, 170) : withAlpha(outer, active ? 150 : 110));
        Renderer2D.renderQuad(context.getMatrices(), left + 1, bottom - 2, right - 1, bottom - 1, withAlpha(primary, active ? 155 : 90));
        Renderer2D.renderQuad(context.getMatrices(), left + 1, top + 1, right - 1, top + 2, withAlpha(bright, active ? 90 : 58));
    }

    public static void renderClickGuiBody(DrawContext context, float left, float top, float right, float bottom, Color accent) {
        ClickGuiModule module = getClickGuiModule();
        int alpha = module == null ? 118 : Math.min(220, module.frameAlpha.getValue().intValue() - 18);
        Color body = withAlpha(mix(new Color(8, 6, 16), mix(accent, getPanelAccentColor(module), 0.45f), 0.3f), alpha);
        Renderer2D.renderQuad(context.getMatrices(), left, top, right, bottom, body);
        Renderer2D.renderGradient(context.getMatrices(), left, top, right, bottom, new Color(0, 0, 0, 0), withAlpha(mix(getPanelMainColor(module), accent, 0.55f), alpha / 3));
        Renderer2D.renderOutline(context.getMatrices(), left, top, right, bottom, withAlpha(mix(accent, getPanelMainColor(module), 0.55f), 100));
    }

    public static void renderPulseLine(DrawContext context, float startX, float startY, float endX, float endY, float amplitude, Color color, float time, float seed) {
        float length = Math.max(1.0f, (float) Math.hypot(endX - startX, endY - startY));
        float step = 4.0f;
        float directionX = (endX - startX) / length;
        float directionY = (endY - startY) / length;
        float normalX = -directionY;
        float normalY = directionX;
        float previousX = startX;
        float previousY = startY;

        for (float distance = step; distance <= length; distance += step) {
            float progress = distance / length;
            float wave = getWave(progress, time, seed) * amplitude;
            float x = startX + directionX * distance + normalX * wave;
            float y = startY + directionY * distance + normalY * wave;
            Renderer2D.renderLine(context.getMatrices(), previousX, previousY, x, y, color);
            previousX = x;
            previousY = y;
        }
    }

    public static void renderPulseLine(DrawContext context, float left, float centerY, float right, float amplitude, Color color, float time, float seed) {
        float width = Math.max(1.0f, right - left);
        float step = 4.0f;
        float previousX = left;
        float previousY = centerY + getWave(0.0f, time, seed) * amplitude;

        for (float x = left + step; x <= right; x += step) {
            float progress = (x - left) / width;
            float y = centerY + getWave(progress, time, seed) * amplitude;
            Renderer2D.renderLine(context.getMatrices(), previousX, previousY, x, y, color);
            previousX = x;
            previousY = y;
        }
    }

    private static boolean shouldRenderPanelShader(ClickGuiModule module, boolean categorySurface) {
        if (module == null) {
            return true;
        }
        if ("None".equals(module.panelShaderType.getValue())) {
            return false;
        }
        return categorySurface ? module.categoryShaders.getValue() : module.moduleShaders.getValue();
    }

    private static void renderPanelShader(DrawContext context, float left, float top, float right, float bottom, Color primary, Color secondary, float time, float drift, String shader, int opacity, float intensity, int glowWidth) {
        Color panelHighlight = mix(primary, Color.WHITE, 0.18f);
        switch (shader) {
            case "Hologram" -> renderStaticBands(context, left, top, right, bottom, primary, secondary, time, drift, opacity, intensity, glowWidth, 6, 0.12f, false);
            case "Rainbow" -> renderSpectrumBands(context, left, top, right, bottom, time, drift, opacity, intensity * 0.75f, primary, secondary, panelHighlight);
            case "Cyberpunk" -> {
                renderStaticBands(context, left, top, right, bottom, primary, mix(panelHighlight, secondary, 0.3f), time, drift, opacity, intensity, glowWidth, 7, 0.1f, true);
                for (int i = 0; i < 4; i++) {
                    float y = top + ((i + 1) * (bottom - top) / 5.0f);
                    Renderer2D.renderLine(context.getMatrices(), left, y + i, right, y - 4 + i, withAlpha(mix(panelHighlight, Color.WHITE, 0.15f), opacity / 3 + 10));
                }
            }
            case "Plasma" -> renderStaticBands(context, left, top, right, bottom, mix(primary, HOT_PINK, 0.2f), mix(secondary, RICH_PURPLE, 0.4f), time, drift, opacity, intensity, glowWidth, 8, 0.16f, false);
            case "Liquid" -> renderLiquidBands(context, left, top, right, bottom, time, drift, opacity, intensity * 0.82f, primary, secondary, panelHighlight);
            case "Prism" -> {
                renderSpectrumBands(context, left, top, right, bottom, time, drift, opacity / 2 + 18, intensity * 0.6f, primary, secondary, panelHighlight);
                renderStaticBands(context, left, top, right, bottom, primary, secondary, time, drift, opacity / 2 + 12, intensity, glowWidth, 5, 0.08f, false);
            }
            case "Scanline" -> {
                renderStaticBands(context, left, top, right, bottom, primary, secondary, time, drift, opacity / 2 + 18, intensity, glowWidth, 10, 0.06f, false);
                for (float y = top + 2; y < bottom; y += 3.0f) {
                    Renderer2D.renderQuad(context.getMatrices(), left, y, right, y + 1, withAlpha(primary, opacity / 8 + 3));
                }
            }
            default -> renderStaticBands(context, left, top, right, bottom, primary, secondary, time, drift, opacity, intensity, glowWidth, 6, 0.1f, false);
        }
    }

    private static void renderStaticBands(DrawContext context, float left, float top, float right, float bottom, Color primary, Color secondary, float time, float drift, int opacity, float intensity, int glowWidth, int bands, float phaseOffset, boolean accentBlend) {
        float height = Math.max(1.0f, bottom - top);

        for (int i = 0; i < bands; i++) {
            float base = top + ((float) i + 0.65f) / bands * height;
            float bandCenter = base + (float) Math.sin(time * drift + i * phaseOffset) * (drift <= 0.0f ? 0.0f : 0.65f + intensity * 0.6f);
            float bandThickness = Math.max(1.5f, glowWidth * 0.35f + intensity * (0.7f + i * 0.05f));
            Color blendColor = accentBlend ? mix(primary, secondary, 0.5f) : primary;
            Color bandColor = withAlpha(mix(i % 2 == 0 ? primary : secondary, blendColor, accentBlend ? 0.25f : 0.12f), opacity / 2 + 10);
            Renderer2D.renderGradient(context.getMatrices(), left, bandCenter - bandThickness, right, bandCenter + bandThickness, new Color(0, 0, 0, 0), bandColor);
            Renderer2D.renderQuad(context.getMatrices(), left, bandCenter - 0.6f, right, bandCenter + 0.6f, withAlpha(bandColor, Math.min(255, bandColor.getAlpha() + 24)));
        }
    }

    private static void renderWideBands(DrawContext context, float left, float top, float right, float bottom, float time, int opacity, float intensity, Color primary, Color secondaryColor, Color highlight, boolean accentBlend) {
        Color widePrimary = mix(primary, accent(44, 255), 0.28f);
        Color wideSecondary = mix(secondaryColor, highlight, 0.22f);
        renderStaticBands(context, left, top, right, bottom, widePrimary, wideSecondary, time, 0.18f, opacity, intensity, 7, 8, 0.14f, accentBlend);
    }

    private static void renderLiquidBands(DrawContext context, float left, float top, float right, float bottom, float time, float drift, int opacity, float intensity, Color primaryColor, Color accentColor, Color highlightColor) {
        int layers = 5;
        float height = Math.max(1.0f, bottom - top);
        Color primary = mix(primaryColor, accent(80, 255), 0.2f);
        Color secondaryColor = mix(accentColor, highlightColor, 0.15f);

        for (int i = 0; i < layers; i++) {
            float wave = (float) Math.sin(time * drift + i * 0.7f);
            float center = top + (i + 0.55f) / layers * height + wave * (5.0f + intensity * 7.0f);
            float size = 6.0f + i * 1.4f + intensity * 4.5f;
            Color layer = withAlpha(mix(i % 2 == 0 ? primary : secondaryColor, Color.WHITE, 0.08f), opacity / 3 + 10);
            Renderer2D.renderGradient(context.getMatrices(), left, center - size, right, center + size, new Color(0, 0, 0, 0), layer);
            Renderer2D.renderGradient(context.getMatrices(), left, center - size * 0.45f, right, center + size * 0.45f, withAlpha(layer, layer.getAlpha() + 22), new Color(0, 0, 0, 0));
        }
    }

    private static void renderSpectrumBands(DrawContext context, float left, float top, float right, float bottom, float time, float drift, int opacity, float intensity, Color primaryColor, Color accentColor, Color highlightColor) {
        Color[] palette = new Color[]{
                primaryColor,
                accentColor,
                mix(highlightColor, accentColor, 0.28f),
                mix(primaryColor, Color.WHITE, 0.18f)
        };
        float height = Math.max(1.0f, bottom - top);

        for (int i = 0; i < palette.length + 2; i++) {
            float wave = (float) Math.sin(time * drift + i * 0.75f);
            float center = top + (i + 0.55f) / (palette.length + 2) * height + wave * (drift <= 0.0f ? 0.0f : 0.5f + intensity * 0.7f);
            float band = 2.0f + intensity * 2.4f;
            Color color = withAlpha(palette[i % palette.length], opacity / 2 + 12);
            Renderer2D.renderGradient(context.getMatrices(), left, center - band, right, center + band, new Color(0, 0, 0, 0), color);
            Renderer2D.renderQuad(context.getMatrices(), left, center - 0.5f, right, center + 0.5f, withAlpha(color, Math.min(255, color.getAlpha() + 32)));
        }
    }

    private static float getWave(float progress, float time, float seed) {
        float wave = (float) (Math.sin(progress * 12.0f + time * 2.1f + seed * 8.0f) * 0.35f);
        wave += (float) (Math.sin(progress * 25.0f - time * 1.45f + seed * 17.0f) * 0.12f);

        float pulsePosition = (time * 0.2f + seed) % 1.0f;
        float distance = Math.abs(progress - pulsePosition);
        float spike = Math.max(0.0f, 1.0f - distance / 0.03f);
        float preSpike = Math.max(0.0f, 1.0f - Math.abs(progress - pulsePosition + 0.025f) / 0.02f);
        float postSpike = Math.max(0.0f, 1.0f - Math.abs(progress - pulsePosition - 0.04f) / 0.03f);

        return wave - preSpike * 0.35f + spike * 1.85f - postSpike * 0.5f;
    }

    private static void renderGrid(DrawContext context, float width, float height) {
        renderGrid(context, width, height, 8, 7);
    }

    private static void renderGrid(DrawContext context, float width, float height, int verticalAlpha, int horizontalAlpha) {
        for (int x = 0; x < width; x += 40) {
            Renderer2D.renderQuad(context.getMatrices(), x, 0, x + 1, height, new Color(255, 255, 255, verticalAlpha));
        }

        for (int y = 0; y < height; y += 36) {
            Renderer2D.renderQuad(context.getMatrices(), 0, y, width, y + 1, new Color(255, 255, 255, horizontalAlpha));
        }
    }

    private static ClickGuiModule getClickGuiModule() {
        try {
            if (Solstice.MODULE_MANAGER != null) {
                return Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Color getBackgroundMainColor(ClickGuiModule module) {
        return module == null ? HOT_PINK : module.backgroundShaderColor.getColor();
    }

    private static Color getBackgroundAccentColor(ClickGuiModule module) {
        return module == null ? RICH_PURPLE : module.backgroundShaderAccent.getColor();
    }

    private static Color getPanelMainColor(ClickGuiModule module) {
        return module == null ? HOT_PINK : module.panelShaderColor.getColor();
    }

    private static Color getPanelAccentColor(ClickGuiModule module) {
        return module == null ? RICH_PURPLE : module.panelShaderAccent.getColor();
    }

    private static Color getPanelHighlightColor(ClickGuiModule module) {
        return module == null ? CYAN_GLOW : mix(module.panelShaderColor.getColor(), Color.WHITE, 0.22f);
    }
}
