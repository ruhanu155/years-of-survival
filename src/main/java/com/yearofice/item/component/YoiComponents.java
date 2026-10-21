package com.yearofice.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.food.Doneness;
import java.util.Locale;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Layer 6, Item model: the data component types every system shares (Minecraft development › Layer 6: "component
 * types in {@code item.component.YoiComponents}"). Each id is registered here once; systems read and write the
 * values through their own api services.
 *
 * <ul>
 *   <li>{@code yearofice:fresh}: state, clock, due and life (Layer 5). First written by the ecosystem's carcasses
 *       (PR #15); moved here under the same id and codec, so saves read unchanged (RECONCILE 169).</li>
 *   <li>{@code yearofice:weight_g}: grams one item weighs, overriding the data map for one stack (a landed fish).
 *       First written by fishing (PR #14); moved here under the same id and codec (RECONCILE 168).</li>
 *   <li>{@code yearofice:doneness}: the colour ring of food taken off a spit (Blueprint › Cooking › Doneness: Rare,
 *       Done or Burnt). New in the slice; Food and hunting writes it.</li>
 * </ul>
 */
public final class YoiComponents {
    private YoiComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    /** The {@code yearofice:fresh} codec: snake_case keys, the state by its key (raw, cooked, smoked, dried). */
    public static final Codec<Fresh> FRESH_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(s -> FoodState.valueOf(s.toUpperCase(Locale.ROOT)), FoodState::key).fieldOf("state").forGetter(Fresh::state),
            Codec.INT.fieldOf("clock").forGetter(Fresh::clock),
            Codec.DOUBLE.fieldOf("due_dh").forGetter(Fresh::dueDh),
            Codec.DOUBLE.fieldOf("life_dh").forGetter(Fresh::lifeDh),
            Codec.INT.optionalFieldOf("traits", 0).forGetter(Fresh::traits),
            Codec.BOOL.optionalFieldOf("known", true).forGetter(Fresh::known)
    ).apply(i, Fresh::new));

    public static final Codec<Doneness.Ring> RING_CODEC = Codec.STRING.xmap(
            s -> Doneness.Ring.valueOf(s.toUpperCase(Locale.ROOT)), r -> r.name().toLowerCase(Locale.ROOT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Fresh>> FRESH =
            COMPONENTS.registerComponentType("fresh", b -> b.persistent(FRESH_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(FRESH_CODEC)));

    /** Grams one item weighs (a landed fish's live weight). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WEIGHT_G =
            COMPONENTS.registerComponentType("weight_g", b -> b.persistent(Codec.intRange(0, 1_000_000)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Doneness.Ring>> DONENESS =
            COMPONENTS.registerComponentType("doneness", b -> b.persistent(RING_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(RING_CODEC)));

    /** {@code yearofice:nutrients}: what one item of a dish or dried stack gives (kcal, protein, fat, vitamin C, water). */
    public static final Codec<com.yearofice.sim.food.Nutrients> NUTRIENTS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 1e7).fieldOf("kcal").forGetter(com.yearofice.sim.food.Nutrients::kcal),
            Codec.doubleRange(0, 1e7).fieldOf("protein_g").forGetter(com.yearofice.sim.food.Nutrients::proteinG),
            Codec.doubleRange(0, 1e7).fieldOf("fat_g").forGetter(com.yearofice.sim.food.Nutrients::fatG),
            Codec.doubleRange(0, 1e7).fieldOf("vit_c_mg").forGetter(com.yearofice.sim.food.Nutrients::vitCMg),
            Codec.doubleRange(0, 1e7).fieldOf("water_ml").forGetter(com.yearofice.sim.food.Nutrients::waterMl)
    ).apply(i, com.yearofice.sim.food.Nutrients::new));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.yearofice.sim.food.Nutrients>> NUTRIENTS =
            COMPONENTS.registerComponentType("nutrients", b -> b.persistent(NUTRIENTS_CODEC).networkSynchronized(ByteBufCodecs.fromCodec(NUTRIENTS_CODEC)));

    /** {@code yearofice:dish_id}: the name the notebook gives a dish ({@code camp_stew}, {@code akutaq}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> DISH_ID =
            COMPONENTS.registerComponentType("dish_id", b -> b.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** {@code yearofice:trophy_size}: measure, value, z-score and class of a big-game kill (M6 lane D, A5). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TrophySize>> TROPHY_SIZE =
            COMPONENTS.registerComponentType("trophy_size", b -> b.persistent(TrophySize.CODEC).networkSynchronized(TrophySize.STREAM));

    public static void register(IEventBus modBus) { COMPONENTS.register(modBus); }
}
