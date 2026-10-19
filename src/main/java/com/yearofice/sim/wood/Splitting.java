package com.yearofice.sim.wood;

/**
 * Splitting a round on the chopping block (Trees, felling and firewood › Splitting): a strike on the crack in the
 * end grain splits it at once, anywhere else it takes the tool's count; each strike is 1 game minute. A round
 * splits into one split piece of the same weight (4 rounds or 4 splits are the 15 kg armload).
 *
 * <p>The crack is a line through the centre of the round's end, in one of 4 directions (0°, 45°, 90°, 135°), set
 * when the round is put on the block. A strike is on it within {@link #CRACK_HALF_WIDTH} of a block.
 */
public final class Splitting {
    private Splitting() {}

    /** Game minutes a strike. */
    public static final double STRIKE_MIN = 1.0;
    /** Half the crack's width, in blocks (2 px of 16 either side). */
    public static final double CRACK_HALF_WIDTH = 0.125;

    /** The crack's direction in degrees for index 0–3. */
    public static double crackDeg(int index) { return 45.0 * Math.floorMod(index, 4); }

    /**
     * Whether a strike at ({@code u}, {@code v}) on the block's top face, each 0–1 across it, lands on the crack
     * with direction index {@code crack}.
     */
    public static boolean onCrack(double u, double v, int crack) {
        double a = Math.toRadians(crackDeg(crack));
        double x = u - 0.5, y = v - 0.5;
        if (x * x + y * y > 0.45 * 0.45) return false; // off the round's end
        double dist = Math.abs(x * Math.cos(a) + y * Math.sin(a)); // from the line along compass bearing a (0° north-south)
        return dist <= CRACK_HALF_WIDTH;
    }

    /** Strikes still needed after {@code done} strikes off the crack, for a tool's count. */
    public static int strikesLeft(ToolStats tool, int done) { return Math.max(0, tool.splitStrikes() - done); }
}
