package com.yearofice.build.wood;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.fire.Seasoning;
import com.yearofice.sim.wood.WoodKind;
import com.yearofice.world.logs.FallingTreeEntity;
import com.yearofice.world.logs.FelledLogEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
 * Building's M2 wood entries (Trees, felling and firewood): the 16 {@code <wood>_round} and {@code <wood>_split}
 * items, the crate {@code hatchet}, the {@code chopping_block}, the {@code falling_tree} and {@code felled_log}
 * entities, the {@code tree.*} sounds, the {@code seasoning} component and the {@code fell_stops} tag. Every id
 * here is in {@code studio/inventory.json}.
 */
public final class WoodRegistry {
    private WoodRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    /** {@code #yearofice:fell_stops}: placed logs, log walls and every piece; the fall search never crosses one (A1). */
    public static final TagKey<Block> FELL_STOPS = TagKey.create(Registries.BLOCK, id("fell_stops"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WoodItems.Stamp>> SEASONING =
            COMPONENTS.registerComponentType("seasoning", b -> b.persistent(WoodItems.Stamp.CODEC).networkSynchronized(WoodItems.Stamp.STREAM));

    public static final Map<WoodKind, DeferredItem<WoodPieceItem>> ROUNDS = new EnumMap<>(WoodKind.class);
    public static final Map<WoodKind, DeferredItem<WoodPieceItem>> SPLITS = new EnumMap<>(WoodKind.class);

    static {
        for (WoodKind w : WoodKind.values()) {
            ROUNDS.put(w, ITEMS.registerItem(w.roundId(), p -> new WoodPieceItem(w, Seasoning.Form.ROUND, p), new Item.Properties().stacksTo(4)));
            SPLITS.put(w, ITEMS.registerItem(w.splitId(), p -> new WoodPieceItem(w, Seasoning.Form.SPLIT, p), new Item.Properties().stacksTo(4)));
        }
    }

    /** The crate hatchet (Items › hatchet; Tools table row "Crate hatchet"), pulled forward from M3's kit for felling. */
    public static final DeferredItem<Item> HATCHET = ITEMS.registerItem("hatchet", Item::new, new Item.Properties().stacksTo(1));

    public static final DeferredBlock<ChoppingBlock> CHOPPING_BLOCK = BLOCKS.registerBlock("chopping_block", ChoppingBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> CHOPPING_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("chopping_block", CHOPPING_BLOCK, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChoppingBlockEntity>> CHOPPING_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("chopping_block", () -> BlockEntityType.Builder.of(ChoppingBlockEntity::new, CHOPPING_BLOCK.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<FallingTreeEntity>> FALLING_TREE = ENTITIES.register("falling_tree",
            () -> EntityType.Builder.<FallingTreeEntity>of(FallingTreeEntity::new, MobCategory.MISC).sized(0.8f, 1.0f).clientTrackingRange(10)
                    .updateInterval(20).build("yearofice:falling_tree"));
    public static final DeferredHolder<EntityType<?>, EntityType<FelledLogEntity>> FELLED_LOG = ENTITIES.register("felled_log",
            () -> EntityType.Builder.<FelledLogEntity>of(FelledLogEntity::new, MobCategory.MISC).sized(0.7f, 0.6f).clientTrackingRange(10)
                    .updateInterval(40).build("yearofice:felled_log"));

    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_CHOP = sound("tree.chop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_CREAK = sound("tree.creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_FALL = sound("tree.fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> TREE_SPLIT = sound("tree.split");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }

    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
    }
}
