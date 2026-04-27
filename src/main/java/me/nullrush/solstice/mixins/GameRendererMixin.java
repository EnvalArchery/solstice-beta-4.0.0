package me.nullrush.solstice.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.impl.RenderEntityEvent;
import me.nullrush.solstice.events.impl.RenderShaderEvent;
import me.nullrush.solstice.events.impl.RenderWorldEvent;
import me.nullrush.solstice.modules.impl.miscellaneous.FOVModifierModule;
import me.nullrush.solstice.modules.impl.player.NoEntityTraceModule;
import me.nullrush.solstice.modules.impl.visuals.FreeLookModule;
import me.nullrush.solstice.modules.impl.visuals.FreecamModule;
import me.nullrush.solstice.modules.impl.visuals.NoRenderModule;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import me.nullrush.solstice.utils.graphics.Renderer3D;
import me.nullrush.solstice.utils.minecraft.WorldUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.profiler.Profilers;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void renderWorld$HEAD(RenderTickCounter tickCounter, CallbackInfo info) {
        Renderer3D.prepare();
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    private void renderWorld$swap(RenderTickCounter tickCounter, CallbackInfo info, @Local(ordinal = 2) Matrix4f matrix4f3, @Local(ordinal = 1) float tickDelta, @Local MatrixStack matrixStack) {
        RenderSystem.getModelViewStack().pushMatrix();

        RenderSystem.getModelViewStack().mul(matrix4f3);
        RenderSystem.getModelViewStack().mul(matrixStack.peek().getPositionMatrix().invert());

        Solstice.EVENT_HANDLER.post(new RenderWorldEvent(matrixStack, tickDelta));

        Renderer3D.draw(Renderer3D.QUADS, Renderer3D.DEBUG_LINES, false);
        Renderer3D.draw(Renderer3D.SHINE_QUADS, Renderer3D.SHINE_DEBUG_LINES, true);

        Solstice.EVENT_HANDLER.post(new RenderWorldEvent.Post(matrixStack, tickDelta));
        Solstice.EVENT_HANDLER.post(new RenderEntityEvent.Post());

        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void renderWorld$TAIL(RenderTickCounter renderTickCounter, CallbackInfo info) {
        Solstice.EVENT_HANDLER.post(new RenderShaderEvent.Post());
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void tiltViewWhenHurt(CallbackInfo info) {
        if (Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).hurtCamera.getValue()) {
            info.cancel();
        }
    }

    @Inject(method = "updateCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"), cancellable = true)
    private void updateCrosshairTarget(float tickDelta, CallbackInfo info) {
        if (Solstice.MODULE_MANAGER.getModule(FreecamModule.class).isToggled()) {
            Profilers.get().pop();
            client.crosshairTarget = WorldUtils.getRaytraceTarget(Solstice.MODULE_MANAGER.getModule(FreecamModule.class).getFreeYaw(), Solstice.MODULE_MANAGER.getModule(FreecamModule.class).getFreePitch(), Solstice.MODULE_MANAGER.getModule(FreecamModule.class).getFreeX(), Solstice.MODULE_MANAGER.getModule(FreecamModule.class).getFreeY(), Solstice.MODULE_MANAGER.getModule(FreecamModule.class).getFreeZ());
            info.cancel();
            return;
        }

        if (Solstice.MODULE_MANAGER.getModule(FreeLookModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(FreeLookModule.class).cameraTrace.getValue()) {
            Profilers.get().pop();
            client.crosshairTarget = WorldUtils.getRaytraceTarget(Solstice.MODULE_MANAGER.getModule(FreeLookModule.class).getFreeYaw(), Solstice.MODULE_MANAGER.getModule(FreeLookModule.class).getFreePitch(), client.player.getX(), client.player.getY(), client.player.getZ());
            info.cancel();
        }
    }

    @ModifyExpressionValue(method = "findCrosshairTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"))
    private @Nullable EntityHitResult findCrosshairTarget(@Nullable EntityHitResult original) {
        if (Solstice.MODULE_MANAGER.getModule(NoEntityTraceModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(NoEntityTraceModule.class).shouldIgnore()) {
            return null;
        }

        return original;
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void showFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).isToggled() && Solstice.MODULE_MANAGER.getModule(NoRenderModule.class).totemAnimation.getValue()) {
            info.cancel();
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", shift = At.Shift.AFTER))
    private void renderWorld(RenderTickCounter tickCounter, CallbackInfo info, @Local(ordinal = 0) Matrix4f matrix4f) {
        MatrixStack matrix = new MatrixStack();
        matrix.peek().getPositionMatrix().mul(matrix4f);

        Renderer2D.LAST_PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        Renderer2D.LAST_MODEL_MATRIX.set(RenderSystem.getModelViewMatrix());
        Renderer2D.LAST_WORLD_MATRIX.set(matrix.peek().getPositionMatrix());
    }

    @Inject(method = "getFov", at = @At("TAIL"), cancellable = true)
    private void getFOV(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> info) {
        FOVModifierModule module = Solstice.MODULE_MANAGER.getModule(FOVModifierModule.class);
        if(module.isToggled()) {
            if(info.getReturnValue() == 70 && !module.items.getValue()) return;
            info.setReturnValue(module.fov.getValue().floatValue());
        }
    }
}
