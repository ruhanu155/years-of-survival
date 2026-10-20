package com.yearofice.client.hud;

import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
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
 * The client smoke for the HUD stack (prototype C1; Definition of done › the client smoke's screenshots). Off unless
 * {@code YEAROFICE_SMOKE=hud}: from the title screen it creates a flat creative world, sets the clock to a May
 * noon, then saves screenshots of the full debug stack, the battery-at-0 state, F1 and the plain HUD at GUI scales 2
 * and 4 to {@code <game dir>/screenshots/hud_<state>_s<scale>.png}, and closes the game. Extra shots can be queued by
 * other smokes through {@link #queue}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HudSmoke {
    private HudSmoke() {}

    static final boolean ON = "hud".equals(System.getenv("YEAROFICE_SMOKE"));

    /** One screenshot: a name and what to set up before it. */
    public record Shot(String name, int scale, Runnable setup) {}

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static int ticks = -1, inWorld = 0, shot = 0, wait = 0;
    private static boolean created = false;

    /** Other smokes add their shots here, before the world loads. */
    public static void queue(Shot s) { SHOTS.add(s); }

    static void plan() {
        Minecraft mc = Minecraft.getInstance();
        for (int scale : new int[] {2, 4}) {
            SHOTS.add(new Shot("full", scale, () -> { HudDebug.set(HudDebug.Mode.FULL); mc.options.hideGui = false; }));
            SHOTS.add(new Shot("battery0", scale, () -> { HudDebug.set(HudDebug.Mode.BATTERY_ZERO); mc.options.hideGui = false; }));
            SHOTS.add(new Shot("f1", scale, () -> { HudDebug.set(HudDebug.Mode.FULL); mc.options.hideGui = true; }));
            // The vitals strip and the body figure from the sample body: a cold, wet, thin body with a frozen foot, a hurt
            // hand, a crooked arm and a lost finger (M4).
            SHOTS.add(new Shot("vitals_strip", scale, () -> { HudDebug.set(HudDebug.Mode.FULL); mc.options.hideGui = false; }));
            SHOTS.add(new Shot("plain", scale, () -> { HudDebug.set(HudDebug.Mode.OFF); mc.options.hideGui = false; }));
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        ticks++;
        if (!created && mc.level == null && ticks > 40) {
            // The first launch shows the accessibility onboarding screen before the title screen; skip past it.
            mc.options.onboardAccessibility = false;
            created = true;
            LevelSettings settings = new LevelSettings("yoi_hud_smoke", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("yoi_hud_smoke", settings, new WorldOptions(3141592L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    new TitleScreen());
            return;
        }
        if (mc.player == null || mc.level == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (mc.screen != null && inWorld < 120) return;
        inWorld++;
        if (inWorld == 10) {
            // GUI scale 4 needs a window of at least 1280 × 960; the tutorial toast would cover the top right.
            org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
            mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.getToasts().clear();
        }
        if (inWorld == 20 && mc.getSingleplayerServer() != null) {
            mc.getSingleplayerServer().execute(() -> {
                long day = ClockService.get().absDay();
                ClockService.get().setNow(day * 864_000L + 12 * 60 * 600L);
            });
            if (SHOTS.isEmpty()) plan();
            else {
                List<Shot> extra = new ArrayList<>(SHOTS);
                SHOTS.clear();
                plan();
                SHOTS.addAll(extra);
            }
        }
        if (inWorld < 120) return;
        if (shot >= SHOTS.size()) {
            if (shot == SHOTS.size()) {
                YearOfIce.LOG.info("SMOKE HUD DONE: {} screenshots", SHOTS.size());
                shot++;
                mc.stop();
            }
            return;
        }
        Shot s = SHOTS.get(shot);
        if (wait == 0) {
            mc.options.guiScale().set(s.scale());
            mc.resizeDisplay();
            s.setup().run();
        }
        wait++;
        if (wait < 15) return;
        String name = "hud_" + s.name() + "_s" + s.scale() + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
        shot++;
        wait = 0;
    }
}
