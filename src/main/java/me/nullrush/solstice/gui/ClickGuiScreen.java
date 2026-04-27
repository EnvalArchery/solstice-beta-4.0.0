package me.nullrush.solstice.gui;

import lombok.Getter;
import lombok.Setter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.api.DescriptionFrame;
import me.nullrush.solstice.gui.api.GuiVisuals;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.impl.core.ClickGuiModule;
import me.nullrush.solstice.modules.impl.core.HUDModule;
import me.nullrush.solstice.modules.impl.core.HUDEditorModule;
import me.nullrush.solstice.modules.impl.core.ColorModule;
import me.nullrush.solstice.gui.api.Button;
import me.nullrush.solstice.gui.api.Frame;
import me.nullrush.solstice.gui.impl.ModuleButton;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import me.nullrush.solstice.utils.system.Timer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ClickGuiScreen extends Screen {
    private final ArrayList<Frame> frames = new ArrayList<>();
    private final ArrayList<Frame> hudFrames = new ArrayList<>();
    private final ArrayList<Button> buttons = new ArrayList<>();
    private final DescriptionFrame descriptionFrame;
    private final Frame hudEditorFrame;
    private final Frame hudThemeFrame;

    private final Timer lineTimer = new Timer();
    private boolean showLine = false;
    private Color colorClipboard = null;
    private boolean hudEditorMode = false;
    private boolean backgroundShaderMenuOpen = false;
    private int backgroundShaderMenuX = 0;
    private int backgroundShaderMenuY = 0;

    private static final int BACKGROUND_SHADER_MENU_WIDTH = 190;
    private static final int BACKGROUND_SHADER_ROW_HEIGHT = 13;
    private static final int BACKGROUND_SHADER_ROW_COUNT = 5;

    public ClickGuiScreen() {
        super(Text.literal(Solstice.MOD_ID + "-click-gui"));

        int x = 6;
        for (Module.Category category : getClickGuiCategoryOrder()) {
            frames.add(new Frame(category, x, 3, 100, 13));
            x += 104;
        }

        this.descriptionFrame = new DescriptionFrame(x, 3, 200, 13);
        this.hudEditorFrame = new Frame(Module.Category.CORE, 6, 3, 120, 13);
        this.hudEditorFrame.getButtons().clear();

        ModuleButton hudModuleButton = new ModuleButton(Solstice.MODULE_MANAGER.getModule(HUDModule.class), this.hudEditorFrame, 13);
        hudModuleButton.setOpen(true);
        this.hudEditorFrame.getButtons().add(hudModuleButton);

        this.hudThemeFrame = new Frame(Module.Category.CORE, 132, 3, 120, 13);
        this.hudThemeFrame.getButtons().clear();

        ModuleButton colorModuleButton = new ModuleButton(Solstice.MODULE_MANAGER.getModule(ColorModule.class), this.hudThemeFrame, 13);
        colorModuleButton.setOpen(true);
        this.hudThemeFrame.getButtons().add(colorModuleButton);

        ModuleButton clickGuiModuleButton = new ModuleButton(Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class), this.hudThemeFrame, 13);
        this.hudThemeFrame.getButtons().add(clickGuiModuleButton);

        hudFrames.add(hudEditorFrame);
        hudFrames.add(hudThemeFrame);
    }

    @Override
    protected void init() {
        super.init();

        float scale = getGuiScale();
        int scaledWidth = (int) (width / scale);
        int scaledHeight = (int) (height / scale);
        int left = 10;
        int top = 18;
        int spacing = 8;
        int right = 10;
        int descWidth = Math.max(164, Math.min(214, scaledWidth / 5));
        if (hudEditorMode) {
            int contentTop = top + 16;
            int gap = 12;
            int editorWidth = Math.max(190, Math.min(246, scaledWidth / 3));
            int themeWidth = Math.max(152, Math.min(182, scaledWidth / 4));
            boolean stacked = left + editorWidth + gap + themeWidth + gap + descWidth + right > scaledWidth;

            hudEditorFrame.setX(left);
            hudEditorFrame.setY(contentTop);
            hudEditorFrame.setWidth(stacked ? Math.max(200, scaledWidth - left - right - descWidth - 14) : editorWidth);
            hudEditorFrame.setOpen(true);

            if (!hudEditorFrame.getButtons().isEmpty() && hudEditorFrame.getButtons().getFirst() instanceof ModuleButton moduleButton) {
                moduleButton.setOpen(true);
            }

            hudThemeFrame.setOpen(true);
            hudThemeFrame.setWidth(stacked ? Math.max(148, scaledWidth - left - right - descWidth - 14) : themeWidth);
            hudThemeFrame.setX(stacked ? left : hudEditorFrame.getX() + hudEditorFrame.getWidth() + gap);
            hudThemeFrame.setY(stacked ? contentTop + 178 : contentTop);

            if (!hudThemeFrame.getButtons().isEmpty() && hudThemeFrame.getButtons().getFirst() instanceof ModuleButton moduleButton) {
                moduleButton.setOpen(true);
            }

            descriptionFrame.setX(stacked ? scaledWidth - descWidth - right : hudThemeFrame.getX() + hudThemeFrame.getWidth() + gap);
            descriptionFrame.setY(contentTop);
            descriptionFrame.setWidth(Math.min(descWidth, scaledWidth - descriptionFrame.getX() - right));
        } else {
            int frameCount = Math.max(1, frames.size());
            int usableWidth = scaledWidth - left - right - spacing * (frameCount - 1);
            int frameWidth = Math.max(84, Math.min(110, usableWidth / frameCount));
            int x = left;

            for (Frame frame : frames) {
                frame.setX(x);
                frame.setY(top);
                frame.setWidth(frameWidth);
                x += frameWidth + spacing;
            }

            descriptionFrame.setX(Math.max(left, scaledWidth - descWidth - right));
            descriptionFrame.setY(Math.min(scaledHeight - 80, top + 152));
            descriptionFrame.setWidth(descWidth);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (lineTimer.hasTimeElapsed(400L)){
            showLine = !showLine;
            lineTimer.reset();
        }

        float scale = getGuiScale();
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        int scaledWidth = (int) (width / scale);

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);

        float time = GuiVisuals.getTime();
        String title = hudEditorMode ? Solstice.MOD_NAME + " HUD Editor" : Solstice.MOD_NAME + " Control Surface";
        Solstice.FONT_MANAGER.drawTextWithShadow(context, title, 16, 6, Color.WHITE);
        GuiVisuals.renderPulseLine(context, 16, 15, Math.min(scaledWidth - 16, 236), 4.0f, GuiVisuals.accent(50, 180), time, 0.31f);

        descriptionFrame.setDescription("");
        for(Frame frame : getActiveFrames()) frame.render(context, scaledMouseX, scaledMouseY, delta);

        if (hudEditorMode) {
            renderHudEditorPreview(context, scaledWidth, (int) (height / scale));
        }

        descriptionFrame.render(context, scaledMouseX, scaledMouseY, delta);
        renderBackgroundShaderMenu(context, scaledMouseX, scaledMouseY);
        context.getMatrices().pop();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double scale = getGuiScale();
        for(Frame frame : getActiveFrames()) frame.mouseDragged(mouseX / scale, mouseY / scale, button, deltaX / scale, deltaY / scale);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scale = getGuiScale();
        double scaledMouseX = mouseX / scale;
        double scaledMouseY = mouseY / scale;

        if (backgroundShaderMenuOpen && handleBackgroundShaderMenuClick(scaledMouseX, scaledMouseY, button)) {
            return true;
        }

        if (button == 1 && !isHoveringGuiSurface(scaledMouseX, scaledMouseY)) {
            openBackgroundShaderMenu((int) scaledMouseX, (int) scaledMouseY);
            return true;
        }

        if (backgroundShaderMenuOpen && !isHoveringBackgroundShaderMenu(scaledMouseX, scaledMouseY) && button == 0) {
            backgroundShaderMenuOpen = false;
        }

        for (Frame frame : getActiveFrames()) {
            frame.mouseClicked(scaledMouseX, scaledMouseY, button);
        }

        descriptionFrame.mouseClicked(scaledMouseX, scaledMouseY, button);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double scale = getGuiScale();
        for (Frame frame : getActiveFrames()) {
            frame.mouseReleased(mouseX / scale, mouseY / scale, button);
        }

        descriptionFrame.mouseReleased(mouseX / scale, mouseY / scale, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double scale = getGuiScale();
        for (Frame frame : getActiveFrames()) {
            frame.mouseScrolled(mouseX / scale, mouseY / scale, horizontalAmount, verticalAmount);
        }

        return this.hoveredElement(mouseX, mouseY).filter(element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)).isPresent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Frame frame : getActiveFrames()) {
            frame.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (Frame frame : getActiveFrames()) {
            frame.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        ClickGuiModule module = Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class);
        if(module.blur.getValue()) applyBlur();
        GuiVisuals.renderOverlay(context, this.width, this.height, GuiVisuals.getTime());
        Renderer2D.renderQuad(context.getMatrices(), 0, 0, this.width, this.height, new Color(0, 0, 0, 10));
    }

    @Override
    public void close() {
        super.close();
        if (hudEditorMode) {
            Solstice.MODULE_MANAGER.getModule(HUDEditorModule.class).setToggled(false);
        } else {
            Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class).setToggled(false);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public static Color getButtonColor(int index, int alpha) {
        Color color = Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class).isRainbow() ? ColorUtils.getOffsetRainbow(index*10L) : Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class).color.getColor();
        return ColorUtils.getColor(color, alpha);
    }

    private float getGuiScale() {
        return Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class).guiScale.getValue().floatValue();
    }

    private List<Frame> getActiveFrames() {
        return hudEditorMode ? hudFrames : frames;
    }

    private void renderHudEditorPreview(DrawContext context, int scaledWidth, int scaledHeight) {
        HUDModule hudModule = Solstice.MODULE_MANAGER.getModule(HUDModule.class);

        int panelWidth = Math.min(230, Math.max(180, scaledWidth / 4));
        int panelX = Math.max(10, hudEditorFrame.getX());
        int panelY = Math.min(Math.max(hudThemeFrame.getY() + hudThemeFrame.getTotalHeight() + 10, hudEditorFrame.getY() + hudEditorFrame.getTotalHeight() + 10), scaledHeight - 92);
        int panelHeight = 72;

        GuiVisuals.renderClickGuiPanel(context, panelX, panelY, panelX + panelWidth, panelY + panelHeight, GuiVisuals.accent(panelY + 32, 255), true, false, true);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, "Preview", panelX + 3, panelY + 2, Color.WHITE);
        GuiVisuals.renderPulseLine(context, panelX + 42, panelY + 7.0f, panelX + panelWidth - 6, 2.0f, GuiVisuals.accent(panelY + 88, 115), GuiVisuals.getTime(), 0.71f);

        int enabledSections = getEnabledHudSections(hudModule);
        String leftText = Formatting.GRAY + "Scale " + Formatting.WHITE + String.format("%.2fx", hudModule.guiScale.getValue().floatValue());
        String middleText = Formatting.GRAY + "Sections " + Formatting.WHITE + enabledSections;
        String rightText = Formatting.GRAY + "Modules " + Formatting.WHITE + hudModule.moduleListFilter.getValue();

        Solstice.FONT_MANAGER.drawTextWithShadow(context, leftText, panelX + 4, panelY + 18, Color.WHITE);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, middleText, panelX + 4, panelY + 29, Color.WHITE);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, rightText, panelX + 4, panelY + 40, Color.WHITE);

        String footer = (hudModule.colorMode.getValue().equals("Custom") ? "Custom" : hudModule.colorMode.getValue()) + Formatting.GRAY + " / " + Formatting.WHITE +
                (hudModule.coordinates.getValue() ? "Coords" : "NoCoords") + Formatting.GRAY + " / " + Formatting.WHITE +
                (hudModule.potions.getValue() ? "Potions" : "Clean");
        Solstice.FONT_MANAGER.drawTextWithShadow(context, footer, panelX + 4, panelY + 54, Color.WHITE);
    }

    private int getEnabledHudSections(HUDModule hudModule) {
        int enabledSections = 0;
        if (hudModule.watermark.getValue()) enabledSections++;
        if (hudModule.welcomer.getValue()) enabledSections++;
        if (hudModule.moduleList.getValue()) enabledSections++;
        if (hudModule.playerRadar.getValue()) enabledSections++;
        if (hudModule.armor.getValue() || hudModule.totemCounter.getValue() || hudModule.crystalCounter.getValue() || hudModule.xpCounter.getValue()) enabledSections++;
        if (hudModule.health.getValue() || hudModule.ping.getValue() || hudModule.tps.getValue() || hudModule.fps.getValue() || hudModule.durability.getValue() || !hudModule.speed.getValue().equalsIgnoreCase("None") || hudModule.uptime.getValue() || hudModule.serverBrand.getValue()) enabledSections++;
        if (hudModule.potions.getValue()) enabledSections++;
        if (hudModule.coordinates.getValue() || hudModule.direction.getValue()) enabledSections++;
        return enabledSections;
    }

    private void renderBackgroundShaderMenu(DrawContext context, int mouseX, int mouseY) {
        if (!backgroundShaderMenuOpen) {
            return;
        }

        ClickGuiModule module = Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class);
        int menuHeight = getBackgroundShaderMenuHeight();
        int titleBottom = backgroundShaderMenuY + 13;

        GuiVisuals.renderClickGuiPanel(context, backgroundShaderMenuX, backgroundShaderMenuY, backgroundShaderMenuX + BACKGROUND_SHADER_MENU_WIDTH, titleBottom, GuiVisuals.accent(backgroundShaderMenuY + 14, 255), true, isHoveringBackgroundShaderMenu(mouseX, mouseY), true);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, "Background Shaders", backgroundShaderMenuX + 4, backgroundShaderMenuY + 2, Color.WHITE);
        GuiVisuals.renderPulseLine(context, backgroundShaderMenuX + 90, backgroundShaderMenuY + 7.0f, backgroundShaderMenuX + BACKGROUND_SHADER_MENU_WIDTH - 6, 1.8f, GuiVisuals.accent(backgroundShaderMenuY + 42, 120), GuiVisuals.getTime(), 0.42f);

        GuiVisuals.renderClickGuiBody(context, backgroundShaderMenuX + 1, titleBottom, backgroundShaderMenuX + BACKGROUND_SHADER_MENU_WIDTH - 1, backgroundShaderMenuY + menuHeight, GuiVisuals.accent(backgroundShaderMenuY + 38, 255));

        renderBackgroundShaderMenuRow(context, mouseX, mouseY, 0, "Enabled", module.backgroundShaders.getValue() ? "On" : "Off", module.backgroundShaders.getValue());
        renderBackgroundShaderMenuRow(context, mouseX, mouseY, 1, "Type", module.backgroundShaderType.getValue(), true);
        renderBackgroundShaderMenuRow(context, mouseX, mouseY, 2, "Opacity", String.valueOf(module.backgroundShaderOpacity.getValue().intValue()), true);
        renderBackgroundShaderMenuRow(context, mouseX, mouseY, 3, "Speed", String.valueOf(module.backgroundShaderSpeed.getValue().intValue()), true);
        renderBackgroundShaderMenuRow(context, mouseX, mouseY, 4, "Intensity", String.valueOf(module.backgroundShaderIntensity.getValue().intValue()), true);
    }

    private void renderBackgroundShaderMenuRow(DrawContext context, int mouseX, int mouseY, int row, String label, String value, boolean active) {
        int rowTop = backgroundShaderMenuY + 14 + row * BACKGROUND_SHADER_ROW_HEIGHT;
        int rowBottom = rowTop + BACKGROUND_SHADER_ROW_HEIGHT - 1;
        boolean hovered = mouseX >= backgroundShaderMenuX + 2 && mouseX <= backgroundShaderMenuX + BACKGROUND_SHADER_MENU_WIDTH - 2 && mouseY >= rowTop && mouseY <= rowBottom;

        GuiVisuals.renderClickGuiPanel(context, backgroundShaderMenuX + 2, rowTop, backgroundShaderMenuX + BACKGROUND_SHADER_MENU_WIDTH - 2, rowBottom, GuiVisuals.accent(rowTop + 28, 255), active, hovered, false);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, label, backgroundShaderMenuX + 6, rowTop + 2, Color.WHITE);
        Solstice.FONT_MANAGER.drawTextWithShadow(context, Formatting.GRAY + value, backgroundShaderMenuX + BACKGROUND_SHADER_MENU_WIDTH - 8 - Solstice.FONT_MANAGER.getWidth(value), rowTop + 2, Color.WHITE);
    }

    private boolean handleBackgroundShaderMenuClick(double mouseX, double mouseY, int button) {
        if (!isHoveringBackgroundShaderMenu(mouseX, mouseY)) {
            if (button == 0 || button == 1) {
                backgroundShaderMenuOpen = false;
            }
            return false;
        }

        if (mouseY < backgroundShaderMenuY + 13) {
            return true;
        }

        int row = (int) ((mouseY - (backgroundShaderMenuY + 14)) / BACKGROUND_SHADER_ROW_HEIGHT);
        if (row < 0 || row >= BACKGROUND_SHADER_ROW_COUNT) {
            return true;
        }

        ClickGuiModule module = Solstice.MODULE_MANAGER.getModule(ClickGuiModule.class);
        boolean backwards = button == 1;

        switch (row) {
            case 0 -> module.backgroundShaders.setValue(!module.backgroundShaders.getValue());
            case 1 -> cycleMode(module.backgroundShaderType, backwards);
            case 2 -> adjustNumber(module.backgroundShaderOpacity, backwards ? -10 : 10);
            case 3 -> adjustNumber(module.backgroundShaderSpeed, backwards ? -1 : 1);
            case 4 -> adjustNumber(module.backgroundShaderIntensity, backwards ? -6 : 6);
            default -> {
            }
        }

        return true;
    }

    private void openBackgroundShaderMenu(int mouseX, int mouseY) {
        int scaledWidth = (int) (width / getGuiScale());
        int scaledHeight = (int) (height / getGuiScale());
        int menuHeight = getBackgroundShaderMenuHeight();

        backgroundShaderMenuX = Math.max(6, Math.min(mouseX, scaledWidth - BACKGROUND_SHADER_MENU_WIDTH - 6));
        backgroundShaderMenuY = Math.max(18, Math.min(mouseY, scaledHeight - menuHeight - 6));
        backgroundShaderMenuOpen = true;
    }

    private boolean isHoveringBackgroundShaderMenu(double mouseX, double mouseY) {
        if (!backgroundShaderMenuOpen) {
            return false;
        }

        return mouseX >= backgroundShaderMenuX && mouseX <= backgroundShaderMenuX + BACKGROUND_SHADER_MENU_WIDTH
                && mouseY >= backgroundShaderMenuY && mouseY <= backgroundShaderMenuY + getBackgroundShaderMenuHeight();
    }

    private int getBackgroundShaderMenuHeight() {
        return 14 + BACKGROUND_SHADER_ROW_COUNT * BACKGROUND_SHADER_ROW_HEIGHT + 3;
    }

    private boolean isHoveringGuiSurface(double mouseX, double mouseY) {
        if (isHoveringBackgroundShaderMenu(mouseX, mouseY) || descriptionFrame.isHovering(mouseX, mouseY) || isHoveringHudEditorPreview(mouseX, mouseY)) {
            return true;
        }

        for (Frame frame : getActiveFrames()) {
            if (mouseX >= frame.getX() && mouseX <= frame.getX() + frame.getWidth() && mouseY >= frame.getY() && mouseY <= frame.getY() + frame.getTotalHeight()) {
                return true;
            }
        }

        return false;
    }

    private boolean isHoveringHudEditorPreview(double mouseX, double mouseY) {
        if (!hudEditorMode) {
            return false;
        }

        int scaledWidth = (int) (width / getGuiScale());
        int scaledHeight = (int) (height / getGuiScale());
        int panelWidth = Math.min(230, Math.max(180, scaledWidth / 4));
        int panelX = Math.max(10, hudEditorFrame.getX());
        int panelY = Math.min(Math.max(hudThemeFrame.getY() + hudThemeFrame.getTotalHeight() + 10, hudEditorFrame.getY() + hudEditorFrame.getTotalHeight() + 10), scaledHeight - 92);
        int panelHeight = 72;

        return mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight;
    }

    private void cycleMode(ModeSetting setting, boolean backwards) {
        int index = setting.getModes().indexOf(setting.getValue());
        if (index < 0) {
            index = 0;
        }

        int nextIndex = backwards ? index - 1 : index + 1;
        if (nextIndex < 0) {
            nextIndex = setting.getModes().size() - 1;
        } else if (nextIndex >= setting.getModes().size()) {
            nextIndex = 0;
        }

        setting.setValue(setting.getModes().get(nextIndex));
    }

    private void adjustNumber(NumberSetting setting, double delta) {
        switch (setting.getType()) {
            case LONG -> setting.setValue(setting.getValue().longValue() + (long) delta);
            case FLOAT -> setting.setValue(setting.getValue().floatValue() + (float) delta);
            case DOUBLE -> setting.setValue(setting.getValue().doubleValue() + delta);
            default -> setting.setValue(setting.getValue().intValue() + (int) delta);
        }
    }

    public void setHudEditorMode(boolean hudEditorMode) {
        this.hudEditorMode = hudEditorMode;
        if (client != null) {
            init(client, width, height);
        }
    }

    private List<Module.Category> getClickGuiCategoryOrder() {
        return List.of(
                Module.Category.COMBAT,
                Module.Category.MOVEMENT,
                Module.Category.VISUALS,
                Module.Category.MISCELLANEOUS,
                Module.Category.PLAYER,
                Module.Category.CORE
        );
    }
}
