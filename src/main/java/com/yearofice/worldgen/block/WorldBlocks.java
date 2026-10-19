package com.yearofice.worldgen.block;

import com.yearofice.core.YearOfIce;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The M2 world blocks (Minecraft development › Registry table: terrain, water, ice and snow; plants, trees and
 * fungi). None has a block item: world blocks drop what their loot table says, and the loot tables and their
 * items (angular stone, bark, rounds) land with the systems that make them. No block ticks at random
 * ({@code world/no_random_ticks}); none falls under gravity.
 *
 * <p>Ground: {@code deep_ground} and {@code deep_rock} are unbreakable (Explore, don't dig); {@code face_rock}
 * and the nodes in it break with a pick; soils and surfaces with a shovel.
 */
public final class WorldBlocks {
    private WorldBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    /** Every block here by its path, in registration order. */
    public static Map<String, Block> byName() {
        Map<String, Block> out = new LinkedHashMap<>();
        for (var e : BLOCKS.getEntries()) out.put(e.getId().getPath(), e.get());
        return out;
    }

    private static BlockBehaviour.Properties deep(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(-1.0F, 3600000.0F).noLootTable().isValidSpawn((s, l, p, e) -> false)
                .pushReaction(PushReaction.BLOCK).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties ground(MapColor c, SoundType s, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(hardness).sound(s);
    }

    private static BlockBehaviour.Properties rock(MapColor c, float hardness) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(hardness, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    // Deep ground and rock.
    public static final DeferredBlock<Block> DEEP_GROUND = BLOCKS.registerBlock("deep_ground", Block::new, deep(MapColor.COLOR_GRAY));
    public static final DeferredBlock<Block> DEEP_ROCK = BLOCKS.registerBlock("deep_rock", Block::new, deep(MapColor.DEEPSLATE));
    public static final DeferredBlock<Block> FACE_ROCK = BLOCKS.registerBlock("face_rock", Block::new, rock(MapColor.STONE, 2.5F));
    public static final DeferredBlock<NodeBlock.Iron> IRON_OUTCROP = BLOCKS.registerBlock("iron_outcrop", NodeBlock.Iron::new,
            rock(MapColor.TERRACOTTA_BROWN, 3.0F));
    public static final DeferredBlock<NodeBlock.Coal> COAL_SEAM = BLOCKS.registerBlock("coal_seam", NodeBlock.Coal::new,
            rock(MapColor.COLOR_BLACK, 2.0F));

    // Soils (two blocks under the surface) and surfaces.
    public static final DeferredBlock<Block> SILT = BLOCKS.registerBlock("silt", Block::new, ground(MapColor.DIRT, SoundType.ROOTED_DIRT, 0.5F));
    public static final DeferredBlock<Block> LOESS = BLOCKS.registerBlock("loess", Block::new, ground(MapColor.SAND, SoundType.ROOTED_DIRT, 0.5F));
    public static final DeferredBlock<Block> TILL = BLOCKS.registerBlock("till", Block::new, ground(MapColor.TERRACOTTA_LIGHT_GRAY, SoundType.GRAVEL, 0.8F));
    public static final DeferredBlock<Block> RIVER_SAND = BLOCKS.registerBlock("river_sand", Block::new, ground(MapColor.SAND, SoundType.SAND, 0.5F));
    public static final DeferredBlock<Block> GRAVEL = BLOCKS.registerBlock("gravel", Block::new, ground(MapColor.STONE, SoundType.GRAVEL, 0.6F));
    public static final DeferredBlock<Block> PEAT = BLOCKS.registerBlock("peat", Block::new, ground(MapColor.COLOR_BROWN, SoundType.MUD, 0.5F));
    public static final DeferredBlock<Block> CLAY_BANK = BLOCKS.registerBlock("clay_bank", Block::new, ground(MapColor.CLAY, SoundType.MUD, 0.6F));
    public static final DeferredBlock<Block> FROZEN_SOIL = BLOCKS.registerBlock("frozen_soil", Block::new,
            ground(MapColor.TERRACOTTA_CYAN, SoundType.STONE, 2.0F).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> FOREST_DUFF = BLOCKS.registerBlock("forest_duff", Block::new, ground(MapColor.PODZOL, SoundType.ROOTED_DIRT, 0.5F));
    public static final DeferredBlock<Block> MOSS_MAT = BLOCKS.registerBlock("moss_mat", Block::new, ground(MapColor.COLOR_GREEN, SoundType.MOSS, 0.4F));
    public static final DeferredBlock<Block> GRASS_TURF = BLOCKS.registerBlock("grass_turf", Block::new, ground(MapColor.GRASS, SoundType.GRASS, 0.5F));
    public static final DeferredBlock<Block> TUNDRA_TURF = BLOCKS.registerBlock("tundra_turf", Block::new, ground(MapColor.TERRACOTTA_GREEN, SoundType.GRASS, 0.5F));
    public static final DeferredBlock<Block> LICHEN_MAT = BLOCKS.registerBlock("lichen_mat", Block::new, ground(MapColor.TERRACOTTA_WHITE, SoundType.MOSS, 0.4F));
    public static final DeferredBlock<Block> SCREE_STONES = BLOCKS.registerBlock("scree_stones", Block::new, ground(MapColor.STONE, SoundType.GRAVEL, 0.8F));
    public static final DeferredBlock<Block> RUST_STAIN = BLOCKS.registerBlock("rust_stain", Block::new, ground(MapColor.TERRACOTTA_ORANGE, SoundType.GRAVEL, 0.6F));
    public static final DeferredBlock<RiffleCobbleBlock> RIFFLE_COBBLE = BLOCKS.registerBlock("riffle_cobble", RiffleCobbleBlock::new,
            ground(MapColor.STONE, SoundType.STONE, 1.0F).noOcclusion());

    // Trees.
    public static final DeferredBlock<YoiLogBlock> WHITE_SPRUCE_LOG = BLOCKS.registerBlock("white_spruce_log", YoiLogBlock::new, YoiLogBlock.props(MapColor.PODZOL));
    public static final DeferredBlock<YoiLeavesBlock> WHITE_SPRUCE_LEAVES = BLOCKS.registerBlock("white_spruce_leaves", YoiLeavesBlock::new, YoiLeavesBlock.props());
    public static final DeferredBlock<YoiLogBlock> BLACK_SPRUCE_LOG = BLOCKS.registerBlock("black_spruce_log", YoiLogBlock::new, YoiLogBlock.props(MapColor.TERRACOTTA_BLACK));
    public static final DeferredBlock<YoiLeavesBlock> BLACK_SPRUCE_LEAVES = BLOCKS.registerBlock("black_spruce_leaves", YoiLeavesBlock::new, YoiLeavesBlock.props());
    public static final DeferredBlock<YoiLogBlock> BIRCH_LOG = BLOCKS.registerBlock("birch_log", YoiLogBlock::new, YoiLogBlock.props(MapColor.SAND));
    public static final DeferredBlock<YoiLeavesBlock> BIRCH_LEAVES = BLOCKS.registerBlock("birch_leaves", YoiLeavesBlock::new, YoiLeavesBlock.props());
    public static final DeferredBlock<ShrubStemBlock> SHRUB_STEM = BLOCKS.registerBlock("shrub_stem", ShrubStemBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(0.8F).sound(SoundType.BAMBOO).noOcclusion());
    public static final DeferredBlock<BrushBlock> WILLOW_BRUSH = BLOCKS.registerBlock("willow_brush", BrushBlock::new, BrushBlock.props());
    public static final DeferredBlock<StumpBlock> STUMP = BLOCKS.registerBlock("stump", StumpBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<FallenLogBlock> FALLEN_LOG = BLOCKS.registerBlock("fallen_log", FallenLogBlock::new, YoiLogBlock.props(MapColor.PODZOL));

    // Snow (laid by the snow pass from M7; registered now so saves and the layers agree on it).
    public static final DeferredBlock<SnowCoverBlock> SNOW_COVER = BLOCKS.registerBlock("snow_cover", SnowCoverBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1F).sound(SoundType.SNOW)
                    .isViewBlocking((s, l, p) -> s.getValue(SnowCoverBlock.LAYERS) >= 8));

    public static void register(IEventBus modBus) { BLOCKS.register(modBus); }
}
