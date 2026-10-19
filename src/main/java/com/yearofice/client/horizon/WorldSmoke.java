package com.yearofice.client.horizon;

import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.WaterStage;
import com.yearofice.worldgen.gen.YoiChunkGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.server.IntegratedServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The world client smoke (Definition of done › the client smoke's screenshots). Off unless the environment
 * variable {@code YEAROFICE_SMOKE=world} is set: then it creates a fresh singleplayer world on the slice valley
 * ({@code YEAROFICE_SMOKE_PRESET}, default {@code slice_valley}; seed {@code YEAROFICE_SMOKE_SEED}, default
 * 3141592), waits for the spawn, and saves screenshots at the outpost spawn, over the outpost, at the nearest creek,
 * at the slice's fishing spot and from the highest ridge near the start, then closes the game. Never in a normal launch.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class WorldSmoke {
    private WorldSmoke() {}

    static final boolean ON = "world".equals(System.getenv("YEAROFICE_SMOKE"));
    static final String PRESET = env("YEAROFICE_SMOKE_PRESET", "slice_valley");
    static final String SEED = env("YEAROFICE_SMOKE_SEED", "3141592");
    static final int SETTLE = Integer.parseInt(env("YEAROFICE_SMOKE_SETTLE", "300"));

    private static String env(String k, String d) {
        String v = System.getenv(k);
        return v == null || v.isEmpty() ? d : v;
    }

    private enum Phase { TITLE, CREATE, LOADING, PLAY, DONE }

    private static Phase phase = Phase.TITLE;
    private static int ticks = 0, shot = -1;
    private static final List<String[]> SHOTS = new ArrayList<>();

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!ON || phase == Phase.DONE) return;
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
                    ui.setName("yoi-smoke-" + PRESET);
                    ui.setSeed(SEED);
                    ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    ui.setAllowCommands(true);
                    boolean ok = PresetDefaults.select(ui, PRESET);
                    YearOfIce.LOG.info("Smoke: preset {} selected {}", PRESET, ok);
                    press(s, "selectWorld.create");
                    phase = Phase.LOADING;
                    ticks = 0;
                }
            }
            case LOADING -> {
                if (mc.screen != null && !(mc.screen instanceof CreateWorldScreen) && ticks % 20 == 0) {
                    // An experimental-settings or other confirm screen: take the first button that goes on.
                    pressAny(mc.screen);
                }
                if (mc.player != null && mc.level != null && mc.screen == null) {
                    phase = Phase.PLAY;
                    ticks = 0;
                }
            }
            case PLAY -> play(mc);
            default -> {}
        }
    }

    private static void play(Minecraft mc) {
        if (ticks == 60) {
            mc.options.hideGui = true;
            command(mc, "time set 6000");
            command(mc, "gamerule doDaylightCycle false");
            command(mc, "weather clear");
            plan(mc);
            shot = 0;
            ticks = 61;
            if (!SHOTS.isEmpty()) command(mc, SHOTS.get(0)[1]);
            return;
        }
        if (shot < 0 || ticks < 61 + SETTLE) return;
        String[] s = SHOTS.get(shot);
        Screenshot.grab(mc.gameDirectory, s[0] + ".png", mc.getMainRenderTarget(), msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
        YearOfIce.LOG.info("Smoke: shot {} at {}", s[0], s[1]);
        shot++;
        ticks = 61;
        if (shot >= SHOTS.size()) {
            YearOfIce.LOG.info("SMOKE WORLD DONE");
            phase = Phase.DONE;
            mc.stop();
            return;
        }
        command(mc, SHOTS.get(shot)[1]);
    }

    private static void plan(Minecraft mc) {
        SHOTS.clear();
        IntegratedServer srv = mc.getSingleplayerServer();
        if (srv == null || !(srv.overworld().getChunkSource().getGenerator() instanceof YoiChunkGenerator g) || g.world() == null) {
            YearOfIce.LOG.warn("Smoke: not a Year of Ice world");
            return;
        }
        ValleyWorld w = g.world();
        int[] sp = w.spawn();
        double[] yaw = {0, 90, 180, 270};
        add("m2_spawn", sp[0] + 0.5, sp[1], sp[2] + 0.5, yaw[sp[3] / 90], 5);
        // Back from the door, looking at the cabin across the pad.
        double back = (yaw[sp[3] / 90] + 180) % 360;
        double bx = -Math.sin(Math.toRadians(yaw[sp[3] / 90])) * 12, bz = Math.cos(Math.toRadians(yaw[sp[3] / 90])) * 12;
        add("m2_outpost", sp[0] + 0.5 + bx, sp[1] + 6, sp[2] + 0.5 + bz, back, 25);
        int[] c = w.nearestWater(sp[0], sp[2], 400, WaterStage.CREEK);
        if (c != null) {
            ValleyWorld.Columns col = w.columns(c[0] + 10, c[1] + 10, 1, 1);
            add("m2_creek", c[0] + 10.5, col.top(0) + 4, c[1] + 10.5, 135, 25);
        }
        if (w.sliceSpot != null) {
            int x = (int) Math.floor(w.sliceSpot.x()), z = (int) Math.floor(w.sliceSpot.z());
            ValleyWorld.Columns col = w.columns(x - 8, z - 8, 1, 1);
            add("m2_fishing_spot", x - 7.5, Math.max(col.top(0), w.columns(x, z, 1, 1).top(0)) + 5, z - 7.5, -45, 30);
        }
        int[] hp = w.highPoint(sp[0], sp[2], 450);
        ValleyWorld.Columns col = w.columns(hp[0], hp[1], 1, 1);
        double dx = sp[0] - hp[0], dz = sp[2] - hp[1];
        double look = Math.toDegrees(Math.atan2(-dx, dz));
        add("m2_ridge", hp[0] + 0.5, col.top(0) + 3, hp[1] + 0.5, look, 15);
    }

    private static void add(String name, double x, double y, double z, double yaw, double pitch) {
        SHOTS.add(new String[] {name, String.format(Locale.ROOT, "tp @s %.1f %.1f %.1f %.1f %.1f", x, y, z, yaw, pitch)});
    }

    private static void command(Minecraft mc, String c) {
        if (mc.player == null) return;
        mc.player.connection.sendCommand(c);
        // Creative flight, so the camera stays where it was put.
        mc.player.getAbilities().flying = true;
        mc.player.onUpdateAbilities();
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
            if (child instanceof Button b && b.active) {
                YearOfIce.LOG.info("Smoke: pressing '{}' on {}", b.getMessage().getString(), s.getClass().getSimpleName());
                b.onPress();
                return;
            }
        }
    }
}
