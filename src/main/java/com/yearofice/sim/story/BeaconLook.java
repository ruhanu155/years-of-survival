package com.yearofice.sim.story;

/**
 * How strongly the objective beacon draws (Blueprint › Start site: "a soft light column visible through trees, one
 * at a time"). It never clutters: it is faint at most, it fades out as the player closes on the target, so the
 * place itself takes over, and it is gone inside the near distance. Pure numbers; the client renderer calls it.
 */
public final class BeaconLook {
    private BeaconLook() {}

    /** Column height above the target, blocks. */
    public static final double HEIGHT = 24;
    /** Column half-width, blocks. */
    public static final double HALF_WIDTH = 0.3;

    /**
     * The column's base opacity at {@code distanceM} from the camera: 0 within {@code fadeNearM}, rising in a
     * straight line to {@code maxAlpha} at {@code fadeFullM}, and {@code maxAlpha} beyond.
     */
    public static double alpha(double distanceM, double fadeNearM, double fadeFullM, double maxAlpha) {
        if (!(distanceM > fadeNearM)) return 0;
        if (fadeFullM <= fadeNearM || distanceM >= fadeFullM) return maxAlpha;
        return maxAlpha * (distanceM - fadeNearM) / (fadeFullM - fadeNearM);
    }

    /** Opacity along the column, 1 at the foot to 0 at the top, so it reads as light, not a pole. */
    public static double along(double heightShare) {
        double h = Math.max(0, Math.min(1, heightShare));
        return (1 - h) * (1 - h);
    }
}
