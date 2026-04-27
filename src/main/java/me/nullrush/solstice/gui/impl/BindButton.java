package me.nullrush.solstice.gui.impl;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.api.Button;
import me.nullrush.solstice.gui.api.Frame;
import me.nullrush.solstice.settings.impl.BindSetting;
import me.nullrush.solstice.utils.input.KeyboardUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class BindButton extends Button {
    private final BindSetting setting;
    private boolean listening = false;

    public BindButton(BindSetting setting, Frame parent, int height) {
        super(setting, parent, height, setting.getDescription());
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderRow(context, mouseX, mouseY, listening, getY());
        Solstice.FONT_MANAGER.drawTextWithShadow(context, setting.getTag(), getX() + getTextPadding() + 1, getY() + 2, Color.WHITE);

        String bind = listening ? "..." : KeyboardUtils.getKeyName(setting.getValue());
        Solstice.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + bind, getX() + getWidth() - getTextPadding() - 1 - Solstice.FONT_MANAGER.getWidth(bind), getY() + 2, Color.WHITE);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                listening = true;
                playClickSound();
            } else {
                setting.setValue(0);
            }

            if(listening) {
                if (button == 1 || button == 2 || button == 3 || button == 4) {
                    setting.setValue(-button - 1);
                    listening = false;
                }
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                setting.setValue(0);
            } else {
                setting.setValue(keyCode);
            }
            listening = false;
        }
    }
}
