package com.yearofice.sim.fishing;

/**
 * The bait on the hook (Blueprint › Fishing in depth › Tackle, Bait: "Lasts on the hook"): a worm lasts 2 bites or
 * 30 minutes in the water. Every bite uses one, hooked or not; a fish that takes the bait at an unanswered dip
 * takes all of it.
 *
 * <p>Pure Java, immutable. Original Year of Ice code.
 *
 * @param bait        the bait key that fills m_bait ({@code "worm"}), or empty for a bare hook
 * @param bitesLeft   bites before it is gone
 * @param minutesLeft game minutes in the water before it is gone
 */
public record HookBait(String bait, int bitesLeft, int minutesLeft) {
    public static final HookBait NONE = new HookBait("", 0, 0);

    public HookBait {
        if (bait == null || bait.isEmpty() || bitesLeft <= 0 || minutesLeft <= 0) {
            bait = "";
            bitesLeft = 0;
            minutesLeft = 0;
        }
    }

    public boolean present() { return !bait.isEmpty(); }

    public HookBait afterBite() { return new HookBait(bait, bitesLeft - 1, minutesLeft); }

    public HookBait afterMinutes(int minutes) { return new HookBait(bait, bitesLeft, minutesLeft - minutes); }

    public static HookBait fitted(String bait, int bites, int minutes) { return new HookBait(bait, bites, minutes); }
}
