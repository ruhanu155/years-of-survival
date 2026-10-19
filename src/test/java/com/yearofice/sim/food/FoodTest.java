package com.yearofice.sim.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.decay.DecayCore;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.food.Doneness.Fire;
import com.yearofice.sim.food.Doneness.Method;
import com.yearofice.sim.food.Doneness.Ring;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Items › Food values tests (food/units_match_kcal, render_never_gains, dish_sums), mice, and doneness. */
class FoodTest {
    @SuppressWarnings("unchecked")
    static List<FoodValues> load() throws Exception {
        try (var in = FoodTest.class.getResourceAsStream("/data/yearofice/data_maps/item/food.json")) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> root = (Map<String, Object>) MiniJson.parse(text);
            List<FoodValues> out = new ArrayList<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) root.get("values")).entrySet()) {
                Map<String, Object> o = (Map<String, Object>) e.getValue();
                for (String key : o.keySet()) assertEquals(key.toLowerCase(java.util.Locale.ROOT), key, "snake_case keys");
                EnumMap<FoodState, Double> life = new EnumMap<>(FoodState.class);
                for (Map.Entry<String, Object> l : ((Map<String, Object>) o.get("spoil_days_at_5c")).entrySet()) {
                    life.put(FoodState.valueOf(l.getKey().toUpperCase(java.util.Locale.ROOT)), (Double) l.getValue());
                }
                out.add(new FoodValues(e.getKey(), (Double) o.get("kcal_per_100g"), (Double) o.get("protein_g_per_100g"),
                        (Double) o.get("fat_g_per_100g"), (Double) o.get("vit_c_mg_per_100g"), (String) o.get("curve"), life));
            }
            return out;
        }
    }

    static FoodValues row(List<FoodValues> rows, String id) {
        return rows.stream().filter(r -> r.id().equals("yearofice:" + id)).findFirst().orElseThrow();
    }

    @Test
    void units_match_kcal() throws Exception {
        List<FoodValues> rows = load();
        assertTrue(rows.size() >= 40);
        for (FoodValues v : rows) {
            assertEquals(v.kcalPer100g(), v.kcalFromMacros(), 2.0, v.id());
            assertTrue(v.carbsGPer100g() >= 0, v.id());
        }
    }

    @Test
    void render_never_gains() throws Exception {
        List<FoodValues> rows = load();
        double fat = row(rows, "animal_fat").kcal(1000);
        double out = row(rows, "lard").kcal(1000 * FoodValues.RENDER_LARD_SHARE)
                + row(rows, "cracklings").kcal(1000 * FoodValues.RENDER_CRACKLINGS_SHARE);
        assertTrue(out <= 7000 && out <= fat, "render gives " + out);
    }

    @Test
    void dish_sums() throws Exception {
        double stew = row(load(), "moose_meat").kcal(1000) * FoodValues.STEW_WITH_BONES;
        assertEquals(1323, stew, 1);
    }

    @Test
    void lifeAndCurves() throws Exception {
        List<FoodValues> rows = load();
        FoodValues moose = row(rows, "moose_meat");
        assertEquals(DecayCore.tauFromSafeLifeDays(6), moose.refLifeDh(FoodState.RAW), 1e-9);
        assertEquals(30 * 24, moose.refLifeDh(FoodState.SMOKED), 1e-9);
        assertEquals(180 * 24, row(rows, "lard").refLifeDh(FoodState.COOKED), 1e-9);
        assertEquals(1.8 / 6, row(rows, "liver").safeLifeDays(FoodState.RAW) / moose.safeLifeDays(FoodState.RAW), 1e-9);
        assertEquals(1.0, row(rows, "rose_hips").cDays(50), 1e-9);
    }

    @Test
    void cacheLosesTwoToThreePercentAWeekToMiceNoneWhenRinged() {
        long seed = 31L;
        int units = 1400; // 140 kg of meat in 100 g units
        int[] months = {10, 11, 12, 1, 2, 3, 4};
        int week = 0;
        for (int m : months) {
            for (int w = 0; w < 4; w++, week++) {
                int lost = MiceInStores.unitsLost(seed, 10, 64, 10, week, 0, units, MiceInStores.Store.CACHE, 0, m);
                double share = (double) lost / units;
                assertTrue(share >= 0.02 - 1.0 / units && share <= 0.03 + 1.0 / units, "week " + week + " lost " + share);
                assertEquals(0, MiceInStores.unitsLost(seed, 10, 64, 10, week, 0, units, MiceInStores.Store.CACHE, 3, m));
                assertEquals(0, MiceInStores.unitsLost(seed, 10, 64, 10, week, 0, units, MiceInStores.Store.BARREL, 0, m));
                assertEquals(0, MiceInStores.unitsLost(seed, 10, 64, 10, week, 0, units, MiceInStores.Store.ICE_HOUSE, 0, m));
            }
        }
        for (int m = 5; m <= 9; m++) {
            assertEquals(0, MiceInStores.unitsLost(seed, 10, 64, 10, 99, 0, units, MiceInStores.Store.CHEST, 0, m));
        }
    }

    @Test
    void smallStacksLoseTheRightShareOnAverage() {
        long lost = 0;
        int weeks = 4000;
        for (int w = 0; w < weeks; w++) lost += MiceInStores.unitsLost(5L, 0, 0, 0, w, 2, 10, MiceInStores.Store.CHEST, 1, 1);
        double share = lost / (10.0 * weeks);
        assertTrue(share > 0.02 && share < 0.03, "mean share " + share);
    }

    @Test
    void doneness() {
        double t = Doneness.cookMinutes(Method.SPIT, 1.0, false, false, false);
        assertEquals(30, t, 0);
        assertEquals(60, Doneness.cookMinutes(Method.SPIT, 1.0, false, false, true), 0);
        Doneness.Cook c = Doneness.Cook.start(Method.SPIT, t, 0);
        assertEquals(Ring.RAW, c.after(14, Fire.COALS).ring(Fire.COALS));
        assertEquals(Ring.RARE, c.after(15, Fire.COALS).ring(Fire.COALS));
        assertEquals(Ring.DONE, c.after(27, Fire.COALS).ring(Fire.COALS));
        assertEquals(Ring.BURNT, c.after(45, Fire.COALS).ring(Fire.COALS));
        assertEquals(Ring.BURNT, c.after(34, Fire.HOT).ring(Fire.HOT));
        assertEquals(Ring.RAW, c.after(60, Fire.BANKED).ring(Fire.BANKED));
        Doneness.Cook bake = Doneness.Cook.start(Method.COAL_BAKE, 40, 0).after(200, Fire.STEADY);
        assertEquals(Ring.DONE, bake.ring(Fire.STEADY));
        assertEquals(0.5, Doneness.parasiteShare(Ring.RARE), 0);
        assertEquals(0.7, Doneness.calorieShare(Ring.BURNT), 0);
    }
}
