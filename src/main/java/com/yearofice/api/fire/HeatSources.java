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

    private static final Map<Level, Map<BlockPos, Double>> DUTY = new ConcurrentHashMap<>();

    /** A stove posts its duty cycle, 0-1 (the share of a day it is fired); a fire that posts none counts as 1. */
    public static void postDuty(Level level, BlockPos pos, double duty) {
        if (duty >= 1) {
            Map<BlockPos, Double> m = DUTY.get(level);
            if (m != null) m.remove(pos);
        } else DUTY.computeIfAbsent(level, k -> new ConcurrentHashMap<>()).put(pos.immutable(), Math.max(0, duty));
    }

    public static void clear(Level level) { POSTED.remove(level); DUTY.remove(level); }

    public static void clearAll() { POSTED.clear(); DUTY.clear(); }

    /** Watts of every source whose cell {@code inside} accepts (blocks for posted sources, fire coordinates for fires). */
    public static double wattsIn(Level level, Predicate<BlockPos> inside) { return wattsIn(level, inside, false); }

    /** Mean watts over a day: every fire's watts times its posted duty cycle, plus posted sources as they are. What a room's balance settles on. */
    public static double averageIn(Level level, Predicate<BlockPos> inside) { return wattsIn(level, inside, true); }

    private static double wattsIn(Level level, Predicate<BlockPos> inside, boolean byDuty) {
        double w = 0;
        Map<BlockPos, Double> duty = byDuty ? DUTY.get(level) : null;
        for (var f : FireHeatLookup.INSTANCE.fires(level)) {
            BlockPos at = BlockPos.containing(f.x(), f.y(), f.z());
            if (inside.test(at)) w += f.kW() * 1000 * (duty == null ? 1.0 : duty.getOrDefault(at, 1.0));
        }
        Map<BlockPos, Double> m = POSTED.get(level);
        if (m != null) for (var e : m.entrySet()) if (inside.test(e.getKey())) w += e.getValue();
        return w;
    }
}
