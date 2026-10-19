package com.yearofice.sim.weather;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.time.Sun;
import java.util.List;

/**
 * Reads {@code data/yearofice/weather/interior_alaska.json} (written by {@code tools/climate/make_climate.py})
 * into the pure records the clock and the env layer run on: the {@link Climate} and the daylight {@link Sun}.
 * Gson only, no Minecraft classes, so the clock, the env layer and the JUnit tests read the one file the same way.
 */
public final class ClimateJson {
    private ClimateJson() {}

    public record Parsed(Climate climate, Sun sun) {}

    static final List<String> STATES = List.of("clear", "cloudy", "wet", "severe");
    static final List<String> SEASONS = List.of("spring", "summer", "fall", "winter");

    public static Parsed parse(JsonObject root) {
        JsonObject normals = root.getAsJsonObject("normals");
        double[] mean = new double[12], swing = new double[12], lo = new double[12], hi = new double[12],
                precip = new double[12], wind = new double[12], hpa = new double[12], snow = new double[12],
                daylight = new double[12];
        for (int m = 1; m <= 12; m++) {
            JsonObject n = normals.getAsJsonObject(String.format("%02d", m));
            if (n == null) throw new IllegalArgumentException("normals: month " + m + " missing");
            mean[m - 1] = n.get("mean_c").getAsDouble();
            swing[m - 1] = n.get("swing_c").getAsDouble();
            JsonArray b = n.getAsJsonArray("bounds_c");
            lo[m - 1] = b.get(0).getAsDouble();
            hi[m - 1] = b.get(1).getAsDouble();
            precip[m - 1] = n.get("precip_mm").getAsDouble();
            wind[m - 1] = n.get("wind_ms").getAsDouble();
            hpa[m - 1] = n.get("pressure_hpa").getAsDouble();
            snow[m - 1] = n.get("snow_depth_cm").getAsDouble();
            daylight[m - 1] = n.get("daylight_h").getAsDouble();
        }
        double[] kappa = doubles(root.getAsJsonArray("hourly_curve"), 24);
        JsonObject wc = root.getAsJsonObject("wind_curve");
        double[] windCurve = doubles(wc.getAsJsonArray("values"), 24);
        boolean[] windMonths = new boolean[12];
        for (JsonElement e : wc.getAsJsonArray("months")) windMonths[Integer.parseInt(e.getAsString()) - 1] = true;

        JsonObject an = root.getAsJsonObject("anomaly");
        JsonObject mk = root.getAsJsonObject("markov");
        JsonObject windows = mk.getAsJsonObject("seasons");
        int[][] seasonWindow = new int[4][2];
        double[][][] markov = new double[4][4][4];
        for (int s = 0; s < 4; s++) {
            JsonArray w = windows.getAsJsonArray(SEASONS.get(s));
            seasonWindow[s][0] = doy(w.get(0).getAsString());
            seasonWindow[s][1] = doy(w.get(1).getAsString());
            JsonArray rows = mk.getAsJsonArray(SEASONS.get(s));
            for (int i = 0; i < 4; i++) {
                markov[s][i] = doubles(rows.get(i).getAsJsonArray(), 4);
                double sum = 0;
                for (double v : markov[s][i]) sum += v;
                if (Math.abs(sum - 1) > 1e-6) throw new IllegalArgumentException("markov " + SEASONS.get(s) + " row " + i + " sums to " + sum);
            }
        }
        DayState start = DayState.valueOf(mk.get("start_state").getAsString().toUpperCase(java.util.Locale.ROOT));

        JsonObject off = root.getAsJsonObject("state_offset_c");
        JsonObject windR = root.getAsJsonObject("wind_ms");
        double[][] windRange = new double[4][];
        for (int s = 0; s < 4; s++) windRange[s] = doubles(windR.getAsJsonArray(STATES.get(s)), 2);
        JsonObject pr = root.getAsJsonObject("precip");
        JsonObject win = pr.getAsJsonObject("window_h");
        JsonObject vis = root.getAsJsonObject("visibility_m");
        JsonObject air = root.getAsJsonObject("air");
        JsonObject wt = root.getAsJsonObject("wind_terrain");
        JsonObject water = root.getAsJsonObject("water");
        JsonObject ground = root.getAsJsonObject("ground").getAsJsonObject("monthly_c");

        Climate c = new Climate(mean, swing, lo, hi, precip, wind, hpa, snow, daylight, kappa, windCurve, windMonths,
                an.get("keep").getAsDouble(), an.get("noise_c").getAsDouble(), an.get("winter_severe_kick_c").getAsDouble(),
                an.get("start_c").getAsDouble(),
                byState(root.getAsJsonObject("sky_factor")),
                seasonWindow, markov, start,
                doubles(off.getAsJsonArray("summer"), 4), doubles(off.getAsJsonArray("winter"), 4), off.get("winter_below_c").getAsDouble(),
                windRange, byState(root.getAsJsonObject("gust_factor")),
                pr.get("severe_factor").getAsDouble(), pr.get("snow_cm_per_mm").getAsDouble(), pr.get("snow_below_c").getAsDouble(),
                pr.get("shoulder_band_c").getAsDouble(), pr.get("sleet_from_c").getAsDouble(), pr.get("rain_from_c").getAsDouble(),
                new int[] {win.get("wet").getAsInt(), win.get("severe").getAsInt()},
                pr.get("draw_low").getAsDouble(), pr.get("draw_high").getAsDouble(), pr.get("heavy_from_mm_h").getAsDouble(),
                pr.get("drizzle_below_mm_h").getAsDouble(),
                byState(root.getAsJsonObject("cloud_pct")), byState(root.getAsJsonObject("humidity_pct")),
                vis.get("full").getAsDouble(), vis.get("drizzle").getAsDouble(), vis.get("rain").getAsDouble(),
                vis.get("heavy_rain").getAsDouble(), vis.get("light_snow").getAsDouble(), vis.get("snow").getAsDouble(),
                vis.get("blizzard").getAsDouble(), vis.get("light_snow_below_cm_h").getAsDouble(), vis.get("blizzard_from_ms").getAsDouble(),
                air.get("valley_floor_y").getAsDouble(), air.get("lapse_c_per_100m").getAsDouble(),
                air.get("inversion_c_per_100m").getAsDouble(), air.get("inversion_max_c").getAsDouble(),
                air.get("inversion_wind_below_ms").getAsDouble(),
                wt.get("ridge_per_300m").getAsDouble(), wt.get("ridge_max").getAsDouble(), wt.get("canopy_factor").getAsDouble(),
                water.get("follow_days").getAsInt(), water.get("floor_c").getAsDouble(), water.get("creek_offset_c").getAsDouble(),
                water.get("lake_summer_offset_c").getAsDouble(),
                doubles(ground.getAsJsonArray("open_soil"), 12));

        JsonArray rows = root.getAsJsonObject("daylight").getAsJsonArray("rows");
        int n = Calendar.DAYS_PER_YEAR;
        if (rows.size() != n) throw new IllegalArgumentException("daylight: " + rows.size() + " rows, want 365");
        float[][] el = new float[n][24], az = new float[n][24];
        int[] rise = new int[n], set = new int[n];
        for (int d = 0; d < n; d++) {
            JsonObject r = rows.get(d).getAsJsonObject();
            rise[d] = r.get("sunrise_min").getAsInt();
            set[d] = r.get("sunset_min").getAsInt();
            JsonArray e = r.getAsJsonArray("elevation_deg"), a = r.getAsJsonArray("azimuth_deg");
            for (int h = 0; h < 24; h++) {
                el[d][h] = e.get(h).getAsFloat();
                az[d][h] = a.get(h).getAsFloat();
            }
        }
        return new Parsed(c, new Sun(el, az, rise, set));
    }

    private static int doy(String mmdd) {
        String[] p = mmdd.split("-");
        return Calendar.dayOfYear(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    private static double[] byState(JsonObject o) {
        double[] v = new double[4];
        for (int s = 0; s < 4; s++) v[s] = o.get(STATES.get(s)).getAsDouble();
        return v;
    }

    private static double[] doubles(JsonArray a, int n) {
        if (a.size() != n) throw new IllegalArgumentException("expected " + n + " values, got " + a.size());
        double[] v = new double[n];
        for (int i = 0; i < n; i++) v[i] = a.get(i).getAsDouble();
        return v;
    }
}
