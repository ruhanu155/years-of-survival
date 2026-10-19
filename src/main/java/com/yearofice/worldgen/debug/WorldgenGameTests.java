package com.yearofice.worldgen.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.Archetype;
import com.yearofice.worldgen.Landform;
import com.yearofice.worldgen.Pipeline;
import com.yearofice.worldgen.SeedStage;
import com.yearofice.worldgen.SetPieceDef;
import com.yearofice.worldgen.structure.PlacedMask;
import com.yearofice.worldgen.structure.SetPieceTemplates;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Worldgen GameTests. Batch m0: inside a real server, the mod jar's worldgen data loads through the mod class
 * loader and stage 2 gives the landform hash JUnit records. Batch m1: the same for stages 4–5, every set-piece
 * template loads from the jar, and {@code worldgen/setpiece_masked}.
 */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    private WorldgenGameTests() {}

    static final long SEED = 3141592L;
    static final int LANDFORM_HASH = 0x95b627d5;
    /** Stage 4 and 5 hashes of seed 3141592, as {@code M1HashTest} records them in JUnit. */
    static final int WATER_HASH = 0x71d37a3c;
    static final int PLACES_HASH = 0xf8528e7b;

    @GameTest(template = "gametest/empty3", batch = "m0")
    public static void worldgen_landform_hash_matches_junit(GameTestHelper helper) {
        Pipeline pipe = Pipeline.standard();
        SeedStage.SeedParams s = pipe.seed(SEED, 0);
        helper.assertTrue(s.archetype() == Archetype.OXBOW_MARSH, "seed " + SEED + " archetype " + s.archetype());
        helper.assertTrue(s.check(pipe.params()).isEmpty(), "stage 1 check: " + s.check(pipe.params()));
        Landform.Layers land = pipe.landform(s);
        helper.assertTrue(land.hash() == LANDFORM_HASH,
                "landform hash " + Integer.toHexString(land.hash()) + " != " + Integer.toHexString(LANDFORM_HASH));
        helper.succeed();
    }

    /** Stages 1–5 in the server, from the jar's data (set pieces included), match the JUnit golden hashes. */
    @GameTest(template = "gametest/empty3", batch = "m1", timeoutTicks = 400)
    public static void worldgen_m1_layers_match_junit(GameTestHelper helper) {
        Pipeline.Result r = Pipeline.standard().run(SEED);
        helper.assertTrue(r.passed(), "seed " + SEED + " failed: " + r.failures());
        helper.assertTrue(r.attempt() == 0, "seed " + SEED + " rerolled to attempt " + r.attempt());
        helper.assertTrue(r.waterHash() == WATER_HASH,
                "water hash " + Integer.toHexString(r.waterHash()) + " != " + Integer.toHexString(WATER_HASH));
        helper.assertTrue(r.placesHash() == PLACES_HASH,
                "places hash " + Integer.toHexString(r.placesHash()) + " != " + Integer.toHexString(PLACES_HASH));
        helper.succeed();
    }

    /** Every variant named in setpiece data loads as a structure template of its stated size, with no jigsaw. */
    @GameTest(template = "gametest/empty3", batch = "m1")
    public static void worldgen_setpiece_templates_load(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (SetPieceDef d : SetPieceDef.loadAll()) {
            for (SetPieceDef.Variant v : d.variants()) {
                Optional<StructureTemplate> t = SetPieceTemplates.load(level, v.template());
                helper.assertTrue(t.isPresent(), "missing template " + SetPieceTemplates.id(v.template()));
                Vec3i size = t.get().getSize();
                helper.assertTrue(size.getX() == v.footprintX() && size.getZ() == v.footprintZ() && size.getY() == v.heightM(),
                        v.template() + " is " + size + ", data says " + v.footprintX() + "x" + v.heightM() + "x" + v.footprintZ());
                helper.assertTrue(size.getX() <= 48 && size.getZ() <= 48 && size.getY() <= 32, v.template() + " over 48 x 48 x 32");
                helper.assertTrue(t.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                        Blocks.JIGSAW).isEmpty(), v.template() + " holds a jigsaw block");
            }
        }
        helper.succeed();
    }

    /**
     * {@code worldgen/setpiece_masked}: every block a set-piece template places has its placed-mask bit, and ten
     * simulated years of terrain operations (erosion and deposition at random around and inside the piece, each
     * asking the mask first, as M7's terrain-change queue will) change none of them.
     */
    @GameTest(template = "gametest/setpiece_pad", batch = "m1", timeoutTicks = 400)
    public static void worldgen_setpiece_masked(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PlacedMask mask = PlacedMask.get(level);
        BlockPos corner = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos far = corner.offset(31, 19, 31);
        RandomSource rng = RandomSource.create(1979L);
        int templates = 0, opsApplied = 0;
        for (SetPieceDef d : SetPieceDef.loadAll()) {
            for (SetPieceDef.Variant v : d.variants()) {
                clear(level, corner, far);
                mask.clear(corner, far);
                Optional<StructureTemplate> t = SetPieceTemplates.load(level, v.template());
                helper.assertTrue(t.isPresent(), "missing template " + v.template());
                Rotation rot = Rotation.values()[templates % 4];
                BlockPos at = corner.offset(2, 0, 2);
                SetPieceTemplates.placeWithin(level, t.get(), at, rot);
                templates++;

                // Every non-air block in the area was placed by the template and is masked.
                List<BlockPos> placed = new ArrayList<>();
                List<BlockState> states = new ArrayList<>();
                for (BlockPos p : BlockPos.betweenClosed(corner, far)) {
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) {
                        helper.assertFalse(mask.isPlaced(p), v.template() + ": masked air at " + p.toShortString());
                        continue;
                    }
                    helper.assertTrue(mask.isPlaced(p), v.template() + ": " + s + " at " + p.toShortString() + " has no placed_mask bit");
                    placed.add(p.immutable());
                    states.add(s);
                }
                helper.assertTrue(!placed.isEmpty(), v.template() + " placed nothing");

                // Ten years of terrain operations around and through the piece.
                for (int year = 0; year < 10; year++) {
                    for (int op = 0; op < 600; op++) {
                        BlockPos p = corner.offset(rng.nextInt(32), rng.nextInt(16), rng.nextInt(32));
                        if (!mask.mayChange(p)) continue;
                        // Like the terrain-change queue, an op rewrites its own block only (no neighbour shape updates).
                        level.setBlock(p, rng.nextBoolean() ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        opsApplied++;
                    }
                }
                for (int i = 0; i < placed.size(); i++) {
                    BlockState now = level.getBlockState(placed.get(i));
                    helper.assertTrue(now == states.get(i), v.template() + ": terrain ops changed " + placed.get(i).toShortString()
                            + " from " + states.get(i) + " to " + now);
                }
            }
        }
        clear(level, corner, far);
        mask.clear(corner, far);
        helper.assertTrue(templates >= 22, "only " + templates + " templates tested");
        helper.assertTrue(opsApplied > 0, "no terrain operation ran");
        helper.succeed();
    }

    private static void clear(ServerLevel level, BlockPos a, BlockPos b) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
