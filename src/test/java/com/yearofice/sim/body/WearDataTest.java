package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.body.clothing.WearMap;
import com.mojang.serialization.JsonOps;
import java.io.FileReader;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The shipped wear.json rows, through the data map's own codec: fur mittens are safe at −25 °C and not at −26 °C. */
class WearDataTest {
    private static Garment row(String id) throws Exception {
        try (var r = new FileReader("src/main/resources/data/yearofice/data_maps/item/wear.json")) {
            JsonObject v = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("values");
            return WearMap.Wear.CODEC.parse(JsonOps.INSTANCE, v.get("yearofice:" + id)).getOrThrow().garment(id);
        }
    }

    @Test
    void shippedMittensAreSafeAtMinus25AndNotMinus26() throws Exception {
        ClothingState hands = ClothingState.of(Wardrobe.WORK_GLOVES, row("fur_mittens"));
        double limit = Clothing.zoneSafeToC(hands, Zone.HANDS);
        assertEquals(-25, limit, 1e-12);
        double at25 = Warmth.zoneExposureC(com.yearofice.sim.env.EnvSample.outdoors(-25, 0), hands, List.of(), WarmthContext.RESTING, Zone.HANDS);
        double at26 = Warmth.zoneExposureC(com.yearofice.sim.env.EnvSample.outdoors(-26, 0), hands, List.of(), WarmthContext.RESTING, Zone.HANDS);
        int p25 = 0, p26 = 0;
        for (int i = 0; i < 24 * 60; i++) { p25 = FrostModel.stepMinute(p25, limit, at25, false, true).points(); }
        p26 = FrostModel.stepMinute(0, limit, at26, false, true).points();
        assertEquals(0, p25);
        assertTrue(p26 > 0);
    }

    @Test
    void wearRowsMatchTheBlueprintTable() throws Exception {
        assertEquals(15, row("parka").warmthC());
        assertEquals(-35, row("mukluks").safeToC());
        assertEquals(Zone.FEET, row("hare_fur_liners").zone());
        assertEquals(Layer.INNER, row("hare_fur_liners").layer());
    }
}
