package com.yearofice.sim.fishing;

/**
 * The strike (Blueprint › Fishing in depth › Tackle, Hooks and Bait; The fight, as built: Wait and Strike; Food
 * systems › Fishing minigame, phase 3).
 *
 * <ul>
 *   <li>The rod tip twitches before a real bite; then the bobber dips, and the hook's window opens: a steel hook
 *       sets from 0 to 0.6 s after the dip, a gorge from 0.2 to 0.8 s.</li>
 *   <li>A strike inside the window sets the hook, except for the hook's loss on the strike (steel 5%, gorge 20–30%).</li>
 *   <li>A strike outside the window leaves the bait gone 60% of the time.</li>
 *   <li>A nibble is a false bite; striking at one spooks the spot (bites there ×0.5 for 10 game minutes).</li>
 *   <li>A left-mouse tap with no cue twitches the bait and costs nothing.</li>
 *   <li>A dip that passes with no strike: the fish takes the bait.</li>
 * </ul>
 * The server judges with a latency grace on the window's end, since the dip reaches the client and the click
 * returns a round trip late.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class StrikeJudge {
    private StrikeJudge() {}

    /** What the bobber shows when the player strikes. */
    public enum Cue { NONE, NIBBLE, TWITCH, DIP }

    public enum Result { TWITCHED, SPOOKED, EARLY, HOOKED, HOOK_LOST, LATE }

    public record Verdict(Result result, boolean baitGone, boolean spookSpot) {
        public boolean hooked() { return result == Result.HOOKED; }
    }

    /** The hook's window after the dip, and its loss on the strike. */
    public record Hook(double windowLoS, double windowHiS, double lossPct) {
        public static Hook steel() { return new Hook(0.0, 0.6, 5); }

        public static Hook woodGorge() { return new Hook(0.2, 0.8, 30); }

        public static Hook boneGorge() { return new Hook(0.2, 0.8, 20); }
    }

    /**
     * @param sinceDipS   seconds from the dip to the strike reaching the server (ignored for other cues)
     * @param uHook       uniform draw for the hook's loss
     * @param uBait       uniform draw for the bait's loss
     */
    public static Verdict judge(Cue cue, double sinceDipS, Hook hook, double graceS, double missBaitLossPct,
                                double uHook, double uBait) {
        boolean baitRoll = uBait * 100.0 < missBaitLossPct;
        return switch (cue) {
            case NONE -> new Verdict(Result.TWITCHED, false, false);
            case NIBBLE -> new Verdict(Result.SPOOKED, baitRoll, true);
            case TWITCH -> new Verdict(Result.EARLY, baitRoll, false);
            case DIP -> {
                if (sinceDipS < hook.windowLoS()) yield new Verdict(Result.EARLY, baitRoll, false);
                if (sinceDipS > hook.windowHiS() + graceS) yield new Verdict(Result.LATE, baitRoll, false);
                if (uHook * 100.0 < hook.lossPct()) yield new Verdict(Result.HOOK_LOST, baitRoll, false);
                yield new Verdict(Result.HOOKED, false, false);
            }
        };
    }

    /** How long the dip stays open on the server before the fish takes the bait: the window's end plus grace. */
    public static double dipOpenS(Hook hook, double graceS) { return hook.windowHiS() + graceS; }
}
