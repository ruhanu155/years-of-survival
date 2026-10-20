package com.yearofice.world.gametest;

import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.SetPieces;
import com.yearofice.worldgen.StartSite;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.structure.PlacedMask;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * M3 lane A's world GameTest (batch {@code m3_start}): Station Nine on the scored start site of the slice valley
 * (Blueprint › Start site › The outpost layout). The checks themselves run in JUnit on the layers
 * ({@code StartSiteTest}); this one proves the template reaches the world the way the generator places it.
 */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StartSiteGameTests {
    private StartSiteGameTests() {}

    /**
     * {@code start/station_nine}: the outpost stands on the scored site (the search's winner is the placed outpost),
     * the spawn is one block out of a shut door, the firepit's ring of 8 stones surrounds old ash 6 blocks out of the
     * spawn along its facing, and the one supply crate unpacks the starting kit (crate backpack, hatchet, 3 matches and the placeholder kit items).
     */
    @GameTest(template = "gametest/empty3", batch = "m3_start", timeoutTicks = 2400)
    public static void start_station_nine(GameTestHelper h) {
        ServerLevel l = M2WorldGameTests.slice(h);
        ValleyWorld w = M2WorldGameTests.world(l);
        SetPieces.Placement o = w.outpost();
        h.assertTrue(o != null, "no outpost placed");
        StartSite.Site site = w.start();
        h.assertTrue(site != null, "the world was not built through the start-site search");
        h.assertTrue(Math.abs(o.x() - site.x()) < 1e-6 && Math.abs(o.z() - site.z()) < 1e-6, "the outpost is not on the search's winning cell");

        int[] r = ValleyWorld.rect(o);
        for (int cz = (r[1] >> 4) - 1; cz <= ((r[1] + r[3]) >> 4) + 1; cz++)
            for (int cx = (r[0] >> 4) - 1; cx <= ((r[0] + r[2]) >> 4) + 1; cx++) l.getChunk(cx, cz);
        int[] s = w.spawn();
        PlacedMask mask = PlacedMask.get(l);
        if (mask.count(new BlockPos(r[0], s[1], r[1]), new BlockPos(r[0] + r[2] - 1, s[1] + 6, r[1] + r[3] - 1)) == 0) {
            M2WorldGameTests.placeSetPiece(l, w, o);
        }

        var tpl = com.yearofice.worldgen.structure.SetPieceTemplates.load(l, o.template());
        h.assertTrue(tpl.isPresent(), "the outpost template " + o.template() + " does not load");
        double cov = com.yearofice.worldgen.structure.SetPieceTemplates.coverage(l, tpl.get(), new BlockPos(r[0], ValleyWorld.padY(o) + 1, r[1]),
                com.yearofice.worldgen.structure.TemplatePiece.ROTATIONS[o.rotation()]);
        h.assertTrue(cov >= 0.8, "only " + Math.round(cov * 100) + "% of Station Nine's blocks stand after placement");

        // The spawn: free to stand in, one block out of a shut spruce door (the door is behind the player's facing).
        BlockPos spawn = new BlockPos(s[0], s[1], s[2]);
        double yaw = Math.toRadians(s[3]);
        int fx = (int) Math.round(-Math.sin(yaw)), fz = (int) Math.round(Math.cos(yaw));
        h.assertTrue(l.getBlockState(spawn).isAir() && l.getBlockState(spawn.above()).isAir(), "no room to stand at the spawn " + spawn);
        BlockPos door = spawn.offset(-fx, 0, -fz);
        h.assertTrue(l.getBlockState(door).is(Blocks.SPRUCE_DOOR) && l.getBlockState(door.above()).is(Blocks.SPRUCE_DOOR),
                "the spawn is not at the door: " + l.getBlockState(door));

        // The firepit: old ash 6 blocks out of the spawn along its facing, inside a ring of 8 stones.
        BlockPos ash = spawn.offset(6 * fx, 0, 6 * fz);
        h.assertTrue(l.getBlockState(ash).is(Blocks.LIGHT_GRAY_CONCRETE_POWDER), "no old ash 6 m from the door: " + l.getBlockState(ash));
        int ring = 0;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            if ((dx != 0 || dz != 0) && l.getBlockState(ash.offset(dx, 0, dz)).is(Blocks.COBBLESTONE)) ring++;
        }
        h.assertTrue(ring == 8, "the firepit ring has " + ring + " stones");

        // The supply crate: exactly one barrel with the kit's loot table in the footprint, and it unpacks the kit.
        BarrelBlockEntity crate = null;
        int crates = 0;
        String barrels = "";
        for (BlockPos p : BlockPos.betweenClosed(r[0], s[1] - 1, r[1], r[0] + r[2] - 1, s[1] + 6, r[1] + r[3] - 1)) {
            BlockEntity be = l.getBlockEntity(p);
            if (be instanceof BarrelBlockEntity b) { crate = b; crates++; }
            else if (l.getBlockState(p).is(Blocks.BARREL)) barrels += " " + p.toShortString() + ":" + (be == null ? "no block entity" : be.getClass().getSimpleName() + " " + (be instanceof BarrelBlockEntity bb ? bb.getLootTable() + " saved " + bb.saveWithoutMetadata(l.registryAccess()) : ""));
        }
        h.assertTrue(crates == 1 && crate != null, "expected one supply crate, found " + crates + "; barrels:" + barrels + " footprint " + java.util.Arrays.toString(r) + " spawn y " + s[1] + " template barrels "
                + l.getStructureManager().get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "setpiece/outpost/day_one")).map(t -> t.filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), Blocks.BARREL).stream().map(i -> String.valueOf(i.nbt())).toList()).orElse(java.util.List.of("no template")));
        // The crate may already be unpacked by world generation; if not, it names the kit's table and we unpack it here.
        if (crate.getLootTable() != null) {
            h.assertTrue(crate.getLootTable().location().equals(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "player/starting_kit")),
                    "the crate holds " + crate.getLootTable().location());
            crate.unpackLootTable(null);
        }
        java.util.Map<String, Integer> got = new java.util.TreeMap<>();
        for (int i = 0; i < crate.getContainerSize(); i++) {
            var st = crate.getItem(i);
            if (!st.isEmpty()) got.merge(BuiltInRegistries.ITEM.getKey(st.getItem()).getPath(), st.getCount(), Integer::sum);
        }
        java.util.Map<String, Integer> want = new java.util.TreeMap<>();
        String[] ids = {"crate_backpack", "hatchet", "match", "fixed_blade_knife", "steel_bottle", "cooking_pot", "pilot_bread", "medkit",
                "bear_spray", "cord", "seed_packet", "compass", "headlamp", "plant_guide", "solar_panel_small"};
        int[] counts = {1, 1, 3, 1, 1, 1, 38, 1, 1, 10, 1, 1, 1, 1, 1};
        for (int i = 0; i < ids.length; i++) want.put(ids[i], counts[i]);
        h.assertTrue(got.equals(want), "the kit: " + got);
        // Placement used no more than the 20 attempts the search allows, and the winning attempt passed all 12 checks.
        var out = w.startOutcome();
        h.assertTrue(out.rerolls() <= 19 && out.reports().get(out.reports().size() - 1).passed(), "placement took " + out.rerolls() + " rerolls");
        h.succeed();
    }
}
