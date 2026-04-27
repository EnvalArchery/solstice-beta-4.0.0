package me.nullrush.solstice.managers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.mixins.accessors.GameRendererAccessor;
import me.nullrush.solstice.mixins.accessors.PostEffectProcessorAccessor;
import me.nullrush.solstice.utils.IMinecraft;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.util.Identifier;

public class MenuShaderManager implements IMinecraft {
    private final Framebuffer framebuffer;
    private PostEffectProcessor processor;
    private String currentShader = "";

    public MenuShaderManager() {
        framebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
        framebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void prepare() {
        framebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        framebuffer.clear();
        mc.getFramebuffer().beginWrite(false);
    }

    public void render(String shaderName, float speed, float opacity, float shift, float mouseX, float mouseY) {
        PostEffectProcessor activeProcessor = getProcessor(shaderName);
        if (activeProcessor == null) {
            return;
        }

        ShaderProgram program = ((PostEffectProcessorAccessor) activeProcessor).getPasses().getFirst().getProgram();
        float width = mc.getWindow().getFramebufferWidth();
        float height = mc.getWindow().getFramebufferHeight();
        float time = ((System.currentTimeMillis() % 600000L) / 1000.0f) * speed;

        program.addSamplerTexture("DiffuseSampler", framebuffer.getColorAttachment());
        setUniform(program, "resolution", width, height);
        setUniform(program, "iResolution", width, height, 0.0f);
        setUniform(program, "mouse", mouseX * width, (1.0f - mouseY) * height);
        setUniform(program, "time", time);
        setUniform(program, "iTime", time);
        setUniform(program, "alpha", 1.0f);
        setUniform(program, "speed", speed, speed);
        setUniform(program, "shift", shift);
        setUniform(program, "camera_yaw", mouseX);
        setUniform(program, "camera_pitch", mouseY);

        activeProcessor.render(framebuffer, ((GameRendererAccessor) mc.gameRenderer).getPool());
        mc.getFramebuffer().beginWrite(false);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        framebuffer.drawInternal((int) width, (int) height);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    public void resize(int width, int height) {
        framebuffer.resize(width, height);
    }

    private PostEffectProcessor getProcessor(String shaderName) {
        String shaderId = shaderName.toLowerCase();
        if (processor != null && currentShader.equals(shaderId)) {
            return processor;
        }

        try {
            processor = mc.getShaderLoader().loadPostEffect(Identifier.of(Solstice.MOD_ID, "menu/" + shaderId), DefaultFramebufferSet.MAIN_ONLY);
            currentShader = shaderId;
        } catch (Exception exception) {
            Solstice.LOGGER.warn("Failed to load main menu shader {}", shaderId, exception);
            processor = null;
            currentShader = "";
        }

        return processor;
    }

    private void setUniform(ShaderProgram program, String name, float... values) {
        if (program.getUniform(name) == null) {
            return;
        }

        switch (values.length) {
            case 1 -> program.getUniform(name).set(values[0]);
            case 2 -> program.getUniform(name).set(values[0], values[1]);
            case 3 -> program.getUniform(name).set(values[0], values[1], values[2]);
            case 4 -> program.getUniform(name).set(values[0], values[1], values[2], values[3]);
            default -> {
            }
        }
    }
}
