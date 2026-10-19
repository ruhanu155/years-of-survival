package com.yearofice.sim.decay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.decay.DecayCore.Shape;
import com.yearofice.sim.food.EatingLadder;
import com.yearofice.sim.food.EatingLadder.Family;
import com.yearofice.sim.food.EatingLadder.Prep;
import com.yearofice.sim.food.SourFish;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Decay and time › GameTests, as JUnit on the pure cores. Each method is named after its GameTest
 * ({@code yearofice:decay/<test>}); the GameTest proper drives the same cores through the adapters in M5–M9.
 */
class DecayTabTest {
    private static final ColdCurve C = ColdCurve.STANDARD;
    private static final double MEAT_TAU = DecayCore.tauFromSafeLifeDays(6.0);

    /** Hours from a stamp at {@code stampHour} until a closed-chest meat stack first reads Turning. */
    private static int hoursToTurning(double tempC, int stampHour, double tau) {
        DecayClimate clim = DecayClimate.livingRot(Climates.constant(tempC, 2000), C);
        Fresh f = Fresh.stamp(FoodState.RAW, 0, clim.decayHoursAt(stampHour), tau);
        for (int h = stampHour; h <= clim.hours(); h++) {
            if (f.stage(clim.decayHoursAt(h)) >= EatingLadder.TURNING) return h - stampHour;
        }
        return Integer.MAX_VALUE;
    }

    // 1. decay/cold_curve (m5)
    @Test
    void cold_curve() {
        assertEquals(48, hoursToTurning(15, 0, MEAT_TAU), 1);
        assertEquals(144, hoursToTurning(5, 0, MEAT_TAU), 2);
        assertEquals(720, hoursToTurning(-3, 0, MEAT_TAU), 12);

        DecayClimate deep = DecayClimate.livingRot(Climates.constant(-10, 120 * 24), C);
        Fresh f = Fresh.stamp(FoodState.RAW, 0, 0, MEAT_TAU);
        assertEquals(0.0, f.progress(deep.decayHoursAt(deep.hours())), 0);

        double countdown = ThawCountdown.days(ThawCountdown.dayOfYear(2, 18), ThawCountdown.OPEN_POLE_THAW_DAY, 0, 6.0);
        assertTrue(countdown >= 56 && countdown <= 60, "thaw countdown " + countdown);
    }

    /**
     * Flag for the PR: the 1/40 band floors {@code due}, so a stack stamped off a band boundary turns up to one
     * band early. At 5 °C a band is 5.1 h and at −3 °C 25.7 h, wider than cold_curve's ±2 h and ±12 h, so the
     * GameTest must stamp on a band boundary (as {@link #cold_curve} does at D = 0) or widen its tolerance.
     */
    @Test
    void cold_curve_band_offset_is_at_most_one_band() {
        double bandHoursAt5 = MEAT_TAU / Fresh.DEFAULT_BANDS;
        for (int offset = 0; offset < 12; offset++) {
            int h = hoursToTurning(5, offset, MEAT_TAU);
            assertTrue(h <= 144 && h >= 144 - bandHoursAt5 - 1, "offset " + offset + " gave " + h);
        }
    }

    /** A thing in the catch-up fixture: a class on one of the 8 curves, stamped at some hour. */
    private record Thing(String cls, Shape shape, double tau, double factor, String channel, int startHour,
                         double load, Fresh fresh) {}

    // 2. decay/catchup_equals_live (m8)
    @Test
    void catchup_equals_live() {
        int days = 30;
        double[] temps = Climates.diurnal(13, 6, days);
        DecayClimate clim = DecayClimate.builder(temps)
                .rated(DecayClimate.DECAY_HOURS, C.livingRot())
                .rated("rust_hours", ColdCurve::rust)
                .build();
        DecayClimate.Channel dh = clim.channel(DecayClimate.DECAY_HOURS);
        DecayClimate.Channel rust = clim.channel("rust_hours");
        long seed = 1234L;

        List<Thing> things = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int start = i * 17;
            FreshTrait[] places = {FreshTrait.HUNG, FreshTrait.GROUND, FreshTrait.SALTED, FreshTrait.SUN, FreshTrait.BRINED};
            int traits = FreshTrait.mask(places[i]);
            things.add(new Thing("meat", Shape.LINEAR, 0, 0, DecayClimate.DECAY_HOURS, start, 0,
                    Fresh.stamp(FoodState.RAW, 0, dh.at(start), MEAT_TAU, traits, 40)));
            things.add(new Thing("liver", Shape.LINEAR, 0, 0, DecayClimate.DECAY_HOURS, start, 0,
                    Fresh.stamp(FoodState.RAW, 0, dh.at(start), DecayCore.tauFromSafeLifeDays(1.8))));
            things.add(new Thing("cooked_fish", Shape.LINEAR, 0, 0, DecayClimate.DECAY_HOURS, start, 0,
                    Fresh.stamp(FoodState.COOKED, 0, dh.at(start), DecayCore.tauFromSafeLifeDays(9))));
            things.add(new Thing("green_hide", Shape.LINEAR, 72 / 0.9, 1.0 + 0.3 * i, DecayClimate.DECAY_HOURS, start, 0, null));
            things.add(new Thing("deadfall", Shape.PROPORTIONAL, DecayCore.tauFromRatePerValleyYear(0.02 * 5 * 0.9 * 12),
                    1.0, DecayClimate.DECAY_HOURS, start, 0, null));
            things.add(new Thing("sod_roof_pole", Shape.PROPORTIONAL, DecayCore.tauFromRatePerValleyYear(0.04),
                    100 + 40 * i, DecayClimate.DECAY_HOURS, start, Thresholds.loadThreshold(seed, i, 70, 3, start / 24), null));
            things.add(new Thing("iron_ground", Shape.LINEAR, 24 * 30, 1.0 + i, "rust_hours", start, 0, null));
            things.add(new Thing("hay", Shape.LINEAR, DecayCore.VALLEY_YEAR_DECAY_HOURS / 12, 1.0 + 0.5 * i,
                    DecayClimate.DECAY_HOURS, start, 0, null));
        }
        assertEquals(40, things.size());

        int end = clim.hours();
        for (Thing t : things) {
            DecayClimate.Channel ch = clim.channel(t.channel());
            double pLive;
            double pCatch;
            if (t.fresh() != null) {
                // Live: the chest is opened every hour and the stack settled on read. Caught up: read once.
                Fresh live = t.fresh();
                for (int h = t.startHour(); h <= end; h++) live = live.settle(ch.at(h));
                pLive = live.progress(ch.at(end));
                pCatch = t.fresh().settle(ch.at(end)).progress(ch.at(end));
            } else {
                // Incremental hourly settles stay within 0.001 of the closed form...
                double p = 0;
                for (int h = t.startHour(); h < end; h++) {
                    p = DecayCore.settle(t.shape(), p, ch.between(h, h + 1), t.factor(), t.tau());
                }
                assertEquals(p, DecayCore.settle(t.shape(), 0, ch.between(t.startHour(), end), t.factor(), t.tau()), 0.001);
                // ...and a mark read every hour equals one read once, bit for bit.
                DecayCore.Mark mark = new DecayCore.Mark(t.shape(), 0, ch.at(t.startHour()), t.factor(), t.tau());
                double read = 0;
                for (int h = t.startHour(); h <= end; h++) read = mark.progress(ch.at(h));
                pLive = read;
                pCatch = mark.progress(ch.at(end));
                if (t.load() > 0) {
                    boolean fellLive = false;
                    for (int h = t.startHour(); h <= end && !fellLive; h++) fellLive = mark.progress(ch.at(h)) >= t.load();
                    assertEquals(fellLive, ch.at(end) >= mark.dueAt(t.load()), t.cls() + " fall date");
                }
            }
            assertEquals(pLive, pCatch, 0.001, t.cls() + " p");
            assertEquals(pLive, pCatch, 0, t.cls() + " p exact");
            DecayLadder ladder = t.fresh() != null ? DecayLadder.FOOD : DecayLadder.HIDE;
            assertEquals(ladder.stageOf(pLive), ladder.stageOf(pCatch), t.cls() + " stage");
            if (t.load() > 0) assertEquals(pLive >= t.load(), pCatch >= t.load(), t.cls() + " fallen");
        }
    }

    private static double woodR(double bark, double species, double size, double ground, double moisture, double prot) {
        return 0.02 * bark * species * size * ground * moisture * prot;
    }

    /** Valley years for proportional wood at rate r to reach p, in closed form. */
    private static double yearsTo(double r, double p) {
        return DecayCore.valleyYears(DecayCore.decayHoursTo(Shape.PROPORTIONAL, 0, p, 1.0, DecayCore.tauFromRatePerValleyYear(r)));
    }

    // 3. decay/wood_rates (m8)
    @Test
    void wood_rates() {
        double deadfall = woodR(5, 1, 1, 0.9, 1, 1);
        assertEquals(0.09, deadfall, 1e-12);
        assertEquals(4.0, yearsTo(deadfall, DecayLadder.DEADFALL.startOf(1)), 0.2);
        assertEquals(13.4, yearsTo(deadfall, DecayLadder.DEADFALL.startOf(2)), 0.5);

        double sillHeated = woodR(1, 1, 1, 3, 1, 1);
        assertEquals(23, yearsTo(sillHeated, DecayLadder.BUILT_WOOD.startOf(3)), 1);
        double cornerStone = woodR(1, 1, 1, 1, 1, 1);
        assertEquals(35, yearsTo(cornerStone, DecayLadder.BUILT_WOOD.startOf(2)), 1);

        // The same results on the Standard climate year after year: calendar years agree with valley years.
        DecayClimate years = DecayClimate.livingRot(Climates.standardYears(40), C);
        double tau = DecayCore.tauFromRatePerValleyYear(deadfall);
        int punkyYear = -1;
        for (int y = 1; y <= 40 && punkyYear < 0; y++) {
            double p = DecayCore.settle(Shape.PROPORTIONAL, 0, years.decayHoursAt(y * 8760), 1, tau);
            if (p >= 0.3) punkyYear = y;
        }
        assertEquals(4, punkyYear);
    }

    // 4. decay/snow_buffer (m7)
    @Test
    void snow_buffer() {
        double[] before = Climates.diurnal(-15, 4, 7); // the week before, for the trailing snow mean
        double[] week = Climates.concat(Climates.diurnal(-15, 4, 3), Climates.diurnal(-3, 6, 2), Climates.diurnal(-15, 4, 2));
        double[] air = Climates.concat(before, week);
        int from = before.length;
        int to = air.length;

        double[] underSnow = new double[air.length];
        for (int h = 0; h < air.length; h++) {
            double sum = 0;
            int n = 0;
            for (int k = Math.max(0, h - 7 * 24 + 1); k <= h; k++) {
                sum += air[k];
                n++;
            }
            underSnow[h] = ColdCurve.underSnow(sum / n);
        }

        DecayClimate pole = DecayClimate.livingRot(air, C);
        DecayClimate cache = DecayClimate.livingRot(underSnow, C);
        Fresh onPole = Fresh.stamp(FoodState.RAW, 0, pole.decayHoursAt(from), MEAT_TAU, FreshTrait.mask(FreshTrait.HUNG), 1 << 20);
        Fresh inSnow = Fresh.stamp(FoodState.RAW, 1, cache.decayHoursAt(from), MEAT_TAU, 0, 1 << 20);

        double poleLoss = onPole.progress(pole.decayHoursAt(to)) / DecayCore.SAFE_P;
        double snowLoss = inSnow.progress(cache.decayHoursAt(to)) / DecayCore.SAFE_P;
        assertTrue(poleLoss >= 0.06 && poleLoss <= 0.10, "open pole lost " + poleLoss + " of its safe life");
        assertEquals(0.0, snowLoss, 0, "meat under 40 cm of snow");
    }

    // 5. decay/eating_ladder (m5)
    @Test
    void eating_ladder() {
        long seed = 99L;
        int n = 10_000;
        int sickFit = 0, sickWeak = 0, sickStew = 0;
        double nausea = 0;
        for (int i = 0; i < n; i++) {
            double u1 = Thresholds.stream(seed, "eat", 1, i).nextDouble();
            double u2 = Thresholds.stream(seed, "eat", 2, i).nextDouble();
            double u3 = Thresholds.stream(seed, "eat", 3, i).nextDouble();
            if (EatingLadder.rollGutIllness(Family.GAME_MEAT, Prep.RAW, EatingLadder.TURNING, 80, 22, u1)) sickFit++;
            if (EatingLadder.rollGutIllness(Family.GAME_MEAT, Prep.RAW, EatingLadder.TURNING, 40, 22, u2)) sickWeak++;
            if (EatingLadder.rollGutIllness(Family.GAME_MEAT, Prep.STEWED, EatingLadder.TURNING, 80, 22, u3)) sickStew++;
            nausea += EatingLadder.risk(Family.GAME_MEAT, Prep.RAW, EatingLadder.TURNING).nauseaNow();
        }
        assertEquals(50.0, 100.0 * sickFit / n, 1.5);
        assertEquals(40.0, nausea / n, 0);
        assertEquals(62.5, 100.0 * sickWeak / n, 1.5);
        assertEquals(15.0, 100.0 * sickStew / n, 1.0);
    }

    // 6. decay/botulism_container (m5)
    @Test
    void botulism_container() {
        int n = 1000, pit = 0, tin = 0, boiled = 0;
        for (int i = 0; i < n; i++) {
            if (SourFish.toxic(7L, i, 64, -i, 200, SourFish.Container.GRASS_PIT)) pit++;
            boolean t = SourFish.toxic(7L, i, 64, -i, 200, SourFish.Container.LIDDED_TIN);
            if (t) tin++;
            if (SourFish.causesBotulism(t, 10)) boiled++;
        }
        assertEquals(0, pit);
        assertEquals(10.0, 100.0 * tin / n, 2.0);
        assertEquals(0, boiled);
    }

    // 7. decay/old_camp_10y (m9): the sod roof and the peeled cabin, on the pure core.
    @Test
    void old_camp_10y_roof_and_peeled_logs() {
        DecayClimate years = DecayClimate.livingRot(Climates.standardYears(13), C);
        double d5 = years.decayHoursAt(5 * 8760);
        double d10 = years.decayHoursAt(10 * 8760);
        double d13 = years.decayHoursAt(13 * 8760);
        double tauPole = DecayCore.tauFromRatePerValleyYear(0.04);
        // Sod sound for 5 years keeps the poles at r = 0.04; once it erodes, leaks raise f_W to 2: r = 0.08.
        double p10 = DecayCore.settle(Shape.PROPORTIONAL, DecayCore.settle(Shape.PROPORTIONAL, 0, d5, 1, tauPole), d10 - d5, 2, tauPole);
        double p13 = DecayCore.settle(Shape.PROPORTIONAL, DecayCore.settle(Shape.PROPORTIONAL, 0, d5, 1, tauPole), d13 - d5, 2, tauPole);

        int blocks = 18; // the first cabin's 18 m² of sod
        int down10 = 0, down13 = 0;
        for (long seed = 1; seed <= 50; seed++) {
            for (int b = 0; b < blocks; b++) {
                double u = Thresholds.loadThreshold(seed, b % 6, 68, b / 6, 0);
                if (p10 >= u) down10++;
                if (p13 >= u) down13++;
            }
        }
        double share10 = (double) down10 / (50 * blocks);
        assertTrue(share10 >= 0.40 && share10 <= 0.60, "sod roof down at 10 years: " + share10);
        assertEquals(50 * blocks, down13, "all of it by 13");
        assertEquals(roofHash(42, p10), roofHash(42, p10), "same seed, same blocks");

        // Peeled logs on corner stones: r = 0.02 off the ground; under 5% Rotten at 10 years.
        double peeled = DecayCore.settle(Shape.PROPORTIONAL, 0, d10, 1, DecayCore.tauFromRatePerValleyYear(0.02));
        assertTrue(DecayLadder.BUILT_WOOD.stageOf(peeled) < 3, "peeled corner-stone log at " + peeled);
    }

    private static long roofHash(long seed, double p) {
        long h = 1469598103934665603L;
        for (int b = 0; b < 18; b++) {
            boolean down = p >= Thresholds.loadThreshold(seed, b % 6, 68, b / 6, 0);
            h = (h ^ (down ? 1 : 0)) * 1099511628211L;
        }
        return h;
    }

    @Test
    @Disabled("M9: the unpeeled-on-soil 25-35% Rotten figure depends on the first-cabin template's sill and course layout")
    void old_camp_10y_unpeeled_template() {}

    @Test
    @Disabled("M8: needs the year-3 homestead and the chore simulator (UpkeepLedger)")
    void upkeep_budget() {}
}
