package com.yearofice.sim.body;

/** The minute's activity class as it bears on warmth and sweat (Blueprint › Warmth › Activity). */
public enum Activity {
    SLEEP(0),
    REST(0),
    WALK(3),
    /** Heavy work or carrying a load. */
    HEAVY(6),
    RUN(8),
    SWIM(0);

    public final double warmthC;

    Activity(double warmthC) { this.warmthC = warmthC; }

    /** Running or chopping-class work: the activities that sweat under heavy clothing. */
    public boolean sweaty() { return this == HEAVY || this == RUN; }
}
