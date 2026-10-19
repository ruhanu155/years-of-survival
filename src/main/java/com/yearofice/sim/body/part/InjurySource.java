package com.yearofice.sim.body.part;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One {@code injury_source/<id>.json} entry, the pure view (Body tab › Data paths): its Condition loss, how
 * many parts it hits, the hit-part weights, posture weights, the problem roll and the garment tear.
 *
 * <p>Weight keys are part ids ({@code left_arm}), groups that split 50/50 between sides ({@code arms},
 * {@code hands}, {@code legs}, {@code feet}), {@code head} and {@code torso}, or sided by the player's main
 * hand ({@code main_hand}, {@code off_hand}, {@code main_arm}, {@code off_arm}, {@code main_leg},
 * {@code off_leg}, {@code main_foot}, {@code off_foot}). Map iteration order is kept, so weights resolve the
 * same on every JVM.
 *
 * @param partsHit   1 for most sources; 2 for falls over 6 m and 3 over 12 m, rolled apart, the loss split evenly
 * @param systemic   true for sources in {@code #yearofice:systemic}: no part roll, all to the pool
 * @param tearsOuter condition taken off the hit zone's outer garment (−0.15 for animal strikes)
 */
public record InjurySource(String id, double conditionLoss, int partsHit, Map<String, Double> parts,
                           Map<Posture, PostureWeights> posture, List<ProblemRoll> problems, double tearsOuter,
                           boolean systemic) {

    /** A posture's own weights, and whether the first hit of a contact goes to the pack. */
    public record PostureWeights(boolean firstHitPack, Map<String, Double> parts) {
        public PostureWeights {
            parts = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(parts));
        }
    }

    /** One problem the source can roll; the rolls are exclusive and checked in order. */
    public record ProblemRoll(String id, int stage, double chance) {}

    public InjurySource {
        if (partsHit < 1) partsHit = 1;
        parts = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(parts));
        EnumMap<Posture, PostureWeights> pm = new EnumMap<>(Posture.class);
        if (posture != null) pm.putAll(posture);
        posture = java.util.Collections.unmodifiableMap(pm);
        problems = List.copyOf(problems);
    }

    public static InjurySource simple(String id, double loss, Map<String, Double> parts, List<ProblemRoll> problems) {
        return new InjurySource(id, loss, 1, parts, Map.of(), problems, 0, false);
    }

    /** Resolves the weights for a posture and main side into one weight per part, in {@link Part} order. */
    public double[] weights(Posture p, Side main) {
        Map<String, Double> src = parts;
        PostureWeights pw = posture.get(p == null ? Posture.STANDING : p);
        if (pw != null) src = pw.parts();
        double[] w = new double[Part.COUNT];
        for (Map.Entry<String, Double> e : src.entrySet()) add(w, e.getKey(), e.getValue(), main);
        return w;
    }

    static void add(double[] w, String key, double weight, Side main) {
        Side off = main.other();
        switch (key) {
            case "head" -> w[Part.HEAD.ordinal()] += weight;
            case "torso" -> w[Part.TORSO.ordinal()] += weight;
            case "arms" -> pair(w, Part.Group.ARMS, weight);
            case "hands" -> pair(w, Part.Group.HANDS, weight);
            case "legs" -> pair(w, Part.Group.LEGS, weight);
            case "feet" -> pair(w, Part.Group.FEET, weight);
            case "main_hand" -> w[Part.of(Part.Group.HANDS, main).ordinal()] += weight;
            case "off_hand" -> w[Part.of(Part.Group.HANDS, off).ordinal()] += weight;
            case "main_arm" -> w[Part.of(Part.Group.ARMS, main).ordinal()] += weight;
            case "off_arm" -> w[Part.of(Part.Group.ARMS, off).ordinal()] += weight;
            case "main_leg" -> w[Part.of(Part.Group.LEGS, main).ordinal()] += weight;
            case "off_leg" -> w[Part.of(Part.Group.LEGS, off).ordinal()] += weight;
            case "main_foot" -> w[Part.of(Part.Group.FEET, main).ordinal()] += weight;
            case "off_foot" -> w[Part.of(Part.Group.FEET, off).ordinal()] += weight;
            default -> w[Part.byId(key).ordinal()] += weight;
        }
    }

    private static void pair(double[] w, Part.Group g, double weight) {
        w[Part.of(g, Side.LEFT).ordinal()] += weight / 2;
        w[Part.of(g, Side.RIGHT).ordinal()] += weight / 2;
    }
}
