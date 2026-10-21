package com.yearofice.sim.ecosystem;

/**
 * Drive scoring (Blueprint › Drives and daily schedules): hunger, thirst and rest are values 0–1 that rise at a
 * species rate per game hour and fall while the animal acts on them. Each step the animal picks the act with the
 * highest utility {@code U = w(hour) × D}, and switches only when another beats the current act by
 * {@value #SWITCH_MARGIN}, so it does not dither. Safety at or above {@value #SAFETY_OVERRIDE} overrides everything
 * (the state machine's Alert and Flee take over). Pure Java; the animal's routine calls {@link #step}.
 */
public final class Drives {
    private Drives() {}

    public static final double SWITCH_MARGIN = 0.1;
    public static final double SAFETY_OVERRIDE = 0.7;

    public enum Act { FEED, WATER, BED, SAFETY }

    /** Per game hour: how fast each drive rises awake and falls while acted on. */
    public record Rates(double hungerUp, double hungerDown, double thirstUp, double thirstDown, double restUp, double restDown) {
        /** The moose row, the reference (Blueprint › Drives and daily schedules). */
        public static final Rates MOOSE = new Rates(0.08, 0.25, 0.04, 1.0, 0.06, 0.20);
    }

    /** The day's shape: weights for each act in four blocks of the day, as the species file's {@code schedule}. */
    public record Weights(double[] dawn, double[] day, double[] dusk, double[] night) {
        /** w for an hour of day 0–23: dawn 05–09, day 09–17, dusk 17–21, night otherwise. */
        public double[] at(int hour) {
            if (hour >= 5 && hour < 9) return dawn;
            if (hour >= 9 && hour < 17) return day;
            if (hour >= 17 && hour < 21) return dusk;
            return night;
        }

        /** FEED, WATER, BED. */
        public static Weights of(double[] dawn, double[] day, double[] dusk, double[] night) { return new Weights(dawn, day, dusk, night); }

        /** Moose: feeds at dawn, beds in shade midday, drinks then feeds at dusk, feeds and beds in turns at night. */
        public static final Weights MOOSE = new Weights(new double[] {1.5, 0.7, 0.3}, new double[] {0.5, 0.35, 3.1},
                new double[] {1.2, 2.0, 0.3}, new double[] {1.0, 0.1, 1.0});
    }

    public static final class State {
        public double hunger = 0.5, thirst = 0.2, rest = 0.3;
        public Act act = Act.FEED;
        public int switches;
        public double safety;

        public double of(Act a) {
            return switch (a) {
                case FEED -> hunger;
                case WATER -> thirst;
                case BED -> rest;
                case SAFETY -> safety;
            };
        }
    }

    /** One step of {@code dtH} game hours at {@code hourOfDay} (fractional), the animal acting on its choice. */
    public static void step(State s, Rates r, Weights w, double hourOfDay, double dtH) { step(s, r, w, hourOfDay, dtH, true); }

    /**
     * One step. A drive falls only while the animal is {@code engaged} in its act (feeding on forage, at the
     * water, bedded); on the way there (not engaged) every drive keeps rising.
     */
    public static void step(State s, Rates r, Weights w, double hourOfDay, double dtH, boolean engaged) {
        // Drives rise while the animal is not acting on them, and fall while it is.
        s.hunger = clamp(s.hunger + (s.act == Act.FEED && engaged ? -r.hungerDown() : r.hungerUp()) * dtH);
        s.thirst = clamp(s.thirst + (s.act == Act.WATER && engaged ? -r.thirstDown() : r.thirstUp()) * dtH);
        s.rest = clamp(s.rest + (s.act == Act.BED && engaged ? -r.restDown() : r.restUp()) * dtH);
        if (s.safety >= SAFETY_OVERRIDE) {
            s.act = Act.SAFETY;
            return;
        }
        double[] wt = w.at((int) Math.floor(hourOfDay) % 24);
        Act best = s.act == Act.SAFETY ? Act.FEED : s.act;
        double bu = utility(s, best, wt);
        for (Act a : new Act[] {Act.FEED, Act.WATER, Act.BED}) {
            double u = utility(s, a, wt);
            if (a != best && u > bu + SWITCH_MARGIN) {
                best = a;
                bu = u;
            }
        }
        if (best != s.act) {
            s.act = best;
            s.switches++;
        }
    }

    static double utility(State s, Act a, double[] wt) {
        return switch (a) {
            case FEED -> wt[0] * s.hunger;
            case WATER -> wt[1] * s.thirst;
            case BED -> wt[2] * s.rest;
            case SAFETY -> 0;
        };
    }

    static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}
