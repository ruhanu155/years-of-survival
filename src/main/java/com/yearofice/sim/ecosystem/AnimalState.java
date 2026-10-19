package com.yearofice.sim.ecosystem;

/**
 * The slice's part of the shared state machine (Blueprint › How animals act): Routine, Alert, Freeze and Flee.
 * Hiding after a flee is Freeze with a timer. The ordinal is the synced activity byte.
 */
public enum AnimalState {
    ROUTINE, ALERT, FREEZE, FLEE;

    public static AnimalState byId(int id) {
        AnimalState[] v = values();
        return id >= 0 && id < v.length ? v[id] : ROUTINE;
    }
}
