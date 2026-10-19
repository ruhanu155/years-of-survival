package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.worldgen.debug.StructureNbt;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * M1 data: the set pieces load strictly from {@code setpiece/<id>.json}; the seven home-valley set pieces carry
 * 2–3 variants each (the outpost its one {@code day_one}); every variant's template exists at
 * {@code structure/setpiece/<id>/<variant>.nbt}, matches its footprint and height, stays within 48 × 48 × 32,
 * saves no air and holds no jigsaw block; fishing-spot bands and capacities are in valley.json.
 */
class SetPieceDataTest {
    @Test
    void setPiecesLoadWithTheirVariants() {
        List<SetPieceDef> defs = SetPieceDef.loadAll();
        TreeSet<String> ids = new TreeSet<>();
        for (SetPieceDef d : defs) ids.add(d.path());
        assertEquals(new TreeSet<>(List.of("outpost", "plane_wreck", "arvid_cabin", "mining_camp", "fire_lookout", "hot_spring",
                "ice_cave", "beaver_valley")), ids);
        for (SetPieceDef d : defs) {
            int v = d.variants().size();
            if (d.path().equals("outpost")) assertEquals(1, v);
            else assertTrue(v >= 2 && v <= 3, d.id() + " has " + v + " variants");
            assertTrue(d.fit().maxSlopeDeg() <= 25, d.id() + " allows slopes over 25 degrees");
            assertTrue(d.ring().contains(1), d.id() + " ring");
            assertFalse(d.fit().sites().isEmpty(), d.id() + " has no site kinds");
        }
        for (int i = 1; i < defs.size(); i++) assertTrue(defs.get(i - 1).priority() >= defs.get(i).priority(), "priority order");
    }

    @Test
    void everyTemplateExistsAndKeepsTheTemplateRules() throws IOException {
        int count = 0;
        for (SetPieceDef d : SetPieceDef.loadAll()) {
            for (SetPieceDef.Variant v : d.variants()) {
                String res = "data/yearofice/structure/setpiece/" + v.template() + ".nbt";
                assertTrue(v.template().startsWith(d.path() + "/"), v.template() + " is not under " + d.path());
                try (InputStream in = SetPieceDataTest.class.getClassLoader().getResourceAsStream(res)) {
                    assertNotNull(in, "missing " + res);
                    StructureNbt.Template t = StructureNbt.read(in);
                    assertEquals(v.footprintX(), t.sx(), res + " x");
                    assertEquals(v.footprintZ(), t.sz(), res + " z");
                    assertEquals(v.heightM(), t.sy(), res + " height");
                    assertTrue(t.sx() <= 48 && t.sz() <= 48 && t.sy() <= 32, res + " too big");
                    assertFalse(t.holds("minecraft:jigsaw"), res + " holds a jigsaw block");
                    assertFalse(t.holds("minecraft:air"), res + " saves air");
                    assertFalse(t.holds("minecraft:structure_block"), res + " holds a structure block");
                    assertFalse(t.holds("minecraft:snow") || t.holds("minecraft:mud"), res + " uses a banned vanilla block");
                    assertTrue(t.blocks().size() > 0, res + " is empty");
                    assertEquals(StructureNbt.DATA_VERSION, t.dataVersion(), res + " data version");
                }
                count++;
            }
        }
        assertEquals(22, count);
    }

    @Test
    void spotBandsAndCapacitiesAreData() {
        HydroParams.Spots s = Fixture.PIPE.params().hydro().spots();
        assertEquals(0.7, s.type("pool_below_falls").band().min());
        assertEquals(1.0, s.type("pool_below_falls").band().max());
        assertEquals(0.5, s.type("lake_inlet").band().min());
        assertEquals(0.4, s.type("outside_bend").band().min());
        assertEquals(0.3, s.type("beaver_pond").band().min());
        assertEquals(60, s.type("pool_below_falls").capacity()[0]);
        assertEquals(0.8, s.variation().min());
        assertEquals(1.2, s.variation().max());
    }
}
