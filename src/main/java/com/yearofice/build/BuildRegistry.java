package com.yearofice.build;

import com.yearofice.build.camp.BoughBedBlock;
import com.yearofice.build.camp.SpruceBoughsItem;
import com.yearofice.core.YearOfIce;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The building department's registry entries for the M2.5 slice (RECONCILE 181: the bough bed pulled forward from M3).
 * Every id here is in {@code studio/inventory.json}. The bed is a placed build with no item of its own.
 */
public final class BuildRegistry {
    private BuildRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);

    public static final DeferredBlock<BoughBedBlock> BOUGH_BED = BLOCKS.registerBlock("bough_bed", BoughBedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.4f).sound(SoundType.AZALEA_LEAVES)
                    .noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<SpruceBoughsItem> SPRUCE_BOUGHS = ITEMS.registerItem("spruce_boughs", SpruceBoughsItem::new,
            new Item.Properties().stacksTo(64));

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
