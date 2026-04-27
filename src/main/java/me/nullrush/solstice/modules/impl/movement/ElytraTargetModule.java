package me.nullrush.solstice.modules.impl.movement;

import lombok.Getter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.PlayerUpdateEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.modules.impl.combat.AuraModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.minecraft.EntityUtils;
import me.nullrush.solstice.utils.rotations.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "ElytraTarget", description = "Locks your elytra glide onto a target and follows them.", category = Module.Category.MOVEMENT)
public class ElytraTargetModule extends Module {
    public ModeSetting targetMode = new ModeSetting("TargetMode", "The targeting source used while gliding.", "Aura", new String[]{"Aura", "Closest"});
    public NumberSetting range = new NumberSetting("Range", "The maximum distance at which players can be targeted.", 48.0, 6.0, 128.0);
    public NumberSetting followDistance = new NumberSetting("FollowDistance", "The preferred distance kept from the target while chasing.", 4.0, 1.0, 12.0);
    public NumberSetting horizontal = new NumberSetting("Horizontal", "The horizontal chase speed used while gliding.", 2.4, 0.2, 8.0);
    public NumberSetting vertical = new NumberSetting("Vertical", "The maximum vertical correction used while gliding.", 0.8, 0.1, 4.0);
    public NumberSetting extrapolation = new NumberSetting("Extrapolation", "How many ticks ahead the target position should be predicted.", 2, 0, 10);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Rotates toward the target while gliding.", true);
    public BooleanSetting requireElytra = new BooleanSetting("RequireElytra", "Only works while you are actually wearing an elytra.", true);

    @Getter private PlayerEntity target;
    @Getter private Vec3d aimPosition = Vec3d.ZERO;
    @Getter private float targetPitch;

    @Override
    public void onEnable() {
        target = null;
        aimPosition = Vec3d.ZERO;
        targetPitch = 0.0f;
    }

    @Override
    public void onDisable() {
        target = null;
        aimPosition = Vec3d.ZERO;
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;

        target = findTarget();
        aimPosition = target == null ? Vec3d.ZERO : getAimPosition(target);

        if (target != null) {
            targetPitch = RotationUtils.getRotations(aimPosition)[1];

            if (rotate.getValue() && shouldAssist()) {
                float[] rotations = RotationUtils.getRotations(aimPosition);
                targetPitch = rotations[1];
                Solstice.ROTATION_MANAGER.rotate(rotations[0], rotations[1], this);
            }
        }
    }

    public boolean shouldAssist() {
        if (getNull() || target == null) return false;
        if (!mc.player.isGliding()) return false;
        if (requireElytra.getValue() && mc.player.getInventory().getArmorStack(2).getItem() != Items.ELYTRA) return false;
        return mc.player.squaredDistanceTo(target) <= MathHelper.square(range.getValue().doubleValue());
    }

    public Vec3d getChaseVelocity() {
        if (!shouldAssist()) return Vec3d.ZERO;

        Vec3d aim = getAimPosition(target);
        aimPosition = aim;

        Vec3d delta = aim.subtract(mc.player.getPos());
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double desiredHorizontal = horizontal.getValue().doubleValue();

        if (horizontalDistance < followDistance.getValue().doubleValue()) {
            desiredHorizontal *= Math.max(0.2, horizontalDistance / followDistance.getValue().doubleValue());
        } else {
            desiredHorizontal *= Math.min(1.5, 1.0 + ((horizontalDistance - followDistance.getValue().doubleValue()) / Math.max(1.0, followDistance.getValue().doubleValue())) * 0.15);
        }

        double velocityX = 0.0;
        double velocityZ = 0.0;
        if (horizontalDistance > 0.001) {
            velocityX = (delta.x / horizontalDistance) * desiredHorizontal;
            velocityZ = (delta.z / horizontalDistance) * desiredHorizontal;
        }

        double velocityY = MathHelper.clamp(delta.y * 0.18, -vertical.getValue().doubleValue(), vertical.getValue().doubleValue());
        targetPitch = RotationUtils.getRotations(aim)[1];

        return new Vec3d(velocityX, velocityY, velocityZ);
    }

    private Vec3d getAimPosition(PlayerEntity player) {
        Vec3d prediction = player.getVelocity().multiply(extrapolation.getValue().intValue());
        return player.getPos()
                .add(prediction)
                .add(0.0, MathHelper.clamp(player.getHeight() * 0.5, 0.6, 1.0), 0.0);
    }

    private PlayerEntity findTarget() {
        if (targetMode.getValue().equalsIgnoreCase("Aura")) {
            AuraModule aura = Solstice.MODULE_MANAGER.getModule(AuraModule.class);
            if (!aura.isToggled()) return null;

            Entity auraTarget = aura.getTarget();
            if (auraTarget instanceof PlayerEntity player && isValidTarget(player)) {
                return player;
            }

            return null;
        }

        PlayerEntity closestTarget = null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!isValidTarget(player)) continue;

            if (closestTarget == null || mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(closestTarget)) {
                closestTarget = player;
            }
        }

        return closestTarget;
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player == mc.player) return false;
        if (!player.isAlive() || player.getHealth() <= 0.0f) return false;
        if (Solstice.FRIEND_MANAGER.contains(player.getName().getString())) return false;
        if (EntityUtils.isBot(player)) return false;
        return mc.player.squaredDistanceTo(player) <= MathHelper.square(range.getValue().doubleValue());
    }

    @Override
    public String getMetaData() {
        return target == null ? "None" : target.getName().getString();
    }
}
