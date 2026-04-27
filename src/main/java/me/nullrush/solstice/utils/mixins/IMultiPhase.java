package me.nullrush.solstice.utils.mixins;

import net.minecraft.client.render.RenderLayer;

public interface IMultiPhase {
    RenderLayer.MultiPhaseParameters solstice$getParameters();
}
