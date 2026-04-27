package me.nullrush.solstice.mixins;

import me.nullrush.solstice.utils.mixins.IMultiPhase;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhase.class)
public class RenderLayerMultiPhaseMixin implements IMultiPhase {
    @Shadow
    @Final
    private RenderLayer.MultiPhaseParameters phases;

    @Override
    public RenderLayer.MultiPhaseParameters solstice$getParameters() {
        return this.phases;
    }
}
