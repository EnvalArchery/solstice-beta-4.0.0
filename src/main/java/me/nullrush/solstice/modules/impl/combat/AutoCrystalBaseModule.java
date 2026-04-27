package me.nullrush.solstice.modules.impl.combat;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.PlayerUpdateEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.minecraft.InventoryUtils;
import me.nullrush.solstice.utils.minecraft.PositionUtils;
import me.nullrush.solstice.utils.minecraft.WorldUtils;
import me.nullrush.solstice.utils.system.ThreadExecutor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

@RegisterModule(name = "AutoCrystalBase", description = "Places obsidian under the target and two blocks in front and behind them.", category = Module.Category.COMBAT)
public class AutoCrystalBaseModule extends Module {
    public ModeSetting autoSwitch = new ModeSetting("Switch", "The mode that will be used for automatically switching to obsidian.", "Silent", InventoryUtils.SWITCH_MODES);
    public BooleanSetting asynchronous = new BooleanSetting("Asynchronous", "Performs calculations on separate threads.", true);
    public NumberSetting delay = new NumberSetting("Delay", "The amount of ticks that have to be waited for between placements.", 0, 0, 20);
    public NumberSetting limit = new NumberSetting("Limit", "The number of blocks that can be placed per tick.", 3, 1, 3);
    public NumberSetting range = new NumberSetting("Range", "The maximum range at which obsidian will be placed.", 5.0, 0.0, 12.0);
    public NumberSetting enemyRange = new NumberSetting("EnemyRange", "The maximum distance at which enemies can be targeted.", 8.0, 0.0, 16.0);
    public BooleanSetting rotate = new BooleanSetting("Rotate", "Sends a packet rotation whenever placing a block.", true);
    public BooleanSetting strictDirection = new BooleanSetting("StrictDirection", "Only places using directions that face you.", false);
    public BooleanSetting crystalDestruction = new BooleanSetting("CrystalDestruction", "Destroys any crystals that interfere with block placement.", true);
    public BooleanSetting whileEating = new BooleanSetting("WhileEating", "Places blocks normally while eating.", true);
    public BooleanSetting selfDisable = new BooleanSetting("SelfDisable", "Toggles off the module once no valid target exists.", false);
    public BooleanSetting itemDisable = new BooleanSetting("ItemDisable", "Toggles off the module whenever you run out of obsidian.", true);
    public BooleanSetting render = new BooleanSetting("Render", "Whether or not to render the place position.", true);

    private PlayerEntity target = null;
    private List<BlockPos> positions = new ArrayList<>();
    private int ticks = 0;

    @SubscribeEvent
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!whileEating.getValue() && mc.player.isUsingItem()) return;

        Runnable runnable = () -> {
            if (ticks < delay.getValue().intValue()) {
                ticks++;
                return;
            }

            if (autoSwitch.getValue().equalsIgnoreCase("None") && mc.player.getMainHandStack().getItem() != Items.OBSIDIAN) {
                if (itemDisable.getValue()) {
                    Solstice.CHAT_MANAGER.tagged("You are currently not holding obsidian.", getName());
                    setToggled(false);
                }

                clear();
                return;
            }

            int slot = InventoryUtils.find(Items.OBSIDIAN, 0, autoSwitch.getValue().equalsIgnoreCase("AltSwap") || autoSwitch.getValue().equalsIgnoreCase("AltPickup") ? 35 : 8);
            int previousSlot = mc.player.getInventory().selectedSlot;
            if (slot == -1) {
                if (itemDisable.getValue()) {
                    Solstice.CHAT_MANAGER.tagged("No obsidian could be found in your hotbar.", getName());
                    setToggled(false);
                }

                clear();
                return;
            }

            PlayerEntity target = getTarget();
            if (target == null) {
                if (selfDisable.getValue()) setToggled(false);

                clear();
                return;
            }

            List<BlockPos> targetPositions = getPositions(target);
            if (targetPositions.isEmpty()) {
                if (selfDisable.getValue()) setToggled(false);

                this.target = target;
                positions = new ArrayList<>();
                return;
            }

            this.target = target;
            positions = targetPositions;

            InventoryUtils.switchSlot(autoSwitch.getValue(), slot, previousSlot);

            int blocksPlaced = 0;
            for (BlockPos position : targetPositions) {
                if (blocksPlaced >= limit.getValue().intValue()) break;

                Direction direction = WorldUtils.getDirection(position, strictDirection.getValue());
                if (direction == null) continue;

                WorldUtils.placeBlock(position, direction, Hand.MAIN_HAND, rotate.getValue(), crystalDestruction.getValue(), render.getValue());
                blocksPlaced++;
            }

            InventoryUtils.switchBack(autoSwitch.getValue(), slot, previousSlot);
            ticks = 0;
        };

        if (asynchronous.getValue()) ThreadExecutor.execute(runnable);
        else runnable.run();
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) setToggled(false);
    }

    @Override
    public void onDisable() {
        clear();
    }

    @Override
    public String getMetaData() {
        if (target == null) return "";
        return target.getName().getString();
    }

    private PlayerEntity getTarget() {
        PlayerEntity optimalTarget = null;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
            if (Solstice.FRIEND_MANAGER.contains(player.getName().getString())) continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(enemyRange.getValue().doubleValue())) continue;

            if (optimalTarget == null || mc.player.squaredDistanceTo(player) < mc.player.squaredDistanceTo(optimalTarget)) {
                optimalTarget = player;
            }
        }

        return optimalTarget;
    }

    private List<BlockPos> getPositions(PlayerEntity player) {
        List<BlockPos> positions = new ArrayList<>();

        BlockPos base = PositionUtils.getFlooredPosition(player).down();
        addPosition(positions, base);
        addPosition(positions, base.north(2));
        addPosition(positions, base.south(2));

        return positions;
    }

    private void addPosition(List<BlockPos> positions, BlockPos position) {
        if (mc.player.squaredDistanceTo(position.toCenterPos()) > MathHelper.square(range.getValue().doubleValue())) return;
        if (!mc.world.getWorldBorder().contains(position)) return;
        if (!WorldUtils.isPlaceable(position)) return;
        if (WorldUtils.getDirection(position, strictDirection.getValue()) == null) return;
        if (!positions.contains(position)) positions.add(position);
    }

    private void clear() {
        target = null;
        positions = new ArrayList<>();
    }
}
