package com.yearofice.sim.body.part;

/**
 * What the arms and hands allow ({@code BodyService.canUse}): bows, two-handed tools, one-handed tools and
 * fine work (Body tab › Bands and part effects; How parts combine; Health › Broken bones).
 */
public final class Use {
    private Use() {}

    public enum Tool {
        /** Draw force from the main arm; any arm or hand at Badly hurt blocks it. */
        BOW,
        /** Axe, hatchet, maul, paddle, bow drill: two-handed. */
        TWO_HANDED,
        /** Knife, spoon, torch: the main hand, or the off hand at ×2. */
        ONE_HANDED,
        /** Light one-hand work with the bad arm once a splinted break eases from day 11. */
        ROD
    }

    public static boolean canUse(BodyRecord r, Tool tool, Side main, long now) {
        return switch (tool) {
            case BOW -> twoHandedOk(r) && !anyBadly(r);
            case TWO_HANDED -> twoHandedOk(r);
            case ONE_HANDED -> !(PartModel.effectiveBand(r, Part.of(Part.Group.HANDS, main)) == Band.USELESS
                    && PartModel.effectiveBand(r, Part.of(Part.Group.HANDS, main.other())) == Band.USELESS);
            case ROD -> !anyArmBreakBlocking(r, now, true);
        };
    }

    private static boolean anyBadly(BodyRecord r) {
        for (Part p : Part.values()) {
            if (p.group != Part.Group.ARMS && p.group != Part.Group.HANDS) continue;
            Band b = PartModel.effectiveBand(r, p);
            if (b == Band.BADLY_HURT || b == Band.USELESS) return true;
        }
        return false;
    }

    /** Two hands on the tool: no arm at Badly hurt or worse, no hand that cannot grip, no break, no sprained wrist or elbow. */
    private static boolean twoHandedOk(BodyRecord r) {
        for (Part p : Part.values()) {
            if (p.group == Part.Group.ARMS) {
                Band b = PartModel.effectiveBand(r, p);
                if (b == Band.BADLY_HURT || b == Band.USELESS) return false;
            }
            if (p.group == Part.Group.HANDS) {
                Band b = PartModel.effectiveBand(r, p);
                if (b == Band.BADLY_HURT || b == Band.USELESS) return false;
            }
        }
        for (Problem pr : r.problems()) {
            Part.Group g = pr.part().group;
            if (g != Part.Group.ARMS && g != Part.Group.HANDS) continue;
            if (Problems.BROKEN_BONE.equals(pr.id()) || Problems.SPRAIN.equals(pr.id())) return false;
        }
        return true;
    }

    /** An arm or hand break blocks light work until splinted and 11 days on. */
    private static boolean anyArmBreakBlocking(BodyRecord r, long now, boolean light) {
        for (Problem pr : r.problems()) {
            Part.Group g = pr.part().group;
            if ((g == Part.Group.ARMS || g == Part.Group.HANDS) && Problems.BROKEN_BONE.equals(pr.id())) {
                boolean eased = pr.isSplinted() && now - pr.splint() >= Gait.SPLINT_EASE_DAYS * 1440L;
                if (!(light && eased)) return true;
            }
        }
        return false;
    }

    /** "Splinted arms jar loose": without a sling, a stumble fall or a fall over 2 m re-breaks a splinted arm or hand 25% of the time. */
    public static final double REBREAK_CHANCE = 0.25;

    /** Returns the problem after a fall: re-broken (splint off, 30-day timer restarting) or unchanged. */
    public static Problem fallOnSplint(Problem pr, double u, long now) {
        Part.Group g = pr.part().group;
        if (!Problems.BROKEN_BONE.equals(pr.id()) || !pr.isSplinted() || pr.sling()) return pr;
        if (g != Part.Group.ARMS && g != Part.Group.HANDS) return pr;
        if (u >= REBREAK_CHANCE) return pr;
        return new Problem(pr.id(), pr.part(), pr.sub(), pr.stage(), 0, pr.dressed(), pr.infected(), pr.pad(),
                pr.binding(), pr.cleanUntil(), -1, false, pr.wrap(), pr.cooled(), now, -1);
    }
}
