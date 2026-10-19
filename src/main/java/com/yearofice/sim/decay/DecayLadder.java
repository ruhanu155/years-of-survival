package com.yearofice.sim.decay;

import java.util.List;

/**
 * The five-stage ladder (Decay and time › Stages and looks). Every class has five stage slots and names them;
 * the thresholds on p come from the source rule, so food keeps its 60, 30 and 10 freshness lines and wood
 * keeps Structures' quartiles. Stage index 0 is new; stage 4 is the slot reached at the last threshold.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public record DecayLadder(String id, List<String> names, double[] thresholds) {

    public DecayLadder {
        if (names.size() != 5) throw new IllegalArgumentException("a ladder has five stage names");
        if (thresholds.length != 4) throw new IllegalArgumentException("a ladder has four thresholds");
        for (int i = 1; i < 4; i++) {
            if (thresholds[i] < thresholds[i - 1]) throw new IllegalArgumentException("thresholds must rise");
        }
        names = List.copyOf(names);
        thresholds = thresholds.clone();
    }

    private static DecayLadder of(String id, String a, String b, String c, String d, String e,
                                  double t1, double t2, double t3, double t4) {
        return new DecayLadder(id, List.of(a, b, c, d, e), new double[] {t1, t2, t3, t4});
    }

    /** Food: Fresh 100–60, Aging 60–30, Turning 30–10, Spoiled 10–1, Rotten 0. */
    public static final DecayLadder FOOD = of("food", "fresh", "aging", "turning", "spoiled", "rotten", 0.4, 0.7, 0.9, 1.0);
    public static final DecayLadder HIDE = of("hide", "green", "tainted", "slipping", "rotten", "lost", 0.3, 0.6, 0.9, 1.0);
    /** Built wood: collapse at the block's own threshold u, or 0.97 for non-load blocks. */
    public static final DecayLadder BUILT_WOOD = of("built_wood", "sound", "weathered", "soft", "rotten", "collapsed", 0.25, 0.5, 0.75, DecayCore.PROPORTIONAL_LAST_STAGE);
    public static final DecayLadder DEADFALL = of("deadfall", "sound", "punky", "nurse_log", "moss_mound", "soil", 0.3, 0.7, 0.9, DecayCore.PROPORTIONAL_LAST_STAGE);
    public static final DecayLadder IRON = of("iron", "bright", "tarnished", "rusted", "pitted", "rusted_through", 0.1, 0.3, 0.6, 1.0);
    public static final DecayLadder FABRIC = of("fabric", "good", "worn", "frayed", "rags", "scraps", 0.3, 0.7, 0.9, 1.0);
    public static final DecayLadder PLANT = of("plant", "fresh", "brown", "brittle", "rotten", "duff", 0.3, 0.6, 0.9, 1.0);
    public static final DecayLadder CLAY = of("clay", "sound", "pitted", "eroded", "slumped", "mud", 0.25, 0.5, 0.75, 1.0);

    /** Stage index 0–4 for a decay value p. */
    public int stageOf(double p) {
        int s = 0;
        for (double t : thresholds) {
            if (p >= t) s++;
        }
        return s;
    }

    public String nameOf(double p) { return names.get(stageOf(p)); }

    /** The p at which stage {@code stage} (1–4) begins. */
    public double startOf(int stage) {
        if (stage <= 0) return 0.0;
        return thresholds[stage - 1];
    }

    /** Freshness points shown on a food bar: 100 × (1 − p). */
    public static double freshness(double p) { return 100.0 * (1.0 - clamp01(p)); }

    /** The tooltip rounds freshness to 5 points (Items › How a stack reads). */
    public static int tooltipFreshness(double p) { return (int) (Math.round(freshness(p) / 5.0) * 5); }

    static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
}
