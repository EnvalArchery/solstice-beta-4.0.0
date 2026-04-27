package me.nullrush.solstice.modules.impl.core;

import lombok.Getter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import net.minecraft.util.Identifier;

@Getter
@RegisterModule(name = "Capes", description = "Applies the Solstice cape to yourself and to other users.", category = Module.Category.CORE, toggled = true, drawn = false)
public class CapesModule extends Module {
    public CapesModule() {
        this.capeTexture = Identifier.of(Solstice.MOD_ID, "textures/cape.png");
    }

    private final Identifier capeTexture;
}
