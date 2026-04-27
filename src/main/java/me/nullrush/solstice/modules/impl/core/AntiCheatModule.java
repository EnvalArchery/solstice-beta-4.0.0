package me.nullrush.solstice.modules.impl.core;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.AttackEntityEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.CategorySetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.system.Timer;

@RegisterModule(name = "AntiCheat", description = "Centralizes anticheat profile and cooldown compatibility settings.", category = Module.Category.CORE, persistent = true, drawn = false)
public class AntiCheatModule extends Module {
    public ModeSetting antiCheat = new ModeSetting("AntiCheat", "The anticheat profile other modules can use for safer defaults.", "Vanilla", new String[]{"Vanilla", "NCP", "Grim", "Vulcan", "Matrix", "Polar", "Custom"});

    public CategorySetting cooldownCategory = new CategorySetting("Cooldowns", "Settings used to sync combat and item cooldowns with server conditions.");
    public ModeSetting tpsCooldownSync = new ModeSetting("TPSCooldownSync", "Syncs cooldown waits against the current server TPS.", new CategorySetting.Visibility(cooldownCategory), "Attack", new String[]{"Off", "Attack", "UseItem", "Both"});
    public NumberSetting minimumTPS = new NumberSetting("MinimumTPS", "MinimumTPS", "The lowest TPS value used when scaling cooldowns.", new CategorySetting.Visibility(cooldownCategory), 12.0f, 1.0f, 20.0f);
    public NumberSetting maxExtraDelay = new NumberSetting("MaxExtraDelay", "MaxExtraDelay", "The maximum extra cooldown delay added by TPS sync.", new CategorySetting.Visibility(cooldownCategory), 450, 0, 1500);
    public BooleanSetting pauseOnSetback = new BooleanSetting("PauseOnSetback", "Pauses synced cooldown actions briefly after a server setback.", new CategorySetting.Visibility(cooldownCategory), true);
    public NumberSetting setbackPause = new NumberSetting("SetbackPause", "SetbackPause", "How long synced cooldown actions should pause after a setback.", new BooleanSetting.Visibility(pauseOnSetback, true), 250, 0, 1500);

    public CategorySetting strictCategory = new CategorySetting("Strict", "Compatibility toggles that other modules can read.");
    public BooleanSetting strictMovement = new BooleanSetting("StrictMovement", "Prefer conservative movement behavior for strict anticheats.", new CategorySetting.Visibility(strictCategory), false);
    public BooleanSetting strictRotations = new BooleanSetting("StrictRotations", "Prefer server-safe rotation behavior for strict anticheats.", new CategorySetting.Visibility(strictCategory), false);
    public BooleanSetting packetLimit = new BooleanSetting("PacketLimit", "Prefer lower packet spam for strict anticheats.", new CategorySetting.Visibility(strictCategory), true);

    private final Timer attackTimer = new Timer();
    private final Timer useItemTimer = new Timer();

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (mc.player == null || event.getPlayer() != mc.player) return;
        attackTimer.reset();
    }

    public boolean canAttack() {
        if (mc.player == null) return false;
        if (pauseOnSetback.getValue() && !Solstice.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(setbackPause.getValue().longValue())) return false;
        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return false;
        if (!syncsAttacks()) return true;

        return attackTimer.hasTimeElapsed(getScaledCooldownDelay(625L));
    }

    public boolean canUseItem(long vanillaDelay) {
        if (mc.player == null) return false;
        if (pauseOnSetback.getValue() && !Solstice.SERVER_MANAGER.getSetbackTimer().hasTimeElapsed(setbackPause.getValue().longValue())) return false;
        if (!syncsUseItems()) return true;

        return useItemTimer.hasTimeElapsed(getScaledCooldownDelay(vanillaDelay));
    }

    public void recordUseItem() {
        useItemTimer.reset();
    }

    public long getScaledCooldownDelay(long vanillaDelay) {
        float tickRate = Math.clamp(Solstice.SERVER_MANAGER.getTickRate(), minimumTPS.getValue().floatValue(), 20.0f);
        float scale = 20.0f / tickRate;
        long scaledDelay = (long) (vanillaDelay * scale);
        long extraDelay = Math.clamp(scaledDelay - vanillaDelay, 0L, maxExtraDelay.getValue().longValue());
        return vanillaDelay + extraDelay;
    }

    public boolean syncsAttacks() {
        return tpsCooldownSync.getValue().equalsIgnoreCase("Attack") || tpsCooldownSync.getValue().equalsIgnoreCase("Both");
    }

    public boolean syncsUseItems() {
        return tpsCooldownSync.getValue().equalsIgnoreCase("UseItem") || tpsCooldownSync.getValue().equalsIgnoreCase("Both");
    }

    public boolean isGrim() {
        return antiCheat.getValue().equalsIgnoreCase("Grim");
    }

    public boolean isNCP() {
        return antiCheat.getValue().equalsIgnoreCase("NCP");
    }

    public boolean isStrict() {
        return strictMovement.getValue() || strictRotations.getValue() || packetLimit.getValue() || isGrim() || isNCP() || antiCheat.getValue().equalsIgnoreCase("Vulcan") || antiCheat.getValue().equalsIgnoreCase("Matrix") || antiCheat.getValue().equalsIgnoreCase("Polar");
    }

    @Override
    public String getMetaData() {
        return antiCheat.getValue();
    }
}
