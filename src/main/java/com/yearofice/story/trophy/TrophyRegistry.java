package com.yearofice.story.trophy;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
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

/** The trophy wall (A6): {@code trophy_mount} block, its block entity and the {@code trophy_plaque} item. */
public final class TrophyRegistry {
    private TrophyRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredBlock<TrophyMountBlock> TROPHY_MOUNT = BLOCKS.registerBlock("trophy_mount", TrophyMountBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<Item> TROPHY_PLAQUE = ITEMS.registerSimpleItem("trophy_plaque");

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrophyMountBlockEntity>> TROPHY_MOUNT_ENTITY =
            BLOCK_ENTITIES.register("trophy_mount", () -> BlockEntityType.Builder.of(TrophyMountBlockEntity::new, TROPHY_MOUNT.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
