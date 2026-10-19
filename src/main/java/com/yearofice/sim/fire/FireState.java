package com.yearofice.sim.fire;

/**
 * The fire state machine's states (Blueprint › Fire, water and light › The fire state machine). Low, Steady and
 * Hot are settings of {@link #FIRE}, not states: the readout keeps them apart ("Fire, Low").
 */
public enum FireState {
    /** No fire: a bare ring, a laid fire not yet lit, or any timer run out. */
    OUT("Out"),
    /** A live ember or a match flame in the tinder, waiting for puffs (real seconds, not game minutes). */
    EMBER("Ember"),
    /** The tinder bundle is burning: 2 game minutes to catch the kindling. */
    TINDER("Tinder flame"),
    /** Kindling caught: 20 game minutes on its kindling, waiting for an armload. */
    SMALL("Small fire"),
    /** Burning armloads at a setting. */
    FIRE("Fire"),
    /** A fire that burned at least one armload, left unfed: 1 game hour. */
    COALS("Coals"),
    /** Coals under ash: 6 game hours, relit with fresh tinder and one puff. */
    BANKED("Banked");

    public final String word;

    FireState(String word) { this.word = word; }

    /** True while something is burning or glowing in the open (rain and snow reach it). */
    public boolean burning() {
        return this == EMBER || this == TINDER || this == SMALL || this == FIRE || this == COALS;
    }

    /** True while the fire gives flame (and so smoke and crackle). */
    public boolean flaming() {
        return this == TINDER || this == SMALL || this == FIRE;
    }
}
