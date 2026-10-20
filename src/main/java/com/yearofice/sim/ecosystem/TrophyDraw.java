package com.yearofice.sim.ecosystem;

import com.yearofice.core.hash.Mulberry32;

/**
 * Trophy size of a big-game animal (Waters and fish › The fish record book, game page; RECONCILE 170 A5). The size is
 * drawn from the species' normal curve ({@code mean}, {@code sd}) with an age shift in standard deviations; the class
 * comes from the z-score: {@code trophy} for the top 5% (z of 1.645 or more), {@code fine} from z 1.0, else
 * {@code common}. Pure Java. Original Year of Ice code.
 */
public final class TrophyDraw {
    private TrophyDraw() {}

    public static final double TOP5_Z = 1.6449;
    public static final double FINE_Z = 1.0;

    public record Size(double value, double z, String cls) {}

    public static Size draw(double mean, double sd, double ageShiftSd, Mulberry32 r) {
        // Box-Muller from two uniforms; the stream position never depends on the outcome.
        double u1 = Math.max(1e-12, r.nextDouble()), u2 = r.nextDouble();
        double n = Math.sqrt(-2 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        double z = n + ageShiftSd;
        return new Size(mean + z * sd, z, classOf(z));
    }

    public static String classOf(double z) { return z >= TOP5_Z ? "trophy" : z >= FINE_Z ? "fine" : "common"; }
}
