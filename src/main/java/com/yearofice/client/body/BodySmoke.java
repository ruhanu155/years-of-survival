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

    private enum Phase { TITLE, CREATE, LOADING, PLAY, SHOT, DONE }

    private static Phase phase = Phase.TITLE;
    private static int ticks = 0;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!TwoDaysDriver.ON || phase == Phase.DONE) return;
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
                    ui.setName("yoi-smoke-body");
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
                if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
                if (ticks == 20) {
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
                    mc.options.guiScale().set(2);
                    mc.options.hideGui = false;
                    mc.getToasts().clear();
                }
                if (TwoDaysDriver.done) { phase = Phase.SHOT; ticks = 0; }
            }
            case SHOT -> {
                if (ticks == 40) {
                    Screenshot.grab(mc.gameDirectory, "body_two_days_vitals.png", mc.getMainRenderTarget(),
                            msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
                }
                if (ticks == 80) {
                    YearOfIce.LOG.info("SMOKE BODY DONE: {}", TwoDaysDriver.verdict);
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
