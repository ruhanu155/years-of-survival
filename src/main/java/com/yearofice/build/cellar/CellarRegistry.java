package com.yearofice.build.cellar;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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

/** Root cellar and permafrost cellar hatches (M8 lane B). One block entity type serves both; its id is the root cellar's. */
public final class CellarRegistry {
    private CellarRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    private static BlockBehaviour.Properties hatch() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).ignitedByLava();
    }

    public static final DeferredBlock<CellarBlock> ROOT_CELLAR = BLOCKS.registerBlock("root_cellar", CellarBlock::new, hatch());
    public static final DeferredBlock<CellarBlock> PERMAFROST_CELLAR = BLOCKS.registerBlock("permafrost_cellar", CellarBlock::new, hatch());
    public static final DeferredItem<BlockItem> ROOT_CELLAR_ITEM = ITEMS.registerSimpleBlockItem("root_cellar", ROOT_CELLAR, new Item.Properties());
    public static final DeferredItem<BlockItem> PERMAFROST_CELLAR_ITEM = ITEMS.registerSimpleBlockItem("permafrost_cellar", PERMAFROST_CELLAR, new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CellarBlockEntity>> CELLAR_ENTITY =
            BLOCK_ENTITIES.register("root_cellar", () -> BlockEntityType.Builder.of(CellarBlockEntity::new,
                    ROOT_CELLAR.get(), PERMAFROST_CELLAR.get()).build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
