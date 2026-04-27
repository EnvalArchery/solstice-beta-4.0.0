package me.nullrush.solstice.modules.impl.visuals;

import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.PlayerDeathEvent;
import me.nullrush.solstice.events.impl.RenderWorldEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ColorSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.utils.animations.Easing;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.graphics.ModelRenderer;
import me.nullrush.solstice.utils.graphics.Renderer3D;
import me.nullrush.solstice.utils.minecraft.StaticPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RegisterModule(name = "KillEffects", description = "Renders effects on players when they die.", category = Module.Category.VISUALS)
public class KillEffectsModule extends Module {
    public ModeSetting effect = new ModeSetting("Effect", "The effect that will be rendered.", "Lightning", new String[]{"Lightning", "PopCham", "BurstCham", "ShatterCham", "FallingBolt", "Tornado", "Ghost", "All"});
    public ModeSetting ghostMode = new ModeSetting("GhostMode", "The ghost animation used by the ghost effect.", new ModeSetting.Visibility(effect, "Ghost", "All"), "Rise", new String[]{"Rise", "Tornado", "Both"});
    public NumberSetting duration = new NumberSetting("Duration", "The duration of animated kill effects.", 1600, 250, 5000);
    public NumberSetting intensity = new NumberSetting("Intensity", "How many particles and fragments animated effects create.", 12, 3, 36);
    public BooleanSetting particles = new BooleanSetting("Particles", "Spawns small firework particles with kill effects.", true);
    public ColorSetting fillColor = new ColorSetting("FillColor", "The fill color used by chams effects.", new ColorSetting.Color(new Color(155, 60, 255, 75), false, false));
    public ColorSetting outlineColor = new ColorSetting("OutlineColor", "The outline color used by chams effects.", new ColorSetting.Color(new Color(230, 230, 255, 170), false, false));

    private final List<KillEffect> effects = new ArrayList<>();

    @SubscribeEvent
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (mc.world == null || event.getPlayer() == null) return;

        String selected = effect.getValue();
        if (selected.equals("Lightning") || selected.equals("All")) spawnLightning(event.getPlayer().getPos());
        if (selected.equals("PopCham") || selected.equals("All")) effects.add(new PopChamEntity(event.getPlayer()));
        if (selected.equals("BurstCham") || selected.equals("All")) effects.add(new BurstChamEntity(event.getPlayer()));
        if (selected.equals("ShatterCham") || selected.equals("All")) effects.add(new ShatterChamEntity(event.getPlayer()));
        if (selected.equals("FallingBolt") || selected.equals("All")) effects.add(new FallingBolt(event.getPlayer().getPos()));
        if (selected.equals("Tornado") || selected.equals("All")) effects.add(new TornadoInstance(event.getPlayer().getPos()));
        if (selected.equals("Ghost") || selected.equals("All")) addGhost(event.getPlayer());

        if (particles.getValue()) spawnParticles(event.getPlayer().getPos());
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldEvent event) {
        if (getNull()) {
            effects.clear();
            return;
        }

        Iterator<KillEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            KillEffect effect = iterator.next();
            if (effect.finished()) {
                iterator.remove();
                continue;
            }

            effect.render(event);
        }
    }

    private void addGhost(PlayerEntity player) {
        GhostMode mode = GhostMode.from(ghostMode.getValue());
        if (mode == GhostMode.RISE || mode == GhostMode.BOTH) effects.add(new TornadoGhost(player, false));
        if (mode == GhostMode.TORNADO || mode == GhostMode.BOTH) effects.add(new TornadoGhost(player, true));
    }

    private void spawnLightning(Vec3d pos) {
        LightningEntity entity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        entity.setPosition(pos);
        entity.setId(-701 - effects.size());
        mc.world.addEntity(entity);
    }

    private void spawnParticles(Vec3d pos) {
        int amount = intensity.getValue().intValue();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < amount; i++) {
            double x = pos.x + random.nextDouble(-0.6, 0.6);
            double y = pos.y + random.nextDouble(0.2, 1.8);
            double z = pos.z + random.nextDouble(-0.6, 0.6);
            double velocityX = random.nextDouble(-0.05, 0.05);
            double velocityY = random.nextDouble(0.02, 0.12);
            double velocityZ = random.nextDouble(-0.05, 0.05);
            mc.world.addParticle(ParticleTypes.FIREWORK, x, y, z, velocityX, velocityY, velocityZ);
        }
    }

    private Color fade(Color color, float progress) {
        int alpha = (int) Math.clamp(color.getAlpha() * (1.0f - progress), 0.0f, 255.0f);
        return ColorUtils.getColor(color, alpha);
    }

    private abstract class KillEffect {
        protected final long startTime = System.currentTimeMillis();

        protected float progress() {
            return Math.clamp(Easing.toDelta(startTime, duration.getValue().intValue()), 0.0f, 1.0f);
        }

        protected boolean finished() {
            return progress() >= 1.0f;
        }

        protected abstract void render(RenderWorldEvent event);
    }

    private class PopChamEntity extends KillEffect {
        private final StaticPlayerEntity model;
        private final Vec3d origin;

        private PopChamEntity(PlayerEntity player) {
            this.model = new StaticPlayerEntity(player);
            this.origin = player.getPos();
        }

        @Override
        protected void render(RenderWorldEvent event) {
            float progress = progress();
            model.setPosition(origin.add(0.0, progress * 0.85, 0.0));

            Color fill = fade(fillColor.getColor(), progress);
            Color outline = fade(outlineColor.getColor(), progress);
            model.render(event, true, fill, true, outline);
        }
    }

    private class BurstChamEntity extends KillEffect {
        private final StaticPlayerEntity model;
        private final Vec3d origin;

        private BurstChamEntity(PlayerEntity player) {
            this.model = new StaticPlayerEntity(player);
            this.origin = player.getPos();
        }

        @Override
        protected void render(RenderWorldEvent event) {
            float progress = progress();
            float scale = 1.0f + progress * 1.15f;
            model.setPosition(origin.add(0.0, progress * 0.35, 0.0));

            Color fill = fade(fillColor.getColor(), progress);
            Color outline = fade(outlineColor.getColor(), progress);
            ModelRenderer.renderModel(model, true, scale, event.getTickDelta(), new ModelRenderer.Render(true, fill, true, outline, false));
        }
    }

    private class ShatterChamEntity extends KillEffect {
        private final List<Shard> shards = new ArrayList<>();

        private ShatterChamEntity(PlayerEntity player) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Vec3d base = player.getPos();
            int amount = intensity.getValue().intValue();

            for (int i = 0; i < amount; i++) {
                Vec3d start = base.add(random.nextDouble(-0.35, 0.35), random.nextDouble(0.1, 1.75), random.nextDouble(-0.35, 0.35));
                Vec3d velocity = new Vec3d(random.nextDouble(-1.4, 1.4), random.nextDouble(0.35, 1.6), random.nextDouble(-1.4, 1.4));
                shards.add(new Shard(start, velocity, random.nextDouble(0.08, 0.18)));
            }
        }

        @Override
        protected void render(RenderWorldEvent event) {
            float progress = progress();
            Color fill = fade(fillColor.getColor(), progress);
            Color outline = fade(outlineColor.getColor(), progress);

            for (Shard shard : shards) {
                Vec3d pos = shard.start.add(shard.velocity.multiply(progress));
                pos = pos.subtract(0.0, progress * progress * 0.75, 0.0);
                Box box = new Box(pos.x - shard.size, pos.y - shard.size, pos.z - shard.size, pos.x + shard.size, pos.y + shard.size, pos.z + shard.size);
                Renderer3D.renderBox(event.getMatrices(), box, fill);
                Renderer3D.renderBoxOutline(event.getMatrices(), box, outline);
            }
        }
    }

    private class FallingBolt extends KillEffect {
        private final Vec3d target;
        private boolean struck;

        private FallingBolt(Vec3d target) {
            this.target = target;
        }

        @Override
        protected void render(RenderWorldEvent event) {
            float progress = progress();
            Vec3d top = target.add(0.0, 9.0 - progress * 7.0, 0.0);
            Vec3d bottom = target.add(0.0, 1.0 + progress * 0.5, 0.0);
            Color color = fade(outlineColor.getColor(), progress * 0.65f);

            Renderer3D.renderLine(event.getMatrices(), top, bottom, color);
            Renderer3D.renderLine(event.getMatrices(), top.add(0.25, -1.6, 0.0), bottom.add(-0.2, 0.2, 0.15), color);
            Renderer3D.renderLine(event.getMatrices(), top.add(-0.2, -3.0, 0.15), bottom.add(0.18, 0.0, -0.2), color);

            if (!struck && progress > 0.72f) {
                spawnLightning(target);
                struck = true;
            }
        }
    }

    private class TornadoInstance extends KillEffect {
        private final Vec3d origin;
        private final List<TornadoGhost> ghosts = new ArrayList<>();

        private TornadoInstance(Vec3d origin) {
            this.origin = origin;
            int amount = Math.max(6, intensity.getValue().intValue() / 2);
            for (int i = 0; i < amount; i++) ghosts.add(new TornadoGhost(origin, i / (float) amount));
        }

        @Override
        protected void render(RenderWorldEvent event) {
            float progress = progress();
            Color lineColor = fade(outlineColor.getColor(), progress);

            for (int i = 0; i < ghosts.size(); i++) {
                TornadoGhost ghost = ghosts.get(i);
                Vec3d pos = ghost.position(progress);
                double size = 0.08 + progress * 0.08;
                Renderer3D.renderBox(event.getMatrices(), new Box(pos.x - size, pos.y - size, pos.z - size, pos.x + size, pos.y + size, pos.z + size), fade(fillColor.getColor(), progress));

                if (i > 0) {
                    Renderer3D.renderLine(event.getMatrices(), ghosts.get(i - 1).position(progress), pos, lineColor);
                }
            }
        }
    }

    private class TornadoGhost extends KillEffect {
        private final StaticPlayerEntity model;
        private final Vec3d origin;
        private final boolean spiral;
        private final float offset;

        private TornadoGhost(PlayerEntity player, boolean spiral) {
            this.model = new StaticPlayerEntity(player);
            this.origin = player.getPos();
            this.spiral = spiral;
            this.offset = 0.0f;
        }

        private TornadoGhost(Vec3d origin, float offset) {
            this.model = null;
            this.origin = origin;
            this.spiral = true;
            this.offset = offset;
        }

        private Vec3d position(float progress) {
            double angle = (progress * 8.0 + offset * Math.TAU);
            double radius = 0.35 + progress * 1.25;
            double y = 0.2 + progress * 2.25 + offset * 1.2;
            return origin.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }

        @Override
        protected void render(RenderWorldEvent event) {
            if (model == null) return;

            float progress = progress();
            Vec3d pos = spiral ? position(progress) : origin.add(0.0, progress * 1.65, 0.0);
            model.setPosition(pos);

            Color fill = fade(fillColor.getColor(), progress);
            Color outline = fade(outlineColor.getColor(), progress);
            ModelRenderer.renderModel(model, true, 1.0f - progress * 0.25f, event.getTickDelta(), new ModelRenderer.Render(true, fill, true, outline, true));
        }
    }

    private record Shard(Vec3d start, Vec3d velocity, double size) { }

    private enum GhostMode {
        RISE, TORNADO, BOTH;

        private static GhostMode from(String value) {
            return switch (value) {
                case "Tornado" -> TORNADO;
                case "Both" -> BOTH;
                default -> RISE;
            };
        }
    }
}
