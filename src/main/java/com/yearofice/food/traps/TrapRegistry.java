package com.yearofice.food.traps;

import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
 * The trail snare (Blueprint › Traps): {@code yearofice:snare_wire}, which sets {@code yearofice:snare} on a trail
 * cell, and the snare's block entity. The slice's snare is the wire one (it kills at once; cordage, the spring pole
 * and bait land in M5). Every id here is in {@code studio/inventory.json}.
 */
public final class TrapRegistry {
    private TrapRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, YearOfIce.MOD_ID);

    public static final DeferredBlock<SnareBlock> SNARE = BLOCKS.registerBlock("snare", SnareBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.2f).sound(SoundType.CHAIN).noOcclusion().noCollission()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<SnareWireItem> SNARE_WIRE = ITEMS.registerItem("snare_wire",
            p -> new SnareWireItem(SNARE.get(), p), new Item.Properties().stacksTo(16));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SnareBlockEntity>> SNARE_ENTITY =
            BLOCK_ENTITIES.register("snare", () -> BlockEntityType.Builder.of(SnareBlockEntity::new, SNARE.get()).build(null));

    /** "Snare on a hare: thrash and a short squeal, 40 m" (Every concept from the video › Trap sounds). */
    public static final DeferredHolder<SoundEvent, SoundEvent> SNARE_HARE = SOUNDS.register("trap.snare_hare",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "trap.snare_hare")));

    // ---- M5 lane D2: the deadfalls, the snare funnel and their sounds ------------------------------------------------------

    private static BlockBehaviour.Properties groundSet() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.4f).sound(SoundType.WOOD).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredBlock<DeadfallBlock> DEADFALL = BLOCKS.registerBlock("deadfall", p -> new DeadfallBlock(p, false), groundSet());
    public static final DeferredBlock<DeadfallBlock> MOUSE_DEADFALL = BLOCKS.registerBlock("mouse_deadfall", p -> new DeadfallBlock(p, true), groundSet());
    public static final DeferredBlock<SnareFunnelBlock> SNARE_FUNNEL = BLOCKS.registerBlock("snare_funnel", SnareFunnelBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).sound(SoundType.GRASS).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> DEADFALL_ITEM = ITEMS.registerSimpleBlockItem("deadfall", DEADFALL);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOUSE_DEADFALL_ITEM = ITEMS.registerSimpleBlockItem("mouse_deadfall", MOUSE_DEADFALL);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SNARE_FUNNEL_ITEM = ITEMS.registerSimpleBlockItem("snare_funnel", SNARE_FUNNEL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DeadfallBlockEntity>> DEADFALL_ENTITY =
            BLOCK_ENTITIES.register("deadfall", () -> BlockEntityType.Builder.of(DeadfallBlockEntity::new, DEADFALL.get(), MOUSE_DEADFALL.get()).build(null));

    /** "A thud" (Every concept from the video › Trap sounds): vanilla stone re-pitched, 40 m. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DEADFALL_SOUND = SOUNDS.register("trap.deadfall",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "trap.deadfall")));

    /** Logs, rocks, brush and packed snow that make a funnel wall beside a trail snare ({@code funnel_materials}). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> FUNNEL_MATERIALS = net.minecraft.tags.TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "funnel_materials"));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
    }
}
