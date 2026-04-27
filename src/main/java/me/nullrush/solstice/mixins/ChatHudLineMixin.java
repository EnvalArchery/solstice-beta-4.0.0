package me.nullrush.solstice.mixins;

import me.nullrush.solstice.utils.mixins.IChatHudLine;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatHudLine.class)
public class ChatHudLineMixin implements IChatHudLine {
    @Unique private boolean clientMessage = false;
    @Unique private String clientIdentifier = "";

    @Override
    public boolean solstice$isClientMessage() {
        return clientMessage;
    }

    @Override
    public void solstice$setClientMessage(boolean clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public String solstice$getClientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public void solstice$setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }
}