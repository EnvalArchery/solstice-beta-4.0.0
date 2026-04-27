package me.nullrush.solstice.mixins;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.impl.visuals.AmbienceModule;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class PropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void getTimeOfDay(CallbackInfoReturnable<Long> info) {
        if (Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).modifyTime.getValue()) {
            info.setReturnValue(Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).time.getValue().longValue() * 100L);
        }
    }
}
