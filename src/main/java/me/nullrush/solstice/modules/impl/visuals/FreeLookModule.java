package me.nullrush.solstice.modules.impl.visuals;

import lombok.Getter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.TickEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.system.MathUtils;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

@Getter
@RegisterModule(name = "FreeLook", description = "Lets your camera look around independently while your player keeps facing forward.", category = Module.Category.VISUALS)
public class FreeLookModule extends Module {
    public ModeSetting perspective = new ModeSetting("Perspective", "Perspective", "Controls which third person perspective the camera uses while FreeLook is active.", "Back", new String[]{"Back", "Front"});
    public BooleanSetting cameraTrace = new BooleanSetting("CameraTrace", "Camera Trace", "Uses the detached camera direction for crosshair targeting while FreeLook is active.", true);
    public BooleanSetting restorePerspective = new BooleanSetting("RestorePerspective", "Restore Perspective", "Restores your previous perspective when FreeLook gets disabled.", true);
    public NumberSetting sensitivity = new NumberSetting("Sensitivity", "Sensitivity", "Scales how quickly the detached camera turns.", 1.0f, 0.2f, 2.5f);

    private float freeYaw;
    private float freePitch;
    private float prevFreeYaw;
    private float prevFreePitch;

    private Perspective previousPerspective = Perspective.FIRST_PERSON;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            setToggled(false);
            return;
        }

        if (Solstice.MODULE_MANAGER.getModule(FreecamModule.class).isToggled()) {
            Solstice.MODULE_MANAGER.getModule(FreecamModule.class).setToggled(false);
        }

        previousPerspective = mc.options.getPerspective();
        freeYaw = prevFreeYaw = mc.player.getYaw();
        freePitch = prevFreePitch = mc.player.getPitch();

        mc.options.setPerspective(getPerspective());
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        if (restorePerspective.getValue()) {
            mc.options.setPerspective(previousPerspective);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        prevFreeYaw = freeYaw;
        prevFreePitch = freePitch;
        mc.options.setPerspective(getPerspective());
    }

    public void applyLookDelta(double deltaX, double deltaY) {
        prevFreeYaw = freeYaw;
        prevFreePitch = freePitch;

        float scaledDeltaX = (float) (deltaX * 0.15f * sensitivity.getValue().floatValue());
        float scaledDeltaY = (float) (deltaY * 0.15f * sensitivity.getValue().floatValue());

        freeYaw = MathHelper.wrapDegrees(freeYaw + scaledDeltaX);
        freePitch = MathHelper.clamp(freePitch + scaledDeltaY, -90.0f, 90.0f);
    }

    public Perspective getPerspective() {
        return perspective.getValue().equalsIgnoreCase("Front") ? Perspective.THIRD_PERSON_FRONT : Perspective.THIRD_PERSON_BACK;
    }

    @Override
    public String getMetaData() {
        return perspective.getValue();
    }

    public float getFreeYaw() {
        return (float) MathUtils.interpolate(prevFreeYaw, freeYaw, mc.getRenderTickCounter().getTickDelta(true));
    }

    public float getFreePitch() {
        return (float) MathUtils.interpolate(prevFreePitch, freePitch, mc.getRenderTickCounter().getTickDelta(true));
    }
}
