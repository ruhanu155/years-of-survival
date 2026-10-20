package com.yearofice.sim.hunt;

/**
 * The hit-zone shot (Blueprint › The hit-zone model): zone, angle, energy and the species' kill threshold in, an
 * {@link Outcome} out. No hit markers; where the arrow lands decides.
 *
 * <pre>
 *   E_eff = E * f_head * f_range * f_angle,  f_range = max(0, 1 - 0.04 (d - d_eff)) past the effective range
 * </pre>
 */
public final class Shot {
    private Shot() {}

    /** What the arrow did to the animal. */
    public enum Result { MISS, KILL, WOUND, STUN, ANGER }

    /** The wound channel a wounding hit opens. */
    public enum Channel { NONE, VITALS, LIVER, GUT, LEG }

    public enum Angle {
        BROADSIDE(1.0), QUARTERING_AWAY(1.0), QUARTERING_TOWARD(0.6), HEAD_ON(0.3), STRAIGHT_AWAY(0.0);

        public final double factor;

        Angle(double f) { this.factor = f; }

        /** From cos(angle) between the arrow's travel and the animal's facing (1 = shot from behind, along the spine). */
        public static Angle of(double cosTravelFacing) {
            double c = cosTravelFacing;
            if (c >= 0.85) return STRAIGHT_AWAY;
            if (c >= 0.5) return QUARTERING_AWAY;
            if (c > -0.5) return BROADSIDE;
            if (c > -0.85) return QUARTERING_TOWARD;
            return HEAD_ON;
        }
    }

    /**
     * @param result   the verdict
     * @param channel  the wound opened (NONE for a miss, a kill or a stun)
     * @param zone     the zone the arrow decided on ({@code gut} after a failed vital)
     * @param effJ     effective energy tested against the threshold
     * @param note     a short reason for the debug command and the journal
     */
    public record Outcome(Result result, Channel channel, String zone, double effJ, String note) {
        public boolean wounds() { return result == Result.WOUND; }
    }

    public static double rangeFactor(double distM, double effectiveRangeM, HuntTuning t) {
        return distM <= effectiveRangeM ? 1.0 : Math.max(0, 1 - t.rangeFalloffPerM() * (distM - effectiveRangeM));
    }

    public static double effectiveEnergy(double energyJ, ArrowHead head, double distM, double effectiveRangeM, Angle angle, HuntTuning t) {
        return energyJ * head.energyFactor() * rangeFactor(distM, effectiveRangeM, t) * angle.factor;
    }

    /**
     * @param zone        decisive zone name, or null for a clean miss
     * @param thresholdJ  the species' kill threshold
     * @param shoulder    the arrow entered the front 30% of the heart and lungs
     * @param shatterRoll uniform [0,1) roll for a head that shatters on bone
     */
    public static Outcome resolve(String zone, double energyJ, ArrowHead head, double distM, double effectiveRangeM, Angle angle,
                                  double thresholdJ, boolean shoulder, double shatterRoll, HuntTuning t) {
        if (zone == null) return new Outcome(Result.MISS, Channel.NONE, "none", 0, "clean miss");
        if (head.blunt()) {
            boolean small = thresholdJ <= 2.0;
            boolean body = !zone.equals("legs") && !zone.equals("wings");
            if (!small) return new Outcome(Result.ANGER, Channel.NONE, zone, 0, "a blunt only angers big game");
            double e = energyJ * rangeFactor(distM, effectiveRangeM, t);
            if (body && e >= thresholdJ) return new Outcome(Result.KILL, Channel.NONE, zone, e, "blunt kills small game");
            return new Outcome(Result.STUN, Channel.NONE, zone, e, "blunt stuns");
        }
        double eff = effectiveEnergy(energyJ, head, distM, effectiveRangeM, angle, t);
        if (eff <= 0) return new Outcome(Result.WOUND, Channel.GUT, "gut", 0, "out of range: no penetration");
        switch (zone) {
            case "brain", "spine" -> {
                // Head or neck: drops at once when the arrow has the energy to get through, a clean miss otherwise.
                if (angle == Angle.STRAIGHT_AWAY && zone.equals("brain")) return new Outcome(Result.WOUND, Channel.GUT, "gut", eff, "no way in from behind");
                return eff >= thresholdJ * 0.25 ? new Outcome(Result.KILL, Channel.NONE, zone, eff, "drops at once")
                        : new Outcome(Result.MISS, Channel.NONE, zone, eff, "glanced");
            }
            case "heart_lungs", "liver" -> {
                if (angle == Angle.STRAIGHT_AWAY) return new Outcome(Result.WOUND, Channel.GUT, "gut", eff, "from behind the vitals cannot be reached");
                if (shoulder && zone.equals("heart_lungs") && !head.passesShoulder() && head.shatterOnBone() > 0 && shatterRoll < head.shatterOnBone()) {
                    return new Outcome(Result.WOUND, Channel.LEG, "legs", eff, "shattered on the shoulder blade");
                }
                if (eff < thresholdJ) return new Outcome(Result.WOUND, Channel.GUT, "gut", eff, "short of the vitals: behaves as a gut hit");
                return zone.equals("liver") ? new Outcome(Result.WOUND, Channel.LIVER, zone, eff, "liver")
                        : new Outcome(Result.KILL, Channel.VITALS, zone, eff, "heart and lungs");
            }
            case "gut" -> { return new Outcome(Result.WOUND, Channel.GUT, zone, eff, "gut"); }
            default -> { return new Outcome(Result.WOUND, Channel.LEG, zone, eff, "limb"); }
        }
    }

    /** A spear: a thrust into a downed or snared animal's vitals is a heart-and-lung hit; thrown at big game it is never more than a gut hit. */
    public static Outcome spear(boolean thrust, boolean downedOrSnared, double thresholdJ) {
        if (thrust && downedOrSnared) return new Outcome(Result.KILL, Channel.VITALS, "heart_lungs", Double.MAX_VALUE, "thrust into the vitals");
        if (thresholdJ <= 2.0) return new Outcome(Result.KILL, Channel.NONE, "heart_lungs", 0, "kills small game");
        return new Outcome(Result.WOUND, Channel.GUT, "gut", 0, "a thrown spear is never more than a gut hit on big game");
    }
}
