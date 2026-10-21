package com.yearofice.sim.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.yearofice.api.world.ChangeOp;
import com.yearofice.sim.livestock.Care;
import com.yearofice.sim.livestock.Growth;
import com.yearofice.sim.livestock.LivestockTuning;
import com.yearofice.sim.livestock.Spec;
import com.yearofice.world.longterm.LongTermData;
import com.yearofice.world.terrain.TerrainChangeQueue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Test;

/**
 * Five simulated years of an active homestead: a controlled burn every autumn, a wildfire in the wildfire years, an
 * old camp rotting, a beaver dam growing, and goats, reindeer and hens fed from a hay store every hour. The terrain
 * queue must drain inside a game hour at the season budget, and the serialised long-term SavedData must stay under 5 MB.
 */
class FiveYearTest {
    static final LongWorldTuning WORLD = LongWorldTuning.Preset.STANDARD.tuning;
    static final LivestockTuning STOCK = LivestockTuning.Preset.STANDARD.tuning;
    static final int OPS_PER_TICK = SeasonPreset.STANDARD.season.opsPerTick();
    /** Ticks in a game hour at the 24-minute day (the fastest world rate: 20 x 60 x 24 / 24). */
    static final int TICKS_PER_GAME_HOUR = 1200;
    static final int MAX_BURN_OPS = 6000;
    static final long SEED = 20260101L;

    static Spec spec(String id) throws IOException {
        try (Reader r = new InputStreamReader(FiveYearTest.class.getResourceAsStream("/data/yearofice/livestock/" + id + ".json"))) {
            return Spec.parse(JsonParser.parseReader(r).getAsJsonObject());
        }
    }

    static int seq = 0;

    static void submit(TerrainChangeQueue q, long day, ChangeOp.Kind k, int prio, int x, int y, int z, String block) {
        q.submit(new ChangeOp(day, k, prio, new BlockPos(x, y, z), block, false, seq++ & 0xFFFFFF));
    }

    /** The ops of one burn, by the same rules as LongTerm.burn: one tree on a third of the columns, then regrowth. */
    static int burn(TerrainChangeQueue q, int cx, int cz, int radius, long today) {
        int ops = 0;
        for (int r = 0; r <= radius && ops < MAX_BURN_OPS; r++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r || dx * dx + dz * dz > radius * radius) continue;
                    if (ops >= MAX_BURN_OPS) return ops;
                    int x = cx + dx, z = cz + dz;
                    boolean tree = Math.floorMod(x * 31 + z, 3) == 0;
                    if (tree) for (int y = 0; y < 8; y++) { submit(q, today, ChangeOp.Kind.BURN, 0, x, 64 + y, z, "minecraft:air"); ops++; }
                    for (Regrowth.Stage s : Regrowth.Stage.values()) {
                        if (s == Regrowth.Stage.CLEARED || !Regrowth.cellGrows(SEED, x, z, s)) continue;
                        submit(q, Regrowth.stageDay(today, s, WORLD.regrowthTimeScale()), ChangeOp.Kind.REGROW, s.ordinal(), x, 64, z, "yearofice:fireweed");
                        ops++;
                    }
                }
        return ops;
    }

    static int bytes(net.minecraft.world.level.saveddata.SavedData d) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.write(d.save(new CompoundTag(), null), new java.io.DataOutputStream(out));
        return out.size();
    }

    @Test
    void five_years_of_an_active_homestead() throws IOException {
        TerrainChangeQueue q = new TerrainChangeQueue();
        LongTermData colonies = new LongTermData();
        colonies.colonies.add(new LongTermData.Colony(new BlockPos(500, 64, 500), 0, 0, false));
        SplittableRandom rng = new SplittableRandom(SEED);

        // The old camp: 600 rotting blocks (sod roof, log wall, bed) in a 10 x 20 x 3 box, none lost yet.
        boolean[] campAlive = new boolean[600];
        java.util.Arrays.fill(campAlive, true);
        double[] campRate = new double[600];
        for (int i = 0; i < 600; i++) campRate[i] = i % 3 == 0 ? 6.7 : i % 3 == 1 ? 3.5 : 1.5;

        // The stock: 6 goats, 2 reindeer, 12 hens, each on its own care state, fed from a hay store.
        Spec goat = spec("goat"), reindeer = spec("reindeer"), hen = spec("hen");
        List<Spec> specs = new ArrayList<>();
        List<Care.State> states = new ArrayList<>();
        for (int i = 0; i < 6; i++) { specs.add(goat); states.add(Care.State.FRESH); }
        for (int i = 0; i < 2; i++) { specs.add(reindeer); states.add(Care.State.FRESH); }
        for (int i = 0; i < 12; i++) { specs.add(hen); states.add(Care.State.FRESH); }
        double hayKg = 0, debtKg = 0;
        double kid = 0, kidAge = 0;

        int maxDayOps = 0, maxSaved = 0, wildfires = 0, burns = 0, submitted = 0;
        for (long day = 0; day < 5 * 365; day++) {
            int doy = (int) (day % 365), year = (int) (day / 365) + 1;
            int month = com.yearofice.sim.time.Calendar.month(doy);
            // -- the homestead's year --
            if (doy == 153 + 14) { // a controlled burn on Oct 15, 20 m radius
                submitted += burn(q, 0, 0, 20, day);
                burns++;
            }
            if (doy == 100 && Wildfire.wildfireYear(SEED, year, WORLD)) { // a wildfire in the wildfire years: the worst class
                submitted += burn(q, 4000, 4000, 40, day);
                wildfires++;
            }
            if (doy == BeaverDam.GROW_DOY) {
                var c = colonies.colonies.get(0);
                int next = BeaverDam.nextStage(c.stage, 40, false);
                if (next != c.stage) {
                    c.stage = next;
                    int half = 3 + next;
                    for (int dx = -half; dx <= half; dx++) for (int h = 0; h <= next / 2; h++) { submit(q, day, ChangeOp.Kind.DAM, 0, 500 + dx, 64 + h, 500, "yearofice:fallen_log"); submitted++; }
                    for (int p = 0; p < Math.min(400, 60 * next); p++) { submit(q, day, ChangeOp.Kind.POND, 0, 500 + p % 20, 64, 501 + p / 20, "minecraft:water"); submitted++; }
                }
                c.lastGrowDay = day;
            }
            if (doy == 153) { // the camp ages a year each autumn
                for (int i = 0; i < 600; i++) if (campAlive[i] && OldCamp.lost(SEED, i, 64, i / 20, campRate[i] * WORLD.decayRateScale(), 1)) {
                    campAlive[i] = false;
                    submit(q, day, ChangeOp.Kind.DECAY, 0, i, 64, i / 20, "minecraft:air");
                    submitted++;
                }
            }
            // -- the stock, an hour at a time --
            if (day == 0) hayKg += 1500;   // last year's stack
            if (doy == 100) hayKg += 7000; // the hay cut in early August, a full winter and spring
            for (int hour = 0; hour < 24; hour++) {
                double outsideC = 5 + 25 * Math.cos(2 * Math.PI * (doy - 80) / 365.0) - 8;
                for (int a = 0; a < states.size(); a++) {
                    Spec sp = specs.get(a);
                    double ration = sp.feedKgPerDay() / 24;
                    double browse = month >= 5 && month <= 9 ? ration * 0.5 : 0;
                    double eat = Math.min(hayKg, ration - browse);
                    hayKg -= eat;
                    debtKg += ration - browse - eat;
                    double fedFraction = (browse + eat) / ration;
                    states.set(a, Care.step(states.get(a), sp, STOCK, fedFraction, 1.0, Math.max(outsideC, sp.comfortC() + 5)));
                }
            }
            if (doy == 120) { kid = 0; kidAge = 0; }
            kidAge += 1;
            kid = Growth.dayStep(goat, STOCK, kid, kidAge, states.get(0).fed(), true);
            // -- the queue: what is due is applied at the season budget --
            int due = 0;
            List<ChangeOp> all = new ArrayList<>();
            for (long chunk : new ArrayList<>(q.pendingChunks())) {
                var c = new net.minecraft.world.level.ChunkPos(chunk);
                List<ChangeOp> d = q.due(c, day);
                due += d.size();
                all.addAll(d);
                q.done(c, d);
            }
            maxDayOps = Math.max(maxDayOps, due);
            int ticks = (due + OPS_PER_TICK - 1) / OPS_PER_TICK;
            assertTrue(ticks <= TICKS_PER_GAME_HOUR, "day " + day + ": " + due + " due ops need " + ticks + " ticks at " + OPS_PER_TICK + " a tick, over a game hour");
            if (doy == 364) {
                int saved = Math.max(bytes(q), bytes(colonies));
                maxSaved = Math.max(maxSaved, saved);
                assertTrue(bytes(q) < 5 * 1024 * 1024, "year " + year + ": the terrain queue is " + bytes(q) + " bytes");
            }
            for (Care.State s : states) assertTrue(!s.dead(), "day " + day + ": an animal died with hay in the store (" + hayKg + " kg)");
        }
        System.out.printf("five_years: burns=%d wildfires=%d submitted=%d max_due_in_a_day=%d max_saved_bytes=%d pending=%d hay_left_kg=%.0f debt_kg=%.0f%n",
                burns, wildfires, submitted, maxDayOps, maxSaved, q.totalPending(), hayKg, debtKg);
        assertEquals(5, burns, "a burn every autumn");
        assertTrue(maxSaved < 5 * 1024 * 1024, "SavedData under 5 MB: " + maxSaved);
        assertTrue(kid >= 0.5, "a kid born in year five has grown: " + kid);
        // Everything dated before today is gone; what is left is regrowth still to come (up to the 20-year stage).
        assertTrue(q.totalPending() < 40_000, "pending regrowth is bounded: " + q.totalPending());
    }
}
