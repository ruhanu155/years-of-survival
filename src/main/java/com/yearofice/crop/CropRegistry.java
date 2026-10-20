package com.yearofice.crop;

import com.yearofice.core.YearOfIce;
import com.yearofice.food.YoiFoodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The garden's blocks and items: bed, five crops, tools, seeds and harvests. */
public final class CropRegistry {
    private CropRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);
    /** {@code yearofice:seed_lot}: the game year a seed or grain lot was harvested; older lots germinate worse. */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> SEED_LOT =
            COMPONENTS.registerComponentType("seed_lot", b -> b.persistent(com.mojang.serialization.Codec.intRange(0, 100000))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredBlock<GardenBedBlock> GARDEN_BED = BLOCKS.registerBlock("garden_bed", GardenBedBlock::new, GardenBedBlock.props());
    public static final DeferredBlock<CropBlock> POTATO_CROP = BLOCKS.registerBlock("potato_crop", CropBlock::new, CropBlock.props());
    public static final DeferredBlock<CropBlock> BARLEY_CROP = BLOCKS.registerBlock("barley_crop", CropBlock::new, CropBlock.props());
    public static final DeferredBlock<CropBlock> WHEAT_CROP = BLOCKS.registerBlock("wheat_crop", CropBlock::new, CropBlock.props());
    public static final DeferredBlock<CropBlock> TURNIP_CROP = BLOCKS.registerBlock("turnip_crop", CropBlock::new, CropBlock.props());
    public static final DeferredBlock<CropBlock> CABBAGE_CROP = BLOCKS.registerBlock("cabbage_crop", CropBlock::new, CropBlock.props());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropBlockEntity>> CROP_BE = BE_TYPES.register("potato_crop",
            () -> BlockEntityType.Builder.of(CropBlockEntity::new, POTATO_CROP.get(), BARLEY_CROP.get(), WHEAT_CROP.get(),
                    TURNIP_CROP.get(), CABBAGE_CROP.get()).build(null));

    public static final DeferredItem<TillItem> HOE = ITEMS.registerItem("hoe", p -> new TillItem(p, 3), new Item.Properties().stacksTo(1).durability(120));
    public static final DeferredItem<TillItem> DIGGING_STICK = ITEMS.registerItem("digging_stick", p -> new TillItem(p, 6), new Item.Properties().stacksTo(1).durability(60));
    public static final DeferredItem<Item> SHOVEL = ITEMS.registerItem("shovel", Item::new, new Item.Properties().stacksTo(1).durability(200));
    public static final DeferredItem<Item> HAND_QUERN = ITEMS.registerItem("hand_quern", Item::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<SeedItem> SEED_POTATO = ITEMS.registerItem("seed_potato", p -> new SeedItem(p, "yearofice:potato"), new Item.Properties().stacksTo(64));
    public static final DeferredItem<SeedItem> BARLEY_SEED = ITEMS.registerItem("barley_seed", p -> new SeedItem(p, "yearofice:barley"), new Item.Properties().stacksTo(64));
    public static final DeferredItem<SeedItem> WHEAT_SEED = ITEMS.registerItem("wheat_seed", p -> new SeedItem(p, "yearofice:wheat"), new Item.Properties().stacksTo(64));
    public static final DeferredItem<SeedItem> TURNIP_SEED = ITEMS.registerItem("turnip_seed", p -> new SeedItem(p, "yearofice:turnip"), new Item.Properties().stacksTo(64));
    public static final DeferredItem<SeedItem> CABBAGE_SEED = ITEMS.registerItem("cabbage_seed", p -> new SeedItem(p, "yearofice:cabbage"), new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> POTATO = ITEMS.registerItem("potato", YoiFoodItem::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> BARLEY = ITEMS.registerItem("barley", YoiFoodItem::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> WHEAT = ITEMS.registerItem("wheat", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> TURNIP = ITEMS.registerItem("turnip", YoiFoodItem::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CABBAGE = ITEMS.registerItem("cabbage", YoiFoodItem::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> GRAIN_SHEAF = ITEMS.registerItem("grain_sheaf", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> FLAIL = ITEMS.registerItem("flail", Item::new, new Item.Properties().stacksTo(1).durability(150));
    public static final DeferredItem<Item> BARLEY_FLOUR = ITEMS.registerItem("barley_flour", YoiFoodItem::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> STRAW = ITEMS.registerItem("straw", Item::new, new Item.Properties().stacksTo(64));

    public static Block cropBlock(String crop) {
        return switch (crop) {
            case "yearofice:potato" -> POTATO_CROP.get();
            case "yearofice:barley" -> BARLEY_CROP.get();
            case "yearofice:wheat" -> WHEAT_CROP.get();
            case "yearofice:turnip" -> TURNIP_CROP.get();
            default -> CABBAGE_CROP.get();
        };
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BE_TYPES.register(modBus);
        COMPONENTS.register(modBus);
    }
}
