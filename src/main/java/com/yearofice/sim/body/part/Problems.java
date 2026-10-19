package com.yearofice.sim.body.part;

import java.util.EnumSet;
import java.util.Set;

/**
 * The 13 problem types and where each can land (Body tab › Problems by part). Stage numbers: cut 1 minor,
 * 2 deep; burn 1 minor, 2 bad; sprain 1 mild, 2 bad; frostbite 1 frostnip, 2 superficial, 3 deep;
 * infection 1–3. The problem data ({@code injury/<id>.json}) carries timers, effects and treatments; this
 * class holds only what the pure injury path needs.
 */
public final class Problems {
    private Problems() {}

    public static final String CUT = "cut", INFECTION = "infection", BURN = "burn", SPRAIN = "sprain",
            BROKEN_BONE = "broken_bone", FROSTBITE = "frostbite", BLISTERS = "blisters", BRUISE = "bruise",
            SWELLING = "swelling", QUILLS = "quills", LEECHES = "leeches", HEAD_KNOCK = "head_knock",
            SNOW_BLINDNESS = "snow_blindness";

    private static final Set<Part.Group> ALL = EnumSet.allOf(Part.Group.class);

    /** Groups a problem may land on. An unknown id is allowed anywhere. */
    public static Set<Part.Group> allowed(String id) {
        return switch (id) {
            case SPRAIN -> EnumSet.of(Part.Group.HANDS, Part.Group.ARMS, Part.Group.LEGS, Part.Group.FEET);
            case BROKEN_BONE -> EnumSet.of(Part.Group.ARMS, Part.Group.HANDS, Part.Group.LEGS, Part.Group.FEET,
                    Part.Group.TORSO);
            case BLISTERS -> EnumSet.of(Part.Group.FEET, Part.Group.HANDS);
            case SWELLING -> EnumSet.of(Part.Group.HANDS, Part.Group.HEAD, Part.Group.ARMS);
            case QUILLS -> EnumSet.of(Part.Group.HANDS, Part.Group.LEGS);
            case LEECHES -> EnumSet.of(Part.Group.LEGS, Part.Group.FEET);
            case HEAD_KNOCK, SNOW_BLINDNESS -> EnumSet.of(Part.Group.HEAD);
            default -> ALL;
        };
    }

    public static boolean canLand(String id, Part p) { return allowed(id).contains(p.group); }

    /** The sub-place a problem names on a part, or null to roll one: a sprain is the ankle, wrist, elbow or knee; a break the forearm, shin or ribs. */
    public static String fixedSub(String id, Part p) {
        if (SPRAIN.equals(id)) {
            return switch (p.group) {
                case FEET -> "ankle";
                case HANDS -> "wrist";
                case ARMS -> "elbow";
                case LEGS -> "knee";
                default -> null;
            };
        }
        if (BROKEN_BONE.equals(id)) {
            return switch (p.group) {
                case ARMS -> "forearm";
                case LEGS -> "shin";
                case TORSO -> "ribs";
                default -> null;
            };
        }
        return null;
    }

    /** Effect size for triage's third key: higher is worse. */
    public static int severity(Problem p) {
        return switch (p.id()) {
            case BROKEN_BONE -> 6;
            case INFECTION -> 4 + p.stage();
            case CUT -> p.stage() >= 2 ? 5 : 2;
            case FROSTBITE -> 2 + p.stage();
            case BURN, SPRAIN -> p.stage() >= 2 ? 4 : 3;
            case HEAD_KNOCK -> 3;
            default -> 1;
        };
    }

    /** "A new minor problem": the ones the 17th-problem rule folds. */
    public static boolean isMinor(Problem p) {
        return switch (p.id()) {
            case CUT, BURN, SPRAIN -> p.stage() <= 1;
            case BRUISE, SWELLING, BLISTERS, LEECHES, QUILLS -> true;
            default -> false;
        };
    }
}
