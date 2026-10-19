package com.yearofice.sim.env;

/**
 * An open shelter's effect on warmth: a flat bonus and a factor on wind chill. Closed rooms are not
 * shelters; inside one the room temperature replaces air and wind drops out (Blueprint › Warmth › Shelter).
 * The values below are the Blueprint's; the build adapter reads the real one from the shelter piece's data.
 */
public record ShelterState(String id, double bonusC, double windFactor, double rainFactor) {

    public static final ShelterState NONE = new ShelterState("none", 0, 1, 1);
    /** Dense spruce, a reflector wall: wind chill halved, no bonus. */
    public static final ShelterState WIND_BREAK = new ShelterState("wind_break", 0, 0.5, 1);
    /** Lean-to: +3 °C, halves wind chill and the rain (Weather › Caught outside). */
    public static final ShelterState LEAN_TO = new ShelterState("lean_to", 3, 0.5, 0.5);
    /** A space with walls and roof but an air path out (Crafting › Warm rooms › Finding a room). */
    public static final ShelterState OPEN_ENCLOSURE = new ShelterState("open_enclosure", 5, 0, 0);
    public static final ShelterState TREE_WELL = new ShelterState("tree_well", 6, 0, 0.5);
    public static final ShelterState OUTPOST_CABIN = new ShelterState("outpost_cabin", 8, 0, 0);
    public static final ShelterState SNOW_TRENCH = new ShelterState("snow_trench", 10, 0, 0);
    public static final ShelterState CHINKED_CABIN_UNHEATED = new ShelterState("chinked_cabin", 15, 0, 0);
    /** Quinzhee: Structures wins at +25 °C (RECONCILE 138). */
    public static final ShelterState QUINZHEE = new ShelterState("quinzhee", 25, 0, 0);
}
