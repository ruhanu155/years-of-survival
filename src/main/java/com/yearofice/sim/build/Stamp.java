package com.yearofice.sim.build;

import java.util.List;

/**
 * Labour model for building and stamping (M8 test: stamping a saved cabin takes under 10 % of the first build's player
 * time). A first build costs each piece's {@code labour_min}. A stamp reuses the saved plan: the player's time is the
 * cut-and-fit of the first build times {@code factor} (jigs and marked timber), with no per-piece thinking.
 */
public final class Stamp {
    private Stamp() {}

    public record Piece(String id, double labourMin) {}

    public static double firstBuildMin(List<Piece> pieces) {
        return pieces.stream().mapToDouble(Piece::labourMin).sum();
    }

    /** Player minutes to stamp: the saved plan's labour × {@code factor}, plus {@code setupMin} to mark the anchor. */
    public static double stampMin(List<Piece> pieces, double factor, double setupMin) {
        return firstBuildMin(pieces) * factor + setupMin;
    }
}
