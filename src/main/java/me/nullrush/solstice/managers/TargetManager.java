package me.nullrush.solstice.managers;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.ClientConnectEvent;
import me.nullrush.solstice.events.impl.PlayerDeathEvent;
import me.nullrush.solstice.events.impl.TargetDeathEvent;
import me.nullrush.solstice.events.impl.TickEvent;
import me.nullrush.solstice.modules.impl.combat.AuraModule;
import me.nullrush.solstice.modules.impl.combat.AutoCrystalModule;
import me.nullrush.solstice.utils.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;

public class TargetManager implements IMinecraft {
    private final ArrayList<Target> targets = new ArrayList<>();

    public TargetManager() {
        Solstice.EVENT_HANDLER.subscribe(this);
    }

    @SubscribeEvent
    public void onClientConnect(ClientConnectEvent event) {
        targets.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(mc.player == null || mc.world == null) return;

        PlayerEntity caTarget = Solstice.MODULE_MANAGER.getModule(AutoCrystalModule.class).getTarget();
        Entity kaTarget = Solstice.MODULE_MANAGER.getModule(AuraModule.class).target;

        synchronized (targets) {
            targets.removeIf(t -> System.currentTimeMillis() - t.time > 15000); // Remove targets if 15 seconds since last time they were targeted has passed

            if(caTarget != null) targets.add(new Target(caTarget));

            if(kaTarget instanceof PlayerEntity) targets.add(new Target((PlayerEntity) kaTarget));
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(mc.player == null || mc.world == null || !isTarget(event.getPlayer())) return;

        synchronized (targets) {
            Solstice.EVENT_HANDLER.post(new TargetDeathEvent(event.getPlayer()));
            targets.remove(getTarget(event.getPlayer()));
        }
    }

    private Target getTarget(PlayerEntity player) {
        for(Target target : targets) {
            if(target.player == player) return target;
        }
        return null;
    }

    public boolean isTarget(PlayerEntity player) {
        for(Target target : targets) {
            if(target.player == player) return true;
        }
        return false;
    }

    private class Target {
        private final PlayerEntity player;
        private final long time;

        public Target(PlayerEntity player) {
            this.player = player;
            this.time = System.currentTimeMillis();
        }
    }
}
