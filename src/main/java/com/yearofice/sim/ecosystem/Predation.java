package com.yearofice.sim.ecosystem;

import java.util.List;

/**
 * Who hunts whom and how far (Blueprint › Who hunts whom; Land animals in depth › Mob sheets: predators). The table
 * itself sits in each predator's species file ({@code predator.prey}); this is the pure rule set around it:
 * a chase runs at most {@code chaseCapM} metres (wolf 150, lynx 60) and then ends, kill or not.
 */
public final class Predation {
    private Predation() {}

    /** The {@code predator} block of a species file. */
    public record Rule(List<String> prey, double chaseCapM, double detectM, double chaseMps, int huntFromHour, int huntToHour,
                       double killRadiusM, double cooldownMin) {
        public boolean takes(String preyId) { return prey.contains(preyId); }

        /** True when the hour of day is inside the hunting window (it may wrap midnight). */
        public boolean huntsAt(int hour) {
            return huntFromHour <= huntToHour ? hour >= huntFromHour && hour < huntToHour : hour >= huntFromHour || hour < huntToHour;
        }
    }

    /** One chase in progress: the metres run so far against the cap. */
    public static final class Chase {
        private final double capM;
        private double ranM;

        public Chase(double capM) { this.capM = capM; }

        public void advance(double metres) { ranM += Math.max(0, metres); }

        public double ranM() { return ranM; }

        public boolean over() { return ranM >= capM; }

        public double capM() { return capM; }
    }

    /**
     * The predator clock (Field care): from April to October a bear within 1.5 km arrives at a kill or a gut pile
     * with 4% a game hour, doubled at dusk; from November to April wolves arrive with 3% an hour.
     */
    public static double arrivalPerHour(String predator, int month, int hour) {
        boolean bear = predator.endsWith("black_bear") || predator.endsWith("grizzly");
        if (bear) {
            if (month < 4 || month > 10) return 0;
            return hour >= 17 && hour < 21 ? 0.08 : 0.04;
        }
        if (predator.endsWith("wolf")) return month >= 11 || month <= 4 ? 0.03 : 0;
        return 0;
    }

    /**
     * Does a bear reach an uncleared cleaning spot (or a kill) in {@code hours} game hours, starting at
     * {@code startHour} of a day in {@code month}? One seeded roll per hour against {@link #arrivalPerHour}.
     */
    public static boolean bearArrives(long seed, int month, int startHour, int hours) {
        java.util.SplittableRandom r = new java.util.SplittableRandom(seed);
        for (int i = 0; i < hours; i++) {
            if (r.nextDouble() < arrivalPerHour("yearofice:grizzly", month, (startHour + i) % 24)) return true;
        }
        return false;
    }
}
