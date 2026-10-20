package com.yearofice.food.preserve;

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

/**
 * {@code food.preserve}: the Forge Labs racks (RECONCILE 170) and the food cache. Ids {@code drying_rack},
 * {@code smoke_rack} and {@code food_cache} are in {@code studio/inventory-rules.json}. The slice builds each rack as
 * one block; the 2x1x2 and 1x1x3 footprints are a model matter for the Look round.
 */
public final class PreserveRegistry {
    private PreserveRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.8f).sound(SoundType.WOOD).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredBlock<RackBlock> DRYING_RACK = BLOCKS.registerBlock("drying_rack",
            p -> new RackBlock(p, false), wood());
    public static final DeferredBlock<RackBlock> SMOKE_RACK = BLOCKS.registerBlock("smoke_rack",
            p -> new RackBlock(p, true), wood());
    public static final DeferredBlock<FoodCacheBlock> FOOD_CACHE = BLOCKS.registerBlock("food_cache", FoodCacheBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>> DRYING_ENTITY =
            BLOCK_ENTITIES.register("drying_rack", () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new, DRYING_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeRackBlockEntity>> SMOKE_ENTITY =
            BLOCK_ENTITIES.register("smoke_rack", () -> BlockEntityType.Builder.of(SmokeRackBlockEntity::new, SMOKE_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoodCacheBlockEntity>> CACHE_ENTITY =
            BLOCK_ENTITIES.register("food_cache", () -> BlockEntityType.Builder.of(FoodCacheBlockEntity::new, FOOD_CACHE.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
