package com.yearofice.sim.minigame.knapping;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import java.util.List;

/**
 * Judges a knapping attempt from its strike list (Blueprint › Knapping flint; Crafting › Practice; Minecraft
 * development › Player systems and client: the client sends {@code minigame_result} with the strike list and
 * {@code build.work.MinigameJudge} calls this).
 *
 * <p>The rules:
 * <ol>
 *   <li>A strike on a platform within the good angle (60–80°, widening to 50–85° at Practiced) takes a flake:
 *       the platform's cell goes off in the {@link KnappingPattern} grid and the shape changes.</li>
 *   <li>Off the good angle by more than 20° puts a crack line on that side; a second bad strike on a cracked
 *       side shatters the core 50% of the time. Practiced knappers lose the shatter chance on their first such
 *       strike.</li>
 *   <li>Four good flakes make a broadhead blank; two more finish it.</li>
 *   <li>Shivering (warmth 30–50) narrows the good window by a quarter.</li>
 * </ol>
 * The Automatic assist rolls the whole attempt at the practised shatter chance: 25% of cores at practice 0,
 * falling to 5% at Practiced (Crafting › Practice, N = 25).
 *
 * <p>Rolls come from (seed, "knap", player, attempt), so a replayed strike list gets the same verdict.
 * Pure Java. Original Year of Ice code.
 */
public final class KnappingJudge {
    private KnappingJudge() {}

    public static final int PRACTICE_N = 25;
    public static final double GOOD_MIN_DEG = 60, GOOD_MAX_DEG = 80;
    public static final double PRACTICED_MIN_DEG = 50, PRACTICED_MAX_DEG = 85;
    public static final double BAD_BEYOND_DEG = 20;
    public static final double SHATTER_ON_CRACK = 0.5;
    public static final int BLANK_FLAKES = 4, FINISH_FLAKES = 6;
    public static final double SHIVER_WINDOW = 0.75;
    public static final double AUTO_SHATTER_CLUMSY = 0.25, AUTO_SHATTER_PRACTICED = 0.05;

    public enum Result { IN_PROGRESS, BLANK, FINISHED, SHATTERED }

    /**
     * One strike from the screen.
     *
     * @param side 0–3, the core face the platform is on
     * @param cellX the grid cell of the platform
     */
    public record Strike(boolean onPlatform, double angleDeg, int side, int cellX, int cellY) {}

    public record Verdict(Result result, int goodFlakes, KnappingPattern shape, int strikesJudged) {}

    /** The good-angle window for a practice count n, optionally narrowed by shivering. */
    public static double[] window(int n, boolean shivering) {
        double t = Math.min(n, PRACTICE_N) / (double) PRACTICE_N;
        double lo = GOOD_MIN_DEG + (PRACTICED_MIN_DEG - GOOD_MIN_DEG) * t;
        double hi = GOOD_MAX_DEG + (PRACTICED_MAX_DEG - GOOD_MAX_DEG) * t;
        if (shivering) {
            double mid = (lo + hi) / 2, half = (hi - lo) / 2 * SHIVER_WINDOW;
            lo = mid - half;
            hi = mid + half;
        }
        return new double[] {lo, hi};
    }

    public static Verdict judge(long seed, long playerId, long attempt, List<Strike> strikes, int practice,
                                boolean shivering, KnappingPattern target) {
        double[] w = window(practice, shivering);
        boolean practiced = practice >= PRACTICE_N;
        boolean[] cracked = new boolean[4];
        boolean[] freeUsed = new boolean[4];
        KnappingPattern shape = KnappingPattern.full();
        Mulberry32 r = rng(seed, playerId, attempt);
        int good = 0;
        int i = 0;
        for (Strike s : strikes) {
            i++;
            double off = s.angleDeg() < w[0] ? w[0] - s.angleDeg() : (s.angleDeg() > w[1] ? s.angleDeg() - w[1] : 0);
            boolean isGood = s.onPlatform() && off == 0;
            boolean isBad = off > BAD_BEYOND_DEG;
            int side = Math.floorMod(s.side(), 4);
            double roll = r.nextDouble(); // one draw per strike, used or not, so the list replays exactly
            if (isGood) {
                good++;
                if (s.cellX() >= 0 && s.cellX() < shape.width() && s.cellY() >= 0 && s.cellY() < shape.height()) {
                    shape = shape.with(s.cellX(), s.cellY(), false);
                }
                if (good >= FINISH_FLAKES && (target == null || shape.matches(target))) {
                    return new Verdict(Result.FINISHED, good, shape, i);
                }
            } else if (isBad) {
                if (cracked[side]) {
                    if (practiced && !freeUsed[side]) {
                        freeUsed[side] = true;
                    } else if (roll < SHATTER_ON_CRACK) {
                        return new Verdict(Result.SHATTERED, good, shape, i);
                    }
                } else {
                    cracked[side] = true;
                }
            }
        }
        Result res = good >= BLANK_FLAKES ? Result.BLANK : Result.IN_PROGRESS;
        return new Verdict(res, good, shape, strikes.size());
    }

    /** The Automatic assist's shatter chance for a practice count. */
    public static double autoShatterChance(int practice) {
        double t = Math.min(practice, PRACTICE_N) / (double) PRACTICE_N;
        return AUTO_SHATTER_CLUMSY + (AUTO_SHATTER_PRACTICED - AUTO_SHATTER_CLUMSY) * t;
    }

    public static boolean autoShatters(long seed, long playerId, long attempt, int practice) {
        return rng(seed, playerId, attempt).nextDouble() < autoShatterChance(practice);
    }

    private static Mulberry32 rng(long seed, long playerId, long attempt) {
        return new Mulberry32(StageHash.of(seed + ":knap:" + playerId + ":" + attempt));
    }
}
