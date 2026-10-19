package com.yearofice.client.horizon;

import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.time.Rate;
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
 * 3141592), waits for the spawn, sets the clock to 12:00 on May 1, and saves screenshots at the outpost spawn, over
 * the outpost, at the nearest creek, at the slice's fishing spot and from the highest ridge near the start, each once
 * its view has loaded, then closes the game. Never in a normal launch.
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
            noon(mc);
            command(mc, "weather clear");
            plan(mc);
            shot = 0;
            ticks = 61;
            if (!SHOTS.isEmpty()) command(mc, SHOTS.get(0)[1]);
            return;
        }
        if (shot >= 0 && mc.player != null && !mc.player.getAbilities().flying) {
            // Creative flight, so the camera stays where it was put (landing turns it off).
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        }
        if (shot >= 0 && ticks == 61 + SETTLE - 40) command(mc, SHOTS.get(shot)[1]); // again, after any drift
        if (shot < 0 || ticks < 61 + SETTLE) return;
        // Then wait for the view to finish loading: every chunk around the camera on the client and no section
        // left to build, held for a second. Shooting earlier catches the load frontier, where a crown that hangs
        // over from a chunk the server has made but not yet sent shows without its trunk.
        if (!loaded(mc) && ticks < 61 + SETTLE + MAX_WAIT) { ready = 0; return; }
        if (++ready < 20 && ticks < 61 + SETTLE + MAX_WAIT) return;
        if (ticks >= 61 + SETTLE + MAX_WAIT) YearOfIce.LOG.warn("Smoke: shot {} taken before the view finished loading", SHOTS.get(shot)[0]);
        ready = 0;
        String[] s = SHOTS.get(shot);
        Screenshot.grab(mc.gameDirectory, s[0] + ".png", mc.getMainRenderTarget(), msg -> YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()));
        YearOfIce.LOG.info("Smoke: shot {} at {} ({} ticks)", s[0], s[1], ticks - 61);
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

    /** Most ticks a shot waits past the settle for its view to load. */
    static final int MAX_WAIT = 1800;
    private static int ready = 0;

    /** Every chunk within the render distance less two around the camera is on the client, and no section is queued. */
    private static boolean loaded(Minecraft mc) {
        if (mc.level == null || mc.player == null) return false;
        int r = Math.max(2, mc.options.getEffectiveRenderDistance() - 2);
        int cx = mc.player.chunkPosition().x, cz = mc.player.chunkPosition().z;
        for (int dz = -r; dz <= r; dz++) for (int dx = -r; dx <= r; dx++) {
            if (dx * dx + dz * dz > r * r) continue;
            if (!mc.level.getChunkSource().hasChunk(cx + dx, cz + dz)) return false;
        }
        return mc.levelRenderer.hasRenderedAllSections();
    }

    /**
     * Daylight for every shot: the game clock (ClockService, which drives the sky) to 12:00 on May 1, and the world
     * rate pinned to close (6 game seconds a real second), so the light hardly moves while the shots are taken.
     * The integrated server owns the clock, so this runs on its thread.
     */
    private static void noon(Minecraft mc) {
        IntegratedServer srv = mc.getSingleplayerServer();
        if (srv == null) return;
        srv.execute(() -> {
            if (!ClockService.installed()) return;
            ClockService c = ClockService.get();
            c.setNow(12 * 60 * Calendar.DS_PER_MINUTE);
            c.forceRate(Rate.CLOSE);
            YearOfIce.LOG.info("Smoke: clock set to {}", Calendar.format(c.nowDs()));
        });
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
        double bx = -Math.sin(Math.toRadians(yaw[sp[3] / 90])) * 10, bz = Math.cos(Math.toRadians(yaw[sp[3] / 90])) * 10;
        add("m2_outpost", sp[0] + 0.5 + bx, sp[1] + 20, sp[2] + 0.5 + bz, back, 55);
        int[] c = w.nearestWater(sp[0], sp[2], 400, WaterStage.CREEK);
        if (c != null) {
            ValleyWorld.Columns col = w.columns(c[0] + 10, c[1] + 10, 1, 1);
            add("m2_creek", c[0] + 10.5, col.top(0) + 16, c[1] + 10.5, 135, 45);
        }
        if (w.sliceSpot != null) {
            int x = (int) Math.floor(w.sliceSpot.x()), z = (int) Math.floor(w.sliceSpot.z());
            ValleyWorld.Columns col = w.columns(x - 8, z - 8, 1, 1);
            add("m2_fishing_spot", x - 7.5, Math.max(col.top(0), w.columns(x, z, 1, 1).top(0)) + 5, z - 7.5, -45, 30);
        }
        add("m2_valley", sp[0] + 0.5 - 40, sp[1] + 45, sp[2] + 0.5 + 60, 210, 22);
        int[] hp = w.highPoint(sp[0], sp[2], 450);
        ValleyWorld.Columns col = w.columns(hp[0], hp[1], 1, 1);
        double dx = sp[0] - hp[0], dz = sp[2] - hp[1];
        double look = Math.toDegrees(Math.atan2(-dx, dz));
        add("m2_ridge", hp[0] + 0.5, col.top(0) + 12, hp[1] + 0.5, look, 20);
    }

    private static void add(String name, double x, double y, double z, double yaw, double pitch) {
        SHOTS.add(new String[] {name, String.format(Locale.ROOT, "tp @s %.1f %.1f %.1f %.1f %.1f", x, y, z, yaw, pitch)});
    }

    private static void command(Minecraft mc, String c) {
        if (mc.player == null) return;
        mc.player.connection.sendCommand(c);
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
