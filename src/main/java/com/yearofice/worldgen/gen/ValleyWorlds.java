package com.yearofice.worldgen.gen;

import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.Pipeline;
import com.yearofice.worldgen.ValleyWorld;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The bound layers per layer seed, built once per server and shared by every generator, biome source and
 * structure placement that asks for that seed. Built on the thread that first asks (the server thread at level
 * creation), so world creation pays for it before the first chunk.
 */
public final class ValleyWorlds {
    private ValleyWorlds() {}

    private static final Map<Long, ValleyWorld> BY_SEED = new ConcurrentHashMap<>();
    /** Level seed to the layers its dimension of ours uses, for structure placement (which sees only the seed). */
    private static final Map<Long, ValleyWorld> BY_LEVEL_SEED = new ConcurrentHashMap<>();
    private static volatile Pipeline pipeline;

    static Pipeline pipeline() {
        Pipeline p = pipeline;
        if (p == null) pipeline = p = Pipeline.standard();
        return p;
    }

    public static ValleyWorld get(long layerSeed) {
        return BY_SEED.computeIfAbsent(layerSeed, s -> {
            ValleyWorld w = ValleyWorld.build(pipeline(), s);
            YearOfIce.LOG.info("Year of Ice layers for seed {} bound ({}, attempt {})", s, w.seed.archetype(), w.seed.attempt());
            return w;
        });
    }

    static void bindLevelSeed(long levelSeed, ValleyWorld w) { BY_LEVEL_SEED.put(levelSeed, w); }

    public static ValleyWorld forLevelSeed(long levelSeed) { return BY_LEVEL_SEED.get(levelSeed); }

    /** Drops every cached world (server stop), so a new world with another seed starts clean. */
    public static void clear() {
        BY_SEED.clear();
        BY_LEVEL_SEED.clear();
    }
}
