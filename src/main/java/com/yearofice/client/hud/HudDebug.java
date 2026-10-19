package com.yearofice.client.hud;

import net.minecraft.Util;

/**
 * The C1 prototype's debug data source (Player systems and client › P1, the HUD layer stack): when on, every layer
 * draws test content in its anchor, cycling every 2 s between the full stack and the battery-at-0 state, so the
 * stack can be checked for overlaps, F1 and the battery rule without the systems behind it. Off in play; the
 * client smoke ({@code YEAROFICE_SMOKE=hud}) turns it on.
 */
public final class HudDebug {
    private HudDebug() {}

    public enum Mode { OFF, CYCLE, FULL, BATTERY_ZERO }

    private static volatile Mode mode = Mode.OFF;

    public static void set(Mode m) { mode = m; }

    public static Mode mode() { return mode; }

    public static boolean on() { return mode != Mode.OFF; }

    /** The battery-at-0 state: layers 5, 6 and 10 blank. */
    public static boolean batteryZero() {
        return switch (mode) {
            case BATTERY_ZERO -> true;
            case CYCLE -> (Util.getMillis() / 2000) % 2 == 1;
            default -> false;
        };
    }
}
