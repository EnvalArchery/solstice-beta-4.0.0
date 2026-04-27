package me.nullrush.solstice.modules.impl.core;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.TickEvent;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.modules.impl.combat.AutoCrystalModule;
import me.nullrush.solstice.modules.impl.combat.AuraModule;
import me.nullrush.solstice.settings.impl.BooleanSetting;
import me.nullrush.solstice.settings.impl.ModeSetting;
import me.nullrush.solstice.settings.impl.NumberSetting;
import me.nullrush.solstice.settings.impl.StringSetting;
import me.nullrush.solstice.utils.system.MathUtils;
import me.nullrush.solstice.utils.text.FormattingUtils;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

@RegisterModule(name = "RPC", description = "Enables Discord rich presence for the client.", category = Module.Category.CORE)
public class RPCModule extends Module {
    private static final long APPLICATION_ID = 1490070689678626866L;

    public ModeSetting imageMode = new ModeSetting("Image", "The mode for the discord presence image.", "Random", new String[]{"MushyGrape", "Wizard", "EvilMushroom", "Nett", "Yns", "Weed", "Miku", "Miku2", "Rukia", "Rei", "Nigga", "Bladee", "VapeV4", "Skeletrix", "Maxon", "ChillGuy", "Random"});
    public ModeSetting smallImageMode = new ModeSetting("SmallImage", "The small image displayed on the presence.", "MatchLarge", new String[]{"None", "MatchLarge", "Random"});
    public ModeSetting detailsMode = new ModeSetting("Details", "The top line of the discord presence.", "Activity", new String[]{"Custom", "Activity", "Stats", "Modules", "Random"});
    public StringSetting customDetails = new StringSetting("CustomDetails", "The custom text to use in the discord presence details.", new ModeSetting.Visibility(detailsMode, "Custom"), "Solstice 2.4.0");
    public ModeSetting stateMode = new ModeSetting("State", "The bottom line of the discord presence.", "Server", new String[]{"Custom", "Server", "Session", "Combat", "Random"});
    public StringSetting customState = new StringSetting("CustomState", "The custom text to use in the discord presence state.", new ModeSetting.Visibility(stateMode, "Custom"), "powered by Solstice");
    public ModeSetting largeTextMode = new ModeSetting("LargeText", "The text shown while hovering the large image.", "Version", new String[]{"Version", "Server", "Custom"});
    public StringSetting customLargeText = new StringSetting("CustomLargeText", "The custom text shown while hovering the large image.", new ModeSetting.Visibility(largeTextMode, "Custom"), "Solstice");
    public BooleanSetting elapsed = new BooleanSetting("Elapsed", "Shows the elapsed session time on the presence.", true);
    public NumberSetting updateRate = new NumberSetting("UpdateRate", "The amount of ticks between presence updates.", 100, 20, 400);
    public NumberSetting rotateDelay = new NumberSetting("RotateDelay", "The delay in seconds before random text rotates.", 180, 15, 600);

    private static final String[] DETAILS = {
            "closing distance",
            "holding the line",
            "tracking targets",
            "rewriting the session",
            "stacking modules",
            "lining up the next fight",
            "keeping the client sharp",
            "moving clean through packets"
    };

    private static final String[] STATES = {
            "solstice control surface",
            "staying one step ahead",
            "watching the horizon",
            "ready for the next push",
            "tuning the loadout",
            "keeping pressure high"
    };

    private final RichPresence rpc = new RichPresence();

    private int ticks = 0;
    private long lastRotate = 0L;
    private int lastLargeImage = 0;
    private int lastSmallImage = 0;
    private String randomDetails = DETAILS[0];
    private String randomState = STATES[0];

    @Override
    public void onEnable() {
        DiscordIPC.start(APPLICATION_ID, null);
        if (elapsed.getValue()) {
            rpc.setStart(Solstice.UPTIME / 1000L);
        }

        rotateRandoms(true);
        updatePresence(true);
    }

    @Override
    public void onDisable() {
        DiscordIPC.stop();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null && mc.world == null && mc.currentScreen == null) return;

        if (ticks > 0) {
            ticks--;
            return;
        }

        updatePresence(false);
        ticks = updateRate.getValue().intValue();
    }

    private void updatePresence(boolean forceRotate) {
        if (!DiscordIPC.isConnected()) return;

        rotateRandoms(forceRotate);

        lastLargeImage = resolveLargeImage();
        rpc.setLargeImage(String.valueOf(lastLargeImage), resolveLargeText());

        int smallImage = resolveSmallImage();
        if (smallImage >= 0) {
            lastSmallImage = smallImage;
            rpc.setSmallImage(String.valueOf(smallImage), resolveSmallText());
        } else {
            rpc.setSmallImage("", "");
        }

        rpc.setDetails(resolveDetails());
        rpc.setState(resolveState());

        if (elapsed.getValue()) {
            rpc.setStart(Solstice.UPTIME / 1000L);
        } else {
            rpc.setEnd(0L);
        }

        DiscordIPC.setActivity(rpc);
    }

    private void rotateRandoms(boolean force) {
        long rotateDelayMs = rotateDelay.getValue().longValue() * 1000L;
        if (!force && System.currentTimeMillis() - lastRotate < rotateDelayMs) return;

        randomDetails = DETAILS[(int) MathUtils.random(DETAILS.length, 0)];
        randomState = STATES[(int) MathUtils.random(STATES.length, 0)];
        if (imageMode.getValue().equals("Random")) lastLargeImage = randomImage();
        if (smallImageMode.getValue().equals("Random")) lastSmallImage = randomImage();
        lastRotate = System.currentTimeMillis();
    }

    private String resolveDetails() {
        return switch (detailsMode.getValue()) {
            case "Custom" -> customDetails.getValue();
            case "Stats" -> getStatsText();
            case "Modules" -> getModulesText();
            case "Random" -> randomDetails;
            default -> getActivityText();
        };
    }

    private String resolveState() {
        return switch (stateMode.getValue()) {
            case "Custom" -> customState.getValue();
            case "Session" -> getSessionText();
            case "Combat" -> getCombatText();
            case "Random" -> randomState;
            default -> getServerText();
        };
    }

    private String resolveLargeText() {
        return switch (largeTextMode.getValue()) {
            case "Server" -> getServerText();
            case "Custom" -> customLargeText.getValue();
            default -> Solstice.MOD_NAME + " " + Solstice.MOD_VERSION;
        };
    }

    private String resolveSmallText() {
        if (stateMode.getValue().equals("Combat")) return getCombatText();
        if (detailsMode.getValue().equals("Stats")) return getStatsText();
        return getActivityText();
    }

    private int resolveLargeImage() {
        if (imageMode.getValue().equals("Random")) {
            return lastLargeImage == 0 ? randomImage() : lastLargeImage;
        }

        return getImageIndex(imageMode.getValue());
    }

    private int resolveSmallImage() {
        return switch (smallImageMode.getValue()) {
            case "None" -> -1;
            case "Random" -> lastSmallImage == 0 ? randomImage() : lastSmallImage;
            default -> resolveLargeImage();
        };
    }

    private String getActivityText() {
        if (mc.currentScreen != null && mc.player == null) return "In the menus";
        if (mc.world == null || mc.player == null) return "Starting up";
        if (mc.isInSingleplayer()) return "Playing singleplayer";
        if (mc.getCurrentServerEntry() != null) return "Playing " + mc.getCurrentServerEntry().address;
        return "Exploring the world";
    }

    private String getStatsText() {
        if (mc.player == null || mc.world == null) return Solstice.MOD_NAME + " " + Solstice.MOD_VERSION;
        return Solstice.RENDER_MANAGER.getFps() + " FPS | " + Solstice.SERVER_MANAGER.getPing() + "ms | " + String.format("%.2f", Solstice.SERVER_MANAGER.getTickRate()) + " TPS";
    }

    private String getModulesText() {
        long enabledModules = Solstice.MODULE_MANAGER.getModules().stream().filter(Module::isToggled).count();
        return enabledModules + " modules enabled";
    }

    private String getServerText() {
        if (mc.player == null || mc.world == null) return "Main menu";
        if (mc.isInSingleplayer()) return "Singleplayer world";
        return "On " + Solstice.SERVER_MANAGER.getServer();
    }

    private String getSessionText() {
        if (mc.player == null || mc.world == null) {
            String[] uptime = FormattingUtils.formatSeconds((System.currentTimeMillis() - Solstice.UPTIME) / 1000L);
            return "Uptime " + uptime[0] + ":" + uptime[1] + ":" + uptime[2];
        }

        String dimension = getDimensionName();
        return dimension + " " + mc.player.getBlockX() + ", " + mc.player.getBlockZ();
    }

    private String getCombatText() {
        AutoCrystalModule autoCrystalModule = Solstice.MODULE_MANAGER.getModule(AutoCrystalModule.class);
        AuraModule auraModule = Solstice.MODULE_MANAGER.getModule(AuraModule.class);

        PlayerEntity crystalTarget = autoCrystalModule == null ? null : autoCrystalModule.getTarget();
        if (crystalTarget != null) return "Crystal on " + crystalTarget.getName().getString();

        Entity auraTarget = auraModule == null ? null : auraModule.getTarget();
        if (auraTarget != null) return "Aura on " + auraTarget.getName().getString();

        return "No combat target";
    }

    private String getDimensionName() {
        if (mc.world == null) return "Idle";
        if (mc.world.getRegistryKey() == World.NETHER) return "Nether";
        if (mc.world.getRegistryKey() == World.END) return "End";
        return "Overworld";
    }

    private int randomImage() {
        return (int) MathUtils.random(16, 0);
    }

    private int getImageIndex(String mode) {
        return switch (mode) {
            case "Wizard" -> 1;
            case "EvilMushroom" -> 2;
            case "Nett" -> 3;
            case "Yns" -> 4;
            case "Weed" -> 5;
            case "Miku" -> 6;
            case "Rukia" -> 7;
            case "Rei" -> 8;
            case "Nigga" -> 9;
            case "Bladee" -> 10;
            case "VapeV4" -> 11;
            case "Skeletrix" -> 12;
            case "Maxon" -> 13;
            case "ChillGuy" -> 14;
            case "Miku2" -> 15;
            default -> 0;
        };
    }
}
