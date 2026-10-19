package com.yearofice.worldgen;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 1, Seed ({@code stageName "seed"}). Turns Minecraft's level seed and an attempt number into the
 * frozen world parameters record. Under 1 ms.
 *
 * <p>Seed chain (World › Seed variables): {@code seed = hash(levelSeed, "yearofice")},
 * {@code attemptSeed = hash(seed, k)}, and every stage draws from
 * {@code Mulberry32(StageHash.stageSeed(attemptSeed, stageName))}. Seeds are written unsigned in the
 * hashed strings, so they read the same as {@code /yoi info} prints them.
 */
public final class SeedStage {
    public static final String STAGE = "seed";

    private SeedStage() {}

    /** Our seed from Minecraft's level seed. */
    public static int ourSeed(long levelSeed) { return StageHash.stageSeed(levelSeed, "yearofice"); }

    /** The seed of attempt k. */
    public static int attemptSeed(int ourSeed, int attempt) {
        return StageHash.stageSeed(Integer.toUnsignedLong(ourSeed), Integer.toString(attempt));
    }

    /** The RNG seed of a named stage within an attempt. */
    public static int stageSeed(int attemptSeed, String stageName) {
        return StageHash.stageSeed(Integer.toUnsignedLong(attemptSeed), stageName);
    }

    /** A fresh stream for a named stage. Each stage owns its stream; no stage reads another's. */
    public static Mulberry32 rng(int attemptSeed, String stageName) {
        return new Mulberry32(stageSeed(attemptSeed, stageName));
    }

    /** Region roles (Pipeline at a glance › Region roles). */
    public enum RegionRole { CORE, MOUNTAINS, ALPINE_TUNDRA, CANYON, MINING_CREEK, BIG_LAKE, MIXED }

    /** Index of the core, B3: column B (1), row 3 (2), as row * 4 + column. */
    public static final int CORE_REGION = 2 * 4 + 1;

    public static String regionCode(int index) { return "" + (char) ('A' + index % 4) + (index / 4 + 1); }

    /**
     * The frozen parameters record. The archetype comes from {@link #drawArchetype}; the stage stream then
     * draws, in this fixed order: mirror, entry offset, exit offset, virtual catchment, hare phase, then the
     * region-role shuffle.
     */
    public record SeedParams(long levelSeed, int seed, int attempt, int attemptSeed, Archetype archetype,
                             boolean mirror, char entryEdge, double entryOffsetM, char exitEdge,
                             double exitOffsetM, double virtualCatchmentKm2, int harePhase,
                             List<RegionRole> regionRoles) {

        /** Stage 1's check: every value inside its range and every role rule met. Empty when it passes. */
        public List<String> check(WorldgenParams p) {
            List<String> fails = new ArrayList<>();
            double size = p.grid().coreSizeM();
            double lo = p.riverPath().bandMinFraction() * size, hi = p.riverPath().bandMaxFraction() * size;
            if (entryOffsetM < lo || entryOffsetM > hi) fails.add("seed: entry offset " + entryOffsetM);
            if (exitOffsetM < lo || exitOffsetM > hi) fails.add("seed: exit offset " + exitOffsetM);
            if (entryEdge == exitEdge) fails.add("seed: entry and exit on one edge");
            if (!p.seed().virtualCatchmentKm2().contains(virtualCatchmentKm2)) fails.add("seed: catchment");
            if (harePhase < 0 || harePhase >= p.seed().hareCycleYears()) fails.add("seed: hare phase");
            if (regionRoles.size() != 16) fails.add("seed: region count");
            int lakes = 0, tundra = 0, canyon = 0, mining = 0;
            for (int i = 0; i < regionRoles.size(); i++) {
                RegionRole r = regionRoles.get(i);
                if (i < 4 && r != RegionRole.MOUNTAINS) fails.add("seed: north edge " + regionCode(i) + " is " + r);
                if (i == CORE_REGION && r != RegionRole.CORE) fails.add("seed: core role");
                if (r == RegionRole.BIG_LAKE) lakes++;
                if (r == RegionRole.ALPINE_TUNDRA) tundra++;
                if (r == RegionRole.CANYON) canyon++;
                if (r == RegionRole.MINING_CREEK) mining++;
            }
            int wantLakes = archetype == Archetype.LAKE_VALLEY ? 1 : 2;
            if (lakes != wantLakes) fails.add("seed: big lakes " + lakes);
            if (tundra < 1 || canyon < 1 || mining < 1) fails.add("seed: missing a guaranteed role");
            return fails;
        }
    }

    /**
     * The valley archetype: equal odds of 4, stratified. Level seeds fall in blocks of four consecutive values
     * ({@code floorDiv(levelSeed, 4)}); each block gets a seeded order of the four archetypes from
     * {@code Mulberry32(stageSeed(block, "archetype:" + k))}, and a seed takes the entry at its slot. Any one
     * seed still has a 1 in 4 chance of each, rerolls (k + 1) draw a fresh order, and every run of consecutive
     * seeds holds each archetype within one block of 25%: the M0 check "over 200 seeds each archetype appears
     * 25% ±5%" holds by construction instead of by luck.
     */
    public static Archetype drawArchetype(long levelSeed, int attempt) {
        long block = Math.floorDiv(levelSeed, 4L);
        int slot = (int) Math.floorMod(levelSeed, 4L);
        Mulberry32 r = new Mulberry32(StageHash.stageSeed(block, "archetype:" + attempt));
        int[] order = {0, 1, 2, 3};
        for (int i = 3; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = order[i]; order[i] = order[j]; order[j] = t;
        }
        return Archetype.values()[order[slot]];
    }

    public static SeedParams run(long levelSeed, int attempt, WorldgenParams p) {
        int seed = ourSeed(levelSeed);
        int attemptSeed = attemptSeed(seed, attempt);
        Mulberry32 rng = rng(attemptSeed, STAGE);

        Archetype archetype = drawArchetype(levelSeed, attempt);
        boolean mirror = rng.nextDouble() < 0.5;
        double size = p.grid().coreSizeM();
        double lo = p.riverPath().bandMinFraction() * size, hi = p.riverPath().bandMaxFraction() * size;
        double entry = lo + (hi - lo) * rng.nextDouble();
        double exit = lo + (hi - lo) * rng.nextDouble();
        double catchment = p.seed().virtualCatchmentKm2().draw(rng);
        int hare = rng.nextInt(p.seed().hareCycleYears());

        RegionRole[] roles = new RegionRole[16];
        for (int i = 0; i < 4; i++) roles[i] = RegionRole.MOUNTAINS;
        roles[CORE_REGION] = RegionRole.CORE;
        int[] pool = new int[11];
        int n = 0;
        for (int i = 4; i < 16; i++) if (i != CORE_REGION) pool[n++] = i;
        for (int i = pool.length - 1; i > 0; i--) { // Fisher–Yates on the fixed index order
            int j = rng.nextInt(i + 1);
            int t = pool[i]; pool[i] = pool[j]; pool[j] = t;
        }
        RegionRole[] order = archetype == Archetype.LAKE_VALLEY
                ? new RegionRole[] {RegionRole.ALPINE_TUNDRA, RegionRole.CANYON, RegionRole.MINING_CREEK, RegionRole.BIG_LAKE}
                : new RegionRole[] {RegionRole.ALPINE_TUNDRA, RegionRole.CANYON, RegionRole.MINING_CREEK, RegionRole.BIG_LAKE, RegionRole.BIG_LAKE};
        for (int k = 0; k < pool.length; k++) roles[pool[k]] = k < order.length ? order[k] : RegionRole.MIXED;

        return new SeedParams(levelSeed, seed, attempt, attemptSeed, archetype, mirror,
                mirror ? 'E' : 'W', entry, mirror ? 'W' : 'E', exit, catchment, hare, List.of(roles));
    }
}
