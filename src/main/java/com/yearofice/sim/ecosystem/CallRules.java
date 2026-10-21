package com.yearofice.sim.ecosystem;

import com.yearofice.sim.time.Calendar;

/**
 * Animal calls (Land animals in depth › Mob sheets; Water, calls and encounters): which species calls what, how far
 * it carries and when. Pure Java. A range over 128 m goes out as a {@code distant_sound} payload.
 */
public final class CallRules {
    private CallRules() {}

    public record Call(String sound, double rangeM) {}

    /** Wolf howl at dusk, 3 km (the sound test's 3 km howl). */
    public static final Call WOLF_HOWL = new Call("yearofice:entity.wolf.howl", 3000);
    public static final Call MOOSE_GRUNT = new Call("yearofice:call.moose_grunt", 800);
    public static final Call LYNX_YOWL = new Call("yearofice:call.lynx_yowl", 800);
    public static final Call FOX_BARK = new Call("yearofice:call.fox_bark", 300);
    public static final Call VIXEN_SCREAM = new Call("yearofice:call.vixen_scream", 500);
    public static final Call CARIBOU_RUN = new Call("yearofice:call.caribou_run", 600);
    public static final Call WOLVERINE_GROWL = new Call("yearofice:call.wolverine_growl", 60);

    /** The call this species makes unprompted at this date and hour, or null. */
    public static Call routine(String species, int doy, int hour) {
        boolean dusk = hour >= 17 && hour < 20;
        if (species.endsWith("wolf") && dusk) return WOLF_HOWL;
        if (species.endsWith("moose") && Calendar.within(doy, Calendar.dayOfYear(9, 10), Calendar.dayOfYear(10, 10)) && (dusk || hour >= 5 && hour < 8)) return MOOSE_GRUNT;
        if (species.endsWith("lynx") && Calendar.month(doy) == 3 && (hour >= 21 || hour < 4)) return LYNX_YOWL;
        if (species.endsWith("red_fox") && dusk) return Calendar.month(doy) == 2 ? VIXEN_SCREAM : FOX_BARK;
        return null;
    }

    /** True when a listener at {@code distM} hears the call. */
    public static boolean heard(Call c, double distM) { return distM <= c.rangeM(); }
}
