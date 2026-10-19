package com.yearofice.sim.fishing;

import com.yearofice.sim.fishing.BiteModel.Conditions;
import com.yearofice.sim.fishing.BiteModel.TimeBand;
import com.yearofice.sim.fishing.BiteModel.Weather;

/**
 * The slice's bite roll at the bobber, one game minute at a time: what {@code hunt.fishing} reads at the bait,
 * folded into {@link BiteModel.Conditions}, then rolled from a named stream of (seed, spot, minute). Factors the
 * slice has no layer for yet (pressure, hatch, ice) are off; water temperature and turbidity come from config until
 * {@code env.water} lands.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class SliceBite {
    private SliceBite() {}

    public static final int SALT_BITE = 1, SALT_NIBBLE = 2, SALT_LEAD = 3, SALT_HOOK = 4, SALT_BAIT = 5,
            SALT_SIZE = 6, SALT_TROPHY = 7, SALT_BAND = 8;

    /**
     * m_time's band (Blueprint › Fishing in depth › Conditions, m_time row): dawn and dusk are the 2 game hours
     * around sunrise and sunset; 11:00–15:00 is midday; night is the sun below −6°, which is {@code twilightMin}
     * past sunset and before sunrise. Dawn and dusk win over night.
     */
    public static TimeBand timeBand(int minuteOfDay, int sunriseMin, int sunsetMin, int twilightMin) {
        int m = Math.floorMod(minuteOfDay, 1440);
        if (Math.abs(m - sunriseMin) <= 60 || Math.abs(m - sunsetMin) <= 60) return TimeBand.DAWN_DUSK;
        if (m < sunriseMin - twilightMin || m > sunsetMin + twilightMin) return TimeBand.NIGHT;
        if (m >= 11 * 60 && m < 15 * 60) return TimeBand.MIDDAY;
        return TimeBand.DAY;
    }

    /**
     * Everything read at the bait for one minute.
     *
     * @param atSpot     the bobber's column meets the spot rule (m_place 1.0), else 0.05
     * @param dayOfYear  1 = May 1
     */
    public record Inputs(FishSpecies species, FishSeason season, String spotType, boolean atSpot, double spotQ,
                         double spotDepthM, double spotRadiusM, double stockN, double stockK, HookBait bait,
                         int dayOfYear, TimeBand time, Weather weather, double waterTempC, double turbidity,
                         boolean spooked) {}

    public static Conditions conditions(Inputs in) {
        boolean clear = in.weather() == Weather.CLEAR;
        double distance = in.atSpot() ? 0.0 : 1e9;
        return new Conditions(in.time(), clear, in.spotDepthM(), in.weather(), false,
                in.season().factor(in.dayOfYear(), in.spotType()), in.waterTempC(), in.turbidity(), false, false, 0,
                false, in.spotQ(), in.stockN(), in.stockK(), distance, in.spotRadiusM(), in.bait().bait(), in.spooked());
    }

    /** The chance of a bite this minute; a bare hook never bites. */
    public static double pBite(Inputs in) {
        if (!in.bait().present()) return 0.0;
        return BiteModel.pBite(in.species(), conditions(in));
    }

    /** Whether the fish bites in absolute game minute {@code minute} at the spot keyed {@code spotKey}. */
    public static boolean bites(Inputs in, long seed, long spotKey, long minute) {
        return Draws.u(seed, spotKey, minute, SALT_BITE) < pBite(in);
    }

    /** A nibble (a false bite that shakes the bobber) in a minute with no bite: {@code share} × P_bite. */
    public static boolean nibbles(Inputs in, double share, long seed, long spotKey, long minute) {
        return Draws.u(seed, spotKey, minute, SALT_NIBBLE) < share * pBite(in);
    }

    /**
     * The first game minute in [0, maxMinutes) with a bite, from {@code start}, holding the inputs fixed, or −1.
     * The bait's 30 minutes are counted down as the adapter does.
     */
    public static int firstBite(Inputs in, long seed, long spotKey, long start, int maxMinutes) {
        HookBait bait = in.bait();
        for (int i = 0; i < maxMinutes; i++) {
            if (!bait.present()) return -1;
            Inputs now = new Inputs(in.species(), in.season(), in.spotType(), in.atSpot(), in.spotQ(), in.spotDepthM(),
                    in.spotRadiusM(), in.stockN(), in.stockK(), bait, in.dayOfYear(), in.time(), in.weather(),
                    in.waterTempC(), in.turbidity(), in.spooked());
            if (bites(now, seed, spotKey, start + i)) return i;
            bait = bait.afterMinutes(1);
        }
        return -1;
    }
}
