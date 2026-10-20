package com.yearofice.client.body;

import com.yearofice.body.debug.TwoDaysDriver;
import com.yearofice.client.horizon.PresetDefaults;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client half of the M4 proxy check ({@code YEAROFICE_SMOKE=body}): creates a survival slice-valley world with
 * commands off, lets {@link TwoDaysDriver} play two game days on the server, then saves the vitals strip as
 * {@code <game dir>/screenshots/body_two_days_vitals.png} and closes the game. Never in a normal launch.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class BodySmoke {
    private BodySmoke() {}

    private static int lowShotAt = -1, shotAt = -1;
    private static String pendingShot = null;

    private enum Phase { TITLE, CREATE, LOADING, PLAY, SHOT, DONE }

    private static Phase phase = Phase.TITLE;
    private static int ticks = 0;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!(TwoDaysDriver.ON || com.yearofice.body.debug.MonthDriver.ON) || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        ticks++;
        switch (phase) {
            case TITLE -> {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) {
                    mc.options.onboardAccessibility = false;
                    mc.setScreen(new TitleScreen());
                    return;
                }
                if (mc.screen instanceof TitleScreen && ticks > 40) {
                    CreateWorldScreen.openFresh(mc, mc.screen);
                    phase = Phase.CREATE;
                    ticks = 0;
                }
            }
            case CREATE -> {
                if (mc.screen instanceof CreateWorldScreen s && ticks > 20) {
                    WorldCreationUiState ui = s.getUiState();
                    ui.setName(com.yearofice.body.debug.MonthDriver.ON ? "yoi-smoke-month" : "yoi-smoke-body");
                    ui.setSeed("3141592");
                    ui.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    ui.setAllowCommands(false);
                    PresetDefaults.select(ui, "slice_valley");
                    press(s, "selectWorld.create");
                    phase = Phase.LOADING;
                    ticks = 0;
                }
            }
            case LOADING -> {
                if (mc.screen != null && !(mc.screen instanceof CreateWorldScreen) && ticks % 20 == 0) pressAny(mc.screen);
                if (mc.player != null && mc.level != null && mc.screen == null) { phase = Phase.PLAY; ticks = 0; }
            }
            case PLAY -> {
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.getTutorial().stop();
                mc.getToasts().clear(); // the tutorial toast sits where the strip is
                if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
                if (ticks == 20) {
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
                    mc.options.guiScale().set(2);
                    mc.options.hideGui = false;
                    mc.getToasts().clear();
                }
                var cb = com.yearofice.client.hud.ClientBody.get();
                if (lowShotAt < 0 && cb != null && !mc.player.isSleeping() && (com.yearofice.client.hud.ClientBody.warmthWarn(cb) || com.yearofice.client.hud.ClientBody.waterWarn(cb)
                        || com.yearofice.client.hud.ClientBody.caloriesWarn(cb) || com.yearofice.client.hud.ClientBody.energyWarn(cb)
                        || com.yearofice.client.hud.ClientBody.conditionWarn(cb))) lowShotAt = ticks + 5;
                if (lowShotAt >= 0 && ticks == lowShotAt) {
                    YearOfIce.LOG.info("Smoke strip state: warmth {}, calories {}, energy {}, battery {}", cb == null ? -1 : cb.warmth(),
                            cb == null ? -1 : cb.balance24h(), cb == null ? -1 : cb.energy(), cb == null ? -1 : cb.battery());
                    Screenshot.grab(mc.gameDirectory, "body_two_days_vitals.png", mc.getMainRenderTarget(),
                            msg -> YearOfIce.LOG.info("Smoke screenshot (a vital under its warn line): {}", msg.getString()));
                }
                String want = com.yearofice.body.debug.MonthDriver.shot;
                if (want != null) {
                    com.yearofice.body.debug.MonthDriver.shot = null;
                    pendingShot = want;
                    shotAt = ticks + 6;
                    // the strip is drawn in its full form for the frame (display mode only; no gameplay command)
                    com.yearofice.client.hud.HudDebug.set(com.yearofice.client.hud.HudDebug.Mode.FULL);
                }
                if (pendingShot != null && ticks >= shotAt) {
                    String n = pendingShot;
                    pendingShot = null;
                    Screenshot.grab(mc.gameDirectory, n + ".png", mc.getMainRenderTarget(),
                            msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
                }
                if (TwoDaysDriver.done || com.yearofice.body.debug.MonthDriver.done) { phase = Phase.SHOT; ticks = 0; }
            }
            case SHOT -> {
                if (ticks == 40) {
                    Screenshot.grab(mc.gameDirectory, (com.yearofice.body.debug.MonthDriver.ON ? "m5_month_end.png" : "body_two_days_end.png"), mc.getMainRenderTarget(),
                            msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
                }
                if (ticks == 80) {
                    YearOfIce.LOG.info("SMOKE BODY DONE: {}", com.yearofice.body.debug.MonthDriver.ON ? com.yearofice.body.debug.MonthDriver.verdict : TwoDaysDriver.verdict);
                    phase = Phase.DONE;
                    mc.stop();
                }
            }
            default -> {}
        }
    }

    private static void press(Screen s, String key) {
        for (var child : s.children()) {
            if (child instanceof Button b && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().equals(key)) {
                b.onPress();
                return;
            }
        }
        YearOfIce.LOG.warn("Smoke: no {} button on {}", key, s.getClass().getSimpleName());
    }

    private static void pressAny(Screen s) {
        String n = s.getClass().getSimpleName();
        if (!(n.contains("Confirm") || n.contains("Warning") || n.contains("Backup"))) return;
        for (var child : s.children()) {
            if (child instanceof Button b && b.active) { b.onPress(); return; }
        }
    }
}
