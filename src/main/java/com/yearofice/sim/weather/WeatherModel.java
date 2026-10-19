package com.yearofice.sim.weather;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;

/**
 * The home valley's weather for M2: the month normals plus a seeded daily draw (Ready for development › M2;
 * Minecraft engine › M2: "the Markov chain, forecast and storms wait for M7"). What is drawn is the part of Weather
 * and storms › The generator that needs no fronts, air masses or storms:
 *
 * <ol>
 *   <li><b>Day chain.</b> The Blueprint's four seasonal Markov tables, one draw a day, from Clear on May 1 of year 1;
 *       each year continues from the last day of the one before.</li>
 *   <li><b>Anomaly.</b> a_d = keep · a_(d−1) + noise (uniform ±noise_c), plus −6 °C after a winter Severe day.</li>
 *   <li><b>Day mean.</b> The month's normal interpolated between mid-month anchors, plus a_d, plus the state offset
 *       (summer column, or winter below 1 °C), re-centred by the season's long-run state shares so the monthly
 *       mean stays the normal; the winter kick's long-run share is taken out the same way.</li>
 *   <li><b>Amounts.</b> A Wet day gets u·k_m mm and a Severe day 3.5·u·k_m mm, u in [0.5, 1.5], with k_m chosen so
 *       the expected month total is the normal; the precipitation falls in one window (8 h Wet, 12 h Severe)
 *       starting at a drawn hour.</li>
 *   <li><b>Hours.</b> T(d,h) = M_d + S_m · c_s · κ(h), soft-bounded (past a bound an hour is pulled back halfway);
 *       wind v(h) = v_d · δ_m(h) with v_d drawn in the state's range and re-centred to the month's normal wind;
 *       gusts by the state's factor.</li>
 * </ol>
 *
 * Every roll comes from {@code mulberry32(hash(seed, "weather", year))} in a fixed order per day, so the weather
 * never depends on who was watching (Deterministic and replayable). Days before May 1 of year 1 are plain normals.
 */
public final class WeatherModel {

    /** One drawn day. {@code meanC} is the day mean on the valley floor; {@code precipMm} water equivalent. */
    public record Day(long absDay, DayState state, double anomalyC, double normalC, double meanC, double swingC,
                      double windMs, double windDirDeg, double precipMm, int precipStartH, int precipHours) {}

    /** One hour on the valley floor. {@code precipMmH} is water equivalent; snow falls at 1.5 cm a mm. */
    public record Hour(double airC, double windMs, double gustMs, double windDirDeg, Precip precip, double precipMmH,
                       double cloudPct, double humidityPct, double visibilityM, double pressureHpa, DayState state,
                       double snowDepthCm) {}

    /**
     * A fixed first day, May 1 of year 1 (Blueprint › Start site › Day 1 rules: "Day 1 weather is fixed at cloudy,
     * 7 °C high, −3 °C low, wind 2 m/s, no precipitation; the chain starts from it"). The hours run from the low to
     * the high along the climate's hourly curve, the wind holds all day, and day 2 draws from its state and its
     * anomaly. The rolls are drawn as on any day, so every later day keeps its place in the stream.
     */
    public record FixedDay(DayState state, double highC, double lowC, double windMs) {
        public FixedDay {
            if (highC < lowC) throw new IllegalArgumentException("high below low");
            if (windMs < 0) throw new IllegalArgumentException("negative wind");
        }

        public static final FixedDay BLUEPRINT_DAY_1 = new FixedDay(DayState.CLOUDY, 7, -3, 2);
    }

    private final Climate c;
    private final long seed;
    private FixedDay firstDay;
    private final double[][] stationary = new double[4][];
    private final double[] offsetMean = new double[4 * 2];
    private final double[] windMid = new double[4];
    private final double windCurveMean;
    private final List<Day[]> years = new ArrayList<>();
    /** Mid-month anchors for the day mean, adjusted so each month's mean of the interpolated curve is its normal. */
    private final double[] meanAnchors;

    public WeatherModel(Climate climate, long worldSeed) {
        this.c = climate;
        this.seed = worldSeed;
        for (int s = 0; s < 4; s++) {
            stationary[s] = stationary(climate.markov()[s]);
            double sum = 0, win = 0, mid = 0;
            for (int j = 0; j < 4; j++) {
                sum += stationary[s][j] * climate.offsetSummerC()[j];
                win += stationary[s][j] * climate.offsetWinterC()[j];
                mid += stationary[s][j] * (climate.windRangeMs()[j][0] + climate.windRangeMs()[j][1]) / 2;
            }
            offsetMean[s * 2] = sum;
            offsetMean[s * 2 + 1] = win;
            windMid[s] = mid;
        }
        double m = 0;
        for (double v : climate.windCurve()) m += v;
        windCurveMean = m / climate.windCurve().length;
        meanAnchors = meanPreserving(climate.meanC());
    }

    /**
     * Linear interpolation between mid-month anchors bends a month's mean toward its neighbours (September sits
     * between August and a much colder October). The anchors are nudged until every month's mean of the daily
     * curve equals its normal, as "re-centred so the long-run monthly mean equals the normal" asks.
     */
    static double[] meanPreserving(double[] normals) {
        double[] a = normals.clone();
        for (int it = 0; it < 200; it++) {
            double[] sum = new double[12];
            for (int d = 0; d < Calendar.DAYS_PER_YEAR; d++) sum[Calendar.month(d) - 1] += monthly(a, d);
            double worst = 0;
            for (int m = 1; m <= 12; m++) {
                double err = normals[m - 1] - sum[m - 1] / Calendar.lengthOf(m);
                a[m - 1] += err;
                worst = Math.max(worst, Math.abs(err));
            }
            if (worst < 1e-6) break;
        }
        return a;
    }

    public Climate climate() { return c; }

    /** Fixes May 1 of year 1 (null draws it as usual); the drawn years are redrawn from it. */
    public synchronized void fixFirstDay(FixedDay f) {
        if (java.util.Objects.equals(firstDay, f)) return;
        firstDay = f;
        years.clear();
    }

    public synchronized FixedDay firstDay() { return firstDay; }

    private boolean fixed(long absDay) { return firstDay != null && absDay == 0; }

    public long seed() { return seed; }

    /** The long-run state shares of a transition table, by power iteration. */
    static double[] stationary(double[][] p) {
        double[] v = {0.25, 0.25, 0.25, 0.25};
        for (int it = 0; it < 500; it++) {
            double[] n = new double[4];
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) n[j] += v[i] * p[i][j];
            v = n;
        }
        return v;
    }

    public double[] stationaryShares(int season) { return stationary[season].clone(); }

    // ---- normals ----------------------------------------------------------------------------------------------

    /** Calendar months in year order from May, and the day of year of each month's 15th. */
    private static final int[] YEAR_MONTHS = {5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4};
    private static final int[] ANCHOR = new int[12];

    static {
        for (int i = 0; i < 12; i++) ANCHOR[i] = Calendar.dayOfYear(YEAR_MONTHS[i], 15);
    }

    /** A monthly value interpolated between mid-month anchors (the 15th of each month) for a day of year. */
    public static double monthly(double[] byMonth, int dayOfYear) {
        int k = -1;
        for (int i = 11; i >= 0; i--) {
            if (ANCHOR[i] <= dayOfYear) {
                k = i;
                break;
            }
        }
        int i0 = k < 0 ? 11 : k;
        int i1 = (i0 + 1) % 12;
        double span = Math.floorMod(ANCHOR[i1] - ANCHOR[i0], Calendar.DAYS_PER_YEAR);
        double t = Math.floorMod(dayOfYear - ANCHOR[i0], Calendar.DAYS_PER_YEAR) / span;
        double a = byMonth[YEAR_MONTHS[i0] - 1], b = byMonth[YEAR_MONTHS[i1] - 1];
        return a + (b - a) * t;
    }

    public double normalC(int dayOfYear) { return monthly(meanAnchors, dayOfYear); }

    /** Open-ground snow depth: the month-end depths, linear through each month. */
    public double snowDepthCm(int dayOfYear) {
        int month = Calendar.month(dayOfYear);
        int prev = (month + 10) % 12 + 1;
        double t = Calendar.dayOfMonth(dayOfYear) / (double) Calendar.lengthOf(month);
        return c.snowDepthCm()[prev - 1] + (c.snowDepthCm()[month - 1] - c.snowDepthCm()[prev - 1]) * t;
    }

    /** Ground at 0.5 m under open soil (the {@code ground} block's {@code open_soil} row; M4 adds the rest). */
    public double groundC(int dayOfYear) {
        // The row runs May..April.
        double[] byMonth = new double[12];
        for (int i = 0; i < 12; i++) byMonth[(i + 4) % 12] = c.groundOpenSoilC()[i];
        return monthly(byMonth, dayOfYear);
    }

    // ---- the draw ---------------------------------------------------------------------------------------------

    /** The drawn day {@code absDay} (0 = May 1 of year 1). Days before year 1 are normals with no weather. */
    public synchronized Day day(long absDay) {
        if (absDay < 0) {
            int doy = Calendar.dayOfYearOfDay(absDay);
            double n = normalC(doy);
            return new Day(absDay, DayState.CLOUDY, 0, n, n, monthly(c.swingC(), doy), monthly(c.windMs(), doy), 0, 0, 0, 0);
        }
        int year = Calendar.yearOfDay(absDay);
        while (years.size() < year) years.add(drawYear(years.size() + 1, years.isEmpty() ? null : years.get(years.size() - 1)));
        return years.get(year - 1)[Calendar.dayOfYearOfDay(absDay)];
    }

    private Day[] drawYear(int year, Day[] before) {
        Mulberry32 rng = new Mulberry32(StageHash.of(seed + ":weather:" + year));
        Day[] out = new Day[Calendar.DAYS_PER_YEAR];
        DayState state = before == null ? c.startState() : before[before.length - 1].state();
        double a = before == null ? c.anomalyStartC() : before[before.length - 1].anomalyC();
        DayState prev = before == null ? null : before[before.length - 1].state();
        long base = (long) (year - 1) * Calendar.DAYS_PER_YEAR;
        for (int doy = 0; doy < Calendar.DAYS_PER_YEAR; doy++) {
            int season = c.season(doy);
            double uState = rng.nextDouble(), uNoise = rng.nextDouble(), uWind = rng.nextDouble();
            double uDir = rng.nextDouble(), uAmount = rng.nextDouble(), uStart = rng.nextDouble();
            boolean first = before == null && doy == 0;
            if (!first) {
                state = next(c.markov()[season][state.ordinal()], uState);
                a = c.anomalyKeep() * a + (2 * uNoise - 1) * c.anomalyNoiseC();
                if (prev == DayState.SEVERE && season == Climate.WINTER) a += c.winterSevereKickC();
            }
            double normal = normalC(doy);
            boolean winterCol = normal < c.winterBelowC();
            double offset = (winterCol ? c.offsetWinterC() : c.offsetSummerC())[state.ordinal()]
                    - offsetMean[season * 2 + (winterCol ? 1 : 0)];
            // The winter kick is re-centred too: its long-run share of the anomaly is kick · p_severe / (1 − keep).
            double kickMean = season == Climate.WINTER
                    ? c.winterSevereKickC() * stationary[Climate.WINTER][DayState.SEVERE.ordinal()] / (1 - c.anomalyKeep()) : 0;
            double mean = normal + a + offset - kickMean;
            if (first && firstDay != null) {
                // The fixed day: its state and mean, and the anomaly that mean implies, so day 2 continues from it.
                state = firstDay.state();
                offset = (winterCol ? c.offsetWinterC() : c.offsetSummerC())[state.ordinal()]
                        - offsetMean[season * 2 + (winterCol ? 1 : 0)];
                mean = (firstDay.highC() + firstDay.lowC()) / 2;
                a = mean - normal - offset + kickMean;
                out[doy] = new Day(base + doy, state, a, normal, mean, (firstDay.highC() - firstDay.lowC()) / 2,
                        firstDay.windMs(), uDir * 360, 0, 0, 0);
                prev = state;
                continue;
            }
            int month = Calendar.month(doy);
            double[] range = c.windRangeMs()[state.ordinal()];
            double curveMean = c.windCurveMonth()[month - 1] ? windCurveMean : 1.0;
            double wind = (range[0] + (range[1] - range[0]) * uWind) * monthly(c.windMs(), doy) / windMid[season] / curveMean;
            double mm = 0;
            int hours = 0, start = 0;
            if (state == DayState.WET || state == DayState.SEVERE) {
                double[] pi = stationary[season];
                double km = c.precipMm()[month - 1] / (Calendar.lengthOf(month) * (pi[2] + c.severeFactor() * pi[3]));
                double u = c.drawLow() + (c.drawHigh() - c.drawLow()) * uAmount;
                mm = (state == DayState.SEVERE ? c.severeFactor() : 1) * u * km;
                hours = c.windowH()[state == DayState.SEVERE ? 1 : 0];
                start = (int) Math.floor(uStart * (24 - hours + 1));
            }
            out[doy] = new Day(base + doy, state, a, normal, mean, monthly(c.swingC(), doy), wind, uDir * 360, mm, start, hours);
            prev = state;
        }
        return out;
    }

    private static DayState next(double[] row, double u) {
        double acc = 0;
        for (int j = 0; j < 4; j++) {
            acc += row[j];
            if (u < acc) return DayState.values()[j];
        }
        return DayState.values()[3];
    }

    // ---- hours ------------------------------------------------------------------------------------------------

    /** Hourly air on the valley floor at whole hour {@code hour} of {@code absDay}, soft-bounded. */
    public double hourAirC(long absDay, int hour) {
        Day d = day(absDay);
        if (fixed(absDay)) {
            double[] k = c.hourlyCurve();
            double lo = k[0], hi = k[0];
            for (double v : k) {
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
            double share = hi > lo ? (k[hour] - lo) / (hi - lo) : 0.5;
            return firstDay.lowC() + (firstDay.highC() - firstDay.lowC()) * share;
        }
        int doy = Calendar.dayOfYearOfDay(absDay);
        double t = d.meanC() + d.swingC() * c.skyFactor()[d.state().ordinal()] * c.hourlyCurve()[hour];
        int month = Calendar.month(doy);
        double lo = c.boundLoC()[month - 1], hi = c.boundHiC()[month - 1];
        if (t > hi) t = hi + (t - hi) / 2;
        if (t < lo) t = lo + (t - lo) / 2;
        return t;
    }

    /** Air at an absolute minute, linear between the hours. */
    public double airC(long minute) {
        long day = Calendar.absDayOfMinute(minute);
        int mod = (int) Math.floorMod(minute, (long) Calendar.MINUTES_PER_DAY);
        int h = mod / 60;
        double f = (mod % 60) / 60.0;
        double a = hourAirC(day, h);
        double b = h == 23 ? hourAirC(day + 1, 0) : hourAirC(day, h + 1);
        return a + (b - a) * f;
    }

    /** The whole hour record at absolute minute {@code minute} (air interpolated by minute). */
    public Hour hour(long minute) {
        long absDay = Calendar.absDayOfMinute(minute);
        int doy = Calendar.dayOfYearOfDay(absDay);
        int h = (int) Math.floorMod(minute, (long) Calendar.MINUTES_PER_DAY) / 60;
        Day d = day(absDay);
        int month = Calendar.month(doy);
        int s = d.state().ordinal();
        double air = airC(minute);
        double wind = d.windMs() * (!fixed(absDay) && c.windCurveMonth()[month - 1] ? c.windCurve()[h] : 1.0);
        double gust = wind * c.gustFactor()[s];
        double mmH = 0;
        Precip type = Precip.NONE;
        if (d.precipHours() > 0 && h >= d.precipStartH() && h < d.precipStartH() + d.precipHours()) {
            mmH = d.precipMm() / d.precipHours();
            type = precipType(d.meanC(), air, mmH);
        }
        double vis = visibility(type, mmH, wind);
        double humidity = type == Precip.NONE ? c.humidityPct()[s] : 100;
        return new Hour(air, wind, gust, d.windDirDeg(), type, mmH, c.cloudPct()[s], humidity, vis,
                c.pressureHpa()[month - 1], d.state(), snowDepthCm(doy));
    }

    /** Snow below a 1 °C day mean, rain above; within 3 °C of it each hour picks: snow ≤ 0.5, sleet to 2, rain. */
    Precip precipType(double dayMeanC, double hourAirC, double mmH) {
        Precip p;
        if (Math.abs(dayMeanC - c.snowBelowC()) < c.shoulderBandC()) {
            p = hourAirC <= c.sleetFromC() ? Precip.SNOW : hourAirC <= c.rainFromC() ? Precip.MIXED : Precip.RAIN;
        } else {
            p = dayMeanC < c.snowBelowC() ? Precip.SNOW : Precip.RAIN;
        }
        if (p == Precip.RAIN && mmH < c.drizzleBelowMmH()) p = Precip.DRIZZLE;
        return p;
    }

    double visibility(Precip p, double mmH, double windMs) {
        return switch (p) {
            case NONE -> c.visFullM();
            case DRIZZLE -> c.visDrizzleM();
            case RAIN, FREEZING_RAIN, MIXED, HAIL -> mmH > c.heavyFromMmH() ? c.visHeavyRainM() : c.visRainM();
            case SNOW -> windMs > c.blizzardFromMs() ? c.visBlizzardM()
                    : mmH * c.snowCmPerMm() < c.lightSnowBelowCmH() ? c.visLightSnowM() : c.visSnowM();
        };
    }

    /** Heavy: a Severe day's precipitation, or over 4 mm/h. */
    public boolean heavy(Hour h) {
        return h.precip() != Precip.NONE && (h.state() == DayState.SEVERE || h.precipMmH() > c.heavyFromMmH());
    }

    // ---- place ------------------------------------------------------------------------------------------------

    /**
     * Air at height {@code y} from the valley-floor value: the normal lapse (−0.65 °C per 100 m) from the floor, or on
     * a clear, calm winter night the inversion (+3 °C per 100 m above the floor up to +8 °C, then the lapse).
     */
    public double airAtHeight(double floorAirC, double y, boolean inversion) {
        double dz = y - c.floorY();
        if (!inversion || dz <= 0) return floorAirC + c.lapseCPer100m() * dz / 100;
        double top = c.inversionMaxC() / c.inversionCPer100m() * 100;
        if (dz <= top) return floorAirC + c.inversionCPer100m() * dz / 100;
        return floorAirC + c.inversionMaxC() + c.lapseCPer100m() * (dz - top) / 100;
    }

    /** True on a clear winter night (month mean below 0 °C, sun down) with wind under 2 m/s. */
    public boolean inversion(Hour h, int dayOfYear, double sunElevationDeg) {
        return h.state() == DayState.CLEAR && h.windMs() < c.inversionWindBelowMs() && sunElevationDeg < 0
                && normalC(dayOfYear) < 0;
    }

    /** Ridge exposure: ×(1 + 0.4 · clamp((y − floor) / 300 m, 0, 1.5)); canopy ×0.3. */
    public double windAtPlace(double floorWindMs, double y, boolean canopy) {
        double ridge = 1 + c.ridgePer300m() * Math.max(0, Math.min(c.ridgeMax(), (y - c.floorY()) / 300));
        return floorWindMs * ridge * (canopy ? c.canopyFactor() : 1);
    }

    // ---- water ------------------------------------------------------------------------------------------------

    /** The river value: the mean of the last 5 day means (Blueprint › Conditions › Water temperature), floored. */
    public double riverC(long absDay) {
        double sum = 0;
        int n = c.waterFollowDays();
        for (int i = 0; i < n; i++) sum += day(absDay - i).meanC();
        return Math.max(c.waterFloorC(), sum / n);
    }

    /** Creeks run 2 °C colder than the river value. */
    public double creekC(long absDay) { return Math.max(c.waterFloorC(), riverC(absDay) + c.creekOffsetC()); }

    /** Lakes run 2 °C warmer in summer (June–August). */
    public double lakeC(long absDay) {
        int m = Calendar.month(Calendar.dayOfYearOfDay(absDay));
        return riverC(absDay) + (m >= 6 && m <= 8 ? c.lakeSummerOffsetC() : 0);
    }
}
