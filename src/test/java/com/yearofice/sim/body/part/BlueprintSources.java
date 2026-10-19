package com.yearofice.sim.body.part;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rows of Body tab › Injury sources and Falls, stumbles and slips, as test fixtures. In the game these are
 * {@code data/yearofice/injury_source/<id>.json}. The problem chance on event-rate sources (knife slip 5% a
 * job, hatchet glance 2% a swing) is the rate the event fires at, so once the event happens its problem is
 * certain.
 */
final class BlueprintSources {
    private BlueprintSources() {}

    static Map<String, Double> w(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return m;
    }

    static InjurySource.ProblemRoll roll(String id, int stage, double chance) {
        return new InjurySource.ProblemRoll(id, stage, chance);
    }

    static final InjurySource KNIFE_SLIP = InjurySource.simple("knife_slip", 3,
            w("off_hand", 85, "main_hand", 5, "off_arm", 10), List.of(roll(Problems.CUT, 1, 1.0)));

    static final InjurySource HATCHET_GLANCE = InjurySource.simple("hatchet_glance", 10,
            w("off_leg", 50, "main_leg", 20, "off_foot", 15, "main_foot", 10, "off_hand", 5),
            List.of(roll(Problems.CUT, 2, 1.0)));

    static final InjurySource BEAR_SWIPE = new InjurySource("bear_swipe", 15, 1,
            w("head", 20, "torso", 35, "arms", 30, "legs", 15), Map.of(), List.of(roll(Problems.CUT, 2, 0.5)), 0.15,
            false);

    static final InjurySource BEAR_BITE = new InjurySource("bear_bite", 8, 1,
            w("arms", 40, "legs", 25, "torso", 25, "head", 10),
            Map.of(Posture.FACE_DOWN, new InjurySource.PostureWeights(true, w("torso", 45, "legs", 30, "arms", 20, "head", 5)),
                    Posture.CURLED, new InjurySource.PostureWeights(true, w("torso", 45, "legs", 30, "arms", 20, "head", 5))),
            List.of(roll(Problems.CUT, 2, 0.3), roll(Problems.BROKEN_BONE, 1, 0.1)), 0.15, false);

    static final InjurySource MOOSE_STOMP = new InjurySource("moose_stomp", 20, 1,
            w("torso", 40, "legs", 25, "arms", 20, "head", 15),
            Map.of(Posture.FACE_DOWN, new InjurySource.PostureWeights(false, w("torso", 50, "legs", 30, "arms", 20)),
                    Posture.CURLED, new InjurySource.PostureWeights(false, w("torso", 50, "arms", 30, "legs", 20))),
            List.of(roll(Problems.BROKEN_BONE, 1, 0.4)), 0, false);

    static final InjurySource FALL_OVER_3M = new InjurySource("fall_over_3m", 4, 1,
            w("feet", 45, "legs", 25, "hands", 20, "arms", 10), Map.of(), List.of(roll(Problems.SPRAIN, 1, 1.0)), 0, false);

    static final InjurySource FALL_OVER_6M = new InjurySource("fall_over_6m", 10, 2,
            w("feet", 35, "legs", 35, "hands", 10, "arms", 10, "torso", 10), Map.of(),
            List.of(roll(Problems.BROKEN_BONE, 1, 1.0)), 0, false);

    static final InjurySource FALL_OVER_12M = new InjurySource("fall_over_12m", 60, 3,
            w("legs", 30, "feet", 25, "torso", 20, "arms", 15, "head", 10), Map.of(),
            List.of(roll(Problems.BROKEN_BONE, 1, 1.0)), 0, false);

    static final InjurySource WIDOWMAKER = InjurySource.simple("widowmaker", 25, w("head", 70, "torso", 20, "arms", 10),
            List.of(roll(Problems.HEAD_KNOCK, 1, 0.3)));

    static final InjurySource STUMBLE_FALL = InjurySource.simple("stumble", 10,
            w("hands", 30, "legs", 25, "torso", 15, "arms", 15, "feet", 10, "head", 5), List.of());

    static final InjurySource EXPOSURE = new InjurySource("exposure", 5, 1, Map.of(), Map.of(), List.of(), 0, true);

    static final List<InjurySource> ALL = List.of(KNIFE_SLIP, HATCHET_GLANCE, BEAR_SWIPE, BEAR_BITE, MOOSE_STOMP,
            FALL_OVER_3M, FALL_OVER_6M, FALL_OVER_12M, WIDOWMAKER, STUMBLE_FALL, EXPOSURE);
}
