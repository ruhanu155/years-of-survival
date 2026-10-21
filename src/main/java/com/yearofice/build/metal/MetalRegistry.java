package com.yearofice.build.metal;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * M9 lane B's ids: the iron tier's ore, bloom, bar and tools, the bloomery, forge and canoe bed, the sluice box and the
 * fish wheel. Every id is in {@code studio/inventory.json}.
 */
public final class MetalRegistry {
    private MetalRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    // ---- ore, charcoal, bloom and bar ------------------------------------------------------------------------
    /** 3 kg nodule pried from a rock face. */
    public static final DeferredItem<Item> IRON_NODULE = ITEMS.registerSimpleItem("iron_nodule", new Item.Properties().stacksTo(8));
    public static final DeferredItem<Item> BOG_IRON = ITEMS.registerSimpleItem("bog_iron", new Item.Properties().stacksTo(8));
    public static final DeferredItem<Item> SCRAP_IRON = ITEMS.registerSimpleItem("scrap_iron", new Item.Properties().stacksTo(16));
    /** 1 kg of charcoal. */
    public static final DeferredItem<Item> CHARCOAL = ITEMS.registerSimpleItem("charcoal", new Item.Properties().stacksTo(64));
    /** A 3 kg bloom, to be consolidated. */
    public static final DeferredItem<Item> IRON_BLOOM = ITEMS.registerSimpleItem("iron_bloom", new Item.Properties().stacksTo(4));
    /** 0.5 kg of bar iron. */
    public static final DeferredItem<Item> IRON_BAR = ITEMS.registerSimpleItem("iron_bar", new Item.Properties().stacksTo(32));

    // ---- iron goods --------------------------------------------------------------------------------------------
    public static final int IRON_KNIFE_DURABILITY = 8000, IRON_SAW_DURABILITY = 6000, IRON_CHISEL_DURABILITY = 5000;
    public static final DeferredItem<Item> IRON_KNIFE = ITEMS.registerItem("iron_knife", Item::new, new Item.Properties().durability(IRON_KNIFE_DURABILITY));
    public static final DeferredItem<Item> IRON_SAW = ITEMS.registerItem("iron_saw", Item::new, new Item.Properties().durability(IRON_SAW_DURABILITY));
    public static final DeferredItem<Item> IRON_CHISEL = ITEMS.registerItem("iron_chisel", Item::new, new Item.Properties().durability(IRON_CHISEL_DURABILITY));
    public static final DeferredItem<Item> IRON_AXE_HEAD = ITEMS.registerSimpleItem("iron_axe_head", new Item.Properties().stacksTo(8));
    public static final DeferredItem<Item> SQUARE_NAIL = ITEMS.registerSimpleItem("square_nail", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> IRON_POT = ITEMS.registerSimpleItem("iron_pot", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> TRAP_JAWS = ITEMS.registerSimpleItem("trap_jaws", new Item.Properties().stacksTo(8));
    public static final DeferredItem<Item> IRON_FISH_HOOK = ITEMS.registerSimpleItem("iron_fish_hook", new Item.Properties().stacksTo(64));

    // ---- stations and water works ----------------------------------------------------------------------------
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F).sound(SoundType.STONE).noOcclusion(); }

    public static final DeferredBlock<BloomeryBlock> BLOOMERY = BLOCKS.registerBlock("bloomery", BloomeryBlock::new, stone());
    public static final DeferredItem<BlockItem> BLOOMERY_ITEM = ITEMS.registerSimpleBlockItem("bloomery", BLOOMERY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloomeryBlockEntity>> BLOOMERY_ENTITY =
            BLOCK_ENTITIES.register("bloomery", () -> BlockEntityType.Builder.of(BloomeryBlockEntity::new, BLOOMERY.get()).build(null));

    public static final DeferredBlock<ForgeBlock> FORGE = BLOCKS.registerBlock("forge", ForgeBlock::new, stone());
    public static final DeferredItem<BlockItem> FORGE_ITEM = ITEMS.registerSimpleBlockItem("forge", FORGE);

    public static final DeferredBlock<Block> CANOE_BED = BLOCKS.registerSimpleBlock("canoe_bed",
            BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(1.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> CANOE_BED_ITEM = ITEMS.registerSimpleBlockItem("canoe_bed", CANOE_BED);

    public static final DeferredBlock<SluiceBoxBlock> SLUICE_BOX = BLOCKS.registerBlock("sluice_box", SluiceBoxBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> SLUICE_BOX_ITEM = ITEMS.registerSimpleBlockItem("sluice_box", SLUICE_BOX);

    public static final DeferredHolder<SoundEvent, SoundEvent> SLUICE_WASH = SOUNDS.register("work.sluice_wash",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "work.sluice_wash")));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
    }
}
