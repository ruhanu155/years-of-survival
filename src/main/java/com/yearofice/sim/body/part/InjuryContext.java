package com.yearofice.sim.body.part;

/**
 * The caller's side of an {@code InjurySpec}: main side, posture, a forced part (the foot that stepped in,
 * the hand that held it, the cut's part, or {@code /yoi body injure <source> [part]}), a loss override, and
 * the contact state that decides whether the pack takes the first bite.
 *
 * @param lossOverride NaN to use the source's own loss
 * @param now          game minute, written into the new problem's {@code started}
 */
public record InjuryContext(Side mainSide, Posture posture, Part forcedPart, double lossOverride, boolean packOn,
                            boolean firstHitOfContact, long now) {

    public static final InjuryContext DEFAULT = new InjuryContext(Side.RIGHT, Posture.STANDING, null, Double.NaN,
            false, false, 0);

    public InjuryContext {
        if (mainSide == null || mainSide == Side.NONE) mainSide = Side.RIGHT;
        if (posture == null) posture = Posture.STANDING;
    }

    public InjuryContext forcing(Part p) {
        return new InjuryContext(mainSide, posture, p, lossOverride, packOn, firstHitOfContact, now);
    }

    public InjuryContext in(Posture p, boolean pack, boolean firstHit) {
        return new InjuryContext(mainSide, p, forcedPart, lossOverride, pack, firstHit, now);
    }

    public InjuryContext withLoss(double l) {
        return new InjuryContext(mainSide, posture, forcedPart, l, packOn, firstHitOfContact, now);
    }

    public InjuryContext at(long minute) {
        return new InjuryContext(mainSide, posture, forcedPart, lossOverride, packOn, firstHitOfContact, minute);
    }

    public InjuryContext mainHand(Side s) {
        return new InjuryContext(s, posture, forcedPart, lossOverride, packOn, firstHitOfContact, now);
    }
}
