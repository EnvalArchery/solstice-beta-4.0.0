package me.nullrush.solstice.modules.impl.core;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.RenderOverlayEvent;
import me.nullrush.solstice.events.impl.TickEvent;
import me.nullrush.solstice.gui.api.GuiVisuals;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.modules.RegisterModule;
import me.nullrush.solstice.settings.impl.*;
import me.nullrush.solstice.utils.animations.Animation;
import me.nullrush.solstice.utils.animations.Easing;
import me.nullrush.solstice.utils.color.ColorUtils;
import me.nullrush.solstice.utils.graphics.Renderer2D;
import me.nullrush.solstice.utils.minecraft.EntityUtils;
import me.nullrush.solstice.utils.minecraft.WorldUtils;
import me.nullrush.solstice.utils.text.FormattingUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RegisterModule(name = "HUD", description = "Renders information about the game and the client on the screen.", category = Module.Category.CORE, toggled = true, drawn = false)
public class HUDModule extends Module {
    public CategorySetting watermarkCategory = new CategorySetting("Watermark", "The settings for the client's watermark.");
    public BooleanSetting watermark = new BooleanSetting("Watermark", "Enabled", "Renders the client's name and version at the top left.", new CategorySetting.Visibility(watermarkCategory), true);
    public StringSetting watermarkText = new StringSetting("WatermarkText", "Text", "The client name that will be rendered.", new CategorySetting.Visibility(watermarkCategory), Solstice.MOD_NAME);
    public BooleanSetting watermarkVersion = new BooleanSetting("WatermarkVersion", "Version", "Renders the client's version after the name.", new CategorySetting.Visibility(watermarkCategory), true);
    public BooleanSetting watermarkMinecraftVersion = new BooleanSetting("WatermarkMinecraftVersion", "MinecraftVersion", "Renders the client's minecraft version after the version.", new CategorySetting.Visibility(watermarkCategory), false);
    public BooleanSetting watermarkRevision = new BooleanSetting("WatermarkRevision", "Revision", "Renders the client's git revision next to the version.", new BooleanSetting.Visibility(watermarkVersion, true), true);
    public BooleanSetting watermarkSync = new BooleanSetting("WatermarkSync", "ColorSync", "Uses the client's color for the version.", new CategorySetting.Visibility(watermarkCategory), false);

    public CategorySetting welcomerCategory = new CategorySetting("Welcomer", "The settings for the client's welcomer.");
    public BooleanSetting welcomer = new BooleanSetting("Welcomer", "Enabled", "Renders a nice welcome message directed to you.", new CategorySetting.Visibility(welcomerCategory), true);
    public StringSetting welcomerText = new StringSetting("WelcomerText", "Text", "The message that will be rendered.", new CategorySetting.Visibility(welcomerCategory), "Hello, [username]! :^)");
    public BooleanSetting welcomerSync = new BooleanSetting("WelcomerSync", "ColorSync", "Uses the client's color for the username.", new CategorySetting.Visibility(welcomerCategory), false);

    public CategorySetting moduleListCategory = new CategorySetting("ModuleList", "The settings for the client's list of enabled modules.");
    public BooleanSetting moduleList = new BooleanSetting("ModuleList", "Enabled", "Renders every enabled module in an organized list.", new CategorySetting.Visibility(moduleListCategory), true);
    public NumberSetting guiScale = new NumberSetting("GuiScale", "Scale", "The scale that will be used for HUD rendering.", new CategorySetting.Visibility(moduleListCategory), 1.0f, 0.7f, 1.8f);
    public ModeSetting moduleListFilter = new ModeSetting("ModuleListFilter", "Filter", "The categories that will be rendered on the module list.", new CategorySetting.Visibility(moduleListCategory), "Combat", new String[]{"Combat", "All"});
    public BooleanSetting metaData = new BooleanSetting("MetaData", "Whether or not to show module metadata in the module list.", new CategorySetting.Visibility(moduleListCategory), true);
    public ModeSetting moduleColorMode = new ModeSetting("ModuleColor", "Color", "The color mode for the modules on the list.", new CategorySetting.Visibility(moduleListCategory), "Default", new String[]{"Default", "Rainbow", "Random"});
    public ModeSetting moduleListSorting = new ModeSetting("ModuleListSorting", "Sorting", "The sorting for the modules on the list.", new CategorySetting.Visibility(moduleListCategory), "Width", new String[]{"Width", "Alphabetical"});

    public CategorySetting playerRadarCategory = new CategorySetting("Player Radar", "The settings for the client's list of players in render distance.");
    public BooleanSetting playerRadar = new BooleanSetting("PlayerRadar", "Enabled", "Renders the name of every player in render distance.", new CategorySetting.Visibility(playerRadarCategory), true);
    public NumberSetting playerRadarLimit = new NumberSetting("PlayerRadarLimit", "Limit", "The maximum amount of players that will be listed. Setting it to 0 means removing the limiter.", new CategorySetting.Visibility(playerRadarCategory), 8, 0, 100);
    public ModeSetting playerRadarSorting = new ModeSetting("PlayerRadarSorting", "Sorting", "The sorting for the players on the radar.", new CategorySetting.Visibility(playerRadarCategory), "Distance", new String[]{"None", "Distance", "Alphabetical"});
    public BooleanSetting playerRadarAntiBot = new BooleanSetting("PlayerRadarAntiBot", "AntiBot", "Prevents bots from being listed on the radar.", new CategorySetting.Visibility(playerRadarCategory), true);
    public BooleanSetting playerIcons = new BooleanSetting("PlayerIcons", "Icons", "Renders the player's head icon next to their name.", new CategorySetting.Visibility(playerRadarCategory), true);
    public BooleanSetting playerRadarDistance = new BooleanSetting("PlayerRadarDistance", "Distance", "Renders the distance between you and the player.", new CategorySetting.Visibility(playerRadarCategory), true);
    public BooleanSetting playerRadarEntityID = new BooleanSetting("PlayerRadarEntityID", "EntityID", "Renders the player's entity ID.", new CategorySetting.Visibility(playerRadarCategory), false);
    public BooleanSetting playerRadarGameMode = new BooleanSetting("PlayerRadarGameMode", "GameMode", "Renders the player's current gamemode.", new CategorySetting.Visibility(playerRadarCategory), true);
    public BooleanSetting playerRadarPing = new BooleanSetting("PlayerRadarPing", "Ping", "Renders the player's current latency.", new CategorySetting.Visibility(playerRadarCategory), true);
    public BooleanSetting playerRadarHealth = new BooleanSetting("PlayerRadarHealth", "Health", "Renders the player's current health.", new CategorySetting.Visibility(playerRadarCategory), true);
    public BooleanSetting playerRadarTotems = new BooleanSetting("PlayerRadarTotems", "Totems", "Renders the amount of totems that the player has popped.", new CategorySetting.Visibility(playerRadarCategory), true);

    public CategorySetting itemsCategory = new CategorySetting("Items", "The settings for information about items in your inventory and specific item counters.");
    public BooleanSetting armor = new BooleanSetting("Armor", "Renders the armor you're currently wearing and its status.", new CategorySetting.Visibility(itemsCategory), true);
    public ModeSetting armorDurability = new ModeSetting("ArmorDurability", "Durability", "The way that the durability will be rendered in.", new BooleanSetting.Visibility(armor, true), "Both", new String[]{"None", "Bar", "Percentage", "Both"});
    public BooleanSetting totemCounter = new BooleanSetting("TotemCounter", "Renders the amount of totems that you have in your inventory.", new CategorySetting.Visibility(itemsCategory), true);
    public BooleanSetting crystalCounter = new BooleanSetting("CrystalCounter", "Renders the amount of crystals that you have in your inventory.", new CategorySetting.Visibility(itemsCategory), true);
    public BooleanSetting xpCounter = new BooleanSetting("XPCounter", "Renders the amount of totems that you have in your inventory.", new CategorySetting.Visibility(itemsCategory), true);
    public BooleanSetting counterChatOffset = new BooleanSetting("CounterChatOffset", "ChatOffset", "Offsets the crystal and XP counter's positions whenever the chat is open.", new CategorySetting.Visibility(itemsCategory), false);

    public CategorySetting informationCategory = new CategorySetting("Information", "The settings for information about the game and the client.");
    public BooleanSetting health = new BooleanSetting("Health", "Renders your current health in the middle of the screen.", new CategorySetting.Visibility(informationCategory), false);
    public BooleanSetting ping = new BooleanSetting("Ping", "Renders your current latency to the server in milliseconds.", new CategorySetting.Visibility(informationCategory), true);
    public BooleanSetting tps = new BooleanSetting("TPS", "Renders the server's current tick-rate.", new CategorySetting.Visibility(informationCategory), true);
    public BooleanSetting fps = new BooleanSetting("FPS", "Renders the game's frames per second counter.", new CategorySetting.Visibility(informationCategory), true);
    public BooleanSetting durability = new BooleanSetting("Durability", "Renders your held item durability.", new CategorySetting.Visibility(informationCategory), true);
    public ModeSetting speed = new ModeSetting("Speed", "Renders the speed that you are currently moving at.", new CategorySetting.Visibility(informationCategory), "Kilometers", new String[]{"None", "Meters", "Kilometers"});
    public BooleanSetting uptime = new BooleanSetting("Uptime", "Renders the uptime of the client.", new CategorySetting.Visibility(informationCategory), false);
    public BooleanSetting serverBrand = new BooleanSetting("ServerBrand", "Renders the brand of the server that you are currently on.", new CategorySetting.Visibility(informationCategory), false);
    public BooleanSetting informationSync = new BooleanSetting("InformationSync", "ColorSync", "Uses the client's color for the information elements.", new CategorySetting.Visibility(informationCategory), true);
    public BooleanSetting informationChatOffset = new BooleanSetting("InformationChatOffset", "ChatOffset", "Offsets the rendering when the chat is open.", new CategorySetting.Visibility(informationCategory), true);

    public CategorySetting potionsCategory = new CategorySetting("Potions", "The settings for information about potion effects and their status.");
    public BooleanSetting potions = new BooleanSetting("Potions", "Enabled", "Renders the name and status of every potion effect you have.", new CategorySetting.Visibility(potionsCategory), true);
    public BooleanSetting potionIcons = new BooleanSetting("PotionIcons", "Icons", "Whether or not to render the icons next to the potion's name.", new CategorySetting.Visibility(potionsCategory), true);
    public ModeSetting potionColor = new ModeSetting("PotionColor", "Color", "The color that will be used in rendering the potion's text.", new CategorySetting.Visibility(potionsCategory), "Enhanced", new String[]{"Vanilla", "Enhanced", "Client"});
    public ModeSetting potionSorting = new ModeSetting("PotionSorting", "Sorting", "The sorting for the potion effects rendered.", new CategorySetting.Visibility(potionsCategory), "Alphabetical", new String[]{"None", "Width", "Alphabetical"});
    public ModeSetting vanillaPotions = new ModeSetting("VanillaPotions", "The way that the vanilla potion icons will be handled.", new CategorySetting.Visibility(potionsCategory), "Hide", new String[]{"Keep", "Move", "Hide"});

    public CategorySetting positionCategory = new CategorySetting("Position","The settings for information about your current position and velocity.");
    public BooleanSetting coordinates = new BooleanSetting("Coordinates", "Renders your current coordinates.", new CategorySetting.Visibility(positionCategory), true);
    public BooleanSetting netherCoordinates = new BooleanSetting("NetherCoordinates", "Renders your current coordinates in the alternate dimension.", new BooleanSetting.Visibility(coordinates, true), true);
    public BooleanSetting direction = new BooleanSetting("Direction", "Renders the current direction that you are facing.", new CategorySetting.Visibility(positionCategory), true);
    public BooleanSetting positionSync = new BooleanSetting("PositionSync", "ColorSync", "Uses the client's color for the position elements.", new CategorySetting.Visibility(positionCategory), true);
    public BooleanSetting positionChatOffset = new BooleanSetting("PositionChatOffset", "ChatOffset", "Offsets the text when the chat is open.", new CategorySetting.Visibility(positionCategory), true);

    public CategorySetting colorCategory = new CategorySetting("Color", "The settings for the coloring of the text.");
    public ModeSetting colorMode = new ModeSetting("Color", "The color that will be applied to the text.", new CategorySetting.Visibility(colorCategory), "Default", new String[]{"Default", "Rainbow", "Wave", "Custom"});
    public ColorSetting customColor = new ColorSetting("CustomColor", "The color that will be used for the Custom mode.", new ModeSetting.Visibility(colorMode, "Custom"), ColorUtils.getDefaultColor());
    public ModeSetting rainbowMode = new ModeSetting("Rainbow", "The mode for the HUD Rainbow.", new ModeSetting.Visibility(colorMode, "Rainbow"), "Vertical", new String[]{"Vertical", "Horizontal"});
    public NumberSetting rainbowOffset = new NumberSetting("RainbowOffset", "Offset", "The offset that will be applied to the rainbow.", new ModeSetting.Visibility(colorMode, "Rainbow", "Wave"), 10L, 1L, 50L);
    public BooleanSetting inversion = new BooleanSetting("Inversion", "Inverts primary and secondary colors.", new CategorySetting.Visibility(colorCategory), false);

    public CategorySetting styleCategory = new CategorySetting("Style", "The settings for the HUD layout and surface styling.");
    public ModeSetting hudStyle = new ModeSetting("HudStyle", "Style", "The visual style used by the HUD.", new CategorySetting.Visibility(styleCategory), "Modern", new String[]{"Classic", "Modern"});
    public BooleanSetting hudPanels = new BooleanSetting("HudPanels", "Panels", "Renders subtle surfaces behind HUD elements.", new CategorySetting.Visibility(styleCategory), true);
    public BooleanSetting accentBars = new BooleanSetting("AccentBars", "AccentBars", "Renders accent bars on modern HUD panels.", new CategorySetting.Visibility(styleCategory), true);
    public NumberSetting panelOpacity = new NumberSetting("PanelOpacity", "Opacity", "The opacity of the modern HUD panels.", new CategorySetting.Visibility(styleCategory), 110, 0, 220);
    public NumberSetting panelPadding = new NumberSetting("PanelPadding", "Padding", "The spacing inside modern HUD panels.", new CategorySetting.Visibility(styleCategory), 3, 1, 8);
    public NumberSetting elementSpacing = new NumberSetting("ElementSpacing", "Spacing", "The spacing between stacked HUD entries.", new CategorySetting.Visibility(styleCategory), 2, 0, 8);

    private final Animation potionsAnimation = new Animation(300, Easing.Method.EASE_OUT_CUBIC);
    private final Animation chatAnimation = new Animation(300, Easing.Method.EASE_OUT_CUBIC);
    private float chatOffset;

    private List<ModuleEntry> moduleEntries = new ArrayList<>();
    private List<PlayerEntry> playerEntries = new ArrayList<>();
    private List<PotionEntry> potionEntries = new ArrayList<>();

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (moduleList.getValue()) {
            Comparator<Module> widthComparator = Comparator.comparingInt(m -> -Solstice.FONT_MANAGER.getWidth(getModuleText(m)));
            Comparator<Module> alphabeticalComparator = Comparator.comparing(Module::getName);

            List<ModuleEntry> entries = new ArrayList<>();
            List<Module> modules = Solstice.MODULE_MANAGER.getModules().stream()
                    .filter(module -> module.isToggled() || module.getAnimationOffset().get(0) > 0)
                    .filter(module -> module.drawn.getValue())
                    .filter(module -> moduleListFilter.getValue().equals("All") || module.getCategory() == Category.COMBAT)
                    .sorted(moduleListSorting.getValue().equalsIgnoreCase("Width") ? widthComparator : alphabeticalComparator)
                    .toList();

            for (Module module : modules) {
                String text = getModuleText(module);
                entries.add(new ModuleEntry(module, text));
            }

            moduleEntries = entries;
        }

        if (playerRadar.getValue()) {
            Comparator<AbstractClientPlayerEntity> distanceComparator = Comparator.comparingDouble(p -> mc.player.distanceTo(p));
            Comparator<AbstractClientPlayerEntity> alphabeticalComparator = Comparator.comparing(p -> p.getName().getString());

            List<PlayerEntry> entries = new ArrayList<>();
            var playerStream = mc.world.getPlayers().stream()
                    .filter(p -> p != mc.player)
                    .filter(p -> !playerRadarAntiBot.getValue() || !EntityUtils.isBot(p));

            if (playerRadarSorting.getValue().equalsIgnoreCase("Distance")) {
                playerStream = playerStream.sorted(distanceComparator);
            } else if (playerRadarSorting.getValue().equalsIgnoreCase("Alphabetical")) {
                playerStream = playerStream.sorted(alphabeticalComparator);
            }

            if (playerRadarLimit.getValue().longValue() > 0) {
                playerStream = playerStream.limit(playerRadarLimit.getValue().longValue());
            }

            List<AbstractClientPlayerEntity> players = playerStream.toList();

            for (PlayerEntity player : players) {
                Identifier headTexture = null;

                if (playerIcons.getValue() && mc.getNetworkHandler() != null) {
                    PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getName().getString());
                    if (entry != null) {
                        headTexture = entry.getSkinTextures().texture();
                    }
                }

                String text = player.getName().getString();

                if (playerRadarDistance.getValue()) text += Formatting.WHITE + " " + new DecimalFormat("0.0").format(mc.player.distanceTo(player));
                if (playerRadarEntityID.getValue()) text += Formatting.WHITE + " " + player.getId();
                if (playerRadarGameMode.getValue()) text += Formatting.WHITE + " [" + EntityUtils.getGameModeName(EntityUtils.getGameMode(player)) + "]";
                if (playerRadarPing.getValue()) text += Formatting.WHITE + " " + EntityUtils.getLatency(player) + "ms";
                if (playerRadarHealth.getValue()) text += ColorUtils.getHealthColor(player.getHealth() + player.getAbsorptionAmount()) + " " + new DecimalFormat("0.0").format(player.getHealth() + player.getAbsorptionAmount()) + Formatting.RESET;

                int pops = Solstice.WORLD_MANAGER.getPoppedTotems().getOrDefault(player.getUuid(), 0);
                if (playerRadarTotems.getValue() && pops > 0) text += ColorUtils.getTotemColor(pops) + " -" + pops + Formatting.RESET;

                entries.add(new PlayerEntry(player, text, headTexture));
            }

            playerEntries = entries;
        }

        if (potions.getValue()) {
            Comparator<StatusEffectInstance> widthComparator = Comparator.comparingInt(e -> -Solstice.FONT_MANAGER.getWidth(e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1)));
            Comparator<StatusEffectInstance> alphabeticalComparator = Comparator.comparing(e -> e.getEffectType().value().getName().getString());

            List<PotionEntry> entries = new ArrayList<>();
            var effectStream = mc.player.getStatusEffects().stream();
            if (potionSorting.getValue().equalsIgnoreCase("Width")) {
                effectStream = effectStream.sorted(widthComparator);
            } else if (potionSorting.getValue().equalsIgnoreCase("Alphabetical")) {
                effectStream = effectStream.sorted(alphabeticalComparator);
            }

            List<StatusEffectInstance> effects = effectStream.toList();

            for (StatusEffectInstance effect : effects) {
                String text = getPotionText(effect);
                Sprite sprite = null;

                if (potionIcons.getValue()) sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());

                entries.add(new PotionEntry(text, sprite, potionColor.getValue().equalsIgnoreCase("Vanilla") ? new Color(effect.getEffectType().value().getColor()) : potionColor.getValue().equalsIgnoreCase("Enhanced") ? (EntityUtils.POTION_COLORS.containsKey(effect.getEffectType().value()) ? EntityUtils.POTION_COLORS.get(effect.getEffectType().value()) : new Color(effect.getEffectType().value().getColor())) : null));
            }

            potionEntries = entries;
        }
    }

    @SubscribeEvent
    public void renderWatermark(RenderOverlayEvent event) {
        if (mc.player == null) return;
        beginHudScale(event.getMatrices());

        chatOffset = chatAnimation.get(mc.currentScreen instanceof ChatScreen ? 14 : 0);
        float topY = 2.0f;
        float leftX = 2.0f;

        if (watermark.getValue()) {
            String text = watermarkText.getValue() + (watermarkVersion.getValue() ? (watermarkSync.getValue() ? "" : inversion.getValue() ? Formatting.GRAY : Formatting.WHITE) + " " + Solstice.MOD_VERSION + (watermarkMinecraftVersion.getValue() ? "-mc" + Solstice.MINECRAFT_VERSION : "") + (watermarkRevision.getValue() ? "+" + Solstice.GIT_REVISION + "." + Solstice.GIT_HASH : "") : "");
            drawHudEntry(event.getContext(), text, leftX, topY, false, colorMode.getValue().equals("Rainbow") && rainbowMode.getValue().equals("Horizontal"), null, 0.0f, getHudColor(topY));
            topY += getHudLineStep();
        }

        if (uptime.getValue()) {
            String[] hms = FormattingUtils.formatSeconds((System.currentTimeMillis() - Solstice.UPTIME) / 1000);
            drawHudEntry(event.getContext(), "Uptime " + Formatting.WHITE + hms[0] + ":" + hms[1] + ":" + hms[2], leftX, topY, false, false, informationSync.getValue() ? null : new Color(170, 170, 170), 0.0f, getHudColor(topY + 4.0f));
            topY += getHudLineStep();
        }

        if (welcomer.getValue() && mc.player != null) {
            String text = welcomerText.getValue().replace("[username]", (welcomerSync.getValue() ? "" : inversion.getValue() ? Formatting.GRAY : Formatting.WHITE) + mc.player.getName().getString() + Formatting.RESET);
            float centerX = getScaledHudWidth() / 2.0f - Solstice.FONT_MANAGER.getWidth(text) / 2.0f;
            drawHudEntry(event.getContext(), text, centerX, 6.0f, false, colorMode.getValue().equals("Rainbow") && rainbowMode.getValue().equals("Horizontal"), null, 0.0f, getHudColor(6.0f));
        }

        endHudScale(event.getMatrices());
    }

    @SubscribeEvent
    public void renderModuleList(RenderOverlayEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!moduleList.getValue()) return;
        beginHudScale(event.getMatrices());

        float potionOffset = potionsAnimation.get(!vanillaPotions.getValue().equalsIgnoreCase("Move") || mc.player.getStatusEffects().isEmpty() ? 0 : (EntityUtils.hasNegativeEffects(mc.player) ? 51 : 25));
        float step = getHudLineStep();

        int index = 0;
        for (ModuleEntry entry : moduleEntries) {
            float panelWidth = getHudPanelWidth(entry.text(), 0.0f);
            float x = getScaledHudWidth() - entry.module().getAnimationOffset().get(entry.module().isToggled() ? panelWidth : 0.0f) + getHudPanelInset();
            float y = 2 + potionOffset + (index * step);

            drawModuleText(entry.module(), event.getContext(), entry.text(), x, y, panelWidth);
            index++;
        }

        endHudScale(event.getMatrices());
    }

    @SubscribeEvent
    public void renderPlayerRadar(RenderOverlayEvent event) {
        if (!playerRadar.getValue()) return;
        beginHudScale(event.getMatrices());

        float yBase = Math.max(2 + (Solstice.FONT_MANAGER.getHeight() * 2), 2.0f + getTopLeftStackHeight());
        float step = getHudLineStep();
        int offset = 0;
        for (PlayerEntry entry : playerEntries) {
            float y = yBase + (offset * step);
            float textX = 2 + (entry.headTexture() != null ? Solstice.FONT_MANAGER.getHeight() + getHudPadding() + 3.0f : 0.0f);
            float leadingInset = entry.headTexture() != null ? Solstice.FONT_MANAGER.getHeight() + getHudPadding() + 3.0f : 0.0f;
            Color accent = Solstice.FRIEND_MANAGER.contains(entry.player().getName().getString()) ? Solstice.FRIEND_MANAGER.getDefaultFriendColor() : getHudColor(y + 4.0f);

            drawHudEntry(event.getContext(), entry.text(), textX, y, false, false, Solstice.FRIEND_MANAGER.contains(entry.player().getName().getString()) ? accent : null, leadingInset, accent);

            if (entry.headTexture() != null) {
                int iconX = (int) (2 + (isModernHud() ? getHudPadding() : 0.0f));
                int iconY = (int) (y + (isModernHud() ? 1.0f : 0.0f));
                PlayerSkinDrawer.draw(event.getContext(), entry.headTexture(), iconX, iconY, Solstice.FONT_MANAGER.getHeight(), true, false, Color.WHITE.getRGB());
            }

            offset++;
        }

        endHudScale(event.getMatrices());
    }

    @SubscribeEvent
    public void renderItems(RenderOverlayEvent event) {
        if (mc.player == null) return;

        MatrixStack matrices = event.getMatrices();
        beginHudScale(matrices);

        if (armor.getValue()) {
            int offset = 0;
            for (ItemStack stack : mc.player.getArmorItems()) {
                if (stack.isEmpty()) continue;

                int wateroffset = (mc.player.isSubmergedInWater() || mc.player.getAir() < mc.player.getMaxAir()) ? 10 : 0;

                int x = (int) (getScaledHudWidth() / 2 + 69 - (18 * offset));
                int y = (int) (getScaledHudHeight() - 55 - wateroffset);

                renderItemPlate(event.getContext(), x, y, getArmorAccent(stack));
                event.getContext().drawItem(stack, x, y);
                event.getContext().drawStackOverlay(mc.textRenderer, stack, x, y);

                int damage = stack.getDamage();
                int maxDamage = stack.getMaxDamage();

                if (armorDurability.getValue().equalsIgnoreCase("Percentage") || armorDurability.getValue().equalsIgnoreCase("Both") && maxDamage > 0) {
                    matrices.push();
                    matrices.scale(0.625f, 0.625f, 0.625f);
                    drawText(event.getContext(), (((maxDamage - damage) * 100) / maxDamage) + "%", (int) (((mc.getWindow().getScaledWidth() >> 1) + 70 - (18 * offset)) * 1.6F), (int) ((mc.getWindow().getScaledHeight() - 58 - wateroffset) * 1.6F - 5), false, new Color(1.0f - ((maxDamage - damage) / (float) maxDamage), (maxDamage - damage) / (float) maxDamage, 0));
                    matrices.pop();
                }

                offset++;
            }
        }

        if (totemCounter.getValue()) {
            int totems = mc.player.getInventory().count(Items.TOTEM_OF_UNDYING);
            if (totems > 0) {
                ItemStack stack = new ItemStack(Items.TOTEM_OF_UNDYING);
                int x = (int) ((getScaledHudWidth() / 2) - 9);
                int y = (int) (getScaledHudHeight() - 55 - ((mc.player.isSubmergedInWater() && mc.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) ? 10 : 0));

                renderItemPlate(event.getContext(), x, y, new Color(250, 216, 92));
                event.getContext().drawItem(stack, x, y);
                event.getContext().drawStackOverlay(mc.textRenderer, stack, x, y, String.valueOf(totems));
            }
        }

        boolean renderedXpCounter = false;
        if (xpCounter.getValue()) {
            int experienceBottles = mc.player.getInventory().count(Items.EXPERIENCE_BOTTLE);
            if (experienceBottles > 0) {
                ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
                float x = (getScaledHudWidth() / 2) + 106;
                float y = getScaledHudHeight() - 20 - (counterChatOffset.getValue() ? chatOffset : 0);

                matrices.push();
                matrices.translate(x, y, 0);
                renderItemPlate(event.getContext(), 0, 0, new Color(120, 230, 120));
                event.getContext().drawItem(stack, 0, 0);
                event.getContext().drawStackOverlay(mc.textRenderer, stack, 0, 0, String.valueOf(experienceBottles));
                matrices.pop();

                renderedXpCounter = true;
            }
        }

        if (crystalCounter.getValue()) {
            int crystals = mc.player.getInventory().count(Items.END_CRYSTAL);
            if (crystals > 0) {
                ItemStack stack = new ItemStack(Items.END_CRYSTAL);
                float x = (getScaledHudWidth() / 2) + 106;
                float y = getScaledHudHeight() - (renderedXpCounter ? 40 : 20) - (counterChatOffset.getValue() ? chatOffset : 0);

                matrices.push();
                matrices.translate(x, y, 0);
                renderItemPlate(event.getContext(), 0, 0, new Color(188, 140, 255));
                event.getContext().drawItem(stack, 0, 0);
                event.getContext().drawStackOverlay(mc.textRenderer, stack, 0, 0, String.valueOf(crystals));
                matrices.pop();
            }
        }

        endHudScale(matrices);
    }

    @SubscribeEvent
    public void renderInformation(RenderOverlayEvent event) {
        if (mc.player == null) return;

        MatrixStack matrices = event.getMatrices();
        beginHudScale(matrices);

        int offset = 0;
        float step = getHudLineStep();

        float chatOffset = informationChatOffset.getValue() ? this.chatOffset : 0;

        if (health.getValue()) {
            String text = ColorUtils.getHealthColor(mc.player.getHealth() + mc.player.getAbsorptionAmount()) + new DecimalFormat("0").format(mc.player.getHealth() + mc.player.getAbsorptionAmount());
            if (isModernHud()) {
                float x = getScaledHudWidth() / 2.0f - Solstice.FONT_MANAGER.getWidth(text) / 2.0f;
                drawHudEntry(event.getContext(), text, x, getScaledHudHeight() / 2.0f + 16.0f, false, false, Color.WHITE, 0.0f, getHudColor(getScaledHudHeight() / 2.0f));
            }
            Solstice.FONT_MANAGER.drawTextWithOutline(event.getContext(), text, (int) (getScaledHudWidth() / 2 - Solstice.FONT_MANAGER.getWidth(text) / 2.0f), (int) (getScaledHudHeight() / 2 + 16), Color.WHITE, Color.BLACK);
        }

        if (potions.getValue()) {
            for (PotionEntry entry : potionEntries) {
                float y = getScaledHudHeight() - chatOffset - 2 - Solstice.FONT_MANAGER.getHeight() - (step * offset);
                float x = getScaledHudWidth() - 2 - Solstice.FONT_MANAGER.getWidth(entry.text());
                drawHudEntry(event.getContext(), entry.text(), x, y, true, potionColor.getValue().equals("Client") && colorMode.getValue().equals("Rainbow") && rainbowMode.getValue().equals("Horizontal"), entry.color(), entry.sprite() != null ? Solstice.FONT_MANAGER.getHeight() + getHudPadding() + 3.0f : 0.0f, entry.color() == null ? getHudColor(y) : entry.color());

                if (entry.sprite() != null) {
                    matrices.push();
                    float iconX = getScaledHudWidth() - 2 - Solstice.FONT_MANAGER.getWidth(entry.text()) - Solstice.FONT_MANAGER.getHeight() - 2 - (isModernHud() ? getHudPadding() : 0.0f);
                    matrices.translate(iconX, y - 1, 0);
                    event.getContext().drawSpriteStretched(RenderLayer::getGuiTextured, entry.sprite(), 0, 0, Solstice.FONT_MANAGER.getHeight(), Solstice.FONT_MANAGER.getHeight());
                    matrices.pop();
                }
                offset++;
            }
        }

        List<String> informationEntries = new ArrayList<>();

        if (ping.getValue()) informationEntries.add(getPrimary() + "Ping " + getSecondary() + Solstice.SERVER_MANAGER.getPing() + "ms");
        if (fps.getValue()) informationEntries.add(getPrimary() + "FPS " + getSecondary() + Solstice.RENDER_MANAGER.getFps());
        if (durability.getValue()) informationEntries.add("Durability " + (mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()));
        if (!speed.getValue().equalsIgnoreCase("None")) informationEntries.add(getPrimary() + "Speed " + getSecondary() + new DecimalFormat("0.00").format(EntityUtils.getSpeed(mc.player, speed.getValue().equalsIgnoreCase("Meters") ? EntityUtils.SpeedUnit.METERS : EntityUtils.SpeedUnit.KILOMETERS)) + (speed.getValue().equalsIgnoreCase("Meters") ? "m/s" : "km/h"));
        if (serverBrand.getValue()) informationEntries.add(getPrimary() + "Brand " + getSecondary() + Solstice.SERVER_MANAGER.getServerBrand());

        float tickRate = Solstice.SERVER_MANAGER.getTickRate();
        if (tps.getValue()) informationEntries.add(getPrimary() + "TPS " + getSecondary() + (tickRate > 19.79 ? "20.00" : new DecimalFormat("00.00").format(tickRate)));

        if (!informationEntries.isEmpty()) {
            informationEntries.sort(Comparator.comparingInt(Solstice.FONT_MANAGER::getWidth).reversed());
            for (String text : informationEntries) {
                if (text.startsWith("Durability")) {
                    if (mc.player.getMainHandStack().isDamageable()) {
                        int maxDamage = mc.player.getMainHandStack().getMaxDamage(), damage = mc.player.getMainHandStack().getDamage();
                        String s = String.valueOf(maxDamage - damage);
                        float y = getScaledHudHeight() - chatOffset - 2 - Solstice.FONT_MANAGER.getHeight() + (offset * -step);
                        float combinedWidth = Solstice.FONT_MANAGER.getWidth("Durability ") + Solstice.FONT_MANAGER.getWidth(s);

                        if (isModernHud()) {
                            float x = getScaledHudWidth() - 2 - combinedWidth;
                            renderHudPanel(event.getContext(), x - getHudPadding(), y - 1.0f, x + combinedWidth + getHudPadding(), y + Solstice.FONT_MANAGER.getHeight() + getHudPadding(), getHudColor(y), true);
                        }

                        drawText(event.getContext(), getPrimary() + "Durability ", getScaledHudWidth() - 2 - Solstice.FONT_MANAGER.getWidth("Durability ") - Solstice.FONT_MANAGER.getWidth(s), y, informationSync.getValue() ? null : new Color(170, 170, 170));
                        drawText(event.getContext(), s, getScaledHudWidth() - 2 - Solstice.FONT_MANAGER.getWidth(s), y, false, new Color(1.0f - ((maxDamage - damage) / (float) maxDamage), (maxDamage - damage) / (float) maxDamage, 0));
                        offset++;
                    }
                } else {
                    float y = getScaledHudHeight() - chatOffset - 2 - Solstice.FONT_MANAGER.getHeight() + (offset * -step);
                    drawHudEntry(event.getContext(), text, getScaledHudWidth() - 2 - Solstice.FONT_MANAGER.getWidth(text), y, true, false, informationSync.getValue() ? null : new Color(170, 170, 170), 0.0f, getHudColor(y));
                    offset++;
                }
            }
        }

        endHudScale(matrices);
    }

    @SubscribeEvent
    public void renderCoordinates(RenderOverlayEvent event) {
        if (mc.player == null || mc.world == null) return;
        beginHudScale(event.getMatrices());

        float chatOffset = positionChatOffset.getValue() ? this.chatOffset : 0;
        int offset = 0;
        float step = getHudLineStep();

        if (coordinates.getValue())  {
            String text = getSecondary() + String.valueOf(mc.player.getBlockX()) + (netherCoordinates.getValue() ? Formatting.GRAY + " [" + getSecondary() + WorldUtils.getNetherPosition(mc.player.getBlockX()) + Formatting.GRAY + "]" : "") + (inversion.getValue() || positionSync.getValue() ? Formatting.RESET : Formatting.GRAY) + ", " + getSecondary() + mc.player.getBlockY() + (inversion.getValue() || positionSync.getValue() ? Formatting.RESET : Formatting.GRAY) + ", " + getSecondary() + mc.player.getBlockZ() + (netherCoordinates.getValue() ? Formatting.GRAY + " [" + getSecondary() + WorldUtils.getNetherPosition(mc.player.getBlockZ()) + Formatting.GRAY + "]" : "");

            float y = getScaledHudHeight() - chatOffset - offset - Solstice.FONT_MANAGER.getHeight() - 2;
            drawHudEntry(event.getContext(), text, 2, y, false, false, null, 0.0f, getHudColor(y));
            offset += step;
        }

        if (direction.getValue()) {
            String text = getPrimary() + StringUtils.capitalize(mc.player.getMovementDirection().getName()) + (inversion.getValue() ? getSecondary() : Formatting.GRAY) + " [" + (inversion.getValue() ? getSecondary() : Formatting.WHITE) + WorldUtils.getMovementDirection(mc.player.getMovementDirection()) + (inversion.getValue() ? getSecondary() : Formatting.GRAY) + "]";
            float y = getScaledHudHeight() - chatOffset - offset - Solstice.FONT_MANAGER.getHeight() - 2;
            drawHudEntry(event.getContext(), text, 2, y, false, false, positionSync.getValue() ? null : Color.WHITE, 0.0f, getHudColor(y));
        }

        endHudScale(event.getMatrices());
    }

    private void beginHudScale(MatrixStack matrices) {
        float scale = guiScale.getValue().floatValue();
        matrices.push();
        matrices.scale(scale, scale, 1.0f);
    }

    private void endHudScale(MatrixStack matrices) {
        matrices.pop();
    }

    private float getScaledHudWidth() {
        return mc.getWindow().getScaledWidth() / guiScale.getValue().floatValue();
    }

    private float getScaledHudHeight() {
        return mc.getWindow().getScaledHeight() / guiScale.getValue().floatValue();
    }

    private void drawModuleText(Module module, DrawContext context, String text, float x, float y, float panelWidth) {
        Color color = getHudColor(y);
        if (moduleColorMode.getValue().equalsIgnoreCase("Rainbow")) {
            long index = ((long) y / Solstice.FONT_MANAGER.getHeight()) * (rainbowOffset.getValue().longValue() * 10L);
            color = ColorUtils.getOffsetRainbow(index);
        } else if (moduleColorMode.getValue().equals("Random")) {
            color = ColorUtils.getHashColor(module.getName());
        }

        if (isModernHud()) {
            renderHudPanel(context, x - getHudPanelInset(), y - 1.0f, x - getHudPanelInset() + panelWidth, y + Solstice.FONT_MANAGER.getHeight() + getHudPadding(), color, true);
        }

        drawText(context, text, x, y, (moduleColorMode.getValue().equals("Rainbow") || (moduleColorMode.getValue().equals("Default") && colorMode.getValue().equals("Rainbow"))) && rainbowMode.getValue().equals("Horizontal"), color);
    }

    private void drawHudEntry(DrawContext context, String text, float x, float y, boolean rightAligned, boolean rainbow, Color color, float leadingInset, Color accent) {
        if (color == null) color = getHudColor(y);
        if (accent == null) accent = color;

        if (isModernHud()) {
            float left = x - getHudPanelInset() - leadingInset;
            float right = x + Solstice.FONT_MANAGER.getWidth(text) + getHudPadding();
            renderHudPanel(context, left, y - 1.0f, right, y + Solstice.FONT_MANAGER.getHeight() + getHudPadding(), accent, rightAligned);
        }

        drawText(context, text, x, y, rainbow, color);
    }

    private void renderHudPanel(DrawContext context, float left, float top, float right, float bottom, Color accent, boolean accentRight) {
        if (!hudPanels.getValue()) return;

        Color tonedAccent = GuiVisuals.withAlpha(accent, Math.min(255, panelOpacity.getValue().intValue() + 42));
        Color shadow = new Color(0, 0, 0, Math.min(140, panelOpacity.getValue().intValue() / 2 + 30));
        Color fill = GuiVisuals.withAlpha(GuiVisuals.mix(new Color(8, 12, 22), accent, 0.10f), Math.min(235, panelOpacity.getValue().intValue() + 48));
        Color topGlow = GuiVisuals.withAlpha(GuiVisuals.mix(accent, Color.WHITE, 0.18f), Math.min(180, panelOpacity.getValue().intValue() / 2 + 30));
        Color outline = GuiVisuals.withAlpha(GuiVisuals.mix(accent, Color.WHITE, 0.08f), Math.min(210, panelOpacity.getValue().intValue() + 12));

        Renderer2D.renderQuad(context.getMatrices(), left, top, right, bottom, shadow);
        Renderer2D.renderQuad(context.getMatrices(), left + 1, top + 1, right - 1, bottom - 1, fill);
        Renderer2D.renderGradient(context.getMatrices(), left + 1, top + 1, right - 1, top + Math.min(12.0f, bottom - top - 1.0f), topGlow, new Color(0, 0, 0, 0));
        Renderer2D.renderOutline(context.getMatrices(), left, top, right, bottom, outline);

        if (accentBars.getValue()) {
            if (accentRight) {
                Renderer2D.renderQuad(context.getMatrices(), right - 2, top + 1, right - 1, bottom - 1, tonedAccent);
            } else {
                Renderer2D.renderQuad(context.getMatrices(), left + 1, top + 1, left + 2, bottom - 1, tonedAccent);
            }
        }
    }

    private void renderItemPlate(DrawContext context, float x, float y, Color accent) {
        if (!isModernHud() || !hudPanels.getValue()) return;

        renderHudPanel(context, x - 2.0f, y - 2.0f, x + 18.0f, y + 18.0f, accent, false);
    }

    private Color getArmorAccent(ItemStack stack) {
        if (!stack.isDamageable()) return getHudColor(getScaledHudHeight() - 40.0f);

        float durability = (stack.getMaxDamage() - stack.getDamage()) / (float) stack.getMaxDamage();
        return new Color((int) (255.0f * (1.0f - durability)), (int) (255.0f * durability), 70);
    }

    private boolean isModernHud() {
        return hudStyle.getValue().equalsIgnoreCase("Modern");
    }

    private float getHudPadding() {
        return isModernHud() ? panelPadding.getValue().floatValue() : 0.0f;
    }

    private float getHudPanelInset() {
        return isModernHud() ? getHudPadding() + 1.0f : 0.0f;
    }

    private float getHudLineStep() {
        return Solstice.FONT_MANAGER.getHeight() + elementSpacing.getValue().floatValue() + (isModernHud() ? getHudPadding() + 1.0f : 0.0f);
    }

    private float getHudPanelWidth(String text, float leadingInset) {
        return Solstice.FONT_MANAGER.getWidth(text) + leadingInset + (isModernHud() ? (getHudPadding() * 2.0f) + 2.0f : 0.0f);
    }

    private float getTopLeftStackHeight() {
        float height = 0.0f;
        if (watermark.getValue()) height += getHudLineStep();
        if (uptime.getValue()) height += getHudLineStep();
        return height;
    }

    private void drawText(DrawContext context, String text, float x, float y) {
        drawText(context, text, x, y, null);
    }

    public void drawText(DrawContext context, String text, float x, float y, Color color) {
        drawText(context, text, x, y, colorMode.getValue().equals("Rainbow") && rainbowMode.getValue().equals("Horizontal"), color);
    }

    public void drawText(DrawContext context, String text, float x, float y, boolean rainbow, Color color) {
        if (color == null) color = getHudColor(y);

        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(x, y, 0);

        if (rainbow)  {
            Solstice.FONT_MANAGER.drawRainbowString(context, text, 0, 0, rainbowOffset.getValue().longValue() * 5L);
        } else {
            Solstice.FONT_MANAGER.drawTextWithShadow(context, text, 0, 0, color);
        }

        matrices.pop();
    }

    private Color getHudColor(float offset) {
        if (colorMode.getValue().equalsIgnoreCase("Rainbow")) {
            long index = ((long) offset / Solstice.FONT_MANAGER.getHeight()) * (rainbowOffset.getValue().longValue() * 10L);
            return ColorUtils.getOffsetRainbow(index);
        } else if (colorMode.getValue().equalsIgnoreCase("Wave")) {
            long index = ((long) offset / Solstice.FONT_MANAGER.getHeight()) * (rainbowOffset.getValue().longValue() * 20L);
            return ColorUtils.getOffsetWave(ColorUtils.getGlobalColor(), index);
        } else if (colorMode.getValue().equalsIgnoreCase("Custom")) {
            return ColorUtils.getColor(customColor.getColor(), 255);
        }
        return ColorUtils.getGlobalColor();
    }

    private String getModuleText(Module module) {
        return module.getName() + (module.getMetaData().isEmpty() || !metaData.getValue() ? "" : Formatting.GRAY + " [" + Formatting.WHITE + module.getMetaData() + Formatting.GRAY + "]");
    }

    private String getPotionText(StatusEffectInstance effect) {
        return effect.getEffectType().value().getName().getString() + " " + (effect.getAmplifier() + 1) + " " + Formatting.WHITE + StatusEffectUtil.getDurationText(effect, 1.0F, mc.world.getTickManager().getTickRate()).getString().replace(Text.translatable("effect.duration.infinite").getString(), "**:**");
    }

    private Formatting getPrimary() {
        return inversion.getValue() ? Formatting.GRAY : Formatting.RESET;
    }

    private Formatting getSecondary() {
        return inversion.getValue() ? Formatting.RESET : Formatting.WHITE;
    }

    public record ModuleEntry(Module module, String text) {}
    public record PlayerEntry(PlayerEntity player, String text, Identifier headTexture) {}
    public record PotionEntry(String text, Sprite sprite, Color color) {}
}
