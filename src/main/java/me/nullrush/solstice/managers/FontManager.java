package me.nullrush.solstice.managers;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.mixins.accessors.DrawContextAccessor;
import me.nullrush.solstice.mixins.accessors.TextRendererAccessor;
import me.nullrush.solstice.modules.impl.core.FontModule;
import me.nullrush.solstice.utils.IMinecraft;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.font.FontRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
public class FontManager implements IMinecraft {
    private static final Map<String, String> AVAILABLE_FONTS = new HashMap<>();

    static {
        for (String fontName : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            AVAILABLE_FONTS.put(fontName.toLowerCase(Locale.ROOT), fontName);
        }
    }

    private FontRenderer fontRenderer;

    public void drawText(DrawContext context, String text, int x, int y, Color color) {
        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            fontRenderer.drawString(context.getMatrices(), text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, x, y, color.getRGB(), false);
        }
    }

    public void drawTextWithShadow(DrawContext context, String text, int x, int y, Color color) {
        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            if (!module.shadowMode.getValue().equalsIgnoreCase("None")) fontRenderer.drawString(context.getMatrices(), text, x + getShadowOffset(module), y + getShadowOffset(module), color.getRGB(), true);
            fontRenderer.drawString(context.getMatrices(), text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, x, y, color.getRGB(), true);
        }
    }

    public void drawText(DrawContext context, OrderedText text, int x, int y, Color color) {
        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            fontRenderer.drawText(context.getMatrices(), text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, x, y, color.getRGB(), false);
        }
    }

    public void drawTextWithShadow(DrawContext context, OrderedText text, int x, int y, Color color) {
        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            if (!module.shadowMode.getValue().equalsIgnoreCase("None")) fontRenderer.drawText(context.getMatrices(), text, x + getShadowOffset(module), y + getShadowOffset(module), color.getRGB(), true);
            fontRenderer.drawText(context.getMatrices(), text, x, y, color.getRGB(), false);
        } else {
            context.drawText(mc.textRenderer, text, x, y, color.getRGB(), true);
        }
    }

    public void drawTextWithShadow(MatrixStack matrices, String text, int x, int y, VertexConsumerProvider vertexConsumers, Color color) {
        RenderSystem.disableDepthTest();

        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            if (!module.shadowMode.getValue().equalsIgnoreCase("None")) fontRenderer.drawString(matrices, text, x + getShadowOffset(module), y + getShadowOffset(module), color.getRGB(), true);
            fontRenderer.drawString(matrices, text, x, y, color.getRGB(), false);
        } else {
            ((TextRendererAccessor) mc.textRenderer).invokeDrawLayer(text, x, y, TextRendererAccessor.invokeTweakTransparency(color.getRGB()), true, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0, false);
            mc.getBufferBuilders().getEntityVertexConsumers().draw();

            ((TextRendererAccessor) mc.textRenderer).invokeDrawLayer(text, x, y, TextRendererAccessor.invokeTweakTransparency(color.getRGB()), false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0, false);
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
        }

        RenderSystem.enableDepthTest();
    }

    public void drawTextWithOutline(DrawContext context, String text, int x, int y, Color color, Color outlineColor) {
        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            fontRenderer.drawString(context.getMatrices(), FontRenderer.stripControlCodes(text), x + 0.5f, y - 0.5f, outlineColor.getRGB(), false);
            fontRenderer.drawString(context.getMatrices(), FontRenderer.stripControlCodes(text), x - 0.5f, y + 0.5f, outlineColor.getRGB(), false);
            fontRenderer.drawString(context.getMatrices(), FontRenderer.stripControlCodes(text), x + 0.5f, y + 0.5f, outlineColor.getRGB(), false);
            fontRenderer.drawString(context.getMatrices(), FontRenderer.stripControlCodes(text), x - 0.5f, y - 0.5f, outlineColor.getRGB(), false);

            fontRenderer.drawString(context.getMatrices(), text, x, y, color.getRGB(), false);
        } else {
            mc.textRenderer.drawWithOutline(Text.literal(text).asOrderedText(), x, y, color.getRGB(), outlineColor.getRGB(), context.getMatrices().peek().getPositionMatrix(), ((DrawContextAccessor) context).getVertexConsumers(), 0);
        }
    }

    public void drawRainbowString(DrawContext context, String string, int x, int y, long offset) {
        MutableText builder = Text.empty();

        int[] i = {0};
        Text.literal(string).asOrderedText().accept((index, style, codePoint) -> {
            MutableText text = Text.empty();

            if (style.getColor() == null) {
                long index1 = (long) i[0] * offset;
                Color color = ColorUtils.getOffsetRainbow(index1);
                text.append(Text.literal(String.valueOf(Character.toChars(codePoint))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.getRGB()))));
            } else {
                text.append(Text.literal(String.valueOf(Character.toChars(codePoint))).setStyle(style));
            }

            builder.append(text);
            i[0]++;

            return true;
        });

        Solstice.FONT_MANAGER.drawTextWithShadow(context, builder.asOrderedText(), x, y, Color.WHITE);
    }

    public int getWidth(String text) {
        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            return (int) fontRenderer.getTextWidth(text) + module.widthOffset.getValue().intValue();
        } else {
            return mc.textRenderer.getWidth(text);
        }
    }

    public int getHeight() {
        FontModule module = getFontModule();
        if (canUseCustomFont(module)) {
            return (int) fontRenderer.getHeight() + module.heightOffset.getValue().intValue();
        } else {
            return mc.textRenderer.fontHeight;
        }
    }

    public float getShadowOffset() {
        return getShadowOffset(getFontModule());
    }

    public void setFontRenderer(FontRenderer fontRenderer) {
        if (this.fontRenderer != null && this.fontRenderer != fontRenderer) {
            this.fontRenderer.close();
        }

        this.fontRenderer = fontRenderer;
    }

    public String resolveFontName(String name, String fallback) {
        String normalized = normalizeFontKey(name);
        if (normalized != null) {
            String resolved = AVAILABLE_FONTS.get(normalized);
            if (resolved != null) {
                return resolved;
            }
        }

        return fallback;
    }

    public boolean hasFont(String name) {
        return resolveFontName(name, null) != null;
    }

    private FontModule getFontModule() {
        return Solstice.MODULE_MANAGER.getModule(FontModule.class);
    }

    private boolean canUseCustomFont(FontModule module) {
        return module != null && module.isToggled() && module.customFont.getValue() && fontRenderer != null;
    }

    private float getShadowOffset(FontModule module) {
        if (module.shadowMode.getValue().equalsIgnoreCase("None")) {
            return 0.0f;
        } else if (module.shadowMode.getValue().equalsIgnoreCase("Custom")) {
            return module.shadowOffset.getValue().floatValue();
        } else {
            return 1.0f;
        }
    }

    private String normalizeFontKey(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }
}
