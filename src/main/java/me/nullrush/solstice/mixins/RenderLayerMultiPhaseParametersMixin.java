package me.nullrush.solstice.mixins;

import me.nullrush.solstice.utils.mixins.IMultiPhaseParameters;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhaseParameters.class)
public abstract class RenderLayerMultiPhaseParametersMixin implements IMultiPhaseParameters {
    @Shadow @Final private RenderPhase.Target target;

    @Override
    public RenderPhase.Target solstice$getTarget() {
        return this.target;
    }
}
