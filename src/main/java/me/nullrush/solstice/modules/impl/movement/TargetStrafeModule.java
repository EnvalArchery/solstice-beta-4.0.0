package me.nullrush.solstice.modules.impl.movement;

import lombok.Getter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.PlayerMoveEvent;
import me.nullrush.solstice.events.impl.PlayerUpdateEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.modules.impl.combat.AuraModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.minecraft.EntityUtils;
import me.nullrush.solstice.utils.minecraft.MovementUtils;
import me.nullrush.solstice.utils.rotations.RotationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@RegisterModule(name = "TargetStrafe", description = "Circles around your target while keeping sword pressure up.", category = Module.Category.MOVEMENT)
public class TargetStrafeModule extends Module {
    public ModeSetting targetMode = new ModeSetting("TargetMode", "The targeting source used for strafing.", "Aura", new String[]{"Aura", "Closest"});
    public ModeSetting directionMode = new ModeSetting("Direction", "The direction used while circling the target.", "Auto", new String[]{"Auto", "Left", "Right"});
    public NumberSetting range = new NumberSetting("Range", "The maximum distance at which a target can be strafed.", 6.0, 1.0, 12.0);
    public NumberSetting radius = new NumberSetting("Radius", "The preferred distance that will be kept around the target.", 1.9, 0.5, 4.0);
    public NumberSetting baseSpeed = new NumberSetting("BaseSpeed", "The minimum horizontal speed used while strafing.", 0.29, 0.1, 1.0);
    public BooleanSetting requireInput = new BooleanSetting("RequireInput", "Only strafes while you are actively moving.", true);
    public BooleanSetting autoJump = new BooleanSetting("AutoJump", "Automatically jumps while target strafing on ground.", true);
    public BooleanSetting adaptiveDirection = new BooleanSetting("AdaptiveDirection", "Reverses the strafe direction when you collide with walls.", true);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Keeps your rotations locked toward the target while strafing.", false);

    @Getter private PlayerEntity target;
    private int direction = 1;

    @Override
    public void onEnable() {
        target = null;
        direction = 1;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (getNull()) return;

        target = findTarget();
        updateDirection();

        if (rotate.getValue() && target != null && canStrafe()) {
            Solstice.ROTATION_MANAGER.rotate(RotationUtils.getRotations(target), this);
        }
    }

    @SubscribeEvent(priority = Integer.MIN_VALUE)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!canStrafe()) return;

        if (adaptiveDirection.getValue() && mc.player.horizontalCollision && directionMode.getValue().equalsIgnoreCase("Auto")) {
            direction *= -1;
        }

        double horizontalSpeed = Math.hypot(event.getX(), event.getZ());
        horizontalSpeed = Math.max(horizontalSpeed, MovementUtils.getPotionSpeed(baseSpeed.getValue().doubleValue()));

        if (autoJump.getValue() && mc.player.isOnGround()) {
            event.setY(MovementUtils.getPotionJump(0.3999999463558197));
        }

        Vec3d movement = getStrafeVelocity(horizontalSpeed);
        event.setMovement(new Vec3d(movement.x, event.getY(), movement.z));
        event.setCancelled(true);
    }

    private boolean canStrafe() {
        if (getNull() || target == null) return false;
        if (!target.isAlive() || target.getHealth() <= 0.0f) return false;
        if (mc.player.isSneaking() || mc.player.isClimbing() || mc.player.isGliding() || mc.player.isInFluid()) return false;
        if (mc.player.getAbilities().flying || mc.player.fallDistance >= 5.0f) return false;
        if (requireInput.getValue() && !MovementUtils.isMoving()) return false;
        if (mc.player.squaredDistanceTo(target) > MathHelper.square(range.getValue().doubleValue())) return false;
        return !(Solstice.MODULE_MANAGER.getModule(HoleSnapModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(HoleSnapModule.class).hole != null);
    }

    private void updateDirection() {
        if (directionMode.getValue().equalsIgnoreCase("Left")) {
            direction = -1;
        } else if (directionMode.getValue().equalsIgnoreCase("Right")) {
            direction = 1;
        }
    }

    private Vec3d getStrafeVelocity(double speed) {
        Vec3d offset = mc.player.getPos().subtract(target.getPos());
        double distance = Math.max(0.001, Math.sqrt(offset.x * offset.x + offset.z * offset.z));

        double normalX = offset.x / distance;
        double normalZ = offset.z / distance;

        double tangentX = -normalZ * direction;
        double tangentZ = normalX * direction;

        double pull = MathHelper.clamp((distance - radius.getValue().doubleValue()) / Math.max(0.1, radius.getValue().doubleValue()), -1.0, 1.0) * 0.85;
        double motionX = tangentX + normalX * pull;
        double motionZ = tangentZ + normalZ * pull;

        double length = Math.max(0.001, Math.sqrt(motionX * motionX + motionZ * motionZ));
        return new Vec3d((motionX / length) * speed, 0.0, (motionZ / length) * speed);
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
        if (target == null) return direction < 0 ? "Left" : "Right";
        return target.getName().getString();
    }
}
