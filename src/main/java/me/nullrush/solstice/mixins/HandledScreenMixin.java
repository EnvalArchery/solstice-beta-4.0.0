package me.nullrush.solstice.mixins;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.impl.miscellaneous.TooltipsModule;
import me.nullrush.solstice.utils.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements IMinecraft {
    @Shadow @Nullable protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TooltipsModule tooltipsModule = Solstice.MODULE_MANAGER.getModule(TooltipsModule.class);

        if(!tooltipsModule.isToggled()) return;

        if(focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.playerScreenHandler.getCursorStack().isEmpty() && tooltipsModule.hasItems(focusedSlot.getStack())) {
            tooltipsModule.renderInfo(context, mouseX, mouseY, focusedSlot.getStack());
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void drawMouseoverTooltip(DrawContext drawContext, int x, int y, CallbackInfo ci) {
        TooltipsModule tooltipsModule = Solstice.MODULE_MANAGER.getModule(TooltipsModule.class);

        if(!tooltipsModule.isToggled()) return;

        if(focusedSlot != null && !focusedSlot.getStack().isEmpty() && mc.player.playerScreenHandler.getCursorStack().isEmpty() && tooltipsModule.hasItems(focusedSlot.getStack())) {
            ci.cancel();
        }
    }
}
