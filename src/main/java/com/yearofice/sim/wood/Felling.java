package com.yearofice.sim.wood;

/**
 * The felling numbers (Crafting and building in depth › Trees, felling and firewood › Tools, Felling, step by step):
 * swings = ceiling of S × (d ÷ 25 cm)², the first third of them cut the notch, and every time is game minutes.
 * Pure; the felling service and the client's preview both read it.
 */
public final class Felling {
    private Felling() {}

    /** kcal an hour of axe work (Blueprint › Controls: the preview's 450 kcal an hour). */
    public static final double KCAL_PER_HOUR = 450;

    /** Swings a tool with S swings at 25 cm needs for a trunk {@code dCm} thick; at least 1. */
    public static int swings(int s25, double dCm) {
        double x = s25 * (dCm / 25.0) * (dCm / 25.0);
        return Math.max(1, (int) Math.ceil(x - 1e-9));
    }

    /** Notch swings: the first third of {@code swings} (2 of 6), at least 1. */
    public static int notchSwings(int swings) { return Math.max(1, swings / 3); }

    /** Whether {@code tool} can fell a trunk {@code dCm} thick. */
    public static boolean canFell(ToolStats tool, double dCm) { return tool.fells() && dCm <= tool.fellMaxCm() + 1e-9; }

    /** Game minutes to fell with clean swings. */
    public static double minutes(ToolStats tool, double dCm) { return swings(tool.swings25(), dCm) * tool.swingMin(); }

    /** The preview's kcal for {@code minutes} of axe work. */
    public static double kcal(double minutes) { return KCAL_PER_HOUR * minutes / 60.0; }

    /** Compass bearing in degrees (0 north, 90 east, clockwise) of the offset (dx, dz) in block axes (+z is south). */
    public static double bearing(double dx, double dz) {
        double b = Math.toDegrees(Math.atan2(dx, -dz));
        return b < 0 ? b + 360 : b;
    }

    /** Smallest angle between two bearings, 0–180. */
    public static double angleBetween(double a, double b) {
        double d = Math.abs(((a - b) % 360 + 360) % 360);
        return d > 180 ? 360 - d : d;
    }

    /** Unit offset (x, z) of a compass bearing. */
    public static double[] unit(double bearingDeg) {
        double r = Math.toRadians(bearingDeg);
        return new double[] {Math.sin(r), -Math.cos(r)};
    }

    /** "north", "north-east", ... for a bearing (the messenger's fall line). */
    public static String compass(double bearingDeg) {
        String[] names = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
        int i = (int) Math.floor(((bearingDeg % 360 + 360) % 360 + 22.5) / 45.0) % 8;
        return names[i];
    }

    /**
     * The tree's lean in degrees from its base hash (Reading a tree: 0–8°, drunken trees on permafrost 5–20°). The
     * fall rules that use it (a lean over 10° pulls the line) are the M6 hazards; M2 shows it on the tree card.
     */
    public static double leanDeg(int hash, boolean drunken) {
        double u = (hash >>> 8) / 16777216.0;
        return drunken ? 5 + 15 * u : 8 * u;
    }

    /** The lean's compass bearing from the same hash. */
    public static double leanBearing(int hash) { return (hash & 0xFF) * (360.0 / 256.0); }

    /** Seconds the falling tree takes to turn about its base: 1.5 s for a 6 m pole up to 3.0 s for 20 m and over. */
    public static double fallSeconds(double heightM, double minS, double maxS) {
        double u = Math.max(0, Math.min(1, (heightM - 6) / 14.0));
        return minS + (maxS - minS) * u;
    }
}
