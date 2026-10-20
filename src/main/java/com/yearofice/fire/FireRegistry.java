package com.yearofice.fire;

import com.mojang.serialization.Codec;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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

/**
 * The fire team's registry entries (slice scope, M2.5 with the bow drill pulled forward from M3). Every id here is
 * in {@code studio/inventory.json}.
 *
 * <p>{@code yearofice:moisture} is registered here because fire is its first reader (fuel, tinder, kindling and the
 * bow drill's board). It is studio-owned shared state in the master inventory; when {@code item.component.YoiComponents}
 * lands it moves there under the same id.
 */
public final class FireRegistry {
    private FireRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    /** Moisture 0–100%: wood's seasoning, and how wet tinder, kindling and matches are (dryness = 100 − moisture). */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> MOISTURE =
            COMPONENTS.registerComponentType("moisture", b -> b.persistent(Codec.intRange(0, 100)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredBlock<FireRingBlock> FIRE_RING = BLOCKS.registerBlock("fire_ring", FireRingBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6f).sound(SoundType.STONE).noOcclusion()
                    .pushReaction(PushReaction.DESTROY).lightLevel(s -> s.getValue(FireRingBlock.BURN).light));

    /** Fire jumped to a burnable piece (spread); not obtainable. */
    public static final DeferredBlock<FlameBlock> FLAME = BLOCKS.registerBlock("flame", FlameBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).noCollission().noOcclusion().instabreak().lightLevel(s -> 15)
                    .pushReaction(PushReaction.DESTROY).noLootTable());

    public static final DeferredItem<BlockItem> FIRE_RING_ITEM = ITEMS.registerSimpleBlockItem("fire_ring", FIRE_RING, new Item.Properties());
    public static final DeferredItem<Item> TINDER_BUNDLE = ITEMS.registerItem("tinder_bundle", Item::new, new Item.Properties().stacksTo(20));
    public static final DeferredItem<Item> KINDLING = ITEMS.registerItem("kindling", Item::new, new Item.Properties().stacksTo(20));
    /** A fireboard holds 8 notches: 8 attempts, then the worn part is replaced (Crafting › Tool durability). */
    public static final DeferredItem<Item> BOW_DRILL = ITEMS.registerItem("bow_drill", Item::new, new Item.Properties().durability(8));
    /** The ferro rod in its grip with its striker: lights a laid fire, wears a little each strike (Crafting › Tool durability). */
    public static final DeferredItem<Item> FIRE_STARTER = ITEMS.registerItem("fire_starter", Item::new, new Item.Properties().durability(200));
    public static final DeferredItem<Item> MATCH = ITEMS.registerItem("match", Item::new, new Item.Properties().stacksTo(50));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FireRingBlockEntity>> FIRE_RING_ENTITY =
            BLOCK_ENTITIES.register("fire_ring", () -> BlockEntityType.Builder.of(FireRingBlockEntity::new, FIRE_RING.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlameBlockEntity>> FLAME_ENTITY =
            BLOCK_ENTITIES.register("flame", () -> BlockEntityType.Builder.of(FlameBlockEntity::new, FLAME.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> CRACKLE = SOUNDS.register("fire.crackle",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fire.crackle")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BOW_DRILL_STROKE = SOUNDS.register("fire.bow_drill",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fire.bow_drill")));

    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
    }
}
