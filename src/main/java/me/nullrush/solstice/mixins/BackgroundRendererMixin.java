package me.nullrush.solstice.mixins;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.impl.visuals.AmbienceModule;
import me.nullrush.solstice.modules.impl.visuals.NoRenderModule;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.*;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @ModifyArgs(method = "applyFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Fog;<init>(FFLnet/minecraft/client/render/FogShape;FFFF)V"))
    private static void applyFog(Args args, Camera camera, BackgroundRenderer.FogType fogType, Vector4f originalColor, float viewDistance, boolean thickenFog, float tickDelta) {
        if (fogType == BackgroundRenderer.FogType.FOG_TERRAIN && Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).fog.getValue()) {
            args.set(0, viewDistance * 4);
            args.set(1, viewDistance * 4.25f);
        } else {
            if (Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).modifyFog.getValue()) {
                Color color = Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).getRuntimeFogColor();

                args.set(0, Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).fogStart.getValue().floatValue());
                args.set(1, Solstice.MODULE_MANAGER.getModule(AmbienceModule.class).fogEnd.getValue().floatValue());
                args.set(3, color.getRed() / 255.0f);
                args.set(4, color.getGreen() / 255.0f);
                args.set(5, color.getBlue() / 255.0f);
                args.set(6, color.getAlpha() / 255.0f);
            }
        }
    }

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void getFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<BackgroundRenderer.StatusEffectFogModifier> info) {
        if (Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).blindness.getValue()) {
            info.setReturnValue(null);
        }
    }
}
