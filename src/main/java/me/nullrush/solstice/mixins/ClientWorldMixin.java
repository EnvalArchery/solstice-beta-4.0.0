package me.nullrush.solstice.mixins;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.impl.EntitySpawnEvent;
import me.nullrush.solstice.modules.impl.visuals.AmbienceModule;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void getSkyColor(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> info) {
        if (Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).modifySky.getValue()) {
            info.setReturnValue(Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).getRuntimeSkyColor().getRGB());
        }
    }

    @Inject(method = "addEntity", at = @At(value = "HEAD"))
    private void addEntity(Entity entity, CallbackInfo info) {
        Solstice.EVENT_HANDLER.post(new EntitySpawnEvent(entity));
    }
}
