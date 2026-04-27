package me.nullrush.solstice.gui.special;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.gui.api.GuiVisuals;
import me.nullrush.solstice.modules.impl.core.MenuModule;
import me.nullrush.solstice.utils.IMinecraft;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import me.nullrush.solstice.utils.system.MathUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainMenuScreen extends Screen implements IMinecraft {
    private static final int VIDEO_FRAME_COUNT = 200;
    private static final int VIDEO_FRAME_TIME_MS = 125;
    private final String splashText;
    private final Identifier[] videoFrames = new Identifier[VIDEO_FRAME_COUNT];

    private static final int BUTTON_WIDTH = 156;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 8;

    public MainMenuScreen() {
        super(Text.literal(Solstice.MOD_ID + "-menu"));
        splashText = getSplashText();

        for (int i = 0; i < VIDEO_FRAME_COUNT; i++) {
            videoFrames[i] = Identifier.of(Solstice.MOD_ID, "textures/menuvideo/frame_" + String.format("%03d", i + 1) + ".png");
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        if (mc == null) return;
        if (Solstice.MENU_SHADER_MANAGER != null) {
            Solstice.MENU_SHADER_MANAGER.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        }
        for (Identifier frame : videoFrames) {
            mc.getTextureManager().getTexture(frame);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderMenuBackground(context, mouseX, mouseY);
        float time = GuiVisuals.getTime();
        drawChrome(context, mouseX, mouseY, time);
        drawButtons(context, mouseX, mouseY);
        drawStatus(context);
    }

    private void renderMenuBackground(DrawContext context, int mouseX, int mouseY) {
        MenuModule menuModule = Solstice.MODULE_MANAGER.getModule(MenuModule.class);
        String backgroundMode = menuModule.backgroundMode.getValue();

        if (backgroundMode.equalsIgnoreCase("Backdrop")) {
            GuiVisuals.renderBackdrop(context, width, height, GuiVisuals.getTime());
            return;
        }

        if (backgroundMode.equalsIgnoreCase("Solstice") || backgroundMode.equalsIgnoreCase("Blend")) {
            renderVideoBackground(context);
        } else {
            Renderer2D.renderQuad(context.getMatrices(), 0, 0, width, height, new Color(4, 4, 8, 255));
        }

        if (backgroundMode.equalsIgnoreCase("Shader") || backgroundMode.equalsIgnoreCase("Blend")) {
            renderImportedShader(context, menuModule, mouseX, mouseY, backgroundMode.equalsIgnoreCase("Blend") ? menuModule.shaderOpacity.getValue().floatValue() / 100.0f : 1.0f);
        }
    }

    private void renderVideoBackground(DrawContext context) {
        Identifier frame = getCurrentVideoFrame();
        if (frame != null) {
            Renderer2D.renderTexture(context.getMatrices(), 0, 0, width, height, frame, Color.WHITE);
            Renderer2D.renderGradient(context.getMatrices(), 0, 0, width, height, new Color(8, 6, 16, 26), new Color(8, 6, 16, 92));
            Renderer2D.renderSidewaysGradient(context.getMatrices(), 0, 0, width, height, new Color(48, 20, 92, 28), new Color(6, 6, 12, 18));
            return;
        }

        GuiVisuals.renderBackdrop(context, width, height, GuiVisuals.getTime());
    }

    private void renderImportedShader(DrawContext context, MenuModule menuModule, int mouseX, int mouseY, float opacity) {
        if (Solstice.MENU_SHADER_MANAGER == null) {
            return;
        }

        Solstice.MENU_SHADER_MANAGER.prepare();
        Solstice.MENU_SHADER_MANAGER.render(menuModule.shader.getValue(), menuModule.shaderSpeed.getValue().floatValue() / 10.0f, opacity, menuModule.shaderShift.getValue().floatValue(), mouseX / (float) Math.max(1, width), mouseY / (float) Math.max(1, height));
        Renderer2D.renderGradient(context.getMatrices(), 0, 0, width, height, new Color(8, 6, 16, 12), new Color(8, 6, 16, opacity >= 1.0f ? 52 : 22));
    }

    private Identifier getCurrentVideoFrame() {
        if (videoFrames.length == 0) return null;
        int index = (int) ((System.currentTimeMillis() / VIDEO_FRAME_TIME_MS) % videoFrames.length);
        return videoFrames[index];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int startX = width / 2 - BUTTON_WIDTH / 2;
        int startY = height / 2 - 6;

        if (isHovering(startX, startY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            mc.setScreen(new SelectWorldScreen(this));
            playClickSound();
        } else if (isHovering(startX, startY + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            mc.setScreen(new MultiplayerScreen(this));
            playClickSound();
        } else if (isHovering(startX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 2, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            mc.setScreen(new OptionsScreen(this, mc.options));
            playClickSound();
        } else if (isHovering(startX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 3, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            mc.scheduleStop();
            playClickSound();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawChrome(DrawContext context, int mouseX, int mouseY, float time) {
        MatrixStack matrices = context.getMatrices();
        String title = "Solstice";
        float titleWidth = Solstice.FONT_MANAGER.getWidth(title);
        float titleX = width / 2.0f - titleWidth;
        float titleY = height / 2.0f - 58;

        drawText(context, title, titleX, titleY, 2.0f, Color.WHITE);
        drawText(context, "combat utility movement visual control", width / 2.0f - Solstice.FONT_MANAGER.getWidth("combat utility movement visual control") / 2.0f, titleY + 28, 1.0f, new Color(180, 198, 214));
        GuiVisuals.renderPulseLine(context, width / 2.0f - 118, titleY + 42, width / 2.0f + 118, 5.0f, GuiVisuals.accent(90, 210), time, 0.17f);

        String date = new SimpleDateFormat("MM/dd/yy  hh:mm aa").format(new Date());
        drawTag(context, date, 14, 14, mouseX, mouseY, 0);
        drawTag(context, Solstice.MOD_NAME + " " + Solstice.MOD_VERSION, width - 206, 14, mouseX, mouseY, 1);

        if (!splashText.isEmpty()) {
            drawText(context, splashText, width / 2.0f - Solstice.FONT_MANAGER.getWidth(splashText) / 2.0f, titleY + 52, 1.0f, GuiVisuals.accent(180, 235));
        }

        GuiVisuals.renderPanel(context, width / 2.0f + 112, height / 2.0f - 58, width / 2.0f + 244, height / 2.0f + 8, GuiVisuals.accent(220, 255), true, false);
        drawText(context, "signal", width / 2.0f + 124, height / 2.0f - 49, 1.0f, new Color(170, 188, 204));
        drawText(context, "stable", width / 2.0f + 124, height / 2.0f - 34, 1.0f, Color.WHITE);
        GuiVisuals.renderPulseLine(context, width / 2.0f + 122, height / 2.0f - 16, width / 2.0f + 232, 5.0f, GuiVisuals.secondary(260, 170), time * 1.15f, 0.61f);
    }

    private void drawButtons(DrawContext context, int mouseX, int mouseY) {
        int startX = width / 2 - BUTTON_WIDTH / 2;
        int startY = height / 2 - 6;

        drawMenuButton(context, "Singleplayer", startX, startY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY, 0);
        drawMenuButton(context, "Multiplayer", startX, startY + BUTTON_HEIGHT + BUTTON_GAP, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY, 1);
        drawMenuButton(context, "Settings", startX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 2, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY, 2);
        drawMenuButton(context, "Quit", startX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 3, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY, 3);
    }

    private void drawStatus(DrawContext context) {
        String primaryText = "";
        String secondaryText = "";
        Color color = new Color(200, 214, 226);

        if (Solstice.UPDATE_STATUS.equalsIgnoreCase("update-available")) {
            secondaryText = "Update ready";
            primaryText = "Restart to apply the latest Solstice build.";
            color = new Color(255, 196, 88);
        } else if (Solstice.UPDATE_STATUS.equalsIgnoreCase("failed-connection")) {
            secondaryText = "Offline";
            primaryText = "Update check could not reach Solstice services.";
            color = new Color(255, 115, 115);
        } else if (Solstice.UPDATE_STATUS.equalsIgnoreCase("failed")) {
            secondaryText = "Update failed";
            primaryText = "The auto-updater ran into a problem.";
            color = new Color(255, 115, 115);
        } else if (Solstice.UPDATE_STATUS.equalsIgnoreCase("up-to-date")) {
            secondaryText = "Latest build";
            primaryText = "Everything is current.";
        }

        if (primaryText.isEmpty()) {
            return;
        }

        float left = width / 2.0f - 132;
        float top = height - 48;
        float right = width / 2.0f + 132;
        float bottom = height - 16;

        GuiVisuals.renderPanel(context, left, top, right, bottom, color, true, false);
        drawText(context, secondaryText, left + 10, top + 6, 1.0f, color);
        drawText(context, primaryText, left + 10, top + 17, 1.0f, Color.WHITE);
    }

    private void drawMenuButton(DrawContext context, String text, int x, int y, int buttonWidth, int buttonHeight, int mouseX, int mouseY, int index) {
        boolean hovered = isHovering(x, y, buttonWidth, buttonHeight, mouseX, mouseY);
        boolean active = hovered || index == 1;
        GuiVisuals.renderPanel(context, x, y, x + buttonWidth, y + buttonHeight, GuiVisuals.accent(index * 70L, 255), active, hovered);
        GuiVisuals.renderPulseLine(context, x + 68, y + buttonHeight / 2.0f + 1, x + buttonWidth - 8, 2.2f, GuiVisuals.accent(index * 70L + 22, hovered ? 185 : 105), GuiVisuals.getTime() * 1.08f, 0.18f + index * 0.09f);
        drawText(context, text, x + 10, y + 5, 1.0f, hovered ? Color.WHITE : new Color(190, 205, 220));
    }

    private void drawTag(DrawContext context, String text, float x, float y, int mouseX, int mouseY, int index) {
        int width = Solstice.FONT_MANAGER.getWidth(text) + 18;
        boolean hovered = isHovering(x, y, width, 15, mouseX, mouseY);
        GuiVisuals.renderPanel(context, x, y, x + width, y + 15, GuiVisuals.secondary(index * 90L, 255), true, hovered);
        drawText(context, text, x + 8, y + 4, 1.0f, hovered ? Color.WHITE : new Color(176, 194, 210));
    }

    private boolean isHovering(double x, double y, double buttonWidth, double buttonHeight, double mouseX, double mouseY) {
        return x <= mouseX && y <= mouseY && x + buttonWidth > mouseX && y + buttonHeight > mouseY;
    }

    private void drawText(DrawContext context, String text, float x, float y, float scale, Color color) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        Solstice.FONT_MANAGER.drawText(context, text, 0, 0, color);
        context.getMatrices().pop();
    }

    private String getSplashText() {
        String splash = "";
        Identifier identifier = Identifier.of(Solstice.MOD_ID, "splash.txt");

        try {
            Resource resource = mc.getResourceManager().getResource(identifier).orElseThrow();
            List<String> messages = resource.getReader().lines().toList();
            splash = messages.get((int) MathUtils.random(messages.size(), 0));
        } catch (Exception ignored) { }

        return splash;
    }

    private void playClickSound() {
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
