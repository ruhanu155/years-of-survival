package com.yearofice.api.body;

import com.yearofice.sim.body.part.InjurySource;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Posture;
import org.jetbrains.annotations.Nullable;

/**
 * One injury handed to {@link BodyService#applyInjury} (Body tab › Java classes: {@code body.injury.InjurySpec}):
 * the source, an optional loss override, an optional forced part and the posture.
 *
 * @param lossOverride NaN to use the source's own Condition loss
 */
public record InjurySpec(InjurySource source, double lossOverride, @Nullable Part forcedPart, Posture posture) {

    public InjurySpec {
        if (source == null) throw new IllegalArgumentException("source");
        if (posture == null) posture = Posture.STANDING;
    }

    public static InjurySpec of(InjurySource source) {
        return new InjurySpec(source, Double.NaN, null, Posture.STANDING);
    }

    public InjurySpec withLoss(double loss) { return new InjurySpec(source, loss, forcedPart, posture); }

    public InjurySpec forcing(Part p) { return new InjurySpec(source, lossOverride, p, posture); }
}
