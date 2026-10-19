package com.yearofice.sim.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.decay.DecayCore;
import com.yearofice.sim.decay.DecayLadder;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.food.Doneness.Fire;
import com.yearofice.sim.food.Doneness.Ring;
import com.yearofice.sim.food.Roasting.Rule;
import com.yearofice.sim.food.Roasting.Spit;
import org.junit.jupiter.api.Test;

/** The spit roast (Blueprint › Cooking) and the bite rules (Decay and time › Eating decayed food). */
class RoastingTest {
    static final RoastTuning T = RoastTuning.standard();

    @Test
    void cookTimesFollowTheCookingTable() {
        assertEquals(25.0, Roasting.spitMinutes(Rule.WHOLE_HARE, 900, T), 1e-9, "whole hare 25");
        assertEquals(12.5, Roasting.spitMinutes(Rule.WHOLE_HARE, 450, T), 1e-9, "half a hare, half the time");
        assertEquals(10.5, Roasting.spitMinutes(Rule.PER_KG, 350, T), 1e-9, "a ptarmigan's 0.35 kg at 30 min a kg");
        assertEquals(12.0, Roasting.spitMinutes(Rule.WHOLE_FISH, 400, T), 1e-9, "a fish under 1 kg: 12");
        assertEquals(36.0, Roasting.spitMinutes(Rule.WHOLE_FISH, 1200, T), 1e-9, "a 1.2 kg fish at the per-kg rate");
    }

    @Test
    void fireStatesMapToCooking() {
        assertEquals(Fire.STEADY, Roasting.fireOf(FireState.FIRE, FireSetting.STEADY));
        assertEquals(Fire.HOT, Roasting.fireOf(FireState.FIRE, FireSetting.HOT));
        assertEquals(Fire.COALS, Roasting.fireOf(FireState.COALS, FireSetting.LOW));
        assertEquals(Fire.SMALL_FIRE, Roasting.fireOf(FireState.SMALL, FireSetting.STEADY));
        for (Fire f : new Fire[] {Fire.OUT, Fire.EMBER, Fire.TINDER, Fire.SMALL_FIRE, Fire.BANKED}) {
            assertEquals(0.0, Roasting.speed(f, T), "no cooking on " + f);
        }
        assertEquals(1.0, Roasting.speed(Fire.COALS, T), "spit over coals is the table's station");
    }

    /** Ready for development › M2.5 test 4, the formula side: a whole hare is cooked by 20 min on Steady and Done by 22.5. */
    @Test
    void wholeHareOnSteady() {
        Spit s = Spit.load(25, null);
        assertEquals(Ring.RAW, s.after(12, Fire.STEADY, T).ring(Fire.STEADY));
        assertEquals(Ring.RARE, s.after(13, Fire.STEADY, T).ring(Fire.STEADY), "Rare from 50%");
        assertTrue(Roasting.cooked(s.after(20, Fire.STEADY, T).ring(Fire.STEADY)), "cooked at 20 game minutes");
        assertEquals(Ring.DONE, s.after(22.5, Fire.STEADY, T).ring(Fire.STEADY), "Done from 90%");
        assertEquals(22.5, s.minutesUntil(Doneness.DONE_FROM, Fire.STEADY, T), 1e-9);
        assertEquals(Ring.DONE, s.after(37, Fire.STEADY, T).ring(Fire.STEADY));
        assertEquals(Ring.BURNT, s.after(37.5, Fire.STEADY, T).ring(Fire.STEADY), "Burnt at 150%");
    }

    @Test
    void hotBurnsSoonerAndBurntIsLatched() {
        Spit s = Spit.load(25, null).after(28, Fire.HOT, T);
        assertEquals(Ring.DONE, s.ring(Fire.HOT));
        Spit burnt = s.after(1, Fire.HOT, T);
        assertTrue(burnt.burnt(), "past 112.5% on Hot");
        assertEquals(Ring.BURNT, burnt.after(5, Fire.LOW, T).ring(Fire.LOW), "turning the fire down does not unburn it");
        assertEquals(Ring.BURNT, burnt.after(5, Fire.OUT, T).ring(Fire.OUT));
    }

    @Test
    void noHeatNoProgressAndResumeFromTheRing() {
        Spit s = Spit.load(25, null).after(30, Fire.OUT, T);
        assertEquals(0.0, s.progress(), "an out fire cooks nothing");
        assertEquals(Doneness.DONE_FROM, Spit.load(25, Ring.DONE).progress(), "a Done piece goes back on at 90%");
        assertTrue(Spit.load(25, Ring.BURNT).burnt(), "burnt stays burnt");
        Spit hot = Spit.load(25, null).after(30, Fire.STEADY, T);
        assertEquals(Ring.DONE, hot.after(0, Fire.OUT, T).ring(Fire.OUT), "off the heat it reads its last ring short of burnt");
    }

    @Test
    void speedScalesTime() {
        RoastTuning fast = new RoastTuning(30, 25, 900, 12, 1000, 900, 1.0, 1.0, 2.0, 1.0, 32);
        assertEquals(11.25, Spit.load(25, null).minutesUntil(Doneness.DONE_FROM, Fire.HOT, fast), 1e-9);
    }

    /** Cooked meat keeps its raw progress and lasts its cooked spoil_days_at_5c (9 d) from fresh. */
    @Test
    void cookingKeepsProgressOnTheCookedLife() {
        double rawLife = DecayCore.tauFromSafeLifeDays(6), cookedLife = DecayCore.tauFromSafeLifeDays(9);
        Fresh raw = new Fresh(FoodState.RAW, 0, 100 + rawLife, rawLife, 0, true);
        Fresh cooked = raw.withState(FoodState.COOKED, cookedLife, true, 100, 0, 100, Fresh.DEFAULT_BANDS);
        double days = cooked.decayHoursUntil(DecayCore.SAFE_P, 100) / 24.0;
        assertEquals(9.0, days, 9.0 * 0.05, "cooked from fresh: 9 days to Turning at 5 C");
        Fresh oldRaw = new Fresh(FoodState.RAW, 0, 100 + rawLife * 0.5, rawLife, 0, true); // p = 0.5, Aging
        Fresh oldCooked = oldRaw.withState(FoodState.COOKED, cookedLife, true, 100, 0, 100, Fresh.DEFAULT_BANDS);
        assertEquals(DecayLadder.FOOD.stageOf(0.5), oldCooked.stage(100), "cooking does not freshen meat");
    }

    @Test
    void biteRules() {
        Meal.Outcome burnt = Meal.bite(EatingLadder.Family.GAME_MEAT, FoodState.COOKED, Ring.BURNT, 0, 100, 22, 0.99);
        assertEquals(0.7, burnt.kcalShare(), 1e-9);
        assertEquals(-2.0, burnt.spirits(), 1e-9);
        assertEquals(0.0, burnt.gutIllness(), 1e-9);

        Meal.Outcome rare = Meal.bite(EatingLadder.Family.GAME_MEAT, FoodState.COOKED, Ring.RARE, 0, 100, 22, 0.5);
        assertEquals(Meal.PARASITE_GAME * 0.5, rare.parasites(), 1e-9, "Rare halves parasites");
        assertEquals(0.0, Meal.bite(EatingLadder.Family.GAME_MEAT, FoodState.COOKED, Ring.DONE, 0, 100, 22, 0.5).parasites(), 1e-9);
        assertEquals(Meal.PARASITE_FISH, Meal.bite(EatingLadder.Family.FISH, FoodState.RAW, null, 0, 100, 22, 0.5).parasites(), 1e-9);

        Meal.Outcome spoiled = Meal.bite(EatingLadder.Family.GAME_MEAT, FoodState.COOKED, Ring.DONE, EatingLadder.SPOILED, 100, 22, 0.79);
        assertEquals(0.8, spoiled.gutIllness(), 1e-9);
        assertTrue(spoiled.gutIll(), "u 0.79 < 0.8");
        assertFalse(Meal.bite(EatingLadder.Family.GAME_MEAT, FoodState.COOKED, Ring.DONE, EatingLadder.SPOILED, 100, 22, 0.81).gutIll());
        assertEquals(0.95, Meal.bite(EatingLadder.Family.GAME_MEAT, FoodState.COOKED, Ring.DONE, EatingLadder.SPOILED, 40, 22, 0.5).gutIllness(),
                1e-9, "weak: x1.25 capped at 95%");

        Meal.Outcome rawTurning = Meal.bite(EatingLadder.Family.GAME_MEAT, FoodState.RAW, null, EatingLadder.TURNING, 100, 22, 0.5);
        assertEquals(0.5, rawTurning.gutIllness(), 1e-9);
        assertEquals(40, rawTurning.nauseaNow(), 1e-9);
        assertTrue(Meal.bite(EatingLadder.Family.FISH, FoodState.COOKED, Ring.DONE, EatingLadder.ROTTEN, 100, 22, 0.5).refused(), "rotten is gone");
    }
}
