package com.yearofice.body.debug;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.body.BodyAttachments;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.time.Rate;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The M4 proxy check's player ({@code YEAROFICE_SMOKE=body}; never active otherwise): a scripted survivor who takes the
 * crate kit, then plays two game days at the accelerated clock with no debug commands: works by day, eats three meals
 * and drinks boiled creek water, lies down on bare ground at night and wakes in the morning. Every Messenger line
 * goes to {@code smoke/body_two_days.log}; every vital that turns on (cold, thirst, tiredness, hurt, hunger) must have its
 * explanation line, and the survivor must neither collapse nor die. The client smoke takes the screenshot when
 * {@link #done} is set.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class TwoDaysDriver {
    private TwoDaysDriver() {}

    /** {@code body}: lays and keeps a fire; {@code body_nofire}: the fireless variant (RECONCILE 225). */
    public static final boolean FIRE = "body".equals(System.getenv("YEAROFICE_SMOKE"));
    public static final boolean ON = FIRE || "body_nofire".equals(System.getenv("YEAROFICE_SMOKE"));
    public static volatile boolean done = false;
    /** Set at the first sample with any vital under its warn line, so the client can shoot the strip then. */
    public static volatile boolean vitalsLow = false;
    public static volatile String verdict = "";

    static final long DAYS = 2 * 1440L;
    private static int tick = 0, journalSeen = 0, meals = 0, drinks = 0;
    private static long start = -1, nextSample = 0, lastDayMark = -1;
    private static PrintWriter out;
    private static boolean[] was = new boolean[5];
    private static final String[] FLAG_IDS = {"warmth_low", "water_low", "energy_low", "condition_low", "calories_low"};
    private static final long[] pendingSince = {-1, -1, -1, -1, -1};
    private static final int[] pendingJournal = new int[5];
    private static final List<String> failures = new ArrayList<>();
    private static String lastRefusal = "";
    private static long nextMeal = 0;
    private static final java.util.Map<HudLine, Boolean> SEEN = new java.util.IdentityHashMap<>();
    private static final List<HudLine> EXTRA = new ArrayList<>();
    private static boolean indoors = false;
    private static com.yearofice.fire.FireRingBlockEntity ring;
    private static boolean ringLit;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!ON || done || !ClockService.installed() || !HudBus.installed()) return;
        var server = e.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        tick++;
        if (tick < 200) return;
        try {
            if (start < 0) begin(p, server.getServerDirectory());
            step(p);
        } catch (RuntimeException ex) {
            YearOfIce.LOG.error("Two-day driver failed", ex);
            finish(p, "FAIL driver exception " + ex);
        }
    }

    private static void begin(ServerPlayer p, Path dir) {
        try {
            Path f = dir.resolve(FIRE ? "smoke/body_two_days.log" : "smoke/body_nofire_two_days.log");
            Files.createDirectories(f.getParent());
            out = new PrintWriter(Files.newBufferedWriter(f));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        p.setGameMode(GameType.SURVIVAL);
        // Take the kit from the supply crate by the door, as the player does.
        int moved = 0;
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-24, -6, -24), c.offset(24, 8, 24))) {
            if (p.level().getBlockEntity(q) instanceof BarrelBlockEntity b) {
                b.unpackLootTable(p);
                for (int i = 0; i < b.getContainerSize(); i++) {
                    ItemStack st = b.getItem(i);
                    if (!st.isEmpty()) { p.getInventory().add(st.copy()); b.setItem(i, ItemStack.EMPTY); moved++; }
                }
                if (moved > 0) break;
            }
        }
        ClockService.get().forceRate(Rate.SLEEP);
        start = ClockService.get().nowMinute();
        nextSample = start;
        nextMeal = start + 20;
        BodyState s = BodyAttachments.of(p);
        log("START minute " + start + " day-hour " + hour(start) + ", kit stacks taken " + moved + ", warmth " + fmt(s.warmth));
    }

    static double hour(long m) { return Math.floorMod(m, 1440L) / 60.0; }
    static String fmt(double v) { return String.format(java.util.Locale.ROOT, "%.1f", v); }

    private static void log(String s) {
        out.println(s);
        out.flush();
        YearOfIce.LOG.info("TWODAYS {}", s);
    }

    private static void step(ServerPlayer p) {
        long m = ClockService.get().nowMinute();
        long t = m - start;
        double h = hour(m);
        BodyState s = BodyAttachments.of(p);
        BodyService body = BodyService.get();
        // Messenger lines, as they come.
        List<HudLine> journal = HudBus.get().journal(p);
        for (; journalSeen < journal.size(); journalSeen++) {
            HudLine l = journal.get(journalSeen);
            log(String.format("[t+%4d min %s] LINE %s %s", t, clock(m), l.key(), l.args()));
        }
        // Lines that reached the screen (a live messenger) or wait for the clock: also explanations.
        for (HudLine l : HudBus.get().sent(p)) if (SEEN.put(l, Boolean.TRUE) == null) { EXTRA.add(l); log(String.format("[t+%4d min %s] SENT %s %s", t, clock(m), l.key(), l.args())); }
        for (HudLine l : HudBus.get().held(p)) if (SEEN.put(l, Boolean.TRUE) == null) { EXTRA.add(l); log(String.format("[t+%4d min %s] HELD %s %s", t, clock(m), l.key(), l.args())); }
        if (!p.isAlive()) { finish(p, "FAIL the player died at t+" + t); return; }
        if (s.sos != 0) { finish(p, "FAIL collapse: SOS state " + s.sos + " at t+" + t); return; }
        if (t >= DAYS) { finish(p, ""); return; }

        // The day.
        boolean night = h >= 22 || h < 6;
        if (night) {
            if (!p.isSleeping() && tick % 20 == 0) {
                var r = com.yearofice.body.sleep.Sleep.class.cast(com.yearofice.api.body.SleepService.get()).lieOnGround(p, m);
                if (!r.name().equals(lastRefusal)) { lastRefusal = r.name(); log("[" + clock(m) + "] lie down on the ground: " + r); }
            }
        } else {
            if (p.isSleeping()) body.wake(p, "morning");
            if (h >= 8 && h < 18 && tick % 6 == 0) p.swing(InteractionHand.MAIN_HAND); // a day of work
        }
        // Three meals a day: two pilot breads each, the first soon after the start, then every 5.5 awake hours.
        if (m >= nextMeal && !p.isSleeping() && h >= 6 && h < 22) {
            for (int k = 0; k < 2; k++) {
                for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) {
                    ItemStack st = p.getInventory().getItem(slot);
                    if (!st.isEmpty() && BuiltInRegistries.ITEM.getKey(st.getItem()).getPath().equals("pilot_bread")) {
                        if (body.eat(p, st, 150)) { st.shrink(1); log("[" + clock(m) + "] ate pilot bread"); }
                        break;
                    }
                }
            }
            meals++;
            nextMeal = m + 330;
        }
        // At dusk the survivor goes under the cabin roof.
        if (h >= (FIRE ? 20.5 : 21) && h < 22 && !indoors && !p.isSleeping()) {
            indoors = true;
            BlockPos c = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-14, -2, -14), c.offset(14, 4, 14))) {
                if (p.level().getBlockState(q).isAir() && p.level().getBlockState(q.above()).isAir() && p.level().getBlockState(q.below()).isSolid()
                        && !p.level().canSeeSky(q) && !p.level().canSeeSky(q.above())) {
                    p.teleportTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5);
                    log("[" + clock(m) + "] goes under a roof at " + q.toShortString());
                    if (FIRE) layFire(p, q, m);
                    break;
                }
            }
            if (indoors) log("[" + clock(m) + "] (roof search done)");
        }
        if (h >= 6 && h < 20) indoors = false;
        if (!p.isSleeping() && m / 120 > drinks + start / 120 && h >= 7 && h < 22) {
            drinks = (int) (m / 120 - start / 120);
            body.drink(p, 0.5, WaterPurity.Source.CREEK, true);
            log("[" + clock(m) + "] drank 0.5 L of boiled creek water");
        }

        // The fire is fed through the ring's own api whenever it runs low (the survivor wakes to tend it).
        if (FIRE && ring != null && ringLit && !ring.isRemoved() && tick % 20 == 0 && ring.fire().armloads() < 1.0) {
            var ft = com.yearofice.fire.FireConfig.tuning();
            int fed = 0;
            for (int i = 0; i < 4; i++) if (ring.addPart(new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG), ft, p).ok()) fed++;
            if (fed > 0) {
                if (p.isSleeping()) body.wake(p, "tends the fire");
                log("[" + clock(m) + "] feeds the fire " + fed + " logs, fire " + ring.fire().state());
            }
        }

        // Sample the vitals every 10 game minutes; a vital that turned on needs its line.
        if (m >= nextSample) {
            nextSample = m + 10;
            boolean[] now = {s.warmth < 70, s.water < 60, s.energy < 5, s.condition() < 60, s.balance24h() < -1000};
            for (int i = 0; i < now.length; i++) {
                if (now[i] && !was[i] && pendingSince[i] < 0) {
                    pendingSince[i] = m;
                    pendingJournal[i] = Math.max(0, journalSeen - 4);
                    log("[" + clock(m) + "] VITAL turned on: " + FLAG_IDS[i]);
                }
                if (pendingSince[i] >= 0) {
                    boolean said = false;
                    for (int j = pendingJournal[i]; j < journal.size(); j++) said |= journal.get(j).key().endsWith("." + FLAG_IDS[i]);
                    for (HudLine l : EXTRA) said |= l.key().endsWith("." + FLAG_IDS[i]);
                    if (said) pendingSince[i] = -1;
                    else if (m - pendingSince[i] > 15) {
                        failures.add(FLAG_IDS[i] + " turned on at t+" + (pendingSince[i] - start) + " without a line");
                        pendingSince[i] = -1;
                    }
                }
            }
            was = now;
            for (boolean b : now) if (b) vitalsLow = true;
            int frost = 0, hpMin = 100;
            for (Part q : Part.values()) { frost = Math.max(frost, s.parts.partFrost()[q.ordinal()]); hpMin = Math.min(hpMin, s.parts.hp(q)); }
            if (Math.floorMod(t, 60) < 10) {
                log(String.format("[t+%4d min %s] warmth %s water %s energy %s cond %s kg %s kcal24 %s feels %s air %s expo %d frost %d minHp %d %s",
                        t, clock(m), fmt(s.warmth), fmt(s.water), fmt(s.energy), fmt(s.condition()), fmt(s.kg), fmt(s.balance24h()),
                        fmt(s.lastFeel == null ? Double.NaN : s.lastFeel.totalC()), fmt(s.lastAirC), s.exposureMin, frost, hpMin,
                        p.isSleeping() ? "asleep" : "awake"));
            }
        }
    }

    /** Lays the fire ring beside the sleeping spot, tinder, kindling and logs in order, and lights it with a match. */
    private static void layFire(ServerPlayer p, BlockPos q, long m) {
        var level = p.level();
        for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            BlockPos r = q.offset(o[0], 0, o[1]);
            if (level.getBlockState(r).isAir() && level.getBlockState(r.above()).isAir() && level.getBlockState(r.below()).isSolid()) {
                level.setBlock(r, com.yearofice.fire.FireRegistry.FIRE_RING.get().defaultBlockState(), 3);
                if (!(level.getBlockEntity(r) instanceof com.yearofice.fire.FireRingBlockEntity be)) continue;
                var t = com.yearofice.fire.FireConfig.tuning();
                be.addPart(new ItemStack(com.yearofice.fire.FireRegistry.TINDER_BUNDLE.get()), t, p);
                be.addPart(new ItemStack(com.yearofice.fire.FireRegistry.KINDLING.get()), t, p);
                be.addPart(new ItemStack(com.yearofice.fire.FireRegistry.KINDLING.get()), t, p);
                for (int i = 0; i < 4; i++) be.addPart(new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG), t, p);
                var lit = be.strikeMatch(p, new ItemStack(com.yearofice.fire.FireRegistry.MATCH.get()), 0.0);
                be.puff();
                be.puff();
                ring = be;
                ringLit = lit.ok();
                log("[" + clock(m) + "] lays and lights the fire at " + r.toShortString() + ": " + lit.line() + " (" + be.fire().state() + ")");
                return;
            }
        }
        log("[" + clock(m) + "] no spot for a fire ring");
    }

    static String clock(long m) { long d = Math.floorMod(m, 1440L); return String.format("%02d:%02d", d / 60, d % 60); }

    private static void finish(ServerPlayer p, String failure) {
        if (!failure.isEmpty()) failures.add(failure.replaceFirst("^FAIL ", ""));
        verdict = failures.isEmpty() ? "PASS" : "FAIL " + failures;
        log("RESULT " + verdict + " (lines " + journalSeen + ", meals " + meals + ")");
        out.close();
        done = true;
    }
}
