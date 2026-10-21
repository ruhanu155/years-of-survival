package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.yearofice.body.clothing.WearMap;
import com.yearofice.sim.env.EnvSample;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The adapter's inputs: shipped wear rows through {@link Outfit#dress}. */
class OutfitTest {
    private static WornGarment row(JsonObject v, String id) {
        return WornGarment.fresh(WearMap.Wear.CODEC.parse(JsonOps.INSTANCE, v.get("yearofice:" + id)).getOrThrow().garment(id));
    }

    private static List<WornGarment> rows(String... ids) throws Exception {
        try (var r = new FileReader("src/main/resources/data/yearofice/data_maps/item/wear.json")) {
            JsonObject v = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("values");
            List<WornGarment> out = new ArrayList<>();
            for (String id : ids) out.add(row(v, id));
            return out;
        }
    }

    @Test
    void startSetRowsMatchTheStartKitTable() throws Exception {
        ClothingState c = Outfit.dress(rows(Outfit.START_IDS.toArray(new String[0])), new double[Layer.SLOTS]);
        double sum = 0;
        for (int i = 0; i < Layer.SLOTS; i++) if (c.at(i) != null) sum += c.at(i).garment().warmthC();
        assertEquals(StartKit.TABLE_C, sum, 1e-9);
        assertEquals(-22, Clothing.zoneSafeToC(c, Zone.HANDS), 1e-9);
    }

    @Test
    void mittensOverGlovesHoldHandsAtMinus25ButGlovesAloneDoNot() throws Exception {
        double[] dry = new double[Layer.SLOTS];
        ClothingState both = Outfit.dress(rows("work_gloves", "fur_mittens"), dry);
        ClothingState gloves = Outfit.dress(rows("work_gloves"), dry);
        assertEquals(-25, Clothing.zoneSafeToC(both, Zone.HANDS), 1e-9);
        assertEquals(-22, Clothing.zoneSafeToC(gloves, Zone.HANDS), 1e-9);
        assertTrue(Clothing.zoneSafeToC(ClothingState.naked(), Zone.HANDS) > -22);
    }

    @Test
    void wetnessComesFromTheSlotsAndALaterGarmentReplacesTheSlot() throws Exception {
        double[] wet = new double[Layer.SLOTS];
        wet[Layer.slot(Zone.TORSO, Layer.OUTER)] = 0.6;
        ClothingState c = Outfit.dress(rows("rain_shell", "parka"), wet);
        assertEquals("parka", c.at(Zone.TORSO, Layer.OUTER).garment().id());
        assertEquals(0.6, c.wet(Zone.TORSO, Layer.OUTER), 1e-9);
        assertNull(c.at(Zone.TORSO, Layer.INNER));
    }
}
