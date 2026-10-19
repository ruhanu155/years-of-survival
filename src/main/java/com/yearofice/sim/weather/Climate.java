package com.yearofice.sim.weather;

/**
 * The numbers of {@code weather/interior_alaska.json} that the M2 weather draw reads (Weather and storms › Monthly
 * climate at 64°N, The generator; Blueprint › Calendar, daylight and weather). Month arrays are indexed by
 * calendar month − 1 (January 0); state arrays by {@link DayState#ordinal()}; season arrays spring, summer, fall,
 * winter. The env layer parses the file into this record; nothing here reads JSON.
 */
public record Climate(
        double[] meanC, double[] swingC, double[] boundLoC, double[] boundHiC, double[] precipMm, double[] windMs,
        double[] pressureHpa, double[] snowDepthCm, double[] daylightH,
        double[] hourlyCurve, double[] windCurve, boolean[] windCurveMonth,
        double anomalyKeep, double anomalyNoiseC, double winterSevereKickC, double anomalyStartC,
        double[] skyFactor,
        int[][] seasonWindow, double[][][] markov, DayState startState,
        double[] offsetSummerC, double[] offsetWinterC, double winterBelowC,
        double[][] windRangeMs, double[] gustFactor,
        double severeFactor, double snowCmPerMm, double snowBelowC, double shoulderBandC, double sleetFromC,
        double rainFromC, int[] windowH, double drawLow, double drawHigh, double heavyFromMmH, double drizzleBelowMmH,
        double[] cloudPct, double[] humidityPct,
        double visFullM, double visDrizzleM, double visRainM, double visHeavyRainM, double visLightSnowM,
        double visSnowM, double visBlizzardM, double lightSnowBelowCmH, double blizzardFromMs,
        double floorY, double lapseCPer100m, double inversionCPer100m, double inversionMaxC, double inversionWindBelowMs,
        double ridgePer300m, double ridgeMax, double canopyFactor,
        int waterFollowDays, double waterFloorC, double creekOffsetC, double lakeSummerOffsetC,
        double[] groundOpenSoilC) {

    public static final int SPRING = 0, SUMMER = 1, FALL = 2, WINTER = 3;

    /** Season of a day of year (0 = May 1) from the season windows ({@code [from_doy, to_doy]}, wrapping). */
    public int season(int dayOfYear) {
        for (int s = 0; s < 4; s++) {
            int a = seasonWindow[s][0], b = seasonWindow[s][1];
            if (a <= b ? dayOfYear >= a && dayOfYear <= b : dayOfYear >= a || dayOfYear <= b) return s;
        }
        return WINTER;
    }
}
