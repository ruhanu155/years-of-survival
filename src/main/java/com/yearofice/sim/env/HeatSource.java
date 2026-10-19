package com.yearofice.sim.env;

/**
 * One heat source as seen from a sample point: its full bonus, the radius it keeps that bonus to, the
 * radius where it fades to zero, how far away it is, and how many solid blocks sit on the line between.
 * The env layer's heat index ({@code EnvService.registerHeat}) produces these; {@link HeatIndex} sums them.
 */
public record HeatSource(Kind kind, double bonusC, double fullRadiusM, double zeroRadiusM, double distanceM, int occluders) {

    public enum Kind { OPEN_FIRE, STOVE, FIREPLACE, HOT_SPRING, OTHER }

    public HeatSource {
        if (kind == null) throw new IllegalArgumentException("kind");
        if (zeroRadiusM < fullRadiusM) throw new IllegalArgumentException("zero radius inside full radius");
        if (occluders < 0) occluders = 0;
    }

    /** An open fire: Low +10, Steady +18, Hot +25 within 3 m, fading in a straight line to 0 at 8 m. */
    public static HeatSource openFire(FireSetting setting, double distanceM) {
        return new HeatSource(Kind.OPEN_FIRE, setting.bonusC, 3, 8, distanceM, 0);
    }

    /** A burning stove inside a room: up to +10 within 1.5 m, 0 at 3 m (Crafting › Warm rooms › Feels-like indoors). */
    public static HeatSource stove(double distanceM) {
        return new HeatSource(Kind.STOVE, 10, 1.5, 3, distanceM, 0);
    }

    /** A burning fireplace inside a room: up to +15 within 2 m, 0 at 3 m. */
    public static HeatSource fireplace(double distanceM) {
        return new HeatSource(Kind.FIREPLACE, 15, 2, 3, distanceM, 0);
    }

    public HeatSource behind(int blocks) {
        return new HeatSource(kind, bonusC, fullRadiusM, zeroRadiusM, distanceM, blocks);
    }

    public boolean isFire() { return kind != Kind.HOT_SPRING && kind != Kind.OTHER; }
}
