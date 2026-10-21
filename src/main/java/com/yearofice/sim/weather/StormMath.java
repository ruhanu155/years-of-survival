package com.yearofice.sim.weather;

/** Pure storm numbers: the storm arc, the live fields, glaze and hail cover. */
public final class StormMath {
    private StormMath() {}

    /** The live fields of a storm at one moment; {@code level} is intensity × arc. */
    public record Live(double level, double windMs, double gustMs, double precipMmH, double visibilityM) {}

    /** A storm ramps up, peaks mid-way and eases off: 0 at the ends, 1 at the middle (the storm arc, Presentation). */
    public static double arc(double phase) { return Math.sqrt(Math.max(0, Math.sin(Math.PI * Math.min(1, Math.max(0, phase))))); }

    public static Live live(StormDef def, StormTable.Event e, long minute, double severity, double whiteoutM) {
        double phase = (minute / 60.0 - e.startHour()) / e.hours();
        double level = Math.min(1, e.intensity() * arc(phase));
        double wind = StormDef.lerp(def.windMs(), level) * severity;
        double mm = StormDef.lerp(def.precipMmH(), level) * severity;
        double lo = def.whiteout() ? whiteoutM : def.visibilityM()[0];
        double vis = def.visibilityM()[1] - (def.visibilityM()[1] - lo) * level;
        double gust = wind * (def.kind() == StormKind.THUNDERSTORM ? 1.7 : 1.4);
        return new Live(level, wind, gust, mm, vis);
    }

    /** g = 0.7 · Σ r_h · e, e = 1 open and 0.3 under canopy. */
    public static double glazeMm(double sumRateMm, boolean canopy) { return 0.7 * sumRateMm * (canopy ? 0.3 : 1); }

    /** One {@code glaze_layer} per 5 mm, from the first 2 mm, at most 3. */
    public static int glazeLayers(double mm) { return mm < 2 ? 0 : Math.min(3, Math.max(1, (int) (mm / 5))); }

    /** Hail cover 1–3 cm → 1–2 layers. */
    public static int hailLayers(double cm) { return cm < 1 ? 0 : cm < 2 ? 1 : 2; }

    /** Glaze mm accumulated by a freezing-rain storm {@code hoursIn} hours after it began (rate at its level). */
    public static double glazeAfter(StormDef def, StormTable.Event e, double hoursIn, double severity, boolean canopy) {
        double sum = 0;
        long h0 = e.startHour();
        for (int h = 0; h < Math.min(hoursIn, e.hours()); h++) sum += live(def, e, (h0 + h) * 60 + 30, severity, 0).precipMmH();
        return glazeMm(sum, canopy);
    }
}
