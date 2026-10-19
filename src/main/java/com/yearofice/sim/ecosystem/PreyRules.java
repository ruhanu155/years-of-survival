package com.yearofice.sim.ecosystem;

/**
 * The transitions between {@link AnimalState}s for prey that never charge (Blueprint › How animals act, Species
 * responses at awareness 70). Pure Java; the entity's state behaviour calls {@link #next} every tick.
 *
 * <ul>
 *   <li>Flee runs until its distance is covered.</li>
 *   <li>At the species response (70) prey flee; any animal hit flees.</li>
 *   <li>Freezers (hare, grouse, ptarmigan) freeze at 30 and bolt once a sensed threat is inside the flush
 *       distance.</li>
 *   <li>Others turn Alert at 30; an Alert animal with a flee trigger (the moose's 40 m, Ready for development
 *       M2.5 test 7) flees when the threat closes inside it.</li>
 *   <li>Under 20 the animal returns to Routine, unless it is still hiding after a flee.</li>
 * </ul>
 */
public final class PreyRules {
    private PreyRules() {}

    /**
     * A species' {@code behavior} block.
     *
     * @param alertAt      awareness for Alert or Freeze (30)
     * @param responseAt   awareness for the species response (70)
     * @param calmAt       awareness under which it returns to Routine (20)
     * @param freezes      Freeze instead of Alert
     * @param flushM       base flush distance (freezers)
     * @param fleeTriggerM an Alert animal flees inside this distance; 0 for none
     * @param fleeMinM     shortest flee
     * @param fleeMaxM     longest flee
     * @param hideMinMin   shortest hide after a flee, game minutes
     * @param hideMaxMin   longest hide
     */
    public record Behavior(double alertAt, double responseAt, double calmAt, boolean freezes, double flushM,
                           double fleeTriggerM, double fleeMinM, double fleeMaxM, int hideMinMin, int hideMaxMin) {
        public Behavior {
            if (!(calmAt <= alertAt && alertAt <= responseAt)) throw new IllegalArgumentException("thresholds out of order");
            if (fleeMinM > fleeMaxM || hideMinMin > hideMaxMin) throw new IllegalArgumentException("range out of order");
        }
    }

    /**
     * @param a        awareness toward the main threat
     * @param distM    distance to it (∞ when none)
     * @param flushM   the flush distance now (posture and approach applied)
     * @param fleeDone the current flee covered its distance
     * @param hiding   the post-flee hide has time left
     */
    public static AnimalState next(AnimalState cur, Behavior b, double a, double distM, double flushM,
                                   boolean fleeDone, boolean hiding) {
        if (cur == AnimalState.FLEE && !fleeDone) return AnimalState.FLEE;
        if (a >= b.responseAt()) return cur == AnimalState.FLEE ? settled(b) : AnimalState.FLEE;
        if (b.freezes() && a > 0 && distM <= flushM) return cur == AnimalState.FLEE ? settled(b) : AnimalState.FLEE;
        if (!b.freezes() && b.fleeTriggerM() > 0 && cur == AnimalState.ALERT && distM <= b.fleeTriggerM()) return AnimalState.FLEE;
        if (a >= b.alertAt()) return b.freezes() ? AnimalState.FREEZE : AnimalState.ALERT;
        if (hiding) return AnimalState.FREEZE;
        if (a < b.calmAt()) return AnimalState.ROUTINE;
        return cur == AnimalState.FLEE ? settled(b) : cur;
    }

    /** After a flee that ends while the threat is still near, the animal holds still or stays Alert. */
    private static AnimalState settled(Behavior b) { return b.freezes() ? AnimalState.FREEZE : AnimalState.ALERT; }
}
