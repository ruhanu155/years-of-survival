package com.yearofice.hunt.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import com.yearofice.food.YoiFoodItem;
import com.yearofice.sim.fishing.HookBait;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The rod method's registry entries for the M2.5 slice (Ready for development › M2.5: "one rod method (hook, line
 * and bobber with worms, the fight overlay) in {@code hunt.fishing}"; Waters and fish › Fishing ids, tackle and
 * keys). Every id here is in {@code studio/inventory.json}.
 *
 * <ul>
 *   <li>{@code yearofice:rod}: the willow rod and winder. In the slice its hook (steel), line (plant fiber, 20 m
 *       plus 10 m on the winder) and bobber are parts of the rod; the line's condition is the rod's vanilla
 *       durability, as Waters and fish says lines carry it. The {@code yearofice:tackle} component holds the bait.</li>
 *   <li>{@code yearofice:worm}: bait (2 bites or 30 minutes on the hook).</li>
 *   <li>{@code yearofice:arctic_grayling}: the landed fish, the item with the species id (DIRECTOR RULING 101),
 *       carrying its weight in {@code yearofice:weight_g}. The grayling mob is M5 and not registered here.</li>
 *   <li>{@code yearofice:bobber}: the cast bobber entity that replaces vanilla's fishing hook.</li>
 * </ul>
 *
 * <p>{@code yearofice:weight_g} is studio-owned shared state, registered once in {@code item.component.YoiComponents}
 * (RECONCILE 168); fishing writes it on the landed fish, with the fish's raw {@code yearofice:fresh}. The grayling is a
 * {@link YoiFoodItem}: roasted whole on a spit and eaten at its own weight.
 */
public final class FishingRegistry {
    private FishingRegistry() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YearOfIce.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    static final Codec<HookBait> BAIT_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("bait").forGetter(HookBait::bait),
            Codec.INT.fieldOf("bites_left").forGetter(HookBait::bitesLeft),
            Codec.INT.fieldOf("minutes_left").forGetter(HookBait::minutesLeft)
    ).apply(i, HookBait::new));

    static final StreamCodec<ByteBuf, HookBait> BAIT_STREAM = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), HookBait::bait,
            ByteBufCodecs.VAR_INT, HookBait::bitesLeft,
            ByteBufCodecs.VAR_INT, HookBait::minutesLeft,
            HookBait::new);

    /** The rod's tackle. In the slice it carries only the bait on the hook; line, hook and bobber are the rod. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HookBait>> TACKLE =
            COMPONENTS.registerComponentType("tackle", b -> b.persistent(BAIT_CODEC).networkSynchronized(BAIT_STREAM));

    /** Line condition 0–100 % is the rod's durability (Tackle › Lines: plant fiber −3% a fight, −9% in the red). */
    public static final DeferredItem<RodItem> ROD = ITEMS.registerItem("rod", RodItem::new, new Item.Properties().durability(RodItem.LINE_MAX));
    /** The kit rod: a longer cast and a sinew reel; the willow pole above stays the bushcraft rod (M6 lane C). */
    public static final DeferredItem<RodItem> KIT_ROD = ITEMS.registerItem("kit_rod", p -> new RodItem(p, true), new Item.Properties().durability(RodItem.LINE_MAX));
    public static final DeferredItem<Item> WORM = ITEMS.registerItem("worm", Item::new, new Item.Properties().stacksTo(64));
    /** M5 lane C2 baits and lures: roe and fish_chunk are used up on the hook, the spoon and fly stay on the line. */
    public static final DeferredItem<Item> ROE = ITEMS.registerItem("roe", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> FISH_CHUNK = ITEMS.registerItem("fish_chunk", Item::new, new Item.Properties().stacksTo(64));
    /** Guts of a dressed hare or fish: bait, one per 0.1 kg (Blueprint › Food systems › Butchering, Guts 10%). */
    public static final DeferredItem<Item> FISH_GUTS = ITEMS.registerItem("fish_guts", Item::new, new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> SPOON = ITEMS.registerItem("spoon", Item::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> FLY = ITEMS.registerItem("fly", Item::new, new Item.Properties().stacksTo(16));

    public static final net.neoforged.neoforge.registries.DeferredRegister.Blocks BLOCKS =
            net.neoforged.neoforge.registries.DeferredRegister.createBlocks(YearOfIce.MOD_ID);
    private static final net.minecraft.world.level.block.state.BlockBehaviour.Properties GEAR_PROPS =
            net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(0.3f).noOcclusion()
                    .sound(net.minecraft.world.level.block.SoundType.WOOD)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
    public static final net.neoforged.neoforge.registries.DeferredBlock<GearBlocks.SetLine> SET_LINE =
            BLOCKS.registerBlock("set_line", GearBlocks.SetLine::new, GEAR_PROPS);
    public static final net.neoforged.neoforge.registries.DeferredBlock<GearBlocks.GillNet> GILL_NET =
            BLOCKS.registerBlock("gill_net", GearBlocks.GillNet::new, GEAR_PROPS.noCollission());
    public static final net.neoforged.neoforge.registries.DeferredBlock<GearBlocks.WeirBasket> WEIR_BASKET =
            BLOCKS.registerBlock("weir_basket", GearBlocks.WeirBasket::new, GEAR_PROPS);
    /** M6 lane C: the fish weir's stakes, the fish wheel and the tip-up. */
    public static final net.neoforged.neoforge.registries.DeferredBlock<GearBlocks.WeirStake> WEIR_STAKE =
            BLOCKS.registerBlock("weir_stake", GearBlocks.WeirStake::new, GEAR_PROPS);
    public static final net.neoforged.neoforge.registries.DeferredBlock<GearBlocks.FishWheel> FISH_WHEEL =
            BLOCKS.registerBlock("fish_wheel", GearBlocks.FishWheel::new, GEAR_PROPS);
    public static final net.neoforged.neoforge.registries.DeferredBlock<GearBlocks.TipUp> TIP_UP =
            BLOCKS.registerBlock("tip_up", GearBlocks.TipUp::new, GEAR_PROPS);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> WEIR_STAKE_ITEM =
            ITEMS.registerSimpleBlockItem("weir_stake", WEIR_STAKE, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> FISH_WHEEL_ITEM =
            ITEMS.registerSimpleBlockItem("fish_wheel", FISH_WHEEL, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> TIP_UP_ITEM =
            ITEMS.registerSimpleBlockItem("tip_up", TIP_UP, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> SET_LINE_ITEM =
            ITEMS.registerSimpleBlockItem("set_line", SET_LINE, new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> WEIR_BASKET_ITEM =
            ITEMS.registerSimpleBlockItem("weir_basket", WEIR_BASKET, new Item.Properties());
    /** Places three {@code gill_net} blocks in a line and adds one segment to the net there. */
    public static final DeferredItem<GearBlocks.NetSegmentItem> GILL_NET_SEGMENT =
            ITEMS.registerItem("gill_net_segment", GearBlocks.NetSegmentItem::new, new Item.Properties().stacksTo(16));
    /** A whole fish is a single item (Items › Food values: "Single, weight_g"). */
    public static final DeferredItem<Item> ARCTIC_GRAYLING = ITEMS.register("arctic_grayling", () -> new YoiFoodItem(new Item.Properties().stacksTo(1)));

    /** The other species' landed-fish items, same shape as the grayling (item and entity share the species id). */
    public static final java.util.Map<String, DeferredItem<Item>> FISH = new java.util.TreeMap<>();

    static {
        FISH.put(FishingSpecies.GRAYLING, ARCTIC_GRAYLING);
        for (String id : FishingSpecies.IDS) {
            if (!FISH.containsKey(id)) FISH.put(id, ITEMS.register(id, () -> new YoiFoodItem(new Item.Properties().stacksTo(1))));
        }
    }

    public static Item fishItem(String speciesId) {
        DeferredItem<Item> i = FISH.get(speciesId);
        if (i == null) throw new IllegalArgumentException("no fish item for " + speciesId);
        return i.get();
    }

    public static final DeferredHolder<EntityType<?>, EntityType<BobberEntity>> BOBBER = ENTITIES.register("bobber",
            () -> EntityType.Builder.<BobberEntity>of(BobberEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).noSave().noSummon().clientTrackingRange(5).updateInterval(2).build("bobber"));

    /** One live-fish entity type per species (item and entity share the species id); salmon are bigger and swim as a school. */
    public static final java.util.Map<String, DeferredHolder<EntityType<?>, EntityType<FishEntity>>> FISH_TYPES = new java.util.TreeMap<>();

    static {
        for (String id : FishingSpecies.IDS) {
            boolean school = FishEntity.isSchool(id);
            FISH_TYPES.put(id, ENTITIES.register(id, () -> EntityType.Builder.<FishEntity>of(FishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(school ? 0.7f : 0.45f, school ? 0.4f : 0.3f).noSave().clientTrackingRange(8).build(id)));
        }
    }

    public static EntityType<FishEntity> fishType(String species) {
        var h = FISH_TYPES.get(species);
        return h == null ? null : h.get();
    }

    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
    }
}
