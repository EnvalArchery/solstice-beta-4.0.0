package me.nullrush.solstice.gui.impl;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.api.Button;
import me.nullrush.solstice.gui.api.Frame;
import me.nullrush.solstice.settings.impl.CategorySetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class CategoryButton extends Button {
    private final CategorySetting setting;

    public CategoryButton(CategorySetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderRow(context, mouseX, mouseY, setting.isOpen(), getY());
        Solstice.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 1, getY() + 2, Color.WHITE);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, setting.isOpen() ? "-" : "+", getX() + getWidth() - getTextPadding() - 1 - Solstice.FONT_MANAGER.getWidth(setting.isOpen() ? "-" : "+"), getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY) && button == 1) {
            setting.setOpen(!setting.isOpen());
            playClickSound();
        }
    }
}
