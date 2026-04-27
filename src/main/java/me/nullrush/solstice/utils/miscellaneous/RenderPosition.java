package me.nullrush.solstice.utils.miscellaneous;

import lombok.Getter;
import lombok.Setter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.impl.core.RendersModule;
import me.nullrush.solstice.utils.animations.Easing;
import net.minecraft.util.math.BlockPos;

@Setter
@Getter
public class RenderPosition {
    private BlockPos pos;
    private long startTime;

    public RenderPosition(BlockPos pos) {
        this.pos = pos;
        startTime = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof RenderPosition) return ((RenderPosition) o).pos.equals(this.pos);
        return false;
    }

    public float get() {
        return Easing.ease(1.0f - Easing.toDelta(startTime, Solstice.MODULE_MANAGER.getModule(RendersModule.class).duration.getValue().intValue()), Easing.Method.EASE_IN_CUBIC);
    }
}
