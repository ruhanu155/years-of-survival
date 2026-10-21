package com.yearofice.ecosystem.small;

import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.sim.life.LemmingCycle;
import com.yearofice.sim.time.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerLevel;

/**
 * The lemming index L of the level's current year (4-year cycle 0.2, 0.7, 2.5, 0.3, phase drawn from the seed).
 * {@code /yoi lemming set <phase>} pins a phase for the session (a debug override, not saved).
 */
public final class LemmingIndex {
    private LemmingIndex() {}

    private static final AtomicInteger PINNED = new AtomicInteger(-1);

    public static void pin(LemmingCycle.Phase p) { PINNED.set(p == null ? -1 : p.ordinal()); }

    public static void clear() { PINNED.set(-1); }

    public static LemmingCycle.Phase phase(ServerLevel level) {
        int pinned = PINNED.get();
        if (pinned >= 0) return LemmingCycle.Phase.values()[pinned];
        long year = Calendar.yearOfDay(Calendar.absDayOfMinute(SliceClock.minute(level))) - 1;
        return LemmingCycle.phaseOf(LemmingCycle.phase(level.getSeed()), year);
    }

    public static double index(ServerLevel level) { return phase(level).index(); }

    public static boolean bold(ServerLevel level) { return LemmingCycle.bold(index(level)); }

    /** The spawner's multiplier on a species' density: L for the cyclic lemmings, 1 for everything else. */
    public static double multiplier(String speciesId, ServerLevel level) {
        return speciesId.equals("yearofice:brown_lemming") || speciesId.equals("yearofice:collared_lemming") ? LemmingCycle.per100m2(index(level)) : 1.0;
    }
}
