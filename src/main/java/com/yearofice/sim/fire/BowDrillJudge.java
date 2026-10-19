package com.yearofice.sim.fire;

/**
 * The server's verdict on one bow-drill attempt (Minecraft development › Minigames in Minecraft terms › Judging).
 * The minigame runs on the client and sends a summary; this checks it against what the server knows and applies
 * the Blueprint's failure rules. A rejected result counts as a failed attempt and costs its kcal.
 */
public final class BowDrillJudge {
    private BowDrillJudge() {}

    /** What the client says happened. */
    public enum Claim { FLAME, NO_EMBER, EMBER_DIED, SCATTERED, ABORTED }

    /** What the server decides. */
    public enum Outcome { FLAME, NO_EMBER, WET_BOARD, WET_TINDER, TOO_TIRED, EMBER_DIED, SCATTERED, ABORTED, REJECTED }

    /**
     * The client's summary.
     *
     * @param claim     how the attempt ended on the client
     * @param strokes   strokes made
     * @param onBeat    strokes within the beat window
     * @param peakHeat  the heat meter's peak, 0–100
     * @param goodPuffs puffs of 1–1.5 s
     * @param held      the Practiced 6-second hold was used instead of strokes
     */
    public record Summary(Claim claim, int strokes, int onBeat, int peakHeat, int goodPuffs, boolean held) {}

    /**
     * What the server knows.
     *
     * @param elapsedS       real seconds the server measured from the start to the result
     * @param reps           the player's Fire skill reps
     * @param boardMoisture  the bow drill's {@code yearofice:moisture}
     * @param tinderMoisture the ring's tinder moisture
     * @param energyBolts    the body's energy, 0–5 bolts
     */
    public record Facts(double elapsedS, double reps, int boardMoisture, int tinderMoisture, double energyBolts) {}

    /**
     * The verdict: the outcome, the line for the messenger, the practice reps it earns (1 for a fire, 0.5 for a
     * failed try, 0 when nothing was tried), the kcal and the game minutes it charges.
     */
    public record Verdict(Outcome outcome, String line, double reps, int kcal, double gameMinutes) {
        public boolean lit() { return outcome == Outcome.FLAME; }
    }

    /** A try that ended before anything happened (under 3 s and no strokes) costs nothing. */
    public static final double FREE_ABORT_S = 3;

    public static Verdict judge(Summary s, Facts f, FireTuning t) {
        double minutes = BowDrill.gameMinutes(f.elapsedS());
        if (s.claim() == Claim.ABORTED && s.strokes() == 0 && f.elapsedS() < FREE_ABORT_S)
            return new Verdict(Outcome.ABORTED, null, 0, 0, minutes);
        Verdict failed = null;
        if (f.energyBolts() < t.drillMinEnergy())
            failed = fail(Outcome.TOO_TIRED, "Too tired to drill. Eat and rest first.", minutes, t);
        else if (s.claim() == Claim.FLAME && !plausible(s, f, t))
            failed = fail(Outcome.REJECTED, "The drill slipped. Try again.", minutes, t);
        else if (f.boardMoisture() > t.tinderMaxMoisture())
            failed = fail(Outcome.WET_BOARD, "Plenty of smoke, no ember: the board is wet.", minutes, t);
        if (failed != null) return failed;
        return switch (s.claim()) {
            case FLAME -> f.tinderMoisture() > t.tinderMaxMoisture()
                    ? fail(Outcome.WET_TINDER, "The ember would not catch: the tinder is wet.", minutes, t)
                    : new Verdict(Outcome.FLAME, "The tinder catches.", 1, t.drillKcal(), minutes);
            case NO_EMBER -> fail(Outcome.NO_EMBER, "No ember. Keep the beat and the pressure in the green.", minutes, t);
            case EMBER_DIED -> fail(Outcome.EMBER_DIED, "The ember died. Tip and blow sooner.", minutes, t);
            case SCATTERED -> fail(Outcome.SCATTERED, "Blew too long: the ember scattered.", minutes, t);
            case ABORTED -> fail(Outcome.ABORTED, "Stopped drilling.", minutes, t);
        };
    }

    /**
     * Plausibility (the judge's checks): the attempt took at least 70% of the fastest legal run, a held attempt
     * needs Practiced, and a stroked ember needs strokes that could have built 100 heat and three good puffs.
     */
    public static boolean plausible(Summary s, Facts f, FireTuning t) {
        double fastest = s.held() ? BowDrill.fastestHeldS() : BowDrill.fastestStrokedS();
        if (f.elapsedS() < 0.7 * fastest) return false;
        if (s.held()) return BowDrill.practiced(f.reps(), t) && s.goodPuffs() >= BowDrill.PUFFS_FOR_FLAME;
        if (s.peakHeat() < BowDrill.EMBER_AT - 0.5) return false;
        if (BowDrill.heatCeiling(s.strokes(), s.onBeat()) < BowDrill.EMBER_AT - 1e-6) return false;
        return s.goodPuffs() >= BowDrill.PUFFS_FOR_FLAME;
    }

    private static Verdict fail(Outcome o, String line, double minutes, FireTuning t) {
        return new Verdict(o, line, 0.5, t.drillKcal(), minutes);
    }
}
