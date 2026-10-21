package com.yearofice.ecosystem.companion;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.herd.CaribouSnareBlock;
import com.yearofice.ecosystem.herd.CaribouSnareBlockEntity;
import com.yearofice.ecosystem.herd.LeadFenceBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** M9 lane A ids: the sled, the dog sled item, the lead fence and the caribou snare. */
public final class CompanionRegistry {
    private CompanionRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SledEntity>> SLED = ENTITIES.register("sled",
            () -> EntityType.Builder.<SledEntity>of(SledEntity::new, MobCategory.MISC).sized(1.0f, 0.5f).clientTrackingRange(10).updateInterval(2).build("yearofice:sled"));

    public static final DeferredItem<DogSledItem> DOG_SLED = ITEMS.registerItem("dog_sled", DogSledItem::new, new Item.Properties().stacksTo(1));

    /** Four metres of poles and brush: the fence block, one segment per block of wing. */
    public static final DeferredBlock<LeadFenceBlock> LEAD_FENCE = BLOCKS.registerBlock("lead_fence", LeadFenceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<CaribouSnareBlock> CARIBOU_SNARE = BLOCKS.registerBlock("caribou_snare", CaribouSnareBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.3f).sound(SoundType.CHAIN).noOcclusion().noCollission()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> LEAD_FENCE_ITEM = ITEMS.registerSimpleBlockItem("lead_fence", LEAD_FENCE);
    public static final DeferredItem<BlockItem> CARIBOU_SNARE_ITEM = ITEMS.registerSimpleBlockItem("caribou_snare", CARIBOU_SNARE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CaribouSnareBlockEntity>> CARIBOU_SNARE_ENTITY =
            BLOCK_ENTITIES.register("caribou_snare", () -> BlockEntityType.Builder.of(CaribouSnareBlockEntity::new, CARIBOU_SNARE.get()).build(null));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
