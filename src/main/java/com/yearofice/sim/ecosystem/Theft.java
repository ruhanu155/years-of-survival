package com.yearofice.sim.ecosystem;

import com.yearofice.core.hash.Mulberry32;
import java.util.List;
import java.util.Optional;

/**
 * Theft by animals at the camp's stores (Structures and buildables › Thieves at the rack; Items › Theft by animals;
 * RECONCILE 170 A3). Each game hour, while the player is more than 30 m away, every thief that is out at this hour
 * rolls its chance; one that wins takes something it can carry from the store. Pure Java, no Minecraft imports: the
 * adapter reads the store, calls {@link #rollHour} with a named stream and moves the item.
 *
 * <p>Chances per game hour (Structures table): red fox 4% at night, marten 2% at night within 300 m of spruce, gray
 * jay 8% by day, raven 3% by day; the red squirrel takes 10% of a basket of berries or seeds in a day (Items), about
 * 0.44% an hour. Protections: a closed fenced gate stops the fox, only a roof stops the marten, a roof or a lit
 * smoker halves the birds, a tamed wolf within 20 m is ×0.25, a named jay takes nothing, and a player within 30 m
 * makes every chance 0. Fish and meat are what jays, ravens and martens take; the fox also takes hides.
 */
public final class Theft {
    private Theft() {}

    public enum Thief {
        RED_FOX(true), MARTEN(true), GRAY_JAY(false), RAVEN(false), RED_SQUIRREL(false);

        public final boolean night;

        Thief(boolean night) { this.night = night; }
    }

    public enum Kind { MEAT, FISH, HIDE, BERRY_SEED }

    /** One stack in the store: what it is, how heavy one stack is in kg, and how many units it holds. */
    public record Stack(Kind kind, double kg, int units) {}

    /**
     * @param night       the hour is a night hour
     * @param fenced      inside a pole fence with its gate closed
     * @param roofed      under a roof
     * @param smoker      a lit smoker within 4 blocks
     * @param tamedWolf   a tamed wolf within 20 m
     * @param namedJay    the local jay is named (it takes nothing)
     * @param nearSpruce  within 300 m of spruce, where martens live
     * @param playerM     distance to the nearest player, metres
     */
    public record Context(boolean night, boolean fenced, boolean roofed, boolean smoker, boolean tamedWolf, boolean namedJay,
                          boolean nearSpruce, double playerM) {}

    public record Hit(Thief thief, int stack, int units) {}

    public static final double PLAYER_SAFE_M = 30;
    static final double SQUIRREL_PER_HOUR = 1 - Math.pow(0.9, 1.0 / 24.0);

    public static double base(Thief t) {
        return switch (t) {
            case RED_FOX -> 0.04;
            case MARTEN -> 0.02;
            case GRAY_JAY -> 0.08;
            case RAVEN -> 0.03;
            case RED_SQUIRREL -> SQUIRREL_PER_HOUR;
        };
    }

    /** The chance this hour, protections and the preset's multiplier applied. */
    public static double chance(Thief t, Context c, double presetMult) {
        if (c.playerM() <= PLAYER_SAFE_M) return 0;
        if (t.night != c.night()) return 0;
        double p = base(t) * presetMult;
        switch (t) {
            case RED_FOX -> { if (c.fenced()) return 0; }
            case MARTEN -> { if (c.roofed() || !c.nearSpruce()) return 0; }
            case GRAY_JAY -> { if (c.namedJay()) return 0; if (c.roofed() || c.smoker()) p *= 0.5; }
            case RAVEN -> { if (c.roofed() || c.smoker()) p *= 0.5; }
            case RED_SQUIRREL -> { if (c.roofed() && c.fenced()) return 0; }
        }
        if (c.tamedWolf()) p *= 0.25;
        return Math.min(1, p);
    }

    /** True when this thief can carry this stack away. */
    public static boolean takes(Thief t, Stack s) {
        return switch (t) {
            case RED_FOX -> (s.kind == Kind.HIDE && s.kg <= 2.0) || (s.kind == Kind.MEAT && s.kg <= 2.0);
            case MARTEN -> s.kind == Kind.MEAT || s.kind == Kind.FISH;
            case GRAY_JAY -> (s.kind == Kind.MEAT || s.kind == Kind.FISH) && s.kg <= 0.2;
            case RAVEN -> s.kind == Kind.MEAT && s.kg <= 0.5;
            case RED_SQUIRREL -> s.kind == Kind.BERRY_SEED;
        };
    }

    /** How many units the thief takes from a stack: one, or for the squirrel 10% of the contents (at least one). */
    public static int unitsTaken(Thief t, Stack s) {
        if (t == Thief.RED_SQUIRREL) return Math.max(1, (int) Math.round(s.units * 0.1));
        return 1;
    }

    /**
     * One hour of rolls over the store's stacks, thieves in table order, stopping at the first theft. The stream
     * keys the store and the hour, so an unloaded rack replayed on chunk load loses what it would have lost live.
     */
    public static Optional<Hit> rollHour(Context c, double presetMult, Mulberry32 r, List<Stack> stacks) {
        return rollHour(c, presetMult, r, stacks, java.util.Set.of());
    }

    /** As {@link #rollHour(Context, double, Mulberry32, List)}, with thieves in {@code live} skipped: they are in the air as birds and take what they take there. */
    public static Optional<Hit> rollHour(Context c, double presetMult, Mulberry32 r, List<Stack> stacks, java.util.Set<Thief> live) {
        for (Thief t : Thief.values()) {
            double p = live.contains(t) ? 0 : chance(t, c, presetMult);
            double u = r.nextDouble(); // always drawn, so the stream's position does not depend on protections
            if (p <= 0 || u >= p) continue;
            int[] can = new int[stacks.size()];
            int n = 0;
            for (int i = 0; i < stacks.size(); i++) if (stacks.get(i).units > 0 && takes(t, stacks.get(i))) can[n++] = i;
            if (n == 0) continue;
            int i = can[r.nextInt(n)];
            return Optional.of(new Hit(t, i, Math.min(stacks.get(i).units, unitsTaken(t, stacks.get(i)))));
        }
        return Optional.empty();
    }
}
