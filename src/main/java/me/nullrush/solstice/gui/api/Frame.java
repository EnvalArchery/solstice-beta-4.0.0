package me.nullrush.solstice.gui.api;

import lombok.Getter;
import lombok.Setter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.ClickGuiScreen;
import me.nullrush.solstice.gui.impl.WhitelistButton;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.gui.impl.ModuleButton;
import me.nullrush.solstice.modules.impl.core.ClickGuiModule;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;

@Getter @Setter
public class Frame {
    private final Module.Category category;
    private int x, y, width, height, totalHeight, dragX = 0, dragY = 0, textPadding = 3;
    public boolean open = true, dragging = false;
    private final ArrayList<Button> buttons = new ArrayList<>();

    public Frame(Module.Category category, int x, int y, int width, int height) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        for(Module module : Solstice.MODULE_MANAGER.getModules(category)) buttons.add(new ModuleButton(module, this, height));
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(dragging) {
            setX(mouseX - dragX);
            setY(mouseY - dragY);
        }

        this.totalHeight = height;

        if(open) {
            totalHeight += 1;
            for(Button button : buttons) {
                button.setX(x);
                button.setY(y + totalHeight);
                totalHeight += button.getHeight();

                if(button instanceof ModuleButton moduleButton && moduleButton.isOpen()) {
                    for(Button b : moduleButton.getButtons()) {
                        b.getSetting().getVisibility().update();
                        b.setVisible(b.getSetting().getVisibility().isVisible());
                        if(!b.isVisible()) continue;

                        b.setX(x);
                        b.setY(y + totalHeight);
                        totalHeight += b.getHeight();
                    }
                }
            }
        }

        GuiVisuals.renderClickGuiPanel(context, x, y, x + width, y + height, GuiVisuals.accent(y, 255), open, isHovering(mouseX, mouseY), true);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, category.getName(), x + textPadding, y + 2, Color.WHITE);
        GuiVisuals.renderPulseLine(context, x + 44, y + height / 2.0f + 1, x + width - 6, 2.4f, GuiVisuals.accent(y + 35, 145), GuiVisuals.getTime(), y * 0.01f);

        if(open) {
            GuiVisuals.renderClickGuiBody(context, x + 1, y + height, x + width - 1, y + totalHeight + 1, GuiVisuals.accent(y + 32, 255));
            Renderer2D.renderOutline(context.getMatrices(), x, y + height - 1, x + width, y + totalHeight + 1, GuiVisuals.accent(y + 80, 85));
            for(Button button : buttons) {
                button.render(context, mouseX, mouseY, delta);
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                dragging = true;
                dragX = (int) (mouseX - getX());
                dragY = (int) (mouseY - getY());
            } else if(button == 1) {
                open = !open;
            }
        }

        if(open) {
            for(Button b : buttons) b.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }

        for(Button b : buttons) b.mouseReleased(mouseX, mouseY, button);
    }

    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (x <= mouseX && x + width > mouseX) {
            boolean whitelistHandling = false;
            for (Button b : buttons) {
                if (b instanceof ModuleButton moduleButton && moduleButton.isOpen()) {
                    for (Button childButton : moduleButton.getButtons()) {
                        if (childButton instanceof WhitelistButton whitelistButton && whitelistButton.isHandlingScroll(mouseX, mouseY)) {
                            whitelistButton.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                            whitelistHandling = true;
                            break;
                        }
                    }
                }
                if (whitelistHandling) break;
            }
            if (!whitelistHandling) {
                int scrollSpeed = Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class).scrollSpeed.getValue().intValue();
                if (verticalAmount < 0) {
                    setY(getY() - scrollSpeed);
                } else if (verticalAmount > 0) {
                    setY(getY() + scrollSpeed);
                }
            }
        }
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (Button b : buttons) {

            b.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (open) {
            for (Button button : buttons) button.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (open) {
            for (Button button : buttons) button.charTyped(chr, modifiers);
        }
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return x <= mouseX && y <= mouseY && x + width > mouseX && y + height > mouseY;
    }
}
