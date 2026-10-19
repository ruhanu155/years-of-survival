package com.yearofice.sim.fishing;

/**
 * Per-spot stocks (Blueprint › Fishing in depth › Fish stocks).
 *
 * <p>Each spot holds a count N per species with capacity K = q × the type's capacity. A catch removes one fish.
 * Rod fishing feels √(N/K); nets, weirs and the wheel scale by N/K directly. On the first day of each month:
 *
 * <pre>
 *   N ← min(K, N + r · N · (1 − N/K) + i · K)          growth ×0.25 from November to April
 * </pre>
 *
 * Spawning sites shrink next year's spawn capacity by the spawners taken, and a salmon run's harvest share
 * echoes into the run that returns years later.
 *
 * <p>Pure Java, immutable. Original Year of Ice code.
 */
public record FishStock(double n, double k) {
    public static final double WINTER_GROWTH = 0.25;
    public static final double IMMIGRATION_RIVER = 0.05;
    public static final double IMMIGRATION_LAKE = 0.02;

    public FishStock {
        if (k < 0) throw new IllegalArgumentException("capacity must be non-negative");
        n = Math.max(0.0, Math.min(k, n));
    }

    public static FishStock full(double k) { return new FishStock(k, k); }

    public double fraction() { return k == 0 ? 0.0 : n / k; }

    public double mStock() { return BiteModel.mStock(n, k); }

    public FishStock harvest(int fish) { return new FishStock(n - fish, k); }

    /** Monthly regrowth on the first of the month (month 1–12). */
    public FishStock regrow(double rPerMonth, double immigration, int month) {
        double r = (month >= 11 || month <= 4) ? rPerMonth * WINTER_GROWTH : rPerMonth;
        double grown = n + r * n * (1.0 - fraction()) + immigration * k;
        return new FishStock(Math.min(k, grown), k);
    }

    /** When terrain changes q, K rescales and N is clamped to it (Waters and fish › When water moves). */
    public FishStock rescale(double newK) { return new FishStock(Math.min(n, newK), newK); }

    /** Next year's spawn capacity after taking s spawners: K · max(0.2, 1 − 2 s / K). */
    public static double nextSpawnCapacity(double kSpawn, double spawnersTaken) {
        if (kSpawn <= 0) return 0.0;
        return kSpawn * Math.max(0.2, 1.0 - 2.0 * spawnersTaken / kSpawn);
    }

    /** The echo on a run returning years later: e = max(0.4, 1 − 1.5 · max(0, h − 0.2)). */
    public static double echo(double harvestShare) {
        return Math.max(0.4, 1.0 - 1.5 * Math.max(0.0, harvestShare - 0.2));
    }
}
