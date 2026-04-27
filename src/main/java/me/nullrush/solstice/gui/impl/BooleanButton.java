package me.nullrush.solstice.gui.impl;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.ClickGuiScreen;
import me.nullrush.solstice.gui.api.Button;
import me.nullrush.solstice.gui.api.Frame;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;

import java.awt.*;

public class BooleanButton extends Button {
    private final BooleanSetting setting;

    public BooleanButton(BooleanSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderRow(context, mouseX, mouseY, setting.getValue(), getY());
        Solstice.FONT_MANAGER.drawTextWithShadow(context, (setting.getValue() ? "" : Formatting.GRAY) + setting.getTag(), getX() + getTextPadding() + 1, getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && button == 0) {
            setting.setValue(!setting.getValue());
            playClickSound();
        }
    }
}
