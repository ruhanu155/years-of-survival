package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** {@code body/standard.json} and {@code preset/*.json} say what the code does (data and code never drift). */
class BodyDataTest {
    static JsonObject load(String path) throws IOException {
        try (InputStream in = BodyDataTest.class.getResourceAsStream("/data/yearofice/" + path)) {
            assertTrue(in != null, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void standardJsonMatchesMetabolism() throws IOException {
        JsonObject o = load("body/standard.json");
        JsonObject bmr = o.getAsJsonObject("bmr");
        assertEquals(Metabolism.DEFAULT_BMR, bmr.get("default").getAsDouble());
        assertEquals(Metabolism.bmr(bmr.get("kg").getAsDouble(), bmr.get("height_cm").getAsInt(), 0),
                bmr.get("default").getAsDouble(), 1.0);
        assertEquals(Metabolism.AGE, bmr.get("age_years").getAsInt());
        JsonObject act = o.getAsJsonObject("activity_kcal_per_h");
        for (Activity a : Activity.values()) {
            assertEquals(Metabolism.kcalPerHour(a), act.get(a.name().toLowerCase()).getAsDouble(), a.name());
        }
        JsonObject kpk = o.getAsJsonObject("kcal_per_kg");
        assertEquals(BodyTuning.GENTLE.kcalPerKg(), kpk.get("gentle").getAsDouble());
        assertEquals(BodyTuning.STANDARD.kcalPerKg(), kpk.get("standard").getAsDouble());
        assertEquals(BodyTuning.BRUTAL.kcalPerKg(), kpk.get("brutal").getAsDouble());
        assertEquals(BodyTuning.STANDARD.sosBmi(), o.getAsJsonObject("bmi").get("sos").getAsDouble());
        assertEquals(IllnessModel.VITC_FULL_DAYS, o.getAsJsonObject("vitamin_c").get("reserve_days").getAsDouble());
        assertEquals(Metabolism.GUT_ILL_FACTOR, o.getAsJsonObject("gut").get("absorb").getAsDouble());
        assertEquals(IllnessModel.GUT_FLOOR_HOURS, o.getAsJsonObject("gut").get("floor_h").getAsInt());
        assertEquals(RestDay.WORK_MAX_MIN, o.getAsJsonObject("rest_days").get("work_max_min").getAsInt());
        assertEquals(Skin.SUN_FROM_DAY, o.getAsJsonObject("skin").get("sun_from_doy").getAsInt());
    }

    @Test
    void presetFilesMatchTheJavaPresets() throws IOException {
        for (BodyTuning t : new BodyTuning[] {BodyTuning.GENTLE, BodyTuning.STANDARD, BodyTuning.BRUTAL}) {
            JsonObject o = load("preset/" + t.preset() + ".json");
            JsonObject b = o.getAsJsonObject("body");
            assertEquals(t.calorieBurn(), b.get("calorie_burn").getAsDouble(), t.preset());
            assertEquals(t.kcalPerKg(), b.get("kcal_per_kg").getAsDouble(), t.preset());
            assertEquals(t.coldScale(), b.get("cold").getAsDouble(), t.preset());
            assertEquals(t.waterNeed(), b.get("water_need").getAsDouble(), t.preset());
            assertEquals(t.unboiledIllness(), b.get("unboiled_illness").getAsDouble(), t.preset());
            assertEquals(t.bleedScale(), b.get("bleed_scale").getAsDouble(), t.preset());
            assertEquals(t.infectionScale(), b.get("infection_scale").getAsDouble(), t.preset());
            assertEquals(t.digitLoss(), b.get("digit_loss").getAsBoolean(), t.preset());
            JsonObject w = o.getAsJsonObject("wellness");
            WellnessTuning wt = WellnessTuning.of(t.preset());
            assertEquals(wt.illnessScale(), w.get("illness_scale").getAsDouble(), t.preset());
            assertEquals(wt.restHeal(), w.get("rest_heal").getAsDouble(), t.preset());
            assertEquals(wt.sunburnHours(), w.get("sunburn_h").getAsDouble(), t.preset());
            assertEquals(wt.beardIceC(), w.get("beard_ice_temp_c").getAsDouble(), t.preset());
        }
    }

    @Test
    void everyIllnessFileIsNamedForItsIdAndCarriesASource() throws IOException {
        for (String id : IllnessDefs.IDS) {
            JsonObject o = load("illness/" + id + ".json");
            assertEquals("yearofice:" + id, o.get("id").getAsString());
            assertTrue(o.get("source").getAsString().length() > 3, id);
        }
        for (String id : new String[] {"injury/sunburn", "injury/windburn", "action/grease_face", "action/grease_gloves"}) {
            assertEquals("yearofice:" + id.substring(id.indexOf('/') + 1), load(id + ".json").get("id").getAsString());
        }
    }
}
