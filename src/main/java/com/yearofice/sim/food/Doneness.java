package com.yearofice.sim.food;

/**
 * Item heat at a fire station, mapped to our cooking (Blueprint › Cooking; Crafting › Stations; Fire states).
 *
 * <p>TFC heats an item toward a device's target temperature and cooks it at a recipe temperature. Our rules
 * are times, not temperatures: each method has a cook time, and food on a spit, stone or in coals shows a
 * colour ring Raw, Rare, Done, Burnt. Rare is pulled at 50–90% of cook time, Done from 90%, and Burnt comes
 * at 150% of cook time. So progress is cook-minutes over cook time, accrued only while the station's fire is in
 * a cooking state, stored as a value and a stamp, and settled on read, as TFC settles heat.
 *
 * <p>Reimplemented from understanding of TerraFirmaCraft's heat settling (no code copied, so no EUPL terms
 * apply). Pure Java. Numbers the blueprint does not give (cooking speed by fire setting) are 1.0 and flagged.
 */
public final class Doneness {
    private Doneness() {}

    public enum Method {
        SPIT(true), STONE_FRY(true), COAL_BAKE(true), STEW(false), RENDER(false);

        private final boolean showsRing;

        Method(boolean showsRing) { this.showsRing = showsRing; }

        /** Spit, stone and coals show the doneness ring; a pot does not burn its contents. */
        public boolean showsRing() { return showsRing; }
    }

    /** Fire states from Fire in detail that matter to a cook. */
    public enum Fire { OUT, EMBER, TINDER, SMALL_FIRE, LOW, STEADY, HOT, COALS, BANKED }

    public enum Ring { RAW, RARE, DONE, BURNT }

    public static final double RARE_FROM = 0.5;
    public static final double DONE_FROM = 0.9;
    public static final double BURNT_AT = 1.5;
    /**
     * "Burnt comes at 150% of cook time (75% on a Hot fire)". Read as: on a Hot fire the burn line is 75% of
     * 150%, so 112.5%. Flagged as an interpretation in the PR.
     */
    public static final double BURNT_AT_HOT = BURNT_AT * 0.75;

    /** Cook time in game minutes (Blueprint › Cooking table). */
    public static double cookMinutes(Method m, double kg, boolean wholeHare, boolean wholeFishUnder1kg, boolean bear) {
        double t = switch (m) {
            case SPIT -> wholeHare ? 25.0 : wholeFishUnder1kg ? 12.0 : 30.0 * kg;
            case STONE_FRY -> 10.0 * (kg / 0.5);
            case COAL_BAKE -> 40.0;
            case STEW -> 75.0; // 60–90 game minutes; the midpoint
            case RENDER -> 60.0 * kg;
        };
        return bear ? 2.0 * t : t; // bear must be Done at twice the time
    }

    /** True while a fire cooks food on a spit, stone, in coals or under a pot. */
    public static boolean cooks(Fire f) {
        return f == Fire.LOW || f == Fire.STEADY || f == Fire.HOT || f == Fire.COALS;
    }

    /** Coal bake cannot burn on a Low or Steady fire. */
    public static boolean canBurn(Method m, Fire f) {
        if (!m.showsRing()) return false;
        if (m == Method.COAL_BAKE) return f == Fire.HOT;
        return true;
    }

    /** The ring for a progress share of cook time. Hot fires burn sooner. */
    public static Ring ring(double progress, Method m, Fire f) {
        double burnt = f == Fire.HOT ? BURNT_AT_HOT : BURNT_AT;
        if (canBurn(m, f) && progress >= burnt) return Ring.BURNT;
        if (progress >= DONE_FROM) return Ring.DONE;
        if (progress >= RARE_FROM) return Ring.RARE;
        return Ring.RAW;
    }

    /**
     * A cook in progress: progress (share of cook time) at a stamp minute, accruing while the fire cooks.
     * Settle with the fire's state since the stamp; the fire's own state machine supplies its segments.
     */
    public record Cook(Method method, double cookMinutes, double progress, long stampMinute) {
        public Cook {
            if (!(cookMinutes > 0)) throw new IllegalArgumentException("cook time must be positive");
        }

        public static Cook start(Method m, double cookMinutes, long nowMinute) { return new Cook(m, cookMinutes, 0, nowMinute); }

        /** Progress after {@code minutes} more on a fire in state {@code f}. */
        public Cook after(long minutes, Fire f) {
            double add = cooks(f) ? minutes / cookMinutes : 0.0;
            return new Cook(method, cookMinutes, progress + add, stampMinute + minutes);
        }

        public Ring ring(Fire f) { return Doneness.ring(progress, method, f); }
    }

    /** Calories kept: Burnt keeps 70% (and costs 2 Spirits). */
    public static double calorieShare(Ring r) { return r == Ring.BURNT ? 0.7 : 1.0; }

    /** Parasites: Rare halves the chance, Done removes it. */
    public static double parasiteShare(Ring r) {
        return switch (r) {
            case RAW -> 1.0;
            case RARE -> 0.5;
            case DONE, BURNT -> 0.0;
        };
    }
}
