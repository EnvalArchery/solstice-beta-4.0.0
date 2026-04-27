package me.nullrush.solstice.gui.impl;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.ClickGuiScreen;
import me.nullrush.solstice.gui.api.Button;
import me.nullrush.solstice.gui.api.Frame;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ModeButton extends Button {
    private final ModeSetting setting;
    private boolean open = false;

    public ModeButton(ModeSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderRow(context, mouseX, mouseY, open, getY());

        Solstice.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 1, getY() + 2, Color.WHITE);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + setting.getValue(), getX() + getWidth() - getTextPadding() - 1 - Solstice.FONT_MANAGER.getWidth(setting.getValue()), getY() + 2, Color.WHITE);

        if(open) {
            int i = 0;
            for(String s : setting.getModes()) {
                renderRow(context, mouseX, mouseY, setting.getValue().equals(s), getY() + getParent().getHeight() + i);
                Solstice.FONT_MANAGER.drawTextWithShadow(context, (setting.getValue().equals(s) ? "" : Formatting.GRAY) + s, getX() + getTextPadding() + 2, getY() + getParent().getHeight() + i + 2, Color.WHITE);
                i += getParent().getHeight();
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                int choice = setting.getModes().indexOf(setting.getValue());
                choice ++;
                if(choice > setting.getModes().size() - 1) choice = 0;

                setting.setValue(setting.getModes().get(choice));
                playClickSound();
            } else if(button == 1) {
                open = !open;
            }
        }

        if(open && isHoveringModes(mouseX, mouseY)) {
            int choice = MathHelper.clamp((int)(mouseY - getY() - getParent().getHeight())/getParent().getHeight(), 0, setting.getModes().size() - 1);
            setting.setValue(setting.getModes().get(choice));
        }
    }

    @Override
    public int getHeight() {
        return getParent().getHeight() + (open ? getParent().getHeight() * setting.getModes().size() : 0);
    }

    @Override
    public boolean isHovering(double mouseX, double mouseY) {
        return getX() + getPadding() <= mouseX && getY() <= mouseY && getX() + getWidth() - getPadding() > mouseX && getY() + getParent().getHeight() > mouseY;
    }

    public boolean isHoveringModes(double mouseX, double mouseY) {
        int modesHeight = getParent().getHeight() * setting.getModes().size();
        return getX() + getPadding() <= mouseX && getY() + getParent().getHeight() <= mouseY && getX() + getWidth() - getPadding() > mouseX && getY() + getParent().getHeight() + modesHeight > mouseY;
    }
}
