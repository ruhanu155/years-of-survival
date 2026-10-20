package com.yearofice.sim.ecosystem;

import com.yearofice.core.hash.Mulberry32;
import java.util.Optional;

/**
 * Door entry odds (Structures and buildables › Door left open; RECONCILE 170 A2). While the player is more than 20 m
 * away, each open building rolls once a game hour. Pure Java. Original Year of Ice code.
 *
 * <p>Fox, marten and ermine: night, food scent 0.3 or more inside, 5%. Wolf: any time, the pack's hunger 0.6 or more,
 * the pack within 300 m and food inside, 2%. Grizzly or black bear: outside the den months, scent 0.6 or more, 3%.
 */
public final class RaidOdds {
    private RaidOdds() {}

    public enum Raider { FOX, WOLF, BEAR }

    public static final double PLAYER_SAFE_M = 20;

    /**
     * @param doorOpen   a door or hatch of the building stands open
     * @param night      the hour is a night hour
     * @param denMonths  bears are denned (November to March)
     * @param scent      the food scent inside, on the Items 0-3 scale
     * @param packHunger the nearest wolf pack's hunger drive, 0-1
     * @param packWithin300m a wolf pack is within 300 m
     * @param playerM    distance to the nearest player, metres
     */
    public record Door(boolean doorOpen, boolean night, boolean denMonths, double scent, double packHunger,
                       boolean packWithin300m, double playerM) {}

    public static double base(Raider r) {
        return switch (r) { case FOX -> 0.05; case WOLF -> 0.02; case BEAR -> 0.03; };
    }

    /** The chance this hour; {@code scale} is the preset's {@code door_entry_scale}. */
    public static double chance(Raider r, Door d, double scale) {
        if (!d.doorOpen() || d.playerM() <= PLAYER_SAFE_M) return 0;
        boolean ok = switch (r) {
            case FOX -> d.night() && d.scent() >= 0.3;
            case WOLF -> d.packHunger() >= 0.6 && d.packWithin300m() && d.scent() > 0;
            case BEAR -> !d.denMonths() && d.scent() >= 0.6;
        };
        return ok ? Math.min(1, base(r) * scale) : 0;
    }

    /** One hour: raiders in table order, one draw each (always drawn, so the stream position never depends on conditions). */
    public static Optional<Raider> rollHour(Door d, double scale, Mulberry32 r) {
        Raider hit = null;
        for (Raider x : Raider.values()) {
            double u = r.nextDouble();
            if (hit == null && u < chance(x, d, scale)) hit = x;
        }
        return Optional.ofNullable(hit);
    }
}
