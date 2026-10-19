package com.yearofice.sim.decay;

/** Hourly temperature records for the decay tests. */
final class Climates {
    private Climates() {}

    /** Monthly means of the Standard valley year (Decay and time › The decay-day), January first. */
    static final double[] STANDARD_MONTHLY_MEAN_C = {-22, -18, -12, -2, 9, 15, 17, 14, 8, -2, -12, -20};
    static final int[] DAYS_IN_MONTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    static double[] constant(double tempC, int hours) {
        double[] t = new double[hours];
        java.util.Arrays.fill(t, tempC);
        return t;
    }

    /** A Standard year at its monthly means, hour by hour, repeated {@code years} times. */
    static double[] standardYears(int years) {
        double[] t = new double[365 * 24 * years];
        int h = 0;
        for (int y = 0; y < years; y++) {
            for (int m = 0; m < 12; m++) {
                for (int d = 0; d < DAYS_IN_MONTH[m] * 24; d++) t[h++] = STANDARD_MONTHLY_MEAN_C[m];
            }
        }
        return t;
    }

    /** Daily means of the Standard year. */
    static double[] standardDailyMeans() {
        double[] t = new double[365];
        int d = 0;
        for (int m = 0; m < 12; m++) {
            for (int i = 0; i < DAYS_IN_MONTH[m]; i++) t[d++] = STANDARD_MONTHLY_MEAN_C[m];
        }
        return t;
    }

    /** A diurnal record: mean ± amplitude, coldest at 03:00, warmest at 15:00. StrictMath for exact replays. */
    static double[] diurnal(double meanC, double amplitudeC, int days) {
        double[] t = new double[days * 24];
        for (int h = 0; h < t.length; h++) {
            int hod = h % 24;
            t[h] = meanC - amplitudeC * StrictMath.cos(2 * Math.PI * (hod - 3) / 24.0);
        }
        return t;
    }

    static double[] concat(double[]... parts) {
        int n = 0;
        for (double[] p : parts) n += p.length;
        double[] out = new double[n];
        int i = 0;
        for (double[] p : parts) {
            System.arraycopy(p, 0, out, i, p.length);
            i += p.length;
        }
        return out;
    }
}
