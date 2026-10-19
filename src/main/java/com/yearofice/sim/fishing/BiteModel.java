package com.yearofice.sim.fishing;

import java.util.List;

/**
 * The bite roll (Blueprint › Fishing in depth › Conditions, Reading the water, Fish stocks; Wildlife and fish).
 *
 * <pre>
 *   P_bite = b · q · m_bait · clamp(m_time · m_weather · m_season · m_water, 0.1, 3.0) · m_stock · m_place
 * </pre>
 *
 * Each species at a spot rolls separately each game minute; the first success, in species-id order, is the
 * bite. This is the pure formula; the adapter {@code hunt.fishing.BiteModel} fills {@link Conditions} from the
 * Environment field and the weather stage.
 *
 * <p><b>From Tide 2.</b> Tide filters a catch by composable conditions on the hook: depth layer, biome, weather,
 * time of day, moon. We keep that shape (one record of conditions read at the bait, each one a named factor),
 * but our factors scale a chance instead of gating a loot table, because the blueprint wants every factor to
 * shift the odds, not to switch fish on and off. No Tide code is copied.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class BiteModel {
    private BiteModel() {}

    public static final double PRODUCT_MIN = 0.1;
    public static final double PRODUCT_MAX = 3.0;
    public static final double SPOOKED = 0.5;

    public enum TimeBand { DAWN_DUSK, MIDDAY, NIGHT, DAY }

    public enum Weather { CLEAR, CLOUDY, RAIN, STORM, SNOW, BLIZZARD }

    /**
     * Everything read at the bait for one game minute.
     *
     * @param spotDepthM      depth of the spot (for the midday shallow rule)
     * @param seasonFactor    m_season: 1.0 in window, 0.5 first and last month, ×3 at a run, 0 or 0.3 outside
     * @param waterTempC      water temperature
     * @param turbidity       0 clear to 1 muddy
     * @param baitOnBottom    a sinker holds the bait on the bottom (burbot ignore turbidity)
     * @param hatch           an insect hatch is on (June–August, cloudy or rain, water 8–16 °C)
     * @param snowOnIceCm     snow lying on the ice
     * @param distanceToSpotM bait distance from the spot centre
     * @param spotRadiusM     the spot's radius
     * @param spooked         a strike at a nibble spooked this spot in the last 10 game minutes
     */
    public record Conditions(TimeBand time, boolean clearSky, double spotDepthM, Weather weather,
                             boolean pressureFalling, double seasonFactor, double waterTempC, double turbidity,
                             boolean baitOnBottom, boolean underIce, double snowOnIceCm, boolean hatch,
                             double spotQ, double stockN, double stockK, double distanceToSpotM,
                             double spotRadiusM, String baitKey, boolean spooked) {}

    public static double mTime(FishSpecies s, Conditions c) {
        return switch (c.time()) {
            case DAWN_DUSK -> c.underIce() ? 1.2 : 1.5;
            case MIDDAY -> (c.spotDepthM() < 1.5 && c.clearSky()) ? 0.6 : 0.7;
            case NIGHT -> s.nightFactor();
            case DAY -> 1.0;
        };
    }

    public static double mWeather(Conditions c) {
        if (c.pressureFalling()) return 1.4; // the 12 game hours before a front replace the value
        return switch (c.weather()) {
            case CLEAR, SNOW -> 1.0;
            case CLOUDY, RAIN -> 1.2;
            case STORM -> 0.5;
            case BLIZZARD -> 0.3;
        };
    }

    /** m_turbid = 1 − 0.6 · turbidity; bait on the bottom for burbot ignores it. */
    public static double mTurbid(FishSpecies s, Conditions c) {
        if (c.baitOnBottom() && s.id().equals("burbot")) return 1.0;
        return 1.0 - 0.6 * c.turbidity();
    }

    /** Temperature factor, plus the hatch and snow-on-ice rules, which also read the water. */
    public static double mWaterTemp(FishSpecies s, Conditions c) {
        double m = 1.0;
        if (c.waterTempC() < 4.0) m *= 0.7;
        if (c.waterTempC() > 18.0 && s.id().equals("arctic_grayling") && c.spotDepthM() < 1.5) m *= 0.6;
        if (c.hatch() && s.surfaceFeeder() && (c.baitKey().equals("fly") || c.baitKey().equals("insect"))) m *= 1.5;
        if (c.underIce() && c.snowOnIceCm() > 30.0) {
            if (s.id().equals("lake_trout")) m *= 0.8;
            if (s.id().equals("burbot")) m *= 1.2;
        }
        return m;
    }

    public static double mWater(FishSpecies s, Conditions c) { return mTurbid(s, c) * mWaterTemp(s, c); }

    /** m_stock = √(N/K): rod fishing feels a thinning stock gently at first. */
    public static double mStock(double n, double k) {
        if (k <= 0) return 0.0;
        return Math.sqrt(Math.max(0.0, n) / k);
    }

    /** m_place: 1.0 inside a spot's radius, 0.3 within twice it, 0.05 elsewhere. */
    public static double mPlace(double distanceM, double radiusM) {
        if (distanceM <= radiusM) return 1.0;
        if (distanceM <= 2.0 * radiusM) return 0.3;
        return 0.05;
    }

    public static double clampProduct(double v) { return Math.max(PRODUCT_MIN, Math.min(PRODUCT_MAX, v)); }

    /** The chance this species bites in this game minute. */
    public static double pBite(FishSpecies s, Conditions c) {
        double env = clampProduct(mTime(s, c) * mWeather(c) * c.seasonFactor() * mWater(s, c));
        if (c.seasonFactor() == 0.0) return 0.0; // absent outside its window (salmon)
        double p = s.bitesPerMin() * c.spotQ() * s.baitMultiplier(c.baitKey()) * env
                * mStock(c.stockN(), c.stockK()) * mPlace(c.distanceToSpotM(), c.spotRadiusM());
        if (c.spooked()) p *= SPOOKED;
        return Math.min(1.0, p);
    }

    /**
     * Roll one game minute: species in the given order (keep it sorted by id) each roll; the first success bites.
     *
     * @param draws one uniform draw per species, from the spot's named stream for this minute
     * @return the index of the biting species, or −1
     */
    public static int rollMinute(List<FishSpecies> species, List<Conditions> perSpecies, double[] draws) {
        for (int i = 0; i < species.size(); i++) {
            if (draws[i] < pBite(species.get(i), perSpecies.get(i))) return i;
        }
        return -1;
    }
}
