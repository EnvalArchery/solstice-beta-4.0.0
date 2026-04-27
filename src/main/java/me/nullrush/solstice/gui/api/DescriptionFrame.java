package me.nullrush.solstice.gui.api;

import lombok.Getter;
import lombok.Setter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.impl.core.ClickGuiModule;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import me.nullrush.solstice.utils.text.FormattingUtils;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class DescriptionFrame {
    private String description = "";
    private String cachedDescription = "";
    private int x, y, width, height, dragX = 0, dragY = 0, textPadding = 3;
    private int cachedWrapWidth = -1;
    private boolean dragging = false;
    private List<String> wrappedDescription = List.of();

    public DescriptionFrame(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if(dragging) {
            setX(mouseX - dragX);
            setY(mouseY - dragY);
        }

        updateWrappedDescription();

        GuiVisuals.renderClickGuiPanel(context, x, y, x + width, y + height, GuiVisuals.accent(y + 40, 255), !description.isEmpty(), isHovering(mouseX, mouseY), true);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, "Description", x + textPadding, y + 2, Color.WHITE);
        GuiVisuals.renderPulseLine(context, x + 68, y + height / 2.0f + 1, x + width - 6, 2.0f, GuiVisuals.accent(y + 70, 120), GuiVisuals.getTime(), 0.56f);
        if(!description.isEmpty()) {
            GuiVisuals.renderClickGuiBody(context, x + 1, y + height, x + width - 1, y + height + (wrappedDescription.size() * Solstice.FONT_MANAGER.getHeight()) + 6, GuiVisuals.accent(y + 86, 255));
            Renderer2D.renderOutline(context.getMatrices(), x, y + height - 1, x + width, y + height + (wrappedDescription.size() * Solstice.FONT_MANAGER.getHeight()) + 6, GuiVisuals.accent(y + 100, 80));
            int i = 0;
            for(String s : wrappedDescription) {
                Solstice.FONT_MANAGER.drawTextWithShadow(context, s, x + textPadding, y + height + 2 + (Solstice.FONT_MANAGER.getHeight()*i), Color.WHITE);
                i++;
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(isHovering(mouseX, mouseY)) {
            if(button == 0) {
                dragging = true;
                dragX = (int) (mouseX - getX());
                dragY = (int) (mouseY - getY());
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    public boolean isHovering(double mouseX, double mouseY) {
        return x <= mouseX && y <= mouseY && x + width > mouseX && y + height > mouseY;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private void updateWrappedDescription() {
        int wrapWidth = width - textPadding * 2;
        if (description.isEmpty()) {
            if (!wrappedDescription.isEmpty()) {
                wrappedDescription = List.of();
            }
            cachedDescription = "";
            cachedWrapWidth = wrapWidth;
            return;
        }

        if (description.equals(cachedDescription) && cachedWrapWidth == wrapWidth) {
            return;
        }

        wrappedDescription = new ArrayList<>(FormattingUtils.wrapText(description, wrapWidth));
        cachedDescription = description;
        cachedWrapWidth = wrapWidth;
    }
}
