package me.nullrush.solstice.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.impl.PlayerTravelEvent;
import me.nullrush.solstice.modules.impl.movement.KeepSprintModule;
import me.nullrush.solstice.modules.impl.movement.SafeWalkModule;
import me.nullrush.solstice.modules.impl.movement.SpeedModule;
import me.nullrush.solstice.modules.impl.movement.VelocityModule;
import me.nullrush.solstice.modules.impl.player.ReachModule;
import me.nullrush.solstice.utils.IMinecraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IMinecraft {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "isPushedByFluids", at = @At("RETURN"))
    private boolean isPushedByFluids(boolean original) {
        if ((Object) this == mc.player && Solstice.MODULE_MANAGER.getModule(VelocityModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(VelocityModule.class).antiLiquidPush.getValue()) return false;
        return original;
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", shift = At.Shift.AFTER))
    private void attack(CallbackInfo callbackInfo) {
        if (Solstice.MODULE_MANAGER.getModule(KeepSprintModule.class).isToggled()) {
            float multiplier = 0.6f + 0.4f * Solstice.MODULE_MANAGER.getModule(KeepSprintModule.class).motion.getValue().floatValue();
            mc.player.setVelocity(mc.player.getVelocity().x / 0.6 * multiplier, mc.player.getVelocity().y, mc.player.getVelocity().z / 0.6 * multiplier);
            mc.player.setSprinting(true);
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void travel(Vec3d movementInput, CallbackInfo info) {
        PlayerTravelEvent event = new PlayerTravelEvent(movementInput);
        Solstice.EVENT_HANDLER.post(event);

        if (event.isCancelled()) {
            move(MovementType.SELF, getVelocity());
            info.cancel();
        }
    }

    @Inject(method = "getBlockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getBlockInteractionRange(CallbackInfoReturnable<Double> info) {
        if (Solstice.MODULE_MANAGER.getModule(ReachModule.class).isToggled()) {
            info.setReturnValue(Solstice.MODULE_MANAGER.getModule(ReachModule.class).amount.getValue().doubleValue());
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getEntityInteractionRange(CallbackInfoReturnable<Double> info) {
        if (Solstice.MODULE_MANAGER.getModule(ReachModule.class).isToggled()) {
            info.setReturnValue(Solstice.MODULE_MANAGER.getModule(ReachModule.class).amount.getValue().doubleValue());
        }
    }

    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void clipAtLedge(CallbackInfoReturnable<Boolean> info) {
        if (Solstice.MODULE_MANAGER.getModule(SafeWalkModule.class).isToggled()) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "getMovementSpeed", at = @At("HEAD"), cancellable = true)
    private void getMovementSpeed(CallbackInfoReturnable<Float> info) {
        if (Solstice.MODULE_MANAGER.getModule(SpeedModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(SpeedModule.class).mode.getValue().equalsIgnoreCase("Vanilla")) {
            info.setReturnValue(Solstice.MODULE_MANAGER.getModule(SpeedModule.class).vanillaSpeed.getValue().floatValue());
        }
    }
}
