package com.yearofice.sim.fire;

import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.body.WaterPurity.Heat;
import com.yearofice.sim.env.FireSetting;

/**
 * Water in a container on a fire (Blueprint › Fire, water and light › Boiling; Health › Gut illness). Pure Java: a
 * vessel holds {@code litres} of water of one {@link WaterPurity.Source}; {@code heat} runs from 0 (cold) to 1 (a
 * boil) and falls again off the fire; the water is {@code boiled} once it has held a rolling boil for
 * {@link WaterPurity#SAFE_BOIL_MIN} game minutes, and stays safe until the vessel is emptied.
 *
 * <p>The time to a boil is {@link WaterPurity#minutesToBoil}: the heat's minutes for 1 L, and a full 2 L pot 1.6
 * times as long. Boiled water stays hot (the +5 warmth cup) for {@link #HOT_MINUTES} game minutes off the fire.
 */
public record Vessel(double litres, WaterPurity.Source source, boolean boiled, double heat, double boilMinutes) {

    /** Boiled water stays hot for this long off the fire. */
    public static final double HOT_MINUTES = 30;
    /** Heat at or over which the water counts as hot to drink. */
    public static final double HOT_HEAT = 0.5;

    public Vessel {
        if (litres < 0) throw new IllegalArgumentException("litres");
        heat = Math.max(0, Math.min(1, heat));
        boilMinutes = Math.max(0, boilMinutes);
    }

    public static Vessel empty() { return new Vessel(0, WaterPurity.Source.RIVER, false, 0, 0); }

    public static Vessel of(double litres, WaterPurity.Source source) { return new Vessel(litres, source, false, 0, 0); }

    public boolean isEmpty() { return litres <= 1e-9; }

    /** True while the water is at a rolling boil. */
    public boolean boiling() { return !isEmpty() && heat >= 1.0 - 1e-9; }

    /** True when the water is hot enough for the warm-cup bonus. */
    public boolean hot() { return !isEmpty() && heat >= HOT_HEAT; }

    /** The fire's heat as a boiling class, or null when the fire gives none a pot can use. */
    public static Heat heatClass(FireState state, FireSetting setting, FireBuild build) {
        return switch (state) {
            case SMALL -> Heat.SMALL_FIRE;
            case COALS -> Heat.COALS;
            case FIRE -> build == FireBuild.TEEPEE ? Heat.TEEPEE : switch (setting) {
                case LOW -> Heat.SMALL_FIRE;
                case STEADY -> Heat.STEADY_FIRE;
                case HOT -> Heat.HOT_FIRE;
            };
            default -> null;
        };
    }

    /** One game minute on {@code h} (null off the fire). */
    public Vessel minute(Heat h) {
        if (isEmpty()) return this;
        if (h == null) {
            double cooled = heat - 1.0 / HOT_MINUTES;
            return new Vessel(litres, source, boiled, cooled, 0);
        }
        double rise = 1.0 / WaterPurity.minutesToBoil(h, litres);
        double next = heat + rise;
        double rolling = boilMinutes;
        if (next >= 1.0) {
            // The minute that reaches the boil counts only for the part past it.
            double over = heat >= 1.0 ? 1.0 : (next - 1.0) / rise;
            rolling += over;
            next = 1.0;
        } else {
            rolling = 0;
        }
        boolean safe = boiled || WaterPurity.safeAfterRollingBoil(rolling);
        return new Vessel(litres, source, safe, next, rolling);
    }

    /** {@code minutes} whole game minutes on {@code h}. */
    public Vessel minutes(Heat h, int minutes) {
        Vessel v = this;
        for (int i = 0; i < minutes; i++) v = v.minute(h);
        return v;
    }

    /** Takes {@code take} litres out; the rest keeps its state. */
    public Vessel without(double take) {
        double left = Math.max(0, litres - take);
        return left <= 1e-9 ? empty() : new Vessel(left, source, boiled, heat, boilMinutes);
    }

    /** A fresh fill of {@code litres} from {@code from}: cold and unboiled. */
    public static Vessel fresh(double litres, WaterPurity.Source from) { return of(litres, from); }

    /** Can this vessel take a fill from {@code from}? An empty one can; a part-full one only the same unboiled water. */
    public boolean canTake(WaterPurity.Source from, double capacity) {
        if (isEmpty()) return true;
        return !boiled && source == from && litres < capacity - 1e-9;
    }

    /** A top-up of unboiled water of the same source to {@code capacity}; the heat is averaged by volume. */
    public Vessel toppedUp(double capacity) {
        if (litres >= capacity) return this;
        return new Vessel(capacity, source, false, heat * litres / capacity, 0);
    }
}
