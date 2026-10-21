package com.yearofice.client.smoke;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.EncounterStateEvent;
import com.yearofice.api.hud.HudBus;
import com.yearofice.client.horizon.PresetDefaults;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.sim.time.Rate;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * {@code YEAROFICE_SMOKE=m6bear} (M6 proxy check; never active otherwise): ten survival slice-valley worlds, seeds
 * 3141592 to 3141601, one after another in one client session. Each: from 1 September the player stands 30 m from a gut
 * pile left on a creek bank (what field dressing leaves, via {@code Butchering.leaveGuts}), the clock runs two game
 * days, and the driver only watches for a bear (no spawning, no commands). A seed passes when a bear reaches the pile
 * (within 9 m, or has scavenged it). The run passes when at least 7 of 10 seeds do. Log: {@code <game dir>/smoke/m6bear.log}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class M6BearSmoke {
    private M6BearSmoke() {}

    static final boolean ON = "m6bear".equals(System.getenv("YEAROFICE_SMOKE"));
    static final int SEEDS = 10;
    static final long BASE = 3141592L;
    static final long RUN_MIN = 2 * 1440L;

    private enum Phase { TITLE, CREATE, LOADING, SETUP, RUN, LEAVE, DONE }

    private static Phase phase = Phase.TITLE;
    private static int ticks = 0, seedIx = Integer.parseInt(System.getenv().getOrDefault("YEAROFICE_BEAR_FROM", "0")), shotWait = -1;
    private static String shotName = null;
    private static PrintWriter out;
    private static int reached = Integer.parseInt(System.getenv().getOrDefault("YEAROFICE_BEAR_REACHED", "0"));
    private static final List<String> results = new ArrayList<>();

    // per-seed server-side state (written from the server thread, read on the client thread)
    private static volatile BlockPos pile, stand;
    private static volatile long startMin;
    private static volatile boolean ready, finished, bearSeen, bearReached, bearShot, encShot;
    private static volatile double minDist = 9999, startDist = -1;
    private static volatile long reachMin = -1;
    private static volatile int linesBefore, linesAfter, encounterEvents, bearsNear60;
    private static volatile String note = "", pileAct = "none";
    private static volatile int pileShot = 0;

    @SubscribeEvent
    public static void onEnc(EncounterStateEvent e) { if (ON) encounterEvents++; }

    static void log(String s) {
        if (out != null) { out.println(s); out.flush(); }
        YearOfIce.LOG.info("M6BEAR {}", s);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        if (!ON || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        ticks++;
        if (out == null) {
            try {
                Path f = mc.gameDirectory.toPath().resolve("smoke/m6bear.log");
                Files.createDirectories(f.getParent());
                out = new PrintWriter(Files.newBufferedWriter(f, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND));
            } catch (IOException ex) { throw new IllegalStateException(ex); }
        }
        switch (phase) {
            case TITLE -> {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) {
                    mc.options.onboardAccessibility = false;
                    mc.setScreen(new TitleScreen());
                    return;
                }
                if (mc.screen instanceof TitleScreen && ticks > 40) { CreateWorldScreen.openFresh(mc, mc.screen); phase = Phase.CREATE; ticks = 0; }
            }
            case CREATE -> {
                if (mc.screen instanceof CreateWorldScreen s && ticks > 20) {
                    var ui = s.getUiState();
                    ui.setName("yoi-smoke-bear-" + seedIx);
                    ui.setSeed(Long.toString(BASE + seedIx));
                    ui.setGameMode(net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    ui.setAllowCommands(false);
                    PresetDefaults.select(ui, "slice_valley");
                    press(s, "selectWorld.create");
                    phase = Phase.LOADING;
                    ticks = 0;
                    ready = false; finished = false; bearSeen = false; bearReached = false; bearShot = false; encShot = false;
                    minDist = 9999; startDist = -1; reachMin = -1; pileAct = "none"; pileShot = 0; encounterEvents = 0; bearsNear60 = 0; note = "";
                }
            }
            case LOADING -> {
                if (mc.screen != null && !(mc.screen instanceof CreateWorldScreen) && ticks % 20 == 0) pressAny(mc.screen);
                if (mc.player != null && mc.level != null && mc.screen == null && mc.getSingleplayerServer() != null) { phase = Phase.SETUP; ticks = 0; }
            }
            case SETUP -> {
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.getTutorial().stop();
                mc.getToasts().clear();
                if (ticks == 20) {
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
                    mc.options.hideGui = false;
                    com.yearofice.client.hud.HudDebug.set(com.yearofice.client.hud.HudDebug.Mode.FULL);
                }
                if (ticks == 200) mc.getSingleplayerServer().execute(() -> setup(mc));
                if (ready) { phase = Phase.RUN; ticks = 0; }
            }
            case RUN -> {
                mc.getToasts().clear();
                if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
                if (ticks % 10 == 0) mc.getSingleplayerServer().execute(() -> watch(mc));
                else mc.getSingleplayerServer().execute(M6BearSmoke::stopCheck); // RECONCILE 312: the 2-day stop is checked every tick, not every half second
                if (shotWait >= 0 && --shotWait == 0 && shotName != null) {
                    Screenshot.grab(mc.gameDirectory, shotName + ".png", mc.getMainRenderTarget(), m -> YearOfIce.LOG.info("Smoke screenshot: {}", m.getString()));
                    shotName = null;
                }
                if (finished && shotName == null) {
                    String r = String.format(java.util.Locale.ROOT, "seed %d: bear seen %s, reached %s (min distance %.0f m, first seen %.0f m, reach at game minute %d), %s",
                            BASE + seedIx, bearSeen, bearReached, minDist, startDist, reachMin, note);
                    log(r);
                    results.add(r);
                    if (bearReached) reached++;
                    phase = Phase.LEAVE;
                    ticks = 0;
                }
            }
            case LEAVE -> {
                if (ticks == 5) {
                    mc.level.disconnect();
                    mc.disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(net.minecraft.network.chat.Component.literal("Saving")));
                }
                if (ticks > 40 && mc.level == null) {
                    seedIx++;
                    if (seedIx >= SEEDS) {
                        boolean pass = reached >= 7;
                        log("RESULT " + (pass ? "PASS" : "FAIL") + " : bear reached the uncleared spot in " + reached + " of " + SEEDS + " seeds (blueprint: 7 of 10 in 2 days)");
                        out.close();
                        phase = Phase.DONE;
                        mc.stop();
                    } else { mc.setScreen(new TitleScreen()); phase = Phase.TITLE; ticks = 0; }
                }
            }
            default -> {}
        }
    }

    private static boolean ground(ServerLevel l, BlockPos p) {
        return l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir() && l.getFluidState(p).isEmpty()
                && l.getBlockState(p.below()).isFaceSturdy(l, p.below(), net.minecraft.core.Direction.UP) && l.getFluidState(p.below()).isEmpty();
    }

    /** Server thread: a creek bank within 140 m, the gut pile on it, the player 30 m away, the clock at 1 September. */
    private static void setup(Minecraft mc) {
        ServerLevel level = mc.getSingleplayerServer().overworld();
        ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        p.setGameMode(GameType.SURVIVAL);
        p.setInvulnerable(true); // a bear that reaches the player must not end the run; the encounter itself is recorded
        BlockPos c = p.blockPosition();
        BlockPos water = null;
        for (int r = 4; r <= 140 && water == null; r += 2) {
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-r, -25, -r), c.offset(r, 25, r))) {
                if (Math.max(Math.abs(q.getX() - c.getX()), Math.abs(q.getZ() - c.getZ())) != r || !level.hasChunkAt(q)) continue;
                if (level.getFluidState(q).is(FluidTags.WATER) && level.getBlockState(q.above()).isAir()) { water = q.immutable(); break; }
            }
        }
        BlockPos spot = null;
        if (water != null) {
            for (int r = 1; r <= 6 && spot == null; r++) for (BlockPos q : BlockPos.betweenClosed(water.offset(-r, -2, -r), water.offset(r, 2, r))) {
                if (ground(level, q)) { spot = q.immutable(); break; }
            }
        }
        if (spot == null) { spot = c; note = "no creek within 140 m: pile left beside the start; "; }
        boolean made = Butchering.leaveGuts(level, spot, SpeciesBook.get("yearofice:moose"));
        pile = made ? spot : spot.north();
        if (!made) { note += "leaveGuts refused; "; }
        // the player 30 m off on open ground, looking at the pile
        BlockPos st = null;
        for (int a = 0; a < 360 && st == null; a += 15) {
            int x = pile.getX() + (int) Math.round(90 * Math.cos(Math.toRadians(a))), z = pile.getZ() + (int) Math.round(90 * Math.sin(Math.toRadians(a)));
            BlockPos g = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (level.hasChunkAt(g) && ground(level, g) && Math.abs(g.getY() - pile.getY()) < 80) st = g;
        }
        stand = st == null ? c : st;
        Vec3 look = Vec3.atCenterOf(pile).subtract(Vec3.atCenterOf(stand));
        float yaw = (float) (Math.toDegrees(Math.atan2(-look.x, look.z)));
        p.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, 10f);
        ClockService clock = ClockService.get();
        clock.setDay(124); // 1 September (day 1 = 1 May)
        clock.forceRate(Rate.SLEEP);
        startMin = clock.nowMinute();
        linesBefore = HudBus.get().sent(p).size();
        log("seed " + (BASE + seedIx) + ": pile " + pile.toShortString() + " (water " + (water == null ? "none" : water.toShortString()) + "), player " + stand.toShortString() + ", start minute " + startMin);
        shotName = "m6bear_guts_s" + seedIx; // the guts pile
        shotWait = seedIx == 0 ? 30 : -1;
        if (seedIx != 0) shotName = null;
        ready = true;
    }

    /** Server thread, every client tick: a run stops at two game days whatever the clock did between the half-second looks. */
    private static void stopCheck() {
        if (finished) return;
        long used = ClockService.get().nowMinute() - startMin;
        if (used >= RUN_MIN) {
            note += "game minutes run " + used + " (stopped at two days)";
            finished = true;
        }
    }

    /** Server thread, every half second: look for bears near the pile; end after two game days. */
    private static void watch(Minecraft mc) {
        if (finished || mc.getSingleplayerServer() == null) return;
        ServerLevel level = mc.getSingleplayerServer().overworld();
        ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        BlockPos pl = pile;
        long used = ClockService.get().nowMinute() - startMin;
        Vec3 pc = Vec3.atCenterOf(pl);
        for (BearEntity b : level.getEntitiesOfClass(BearEntity.class, new AABB(pl).inflate(500))) {
            double d = b.position().distanceTo(pc);
            if (!bearSeen) { bearSeen = true; startDist = d; }
            minDist = Math.min(minDist, d);
            if (d < 60) { pileAct = b.huntDebug(); if (pileShot == 0 && d < 40 && shotName == null) {
                    pileShot = 1;
                    Vec3 look = b.position().add(0, b.getBbHeight() * 0.6, 0).subtract(p.getEyePosition());
                    p.teleportTo(level, p.getX(), p.getY(), p.getZ(), (float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z))));
                    shotName = "m6bear_pile_s" + seedIx;
                    shotWait = 30;
                } }
            if (d < 9 || b.scavenges() >= 1) {
                if (!bearReached) { bearReached = true; reachMin = used; }
            }
            if (b.distanceTo(p) <= 60) {
                bearsNear60++;
                if (!bearShot) {
                    bearShot = true;
                    Vec3 look = b.position().add(0, b.getBbHeight() * 0.6, 0).subtract(p.getEyePosition());
                    p.teleportTo(level, p.getX(), p.getY(), p.getZ(), (float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z))));
                    shotName = "m6bear_arriving_s" + seedIx;
                    shotWait = 30;
                }
            }
        }
        for (var r : com.yearofice.ecosystem.pool.ValleyPool.REACHED) if (r.species().endsWith("bear") || r.species().endsWith("grizzly")) {
            if (!bearReached) { bearReached = true; reachMin = used; note += "ValleyPool.REACHED " + r + "; "; }
        }
        if (bearShot && !encShot && shotName == null) {
            linesAfter = HudBus.get().sent(p).size();
            if (linesAfter > linesBefore || encounterEvents > 0) { encShot = true; shotName = "m6bear_encounter_line_s" + seedIx; shotWait = 20; }
        }
        if (used >= RUN_MIN || bearReached && shotName == null && (bearShot || used > 60)) {
            if (bearsNear60 > 0) note += "player within 60 m of a bear on " + bearsNear60 + " checks, " + (HudBus.get().sent(p).size() - linesBefore) + " new HUD lines, " + encounterEvents + " EncounterStateEvents while within 60 m of the PLAYER; ";
            else note += "no bear within 60 m of the player; ";
            note += "bear act/drive within 60 m of the pile: " + pileAct + "; game minutes run " + used;
            finished = true;
        }
    }

    private static void press(Screen s, String key) {
        for (var child : s.children()) {
            if (child instanceof Button b && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals(key)) { b.onPress(); return; }
        }
    }

    private static void pressAny(Screen s) {
        String n = s.getClass().getSimpleName();
        if (!(n.contains("Confirm") || n.contains("Warning") || n.contains("Backup"))) return;
        for (var child : s.children()) if (child instanceof Button b && b.active) { b.onPress(); return; }
    }
}
