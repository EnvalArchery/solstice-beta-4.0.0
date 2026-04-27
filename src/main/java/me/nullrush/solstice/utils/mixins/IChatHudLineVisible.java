package me.nullrush.solstice.utils.mixins;

public interface IChatHudLineVisible {
    boolean solstice$isClientMessage();

    void solstice$setClientMessage(boolean clientMessage);

    String solstice$getClientIdentifier();

    void solstice$setClientIdentifier(String clientIdentifier);
}