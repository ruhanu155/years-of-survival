package com.yearofice.ecosystem.raid;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks of the raid systems: the {@code theft_cache} a fox hides a stolen hide or strip in. */
public final class RaidRegistry {
    private RaidRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredBlock<TheftCacheBlock> THEFT_CACHE = BLOCKS.registerBlock("theft_cache", TheftCacheBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.1f).sound(SoundType.MOSS).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TheftCacheBlockEntity>> THEFT_CACHE_ENTITY =
            BLOCK_ENTITIES.register("theft_cache", () -> BlockEntityType.Builder.of(TheftCacheBlockEntity::new, THEFT_CACHE.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
