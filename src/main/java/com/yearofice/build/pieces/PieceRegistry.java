package com.yearofice.build.pieces;

import com.yearofice.core.YearOfIce;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The building pieces (Structures and buildables › Building pieces): wall courses, roofs, floors, path, doors, windows. */
public final class PieceRegistry {
    private PieceRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final TagKey<Block> WALL = tag("wall"), ROOF = tag("roof"), FLOOR = tag("floor"), DOOR = tag("door");

    private static TagKey<Block> tag(String n) { return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, n)); }

    private static BlockBehaviour.Properties props(MapColor c, float strength, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(strength, 3f).sound(s);
    }

    private static <B extends Block> DeferredBlock<B> piece(String id, Supplier<B> make) {
        DeferredBlock<B> b = BLOCKS.register(id, make);
        ITEMS.register(id, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public static final DeferredBlock<Block> WALL_LOG = piece("wall_log", () -> new Block(props(MapColor.WOOD, 2f, SoundType.WOOD)));
    public static final DeferredBlock<Block> WALL_PLANK = piece("wall_plank", () -> new Block(props(MapColor.WOOD, 1.5f, SoundType.WOOD)));
    public static final DeferredBlock<Block> WALL_SOD = piece("wall_sod", () -> new Block(props(MapColor.GRASS, 0.8f, SoundType.GRASS)));
    public static final DeferredBlock<Block> WALL_STONE = piece("wall_stone", () -> new Block(props(MapColor.STONE, 2.5f, SoundType.STONE)));
    public static final DeferredBlock<Block> ROOF_POLE_SOD = piece("roof_pole_sod", () -> new Block(props(MapColor.DIRT, 1.2f, SoundType.GRASS)));
    public static final DeferredBlock<Block> ROOF_PLANK = piece("roof_plank", () -> new Block(props(MapColor.WOOD, 1.2f, SoundType.WOOD)));
    public static final DeferredBlock<Block> ROOF_SHINGLE = piece("roof_shingle", () -> new Block(props(MapColor.WOOD, 1.0f, SoundType.WOOD)));
    public static final DeferredBlock<Block> SLAB_FLOOR = piece("slab_floor", () -> new Block(props(MapColor.WOOD, 1.2f, SoundType.WOOD)));
    public static final DeferredBlock<Block> LOG_DECK = piece("log_deck", () -> new Block(props(MapColor.WOOD, 2f, SoundType.WOOD)));
    public static final DeferredBlock<Block> AWNING = piece("awning", () -> new Block(props(MapColor.COLOR_BROWN, 0.6f, SoundType.WOOL).noOcclusion()));
    public static final DeferredBlock<Block> DRESSED_STONE = piece("dressed_stone", () -> new Block(props(MapColor.STONE, 3f, SoundType.STONE)));
    public static final DeferredBlock<PathBlock> PATH = piece("path", () -> new PathBlock(props(MapColor.DIRT, 0.4f, SoundType.GRAVEL).noOcclusion()));
    public static final DeferredBlock<DoorFrameBlock> DOOR_FRAME = piece("door_frame", () -> new DoorFrameBlock(props(MapColor.WOOD, 1.5f, SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<Block> WINDOW_FRAME = piece("window_frame", () -> new Block(props(MapColor.WOOD, 1.0f, SoundType.GLASS).noOcclusion()));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
