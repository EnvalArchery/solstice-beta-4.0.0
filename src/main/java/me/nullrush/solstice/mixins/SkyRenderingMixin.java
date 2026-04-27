package me.nullrush.solstice.mixins;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.impl.visuals.AmbienceModule;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @Inject(method = "renderSky", at = @At("TAIL"))
    private void renderSky(float red, float green, float blue, CallbackInfo info) {
        if (Solstice.MODULE_MANAGER == null || Solstice.AMBIENCE_SHADER_MANAGER == null) {
            return;
        }

        AmbienceModule ambience = Solstice.MODULE_MANAGER.getModule(AmbienceModule.class);
        if (ambience.isToggled()) {
            ambience.renderSkyShader();
        }
    }
}
