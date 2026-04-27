package me.nullrush.solstice.mixins;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.impl.MouseInputEvent;
import me.nullrush.solstice.events.impl.UnfilteredMouseInputEvent;
import me.nullrush.solstice.modules.impl.visuals.FreeLookModule;
import me.nullrush.solstice.modules.impl.visuals.FreecamModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo info) {
        Solstice.EVENT_HANDLER.post(new UnfilteredMouseInputEvent(button, action, mods));
        if (window == client.getWindow().getHandle() && action == 1 && client.currentScreen == null) {
            Solstice.EVENT_HANDLER.post(new MouseInputEvent(button));
        }
    }

    @ModifyArgs(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void updateMouse$changeLookDirection(Args args) {
        FreecamModule freecamModule = Solstice.MODULE_MANAGER.getModule(FreecamModule.class);
        FreeLookModule freeLookModule = Solstice.MODULE_MANAGER.getModule(FreeLookModule.class);
        double deltaX = args.get(0);
        double deltaY = args.get(1);

        if (freecamModule.isToggled()) {
            freecamModule.applyLookDelta(deltaX, deltaY);
            args.setAll(0.0D, 0.0D);
            return;
        }

        if (freeLookModule.isToggled()) {
            freeLookModule.applyLookDelta(deltaX, deltaY);
            args.setAll(0.0D, 0.0D);
        }
    }
}
