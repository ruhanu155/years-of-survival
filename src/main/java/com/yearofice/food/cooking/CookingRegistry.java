package com.yearofice.food.cooking;

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
 * Cooking's registry entries for the M2.5 slice (Ready for development › M2.5: "roast and boil on the fire ring"):
 * {@code yearofice:spit}, the forked sticks and crossbar that stand over a fire ring when food is put on it, and its
 * block entity. It has no item: right mouse with food on a fire ring sets one up (Blueprint › Cooking: "spit or stick
 * roast, spit over coals"). Every id here is in {@code studio/inventory.json}.
 */
public final class CookingRegistry {
    private CookingRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredBlock<SpitBlock> SPIT = BLOCKS.registerBlock("spit", SpitBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.1f).sound(SoundType.WOOD).noOcclusion().noCollission()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpitBlockEntity>> SPIT_ENTITY =
            BLOCK_ENTITIES.register("spit", () -> BlockEntityType.Builder.of(SpitBlockEntity::new, SPIT.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
