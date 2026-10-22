package com.yearofice.client.smoke;

import com.yearofice.body.clothing.ClothingSlots;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.Layer;
import com.yearofice.sim.body.Zone;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * {@code YEAROFICE_SMOKE=wearlook}: a flat creative world, the player in the start kit seen from the front (third person),
 * then swapped into the winter set, then the inventory screen with every garment icon. Saves {@code screenshots/wear_*.png}
 * and stops. Never active otherwise.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class WearLookSmoke {
    private WearLookSmoke() {}

    static final boolean ON = "wearlook".equals(System.getenv("YEAROFICE_SMOKE"));
    private static final String[] WINTER = {"parka", "hide_pants", "mukluks", "fur_mittens", "fur_hat", "hare_fur_liners"};
    private static final String[] ALL = {"fleece_jacket", "rain_shell", "work_pants", "wool_socks", "hiking_boots", "wool_beanie", "work_gloves",
            "parka", "hide_pants", "mukluks", "fur_mittens", "fur_hat", "hare_fur_liners"};
    private static int ticks = -1, inWorld = 0;
    private static boolean created = false;

    private static ServerPlayer sp(Minecraft mc) { return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0); }

    private static void wearWinter(ServerPlayer p) {
        for (Zone z : Zone.values()) for (Layer l : Layer.values()) {
            var a = ClothingSlots.armor(z, l);
            if (a != null) p.setItemSlot(a, ItemStack.EMPTY);
            else com.yearofice.item.inventory.PackSlots.set(p, ClothingSlots.curios(z, l), 0, ItemStack.EMPTY);
        }
        for (String id : WINTER) {
            ItemStack st = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, id)));
            var w = ClothingSlots.row(st);
            if (w == null) { YearOfIce.LOG.warn("wearlook: no wear row for {}", id); continue; }
            ClothingSlots.put(p, Zone.valueOf(w.zone().toUpperCase(java.util.Locale.ROOT)), Layer.valueOf(w.layer().toUpperCase(java.util.Locale.ROOT)), st);
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "wear_" + name + ".png", mc.getMainRenderTarget(), msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        ticks++;
        if (!created && mc.level == null && ticks > 40) {
            mc.options.onboardAccessibility = false;
            created = true;
            LevelSettings settings = new LevelSettings("yoi_wearlook", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("yoi_wearlook", settings, new WorldOptions(31415L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    new TitleScreen());
            return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null && inWorld < 120) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        inWorld++;
        if (inWorld == 10) {
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.getToasts().clear();
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer p = sp(mc);
                p.teleportTo(mc.getSingleplayerServer().overworld(), 0.5, -60, 0.5, 0, 0);
                p.getInventory().clearContent();
                for (String id : ALL) p.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, id))));
            });
        }
        if (inWorld == 120) shot(mc, "start_kit");
        if (inWorld == 130) mc.getSingleplayerServer().execute(() -> wearWinter(sp(mc)));
        if (inWorld == 200) shot(mc, "winter_set");
        if (inWorld == 210) { mc.options.hideGui = false; mc.setScreen(new InventoryScreen(mc.player)); }
        if (inWorld == 260) shot(mc, "icons");
        if (inWorld == 280) {
            YearOfIce.LOG.info("SMOKE WEARLOOK DONE: 3 screenshots");
            mc.stop();
        }
    }
}
