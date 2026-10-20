package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.worldgen.debug.StructureNbt;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Station Nine's template, read back from {@code structure/setpiece/outpost/day_one.nbt} without Minecraft: the
 * Blueprint › Start site › The outpost layout rows that fit in a template, and the spawn and fire spot that
 * {@code ValleyWorld.spawn} and {@code story.BeaconTargets} derive from it.
 */
class OutpostTemplateTest {
    private final Map<String, String> at = new HashMap<>();
    private final Map<String, Map<String, String>> tags = new HashMap<>();
    private StructureNbt.Template t;

    private void load() throws IOException {
        try (InputStream in = OutpostTemplateTest.class.getClassLoader().getResourceAsStream("data/yearofice/structure/setpiece/outpost/day_one.nbt")) {
            assertNotNull(in);
            t = StructureNbt.read(in);
        }
        for (int i = 0; i < t.blocks().size(); i++) {
            int[] b = t.blocks().get(i);
            String key = b[0] + "," + b[1] + "," + b[2];
            at.put(key, t.palette().get(b[3]).name().replace("minecraft:", ""));
            if (!t.nbtOf(i).isEmpty()) tags.put(key, t.nbtOf(i));
        }
    }

    private String block(int x, int y, int z) { return at.getOrDefault(x + "," + y + "," + z, "air"); }

    @Test
    void theCabinIsFiveByFiveSixWithADoorAndAHalfCavedRoof() throws IOException {
        load();
        SetPieceDef.Variant v = SetPieceDef.load("outpost").variants().get(0);
        assertEquals(12, v.footprintX());
        assertEquals(15, v.footprintZ());
        assertEquals(v.footprintX(), t.sx());
        assertEquals(v.footprintZ(), t.sz());
        // 5 × 6 of spruce logs at x 1–5, z 1–6: corners stand, the door is in the south wall.
        for (int[] c : new int[][] {{1, 1}, {5, 1}, {1, 6}, {5, 6}}) assertEquals("spruce_log", block(c[0], 0, c[1]));
        assertEquals("spruce_door", block(3, 0, 6));
        assertEquals("spruce_door", block(3, 1, 6));
        assertEquals("air", block(0, 0, 3), "nothing outside the wall");
        // The roof: the south half is whole, the north half is a few boards.
        int south = 0, north = 0;
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 6; z++) if (!block(x, 3, z).equals("air")) { if (z <= 3) north++; else south++; }
        assertEquals(15, south, "the south half of the roof stands");
        assertTrue(north > 0 && north < 15 * 2 / 3, "the north half has caved: " + north + " of 15 left");
    }

    @Test
    void theCrateHoldsTheStartingKitInTheDrySouthEastCorner() throws IOException {
        load();
        assertEquals("barrel", block(4, 0, 5));
        assertEquals("yearofice:player/starting_kit", tags.get("4,0,5").get("LootTable"));
        assertFalse(block(4, 3, 5).equals("air"), "a roof over the crate");
        int crates = 0;
        for (Map.Entry<String, Map<String, String>> e : tags.entrySet()) if (e.getValue().containsKey("LootTable")) crates++;
        assertEquals(1, crates, "one supply crate");
        // The bunk of bare boards, the radio on its table, the woodpile, the scrap barrel and the weather post.
        assertEquals("spruce_slab", block(4, 0, 3));
        assertEquals("daylight_detector", block(2, 1, 2));
        assertEquals("spruce_log", block(3, 0, 0));
        assertEquals("cauldron", block(6, 0, 0));
        assertEquals("iron_bars", block(11, 2, 10));
    }

    @Test
    void theFirepitIsSixBlocksOutOfTheSpawnAlongTheDoorLine() throws IOException {
        load();
        SetPieceDef.Variant v = SetPieceDef.load("outpost").variants().get(0);
        int[] spawn = v.spawnOffset();
        assertEquals(3, spawn[0], "the spawn is in line with the door");
        assertEquals("spruce_door", block(spawn[0], 0, spawn[2] - 1), "the spawn is one block outside the door");
        assertEquals("air", block(spawn[0], spawn[1], spawn[2]));
        assertEquals("air", block(spawn[0], spawn[1] + 1, spawn[2]));
        int fz = spawn[2] + 6;
        assertEquals("light_gray_concrete_powder", block(spawn[0], 0, fz), "old ash 6 m from the door");
        int ring = 0;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dz == 0) continue;
            if (block(spawn[0] + dx, 0, fz + dz).equals("cobblestone")) ring++;
        }
        assertEquals(8, ring, "a ring of 8 stones");
        assertTrue(fz + 1 < t.sz(), "the ring is inside the template");
    }

    @Test
    void theMastLiesInTheYardEightBlocksLongBentAtOneJoint() throws IOException {
        load();
        int bars = 0;
        for (Map.Entry<String, String> e : at.entrySet()) if (e.getValue().equals("iron_bars") && e.getKey().endsWith(",0," + 9) || e.getKey().endsWith(",0,10") && e.getValue().equals("iron_bars")) bars++;
        assertEquals(8, bars);
    }
}
