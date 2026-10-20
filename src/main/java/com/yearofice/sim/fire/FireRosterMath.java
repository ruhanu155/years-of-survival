package com.yearofice.sim.fire;

import com.yearofice.sim.env.FireSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The fire roster's arithmetic (Every concept from the video › Fire time and the fire roster). One row per lit fire a
 * player built, at most {@link #MAX_ROWS}, sorted by fuel left; the header totals the day's wood:
 * {@code A_day = sum(1440 * u / (t_armload * k_fuel))}, with coals and banked fires counting 0. A fire under
 * {@link #AMBER_MIN} minutes of fuel with something on it turns amber. Pure Java.
 *
 * @param kind            what the fire is called ("Fire", "Teepee", "Long fire", "Dakota hole")
 * @param state           the fire's state
 * @param setting         its setting
 * @param minutesLeft     game minutes until its state ends on its own
 * @param armloadsPerDay  armloads a day it burns at this setting (0 for coals and banked fires)
 * @param busy            true when a pot (or food) is on it, so running dry costs something
 * @param distanceM       blocks from the player
 */
public final class FireRosterMath {
    private FireRosterMath() {}

    public static final int MAX_ROWS = 16;
    public static final double AMBER_MIN = 15;
    /** The roster lists fires within 2 km. */
    public static final double RANGE_M = 2000;
    /** Past this distance the crackle no longer tells the player, so a fire turning amber sends a line. */
    public static final double HEARD_M = 30;

    public record Row(String kind, FireState state, FireSetting setting, double minutesLeft, double armloadsPerDay, boolean busy,
                      double distanceM) {
        /** Low fuel with something on the fire. */
        public boolean amber() { return busy && state == FireState.FIRE && minutesLeft < AMBER_MIN; }

        /** "Fire, Steady. Fuel 52 min." and its kin; the words of the look readout without weather or pot. */
        public String line() {
            return switch (state) {
                case FIRE -> kind + ", " + Fire.word(setting) + ". Fuel " + FireTime.format(minutesLeft) + "." + (amber() ? " Low fuel." : "");
                case SMALL -> "Small fire. Kindling " + FireTime.format(minutesLeft) + ".";
                case COALS -> "Coals. Out in " + FireTime.format(minutesLeft) + ".";
                case BANKED -> "Banked. Relights for " + FireTime.format(minutesLeft) + ".";
                case TINDER -> "Tinder flame.";
                case EMBER -> "Ember.";
                case OUT -> kind + ".";
            };
        }
    }

    /** True for a fire the roster lists: lit, not just laid or smouldering. */
    public static boolean listed(FireState s) {
        return s == FireState.SMALL || s == FireState.FIRE || s == FireState.COALS || s == FireState.BANKED;
    }

    /** The listed rows within range, least fuel first, at most {@link #MAX_ROWS}. */
    public static List<Row> sorted(List<Row> rows) {
        List<Row> out = new ArrayList<>();
        for (Row r : rows) if (listed(r.state()) && r.distanceM() <= RANGE_M) out.add(r);
        out.sort(Comparator.comparingDouble(Row::minutesLeft));
        return out.size() > MAX_ROWS ? new ArrayList<>(out.subList(0, MAX_ROWS)) : out;
    }

    /** Armloads a day, whole. */
    public static long armloadsPerDay(List<Row> rows) {
        double sum = 0;
        for (Row r : rows) sum += r.armloadsPerDay();
        return Math.round(sum);
    }

    /** "3 fires: about 9 armloads a day." */
    public static String header(List<Row> rows) {
        int n = rows.size();
        if (n == 0) return "No fires lit.";
        long a = armloadsPerDay(rows);
        return n + (n == 1 ? " fire" : " fires") + ": about " + a + (a == 1 ? " armload" : " armloads") + " a day.";
    }
}
