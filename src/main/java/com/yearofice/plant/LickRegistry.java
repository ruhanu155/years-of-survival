package com.yearofice.plant;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Mineral lick salt (video 2, A4): the crust block, its two items, the sound, and the block entity that holds the grams. */
public final class LickRegistry {
    private LickRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    public static final DeferredBlock<SaltCrustBlock> SALT_CRUST = BLOCKS.registerBlock("salt_crust", SaltCrustBlock::new, SaltCrustBlock.props());
    public static final DeferredItem<Item> LICK_CRUST = ITEMS.registerItem("lick_crust", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> LICK_SALT = ITEMS.registerItem("lick_salt", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SaltCrustBlockEntity>> SALT_CRUST_BE = BE_TYPES.register("salt_crust",
            () -> BlockEntityType.Builder.of(SaltCrustBlockEntity::new, SALT_CRUST.get()).build(null));
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUST_SCRAPE = SOUNDS.register("work.crust_scrape",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "work.crust_scrape")));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BE_TYPES.register(modBus);
        SOUNDS.register(modBus);
    }
}
