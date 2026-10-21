package com.yearofice.build.store;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Storage blocks (M8 lane B): crate, barrel, shelf, hanging bar, raised cache legs and the placed lantern. */
public final class StoreRegistry {
    private StoreRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava();
    }

    public static final DeferredBlock<StoreBlock> CRATE = BLOCKS.registerBlock("crate", StoreBlock::new, wood());
    public static final DeferredBlock<StoreBlock> BARREL = BLOCKS.registerBlock("barrel", StoreBlock::new, wood());
    public static final DeferredBlock<StoreBlock> SHELF = BLOCKS.registerBlock("shelf", StoreBlock::new, wood());
    public static final DeferredBlock<StoreBlock> HANGING_BAR = BLOCKS.registerBlock("hanging_bar", StoreBlock::new, wood());
    /** The platform of a raised cache; the poles under it set its height and dressing. */
    public static final DeferredBlock<StoreBlock> CACHE_LEGS = BLOCKS.registerBlock("cache_legs", StoreBlock::new, wood());
    public static final DeferredBlock<CachePoleBlock> CACHE_POLE = BLOCKS.registerBlock("cache_pole", CachePoleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    /** Light level 13 (Placed lanterns); the lard it burns is the lantern item's job. */
    public static final DeferredBlock<Block> PLACED_LANTERN = BLOCKS.registerBlock("placed_lantern", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.5f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(s -> 13).pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<SmokehouseBlock> SMOKEHOUSE = BLOCKS.registerBlock("smokehouse", SmokehouseBlock::new, wood());
    public static final DeferredBlock<FirewoodShedBlock> FIREWOOD_SHED = BLOCKS.registerBlock("firewood_shed", FirewoodShedBlock::new, wood());

    public static final DeferredItem<BlockItem> SMOKEHOUSE_ITEM = ITEMS.registerSimpleBlockItem("smokehouse", SMOKEHOUSE, new Item.Properties());
    public static final DeferredItem<BlockItem> FIREWOOD_SHED_ITEM = ITEMS.registerSimpleBlockItem("firewood_shed", FIREWOOD_SHED, new Item.Properties());
    public static final DeferredItem<BlockItem> CRATE_ITEM = ITEMS.registerSimpleBlockItem("crate", CRATE, new Item.Properties());
    public static final DeferredItem<BlockItem> BARREL_ITEM = ITEMS.registerSimpleBlockItem("barrel", BARREL, new Item.Properties());
    public static final DeferredItem<BlockItem> SHELF_ITEM = ITEMS.registerSimpleBlockItem("shelf", SHELF, new Item.Properties());
    public static final DeferredItem<BlockItem> HANGING_BAR_ITEM = ITEMS.registerSimpleBlockItem("hanging_bar", HANGING_BAR, new Item.Properties());
    public static final DeferredItem<BlockItem> CACHE_LEGS_ITEM = ITEMS.registerSimpleBlockItem("cache_legs", CACHE_LEGS, new Item.Properties());
    public static final DeferredItem<BlockItem> CACHE_POLE_ITEM = ITEMS.registerSimpleBlockItem("cache_pole", CACHE_POLE, new Item.Properties());
    public static final DeferredItem<BlockItem> PLACED_LANTERN_ITEM = ITEMS.registerSimpleBlockItem("placed_lantern", PLACED_LANTERN, new Item.Properties());

    /** One block entity type serves all five containers; its id is the crate's. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoreContainerBlockEntity>> CONTAINER_ENTITY =
            BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder.of(StoreContainerBlockEntity::new,
                    CRATE.get(), BARREL.get(), SHELF.get(), HANGING_BAR.get(), CACHE_LEGS.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokehouseBlockEntity>> SMOKEHOUSE_ENTITY =
            BLOCK_ENTITIES.register("smokehouse", () -> BlockEntityType.Builder.of(SmokehouseBlockEntity::new, SMOKEHOUSE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FirewoodShedBlockEntity>> SHED_ENTITY =
            BLOCK_ENTITIES.register("firewood_shed", () -> BlockEntityType.Builder.of(FirewoodShedBlockEntity::new, FIREWOOD_SHED.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
