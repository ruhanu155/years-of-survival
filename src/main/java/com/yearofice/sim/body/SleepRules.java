package com.yearofice.sim.body;

/**
 * Blueprint › Survival tuning › Sleep, the rules a bed applies (pure Java; the adapter is {@code body.sleep}).
 *
 * <ul>
 *   <li><b>When the player can sleep.</b> Energy below 7, or the clock between 21:00 and 06:00. The clock rule, not
 *       the sun, defines night, because the June sun barely sets at 64°N.</li>
 *   <li><b>Restoring.</b> 1.25 bolts a game hour times quality q; {@link #quality} is the bed's factor (bare ground
 *       0.70, debris bed 0.85, bough bed 0.95, bunk 1.0). Bugs, Spirits, pain and light join it with M4.</li>
 *   <li><b>Sleeping cold.</b> Feels-like counts 2 °C lower asleep, before the bed's own insulation is added (the bough
 *       bed's +5 °C, Blueprint › Making camp). The body model adds both; {@link #warmthC} is the bed's part.</li>
 *   <li><b>What wakes the player.</b> Each cause wakes them at once and names itself: warmth below 40 or wetness over
 *       60%; thirst (water below 15); a large animal within 30 m or a predator within 60 m. A sleep begun at night ends
 *       in the morning (06:00); one begun by day, tired, ends when rested.</li>
 * </ul>
 *
 * <p>Hunger after 4 hours, rain on an unroofed bed and smoke or spreading fire are M3–M4 wake causes; they join
 * {@link Wake} when their inputs exist.
 */
public final class SleepRules {
    private SleepRules() {}

    /** Why a sleeper wakes, in the order the rules check them; NONE keeps them asleep. */
    public enum Wake {
        NONE("none"), COLD("cold"), WET("wet"), THIRST("thirsty"), DANGER("an animal nearby"), MORNING("morning"),
        RESTED("rested");

        /** The cause as the messenger names it ("Woke: 03:10, cold."). */
        public final String said;

        Wake(String said) { this.said = said; }

        public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    /**
     * The numbers of {@code yearofice-common.toml [sleep]}.
     *
     * @param tiredEnergy     below this many bolts a player may sleep at any hour (7)
     * @param nightFromMin    minute of the day the clock's night begins (21:00 = 1,260)
     * @param morningMin      minute of the day it ends, and a night sleeper wakes (06:00 = 360)
     * @param wakeWarmth      warmth below which the cold wakes a sleeper (40)
     * @param wakeWetness     wetness above which a sleeper wakes, 0–1 (0.6)
     * @param wakeWater       water below which thirst wakes a sleeper (15)
     * @param largeAnimalM    any large animal this near wakes a sleeper, metres (30)
     * @param predatorM       any predator this near wakes a sleeper, metres (60)
     * @param groundQuality   q on bare ground, with no bed (0.70)
     */
    public record Tuning(double tiredEnergy, int nightFromMin, int morningMin, double wakeWarmth, double wakeWetness,
                         double wakeWater, double largeAnimalM, double predatorM, double groundQuality) {

        public Tuning {
            if (nightFromMin < 0 || nightFromMin >= 1440 || morningMin < 0 || morningMin >= 1440) {
                throw new IllegalArgumentException("night bounds are minutes of the day");
            }
            if (!(groundQuality > 0) || groundQuality > 1) throw new IllegalArgumentException("quality is 0-1");
        }

        /** The Blueprint's numbers; every preset uses them (the Cold slider already scales the night's chill). */
        public static final Tuning STANDARD = new Tuning(7, 21 * 60, 6 * 60, 40, 0.6, 15, 30, 60, 0.70);
    }

    /** True between the clock's night bounds (21:00–06:00 by default). */
    public static boolean isNight(int minuteOfDay, Tuning t) {
        int m = Math.floorMod(minuteOfDay, 1440);
        return t.nightFromMin() > t.morningMin() ? (m >= t.nightFromMin() || m < t.morningMin())
                : (m >= t.nightFromMin() && m < t.morningMin());
    }

    /** "When the player can sleep": energy below {@code tired_energy}, or the clock's night. */
    public static boolean mayLieDown(double energy, int minuteOfDay, Tuning t) {
        return energy < t.tiredEnergy() || isNight(minuteOfDay, t);
    }

    /**
     * Sleep quality q for a bed factor; {@code bedQuality} NaN means bare ground. The other factors (bugs, Spirits,
     * pain, light) are 1 until M4.
     */
    public static double quality(double bedQuality, Tuning t) {
        double q = Double.isNaN(bedQuality) ? t.groundQuality() : bedQuality;
        return Math.max(0, Math.min(1, q));
    }

    /** Sleep restores less while a fever shows (x0.7): the sleeper is resting, not recovering. */
    public static double feverFactor(BodyState s, long minute, WellnessTuning w) {
        return IllnessModel.active(s, "fever", minute) ? w.feverSleepMult() : 1.0;
    }

    /** The bed's insulation while asleep, °C; NaN (bare ground) is 0. */
    public static double warmthC(double bedWarmthC) { return Double.isNaN(bedWarmthC) ? 0 : bedWarmthC; }

    /**
     * What one check of a sleeper sees.
     *
     * @param minuteOfDay  the clock now, 0–1,439
     * @param nightSleep   the sleep began in the clock's night (else it began by day, tired)
     * @param sleptMin     game minutes asleep so far
     * @param wetness      0–1
     * @param danger       a large animal or predator inside its wake distance
     */
    public record Facts(int minuteOfDay, boolean nightSleep, int sleptMin, double warmth, double wetness, double water,
                        double energy, double energyCap, boolean danger) {}

    /** The first wake cause that holds, or NONE. */
    public static Wake check(Facts f, Tuning t) {
        if (f.warmth() < t.wakeWarmth()) return Wake.COLD;
        if (f.wetness() > t.wakeWetness()) return Wake.WET;
        if (f.water() < t.wakeWater()) return Wake.THIRST;
        if (f.danger()) return Wake.DANGER;
        if (f.nightSleep()) {
            if (f.sleptMin() > 0 && !isNight(f.minuteOfDay(), t)) return Wake.MORNING;
        } else if (f.energy() >= f.energyCap() - 1e-6) {
            return Wake.RESTED;
        }
        return Wake.NONE;
    }
}
