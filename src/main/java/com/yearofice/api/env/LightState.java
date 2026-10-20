package com.yearofice.api.env;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The data component {@code yearofice:light_state} on a carried light: whether it is lit and how many game minutes
 * it has left (a torch's or candle's burn, a headlamp's battery). A stack without the component is unlit with its
 * full burn ahead; {@code left} of 0 on an unlimited light means no limit. Written by {@code fire.light} (lighting,
 * burning down, rain) and read by {@code env.light} and the client's {@code HeldLight}.
 */
public record LightState(boolean lit, int left) {

    public static final Codec<LightState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("lit").forGetter(LightState::lit),
            Codec.intRange(0, 1_000_000).optionalFieldOf("left", 0).forGetter(LightState::left)
    ).apply(i, LightState::new));

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LightState>> LIGHT_STATE =
            COMPONENTS.registerComponentType("light_state", b -> b.persistent(CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CODEC)));

    /** True when the stack is a light that is shining now: lit, or a light that needs no lighting. */
    public static boolean shining(ItemStack stack, LightData d) {
        if (!d.needsLit()) return true;
        LightState s = stack.get(LIGHT_STATE.get());
        return s != null && s.lit();
    }

    /** Minutes left on the stack: its stored count, or the full burn of an unused light. */
    public static int minutesLeft(ItemStack stack, LightData d) {
        LightState s = stack.get(LIGHT_STATE.get());
        return s == null ? d.burnMin() : s.left();
    }

    public static void set(ItemStack stack, boolean lit, int left) { stack.set(LIGHT_STATE.get(), new LightState(lit, Math.max(0, left))); }
}
