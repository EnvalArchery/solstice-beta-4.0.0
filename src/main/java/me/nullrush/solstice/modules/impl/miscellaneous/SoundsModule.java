package me.nullrush.solstice.modules.impl.miscellaneous;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.AttackEntityEvent;
import me.nullrush.solstice.events.impl.TargetDeathEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.CategorySetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.settings.impl.StringSetting;
import me.nullrush.solstice.utils.system.FileUtils;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.io.File;

@RegisterModule(name = "Sounds", description = "Plays custom sounds when something happens.", category = Module.Category.MISCELLANEOUS)
public class SoundsModule extends Module {
    private static final String[] SOUND_PRESETS = new String[]{"Hard", "Metal", "Normal", "Overwatch", "Shot", "Soft", "Sword", "Vine", "Wood", "Custom"};

    CategorySetting killsCategory = new CategorySetting("Kills", "The kills category for the sounds.");
    BooleanSetting killSound = new BooleanSetting("KillSound", "Play sounds when you kill a player.", new CategorySetting.Visibility(killsCategory), true);
    ModeSetting killPreset = new ModeSetting("KillPreset", "Sound", "Selects which bundled kill sound will play.", new CategorySetting.Visibility(killsCategory), "Overwatch", SOUND_PRESETS);
    NumberSetting killVolume = new NumberSetting("KillVolume", "Volume", "The volume for the kill sounds.", new CategorySetting.Visibility(killsCategory), 1.0f, 0.0f, 1.0f);
    StringSetting killName = new StringSetting("KillName", "Name", "The name of the external kill sound file.", new ModeSetting.Visibility(killPreset, "Custom"), "killsound.wav");

    CategorySetting hitsCategory = new CategorySetting("Hits", "The hits category for the sounds.");
    BooleanSetting hitSound = new BooleanSetting("HitSound", "Play sounds when you hit an entity.", new CategorySetting.Visibility(hitsCategory), true);
    ModeSetting hitPreset = new ModeSetting("HitPreset", "Sound", "Selects which bundled hit sound will play.", new CategorySetting.Visibility(hitsCategory), "Sword", SOUND_PRESETS);
    NumberSetting hitVolume = new NumberSetting("HitVolume", "Volume", "The volume for the hit sounds.", new CategorySetting.Visibility(hitsCategory), 1.0f, 0.0f, 1.0f);
    StringSetting hitName = new StringSetting("HitName", "Name", "The name of the external hit sound file.", new ModeSetting.Visibility(hitPreset, "Custom"), "hitsound.wav");

    @SubscribeEvent
    public void onTargetDeath(TargetDeathEvent event) {
        if(getNull()) return;

        if (!killSound.getValue()) return;

        playConfiguredSound(killPreset.getValue(), killName.getValue(), killVolume.getValue().floatValue());
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if(event.getPlayer() != mc.player) return;

        if (!hitSound.getValue()) return;

        playConfiguredSound(hitPreset.getValue(), hitName.getValue(), hitVolume.getValue().floatValue());
    }

    private void playConfiguredSound(String preset, String customFileName, float volume) {
        if ("Custom".equalsIgnoreCase(preset)) {
            FileUtils.playSound(new File(Solstice.MOD_NAME + "/Client/" + customFileName), volume);
            return;
        }

        Identifier soundId = Identifier.of(Solstice.MOD_ID, "combat." + preset.toLowerCase());
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(soundId), volume));
    }
}
