package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.yearofice.world.GroundYield;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;

class GroundYieldTest {
    private static com.google.gson.JsonObject json(String path) throws Exception {
        try (var in = GroundYieldTest.class.getResourceAsStream(path)) {
            return JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
        }
    }

    /** Braided flats double the 1 % flint (Blueprint: twice the usual density); other valleys add nothing. */
    @Test
    void braidedFlatsDoubleTheFlint() {
        assertEquals(0.01, GroundYield.extraFlint(true, 2.0), 1e-12);
        assertEquals(0.0, GroundYield.extraFlint(false, 2.0), 0);
        assertEquals(0.0, GroundYield.extraFlint(true, 1.0), 0);
    }

    /** The loot table's base flint chance is the one GroundYield doubles. */
    @Test
    void baseFlintMatchesTheLootTable() throws Exception {
        var pools = json("/data/yearofice/loot_table/blocks/gravel.json").getAsJsonArray("pools");
        double flint = -1;
        for (var p : pools) {
            var o = p.getAsJsonObject();
            if (o.getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString().equals("yearofice:flint_nodule"))
                flint = o.getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble();
        }
        assertEquals(GroundYield.FLINT_BASE, flint, 1e-12);
    }

    /** A sheaf holds the block's grain (crop kg_per_block, 100 g barley units) and 0.5 kg of straw (one 0.5 kg straw). */
    @Test
    void sheafHoldsOneBlocksGrain() throws Exception {
        var crop = json("/data/yearofice/crop/barley.json").getAsJsonObject("yield");
        var ing = json("/data/yearofice/recipe/station/grain_sheaf.json").getAsJsonArray("ingredients");
        int barley = 0, straw = 0;
        for (var i : ing) {
            var o = i.getAsJsonObject();
            if (o.get("item").getAsString().equals("yearofice:barley")) barley = o.get("count").getAsInt();
            if (o.get("item").getAsString().equals("yearofice:straw")) straw = o.get("count").getAsInt();
        }
        assertEquals(Math.round(crop.get("kg_per_block").getAsDouble() * 10), barley);
        assertEquals(Math.round(crop.get("straw_kg").getAsDouble() / 0.5), straw);
    }
}
