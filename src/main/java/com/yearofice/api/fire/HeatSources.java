package com.yearofice.api.fire;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The one heat-source api (M8-INT): what a room (or a tent) receives in watts. It is every warm fire and lit stove in
 * {@link FireHeatLookup#fires} (stoves arrive there as {@code FireIndex} extras with {@link StoveHeatLookup} giving the
 * flued flag) plus watts that other systems post here by position: the hot tent's stove, GameTest stubs. Rooms read only this.
 */
public final class HeatSources {
    private HeatSources() {}

    private static final Map<Level, Map<BlockPos, Double>> POSTED = new ConcurrentHashMap<>();

    /** Posts (or replaces) a source of {@code watts} at a block; 0 removes it. */
    public static void post(Level level, BlockPos pos, double watts) {
        if (watts <= 0) {
            Map<BlockPos, Double> m = POSTED.get(level);
            if (m != null) m.remove(pos);
        } else POSTED.computeIfAbsent(level, k -> new ConcurrentHashMap<>()).put(pos.immutable(), watts);
    }

    public static void clear(Level level) { POSTED.remove(level); }

    public static void clearAll() { POSTED.clear(); }

    /** Watts of every source whose cell {@code inside} accepts (blocks for posted sources, fire coordinates for fires). */
    public static double wattsIn(Level level, Predicate<BlockPos> inside) { return wattsIn(level, inside, false); }

    /** Watts of the burning fires and stoves only (they draw room air up their flue; posted sources do not). */
    public static double firedIn(Level level, Predicate<BlockPos> inside) { return wattsIn(level, inside, true); }

    private static double wattsIn(Level level, Predicate<BlockPos> inside, boolean firedOnly) {
        double w = 0;
        for (var f : FireHeatLookup.INSTANCE.fires(level)) if (inside.test(BlockPos.containing(f.x(), f.y(), f.z()))) w += f.kW() * 1000;
        Map<BlockPos, Double> m = firedOnly ? null : POSTED.get(level);
        if (m != null) for (var e : m.entrySet()) if (inside.test(e.getKey())) w += e.getValue();
        return w;
    }
}
