package com.yearofice.sim.story;

/**
 * The six prompts of the slice's guided first night (Ready for development › M2.5, Story: "{@code story.Onboarding}
 * with the objective beacon and its 6 prompts"). They are the slice's cut of Blueprint › Start site › The steps:
 * Tinder (5), Lay the fire (8), Light it (9), Eat (11) and Sleep (14), with the snare or the creek standing in for
 * the crate's crackers, which arrive with M3's starting kit.
 *
 * <p>Each step names what its objective beacon points at. The server finds the place; the client only draws it.
 */
public enum FirstNightStep {
    /** Gather tinder and kindling. Beacon: the nearest birch (bark burns wet), else any tree. */
    TINDER("tinder", Target.GATHER),
    /** Set the fire ring. Beacon: the start site's fire spot. */
    RING("ring", Target.FIRE_SPOT),
    /** Lay tinder, kindling and wood in order and light it. Beacon: the ring. */
    LIGHT("light", Target.FIRE),
    /** Get food: a snare on a game trail, the rod at the creek, or a cache ration. Beacon: the creek. */
    FOOD("food", Target.FOOD),
    /** Cook it on a spit over the fire, then eat. Beacon: the fire. */
    EAT("eat", Target.FIRE),
    /** Sleep within the near-fire radius of a burning fire. Beacon: the fire. */
    SLEEP("sleep", Target.FIRE);

    /** What the objective beacon points at during a step. */
    public enum Target { GATHER, FIRE_SPOT, FIRE, FOOD }

    public static final int COUNT = values().length;

    /** The step's id in lang keys and saves ({@code messenger.yearofice.prompt.first_night_<id>}). */
    public final String id;
    public final Target beacon;

    FirstNightStep(String id, Target beacon) {
        this.id = id;
        this.beacon = beacon;
    }

    /** 1-based, as the Guide page counts them. */
    public int number() { return ordinal() + 1; }

    public FirstNightStep next() { return ordinal() + 1 < COUNT ? values()[ordinal() + 1] : null; }

    public static FirstNightStep byId(String id) {
        for (FirstNightStep s : values()) if (s.id.equals(id)) return s;
        throw new IllegalArgumentException("unknown first-night step " + id);
    }

    /**
     * True when {@code f} shows the step is done. A later step's proof also closes an earlier one (a lit fire means
     * the tinder was gathered and the ring set), so a player who works ahead is never held back by a prompt.
     */
    public boolean doneBy(FirstNightFacts f) {
        return switch (this) {
            case TINDER -> (f.tinder() && f.kindling()) || f.fireLit();
            case RING -> f.ring() || f.fireLit();
            case LIGHT -> f.fireLit();
            case FOOD -> f.food() || f.ate();
            case EAT -> f.ate();
            case SLEEP -> f.asleepByFire();
        };
    }
}
