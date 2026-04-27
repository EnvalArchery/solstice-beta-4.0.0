package me.nullrush.solstice.modules.impl.movement;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.PlayerMoveEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.minecraft.HoleUtils;
import me.nullrush.solstice.utils.minecraft.MovementUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RegisterModule(name = "HoleSnap", description = "Pulls you toward your nearest hole.", category = Module.Category.MOVEMENT)
public class HoleSnapModule extends Module {
    public ModeSetting mode = new ModeSetting("Mode", "The movement mode used for getting into a hole.", "Snap", new String[]{"Snap", "Teleport"});
    public NumberSetting range = new NumberSetting("Range", "Range for the holes.", 5, 1, 8);
    public BooleanSetting doubleHoles = new BooleanSetting("DoubleHoles", "Whether or not to snap you to double holes.", true);
    public BooleanSetting quadHoles = new BooleanSetting("QuadHoles", "Whether or not to snap you to quad holes.", true);
    public BooleanSetting step = new BooleanSetting("Step", "Automatically steps when trying to holesnap.", new ModeSetting.Visibility(mode, "Snap"), false);
    public NumberSetting teleportPackets = new NumberSetting("TeleportPackets", "Packets", "How many movement packets should be sent while teleporting to the target hole.", new ModeSetting.Visibility(mode, "Teleport"), 6, 1, 16);
    public BooleanSetting onlyFromHole = new BooleanSetting("OnlyFromHole", "Only teleports when you are already standing in a valid hole.", new ModeSetting.Visibility(mode, "Teleport"), true);
    public BooleanSetting autoDisable = new BooleanSetting("AutoDisable", "Automatically disables after a teleport attempt.", new ModeSetting.Visibility(mode, "Teleport"), true);

    public Box hole = null;

    @Override
    public void onEnable() {
        hole = null;

        if (getNull()) {
            setToggled(false);
            return;
        }

        if (mode.getValue().equalsIgnoreCase("Teleport")) {
            teleportToHole();
            if (autoDisable.getValue()) {
                setToggled(false);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerMove(PlayerMoveEvent event) {
        if (getNull() || mc.player.fallDistance >= 5.0f || mode.getValue().equalsIgnoreCase("Teleport")) return;

        List<HoleUtils.Hole> holes = getHoles();
        if(holes.isEmpty()) return;

        hole = getHoleBox(holes.get(0));

        if(mc.player.getX() == hole.getCenter().x && mc.player.getY() == hole.minY && mc.player.getZ() == hole.getCenter().z) {
            if(Solstice.MODULE_MANAGER.getModule(StepModule.class).isToggled()) Solstice.MODULE_MANAGER.getModule(StepModule.class).setToggled(false);
            if(Solstice.MODULE_MANAGER.getModule(SpeedModule.class).isToggled()) Solstice.MODULE_MANAGER.getModule(SpeedModule.class).setToggled(false);
            setToggled(false);
            return;
        }

        MovementUtils.moveTowards(event, hole.getCenter(), MovementUtils.getPotionSpeed(MovementUtils.DEFAULT_SPEED));
    }

    private void teleportToHole() {
        if (mc.player == null || mc.world == null || mc.player.fallDistance >= 5.0f) {
            return;
        }

        if (onlyFromHole.getValue() && !HoleUtils.isPlayerInHole(mc.player)) {
            return;
        }

        List<HoleUtils.Hole> holes = getHoles();
        if (holes.isEmpty()) {
            return;
        }

        Vec3d start = mc.player.getPos();
        HoleUtils.Hole targetHole = holes.stream()
                .filter(h -> !isCurrentHole(h))
                .findFirst()
                .orElse(null);

        if (targetHole == null) {
            return;
        }

        hole = getHoleBox(targetHole);
        Vec3d destination = getHolePosition(targetHole);
        int packets = Math.max(1, Math.max(teleportPackets.getValue().intValue(), (int) Math.ceil(start.distanceTo(destination) / 0.28)));

        for (int i = 1; i <= packets; i++) {
            double progress = i / (double) packets;
            Vec3d next = start.lerp(destination, progress);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(next.x, next.y, next.z, true, mc.player.horizontalCollision));
        }

        mc.player.setPosition(destination.x, destination.y, destination.z);
        mc.player.setVelocity(0.0, 0.0, 0.0);
    }

    private boolean isCurrentHole(HoleUtils.Hole hole) {
        Vec3d target = getHolePosition(hole);
        return mc.player.squaredDistanceTo(target.x, target.y, target.z) < 0.36;
    }

    private Vec3d getHolePosition(HoleUtils.Hole hole) {
        Box box = getHoleBox(hole);
        return new Vec3d(box.getCenter().x, box.minY, box.getCenter().z);
    }

    private Box getHoleBox(HoleUtils.Hole hole) {
        return hole.box();
    }

    private List<HoleUtils.Hole> getHoles() {
        List<HoleUtils.Hole> holes = new ArrayList<>();

        for (int i = 0; i < Solstice.WORLD_MANAGER.getRadius(range.getValue().doubleValue()); i++) {
            BlockPos position = mc.player.getBlockPos().add(Solstice.WORLD_MANAGER.getOffset(i));

            if(position.getY() > mc.player.getY()) continue;

            HoleUtils.Hole singleHole = HoleUtils.getSingleHole(position, 1);
            if (singleHole != null) {
                holes.add(singleHole);
                continue;
            }

            if (doubleHoles.getValue()) {
                HoleUtils.Hole doubleHole = HoleUtils.getDoubleHole(position, 1);
                if (doubleHole != null) {
                    holes.add(doubleHole);
                    continue;
                }
            }

            if (quadHoles.getValue()) {
                HoleUtils.Hole quadHole = HoleUtils.getQuadHole(position, 1);
                if (quadHole != null) {
                    holes.add(quadHole);
                }
            }
        }

        return holes.stream().sorted(Comparator.comparing(h -> mc.player.squaredDistanceTo(h.box().getCenter().x, h.box().getCenter().y, h.box().getCenter().z))).toList();
    }
}
