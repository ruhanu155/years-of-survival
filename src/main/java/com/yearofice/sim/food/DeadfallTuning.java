package com.yearofice.sim.food;

/**
 * The deadfalls' numbers (Blueprint › Traps: Figure-4 deadfall, Mouse deadfall, Catch chance, Wear), the
 * {@code [deadfall]} section of {@code yearofice-common.toml}.
 *
 * @param bigP        p_s per animal per roll for the figure-4 deadfall's target (marten 0.06)
 * @param bigN        animals of the target in range until the population model reaches the marten (0.6)
 * @param mouseP      p_s per mouse per winter-night roll (0.25)
 * @param mouseN      mice in range of a cache in winter (3)
 * @param killRight   kill chance when the rock fits (0.9)
 * @param killHeavy   kill chance for anything heavier than a tenth of the rock (0.5)
 * @param slipP       the rock slips while setting at practice 0 (0.15)
 * @param sticksLife  triggers the sticks last (10)
 * @param soundRangeM a falling rock is heard this far ("A thud", 40 m)
 * @param mouseSoundRangeM a mouse deadfall's stone is heard this far (8)
 */
public record DeadfallTuning(double bigP, double bigN, double mouseP, double mouseN, double killRight, double killHeavy,
                             double slipP, int sticksLife, double soundRangeM, double mouseSoundRangeM) {

    public static DeadfallTuning standard() { return Preset.STANDARD.tuning; }

    public Deadfall.Tuning big() { return new Deadfall.Tuning(bigP, killRight, killHeavy, slipP, sticksLife); }

    public Deadfall.Tuning mouse() { return new Deadfall.Tuning(mouseP, killRight, killRight, slipP, sticksLife); }

    public enum Preset {
        GENTLE(new DeadfallTuning(0.08, 0.6, 0.30, 3, 0.95, 0.6, 0.10, 14, 40, 8)),
        STANDARD(new DeadfallTuning(0.06, 0.6, 0.25, 3, 0.90, 0.5, 0.15, 10, 40, 8)),
        BRUTAL(new DeadfallTuning(0.04, 0.6, 0.20, 3, 0.85, 0.4, 0.20, 7, 40, 8));

        public final DeadfallTuning tuning;

        Preset(DeadfallTuning t) { this.tuning = t; }
    }
}
