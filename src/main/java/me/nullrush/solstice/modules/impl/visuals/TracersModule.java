package me.nullrush.solstice.modules.impl.visuals;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.RenderWorldEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ColorSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.graphics.Renderer3D;
import me.nullrush.solstice.utils.minecraft.EntityUtils;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

@RegisterModule(name = "Tracers", description = "Renders a line showing where other players are located.", category = Module.Category.VISUALS)
public class TracersModule extends Module {
    public BooleanSetting antiBot = new BooleanSetting("AntiBot", "Prevents bots from having arrow tracers rendered for them.", false);
    public ModeSetting mode = new ModeSetting("Mode", "The mode for the tracers color.", "Distance", new String[]{"Distance", "Custom"});
    public ColorSetting color = new ColorSetting("Color", "The color used for the fill rendering.", ColorUtils.getDefaultOutlineColor());

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if(getNull()) return;

        if (mc.player == null) {
            return;
        }
        boolean prevBobView = mc.options.getBobView().getValue();
        mc.options.getBobView().setValue(false);

        Vec3d pos = EntityUtils.getRenderPos(mc.player, event.getTickDelta());
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = new Vec3d(0.0, 0.0, 1.0).rotateX(-(float) Math.toRadians(camera.getPitch())).rotateY(-(float) Math.toRadians(camera.getYaw())).add(pos.x, mc.player.getEyeHeight(mc.player.getPose()) + pos.y, pos.z);

        for(PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (EntityUtils.isBot(player) && antiBot.getValue()) continue;

            Vec3d playerPos = EntityUtils.getRenderPos(player, event.getTickDelta());
            Renderer3D.renderLine(event.getMatrices(), cameraPos, playerPos, getColor(player));
        }

        mc.options.getBobView().setValue(prevBobView);
    }

    private Color getColor(PlayerEntity player) {
        if(Solstice.FRIEND_MANAGER.contains(player.getName().getString())) return Solstice.FRIEND_MANAGER.getDefaultFriendColor(color.getColor().getAlpha());

        if(mode.getValue().equals("Custom")) return color.getColor();

        float maxDistance = 80;
        float distance = MathHelper.clamp(mc.player.distanceTo(player), 0, maxDistance);
        return new Color(((maxDistance - distance) / maxDistance), 1.0f - (maxDistance - distance) / (float) maxDistance, 0, color.getColor().getAlpha()/255f);
    }
}
