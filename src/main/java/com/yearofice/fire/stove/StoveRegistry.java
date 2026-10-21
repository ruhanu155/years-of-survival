package com.yearofice.fire.stove;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Stoves and stovepipe (M8 lane B). One block entity type serves the three stoves; its id is the tin stove's. */
public final class StoveRegistry {
    private StoveRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    private static BlockBehaviour.Properties stove(MapColor c, SoundType s) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(2f, 6f).sound(s).noOcclusion().lightLevel(st -> st.getValue(StoveBlock.LIT) ? 9 : 0);
    }

    public static final DeferredBlock<StoveBlock> TIN_STOVE = BLOCKS.registerBlock("tin_stove", StoveBlock::new, stove(MapColor.METAL, SoundType.METAL));
    public static final DeferredBlock<StoveBlock> BARREL_STOVE = BLOCKS.registerBlock("barrel_stove", StoveBlock::new, stove(MapColor.COLOR_GRAY, SoundType.METAL));
    public static final DeferredBlock<StoveBlock> MASONRY_STOVE = BLOCKS.registerBlock("masonry_stove", StoveBlock::new, stove(MapColor.STONE, SoundType.STONE));
    public static final DeferredBlock<ChimneyPipeBlock> CHIMNEY_PIPE = BLOCKS.registerBlock("chimney_pipe", ChimneyPipeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1f).sound(SoundType.METAL).noOcclusion());

    public static final DeferredItem<BlockItem> TIN_STOVE_ITEM = ITEMS.registerSimpleBlockItem("tin_stove", TIN_STOVE, new Item.Properties());
    public static final DeferredItem<BlockItem> BARREL_STOVE_ITEM = ITEMS.registerSimpleBlockItem("barrel_stove", BARREL_STOVE, new Item.Properties());
    public static final DeferredItem<BlockItem> MASONRY_STOVE_ITEM = ITEMS.registerSimpleBlockItem("masonry_stove", MASONRY_STOVE, new Item.Properties());
    public static final DeferredItem<BlockItem> CHIMNEY_PIPE_ITEM = ITEMS.registerSimpleBlockItem("chimney_pipe", CHIMNEY_PIPE, new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoveBlockEntity>> STOVE_ENTITY =
            BLOCK_ENTITIES.register("tin_stove", () -> BlockEntityType.Builder.of(StoveBlockEntity::new,
                    TIN_STOVE.get(), BARREL_STOVE.get(), MASONRY_STOVE.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
