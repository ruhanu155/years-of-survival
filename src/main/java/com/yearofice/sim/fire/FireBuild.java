package com.yearofice.sim.fire;

import java.util.Locale;

/**
 * The shape a fire is laid in (Blueprint › Fire, water and light › Fire builds; "the fire builds are shapes laid on
 * top of the layers"). The stove is M8 and stays out. Burn minutes per armload are in {@link FireTuning}; the rules
 * a build adds are here.
 *
 * <ul>
 *   <li>{@link #RING}: the plain ring of 8 stones the slice shipped with. It takes any fuel.</li>
 *   <li>{@link #TEEPEE}: sticks and split wood leaned together; burns an armload in about an hour, hot, and boils
 *       water fastest. Split wood only.</li>
 *   <li>{@link #LONG}: two parallel logs; steady, three hours an armload. Whole logs only.</li>
 *   <li>{@link #DAKOTA}: a dug pit and a draft tunnel; two hours an armload, thin smoke, wind does not reach it, and
 *       it never sparks. It needs a pit: the ring set into the ground with walls on at least three sides.</li>
 * </ul>
 */
public enum FireBuild {
    RING("Ring"), TEEPEE("Teepee"), LONG("Long fire"), DAKOTA("Dakota hole");

    /** The largest armload share of a piece that still counts as split wood (a split is a quarter armload). */
    public static final double SPLIT_MAX_FRACTION = 0.25;
    /** The smallest share that counts as a whole log (a log is half an armload). */
    public static final double LOG_MIN_FRACTION = 0.5;

    public final String word;

    FireBuild(String word) { this.word = word; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static FireBuild byId(String id) {
        for (FireBuild b : values()) if (b.id().equals(id)) return b;
        return null;
    }

    /** The next build in the cycle the empty hand walks; the Dakota hole is skipped when there is no pit. */
    public FireBuild next(boolean pit) {
        FireBuild n = values()[(ordinal() + 1) % values().length];
        return n == DAKOTA && !pit ? RING : n;
    }

    /** Why a piece of {@code armloadFraction} cannot go into this build, or null when it can. */
    public String rejects(double armloadFraction) {
        return switch (this) {
            case TEEPEE -> armloadFraction > SPLIT_MAX_FRACTION + 1e-9 ? "A teepee stands on split wood, not whole logs." : null;
            case LONG -> armloadFraction < LOG_MIN_FRACTION - 1e-9 ? "A long fire is two whole logs side by side." : null;
            default -> null;
        };
    }

    /** Wind does not reach a Dakota hole: ember and match life, and the fuel burn, ignore it. */
    public boolean shelteredFromWind() { return this == DAKOTA; }

    /** A Dakota hole never sparks. */
    public boolean sparks() { return this != DAKOTA; }

    /** Smoke shown is thinned one step in a Dakota hole. */
    public int smoke(int base) { return this == DAKOTA ? Math.min(base, 1) : base; }
}
