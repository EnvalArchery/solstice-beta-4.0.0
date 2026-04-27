package me.nullrush.solstice.modules.impl.core;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.SettingChangeEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.Setting;
import me.nullrush.solstice.settings.impl.*;
import me.nullrush.solstice.utils.font.FontRenderer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.awt.*;

@RegisterModule(name = "Font", description = "Manages the client and the game's font rendering.", category = Module.Category.CORE, drawn = false)
public class FontModule extends Module {
    public CategorySetting customFontCategory = new CategorySetting("CustomFont", "The category for settings relating to custom fonts.");
    public BooleanSetting customFont = new BooleanSetting("CustomFont", "Enabled", "Enables custom font rendering.", new CategorySetting.Visibility(customFontCategory), false);
    public StringSetting name = new StringSetting("Name", "The name of the font that will be rendered.", new CategorySetting.Visibility(customFontCategory), "Verdana");
    public NumberSetting size = new NumberSetting("Size", "The size of the custom font that will be rendered.", new CategorySetting.Visibility(customFontCategory), 18, 8, 48);
    public ModeSetting style = new ModeSetting("Style", "The style that will be used in the font's rendering.", new CategorySetting.Visibility(customFontCategory), "Plain", new String[]{"Plain", "Bold", "Italic", "BoldItalic"});
    public BooleanSetting global = new BooleanSetting("Global", "Applies the custom font rendering on every part of the game.", new CategorySetting.Visibility(customFontCategory), false);

    public CategorySetting fallbackCategory = new CategorySetting("Fallbacks", "Additional fonts used when the primary font cannot render a character.");
    public BooleanSetting useFallbacks = new BooleanSetting("UseFallbacks", "Enabled", "Uses backup fonts for symbols, icons and unsupported glyphs.", new CategorySetting.Visibility(fallbackCategory), true);
    public StringSetting fallbackName = new StringSetting("FallbackName", "The secondary font used when the primary font is missing glyphs.", new BooleanSetting.Visibility(useFallbacks, true), "Segoe UI");
    public StringSetting symbolName = new StringSetting("SymbolFont", "The font used for symbols and special characters.", new BooleanSetting.Visibility(useFallbacks, true), "Segoe UI Symbol");
    public StringSetting emojiName = new StringSetting("EmojiFont", "The font used for emoji-like glyphs and wider unicode characters.", new BooleanSetting.Visibility(useFallbacks, true), "Segoe UI Emoji");

    public CategorySetting renderingCategory = new CategorySetting("Rendering", "Controls how the custom font atlas is generated.");
    public BooleanSetting antiAliasing = new BooleanSetting("AntiAliasing", "AntiAliasing", "Smooths the font edges for a cleaner look.", new CategorySetting.Visibility(renderingCategory), true);
    public BooleanSetting fractionalMetrics = new BooleanSetting("FractionalMetrics", "FractionalMetrics", "Improves spacing precision for custom fonts.", new CategorySetting.Visibility(renderingCategory), true);

    public CategorySetting offsetsCategory = new CategorySetting("Offsets", "Allows you to offset the custom font rendering to make it render perfectly.");
    public NumberSetting xOffset = new NumberSetting("XOffset", "The offset that will be applied to the font on the X axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);
    public NumberSetting yOffset = new NumberSetting("YOffset", "The offset that will be applied to the font on the Y axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);
    public NumberSetting widthOffset = new NumberSetting("WidthOffset", "The offset that will be applied to the font on the X axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);
    public NumberSetting heightOffset = new NumberSetting("HeightOffset", "The font's offset on the Y axis.", new CategorySetting.Visibility(offsetsCategory), 0, -10, 10);

    public CategorySetting shadowsCategory = new CategorySetting("Shadows", "The category for settings related to font shadows.");
    public ModeSetting shadowMode = new ModeSetting("ShadowMode", "Mode", "The way that the shadow will be rendered.", new CategorySetting.Visibility(shadowsCategory), "Default", new String[]{"None", "Default", "Custom"});
    public NumberSetting shadowOffset = new NumberSetting("ShadowOffset", "Offset", "The distance of the shadow from the text being rendered.", new ModeSetting.Visibility(shadowMode, "Custom"), 0.5f, -2.0f, 2.0f);

    @SubscribeEvent
    public void onSettingChange(SettingChangeEvent event) {
        if (isRendererSetting(event.getSetting())) {
            if (!customFont.getValue()) {
                Solstice.FONT_MANAGER.setFontRenderer(null);
                return;
            }

            updateFontRenderer();
        }
    }

    @Override
    public void onEnable() {
        updateFontRenderer();
    }

    @Override
    public void onDisable() {
        Solstice.FONT_MANAGER.setFontRenderer(null);
    }

    private void updateFontRenderer() {
        Solstice.FONT_MANAGER.setFontRenderer(new FontRenderer(createFontStack(), size.getValue().floatValue() / 2.0f, antiAliasing.getValue(), fractionalMetrics.getValue()));
    }

    private Font[] createFontStack() {
        int styleBits = getStyleBits();
        int fontSize = size.getValue().intValue();
        LinkedHashSet<String> fontNames = new LinkedHashSet<>();

        addResolvedFont(fontNames, name.getValue());

        if (useFallbacks.getValue()) {
            addResolvedFont(fontNames, fallbackName.getValue());
            addResolvedFont(fontNames, symbolName.getValue());
            addResolvedFont(fontNames, emojiName.getValue());
        }

        fontNames.add("Dialog");

        List<Font> fonts = new ArrayList<>();
        for (String fontName : fontNames) {
            fonts.add(new Font(fontName, styleBits, fontSize));
        }

        return fonts.toArray(new Font[0]);
    }

    private void addResolvedFont(LinkedHashSet<String> fontNames, String requestedName) {
        String resolvedName = Solstice.FONT_MANAGER.resolveFontName(requestedName, null);
        if (resolvedName != null) {
            fontNames.add(resolvedName);
        }
    }

    private int getStyleBits() {
        return switch (style.getValue().toLowerCase()) {
            case "bolditalic" -> Font.BOLD | Font.ITALIC;
            case "bold" -> Font.BOLD;
            case "italic" -> Font.ITALIC;
            default -> Font.PLAIN;
        };
    }

    private boolean isRendererSetting(Setting setting) {
        return setting == customFont
                || setting == name
                || setting == size
                || setting == style
                || setting == useFallbacks
                || setting == fallbackName
                || setting == symbolName
                || setting == emojiName
                || setting == antiAliasing
                || setting == fractionalMetrics;
    }
}
