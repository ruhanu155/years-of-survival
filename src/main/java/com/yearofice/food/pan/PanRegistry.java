package com.yearofice.food.pan;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Black sand in the pan (video 2, A10): the gold pan (the mining camp's), the crafted {@code birch_pan} at half speed,
 * the rust-stained {@code placer_gravel} and the {@code work.pan_swirl} sound. Ids are in {@code studio/inventory-rules.json}.
 */
public final class PanRegistry {
    private PanRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    public static final DeferredBlock<Block> PLACER_GRAVEL = BLOCKS.registerSimpleBlock("placer_gravel",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6f).sound(SoundType.GRAVEL));
    public static final DeferredItem<BlockItem> PLACER_GRAVEL_ITEM = ITEMS.registerSimpleBlockItem("placer_gravel", PLACER_GRAVEL);

    public static final DeferredItem<Item> GOLD_PAN = ITEMS.registerSimpleItem("gold_pan", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BIRCH_PAN = ITEMS.registerSimpleItem("birch_pan", new Item.Properties().stacksTo(1));
    /** 100 g of magnetite iron sand; 40 of them (4 kg) make one 3 kg iron nodule in the bloomery. */
    public static final DeferredItem<Item> BLACK_SAND = ITEMS.registerSimpleItem("black_sand");

    public static final DeferredHolder<SoundEvent, SoundEvent> PAN_SWIRL = SOUNDS.register("work.pan_swirl",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "work.pan_swirl")));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
    }
}
