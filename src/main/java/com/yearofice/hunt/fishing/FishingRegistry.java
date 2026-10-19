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
    public static final DeferredItem<Item> WORM = ITEMS.registerItem("worm", Item::new, new Item.Properties().stacksTo(64));
    /** A whole fish is a single item (Items › Food values: "Single, weight_g"). */
    public static final DeferredItem<Item> ARCTIC_GRAYLING = ITEMS.register("arctic_grayling", () -> new YoiFoodItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<EntityType<?>, EntityType<BobberEntity>> BOBBER = ENTITIES.register("bobber",
            () -> EntityType.Builder.<BobberEntity>of(BobberEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).noSave().noSummon().clientTrackingRange(5).updateInterval(2).build("bobber"));

    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
    }
}
