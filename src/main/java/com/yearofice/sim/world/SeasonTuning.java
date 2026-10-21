package com.yearofice.sim.world;

/** The calendar's colour and ground numbers ({@code [season]}); days are days of the year, 0 = May 1. */
public record SeasonTuning(
        int leafOutDoy, int leafOutDays, int turnDoy, int turnDays, int dropDoy, int dropDays, int groundFrozenDoy,
        int groundThawDoy, int tamarackFromDoy, int tamarackToDoy, int flushPerTick, int opsPerTick, int chunksPerTick,
        int catchUpChunksPerTick) {}
