package me.nullrush.solstice.modules.impl.miscellaneous;

import lombok.Getter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.PacketReceiveEvent;
import me.nullrush.solstice.events.impl.TickEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.chat.ChatUtils;
import me.nullrush.solstice.utils.system.Timer;
import me.nullrush.solstice.utils.system.ZeroTimer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

@RegisterModule(name = "FastLatency", description = "Makes ping resolving much faster.", category = Module.Category.MISCELLANEOUS)
public class FastLatencyModule extends Module {
    private static final int COMMAND_ID = 1000;

    public ModeSetting method = new ModeSetting("Method", "The ping method used for resolving your latency.", "Command", new String[]{"Command", "Tab", "Hybrid"});
    public NumberSetting delay = new NumberSetting("Delay", "The amount of milliseconds that have to be waited for before resolving your ping again.", 100L, 0, 1000L);
    public NumberSetting timeout = new NumberSetting("Timeout", "The amount of milliseconds to wait before falling back to tab ping.", new ModeSetting.Visibility(method, "Command", "Hybrid"), 1000L, 250L, 5000L);
    public BooleanSetting smoothing = new BooleanSetting("Smoothing", "Smooths sudden latency changes.", false);
    public NumberSetting smoothingFactor = new NumberSetting("SmoothingFactor", "The amount of smoothing applied to latency updates.", new BooleanSetting.Visibility(smoothing, true), 35, 0, 95);

    public BooleanSetting spikeNotifier = new BooleanSetting("SpikeNotifier", "Notifies you in chat whenever your ping spikes.", false);
    public NumberSetting threshold = new NumberSetting("Threshold", "The amount of milliseconds that your ping has to increase for before notifying you.", new BooleanSetting.Visibility(spikeNotifier, true), 30, 0, 1000);

    private final Timer timer = new Timer();
    private final ZeroTimer receivedTimer = new ZeroTimer();

    private long time;
    private boolean pendingCommand;
    @Getter private int latency;

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        if (pendingCommand && receivedTimer.hasTimeElapsed(timeout.getValue().longValue())) {
            if (method.getValue().equals("Hybrid")) {
                updateLatency(getTabLatency());
            }

            pendingCommand = false;
            receivedTimer.zero();
        }

        if (!timer.hasTimeElapsed(delay.getValue().longValue())) return;

        if (method.getValue().equals("Tab")) {
            updateLatency(getTabLatency());
            timer.reset();
            return;
        }

        if (!pendingCommand && receivedTimer.hasTimeElapsed(1000L)) {
            mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(COMMAND_ID, "/w "));
            time = System.currentTimeMillis();
            pendingCommand = true;
            receivedTimer.reset();
            timer.reset();
        }
    }

    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof CommandSuggestionsS2CPacket packet) {
            if (packet.id() == COMMAND_ID && pendingCommand) {
                int ping = (int) (System.currentTimeMillis() - time);
                updateLatency(ping);
                pendingCommand = false;
                receivedTimer.zero();
            }
        }
    }

    private void updateLatency(int ping) {
        if (ping < 0) return;

        int previous = latency;
        int resolved = smoothing.getValue() && previous > 0 ? smooth(previous, ping) : ping;

        if (spikeNotifier.getValue() && previous > 0 && resolved - previous > threshold.getValue().intValue()) {
            Solstice.CHAT_MANAGER.message("Your ping has spiked to " + ChatUtils.getPrimary() + resolved + "ms" + ChatUtils.getSecondary() + " from " + ChatUtils.getPrimary() + previous + "ms" + ChatUtils.getSecondary() + "!", "module-" + getName().toLowerCase() + "-spike");
        }

        latency = resolved;
    }

    private int smooth(int previous, int current) {
        float factor = smoothingFactor.getValue().floatValue() / 100.0f;
        return Math.round(previous * factor + current * (1.0f - factor));
    }

    private int getTabLatency() {
        if (mc.player == null || mc.getNetworkHandler() == null) return latency;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? latency : entry.getLatency();
    }

    @Override
    public String getMetaData() {
        return latency + "ms " + method.getValue();
    }
}
