package com.yearofice.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.fire.Vessel;
import java.util.Locale;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The data component {@code yearofice:water} on a container: what is in it, as the sim's {@link Vessel} (litres, where
 * it came from, whether it has been boiled, how hot it is). Written by the fire ring while a pot boils and by the
 * container items when they fill, drink and pour; read by both and by the tooltip. An empty container carries no
 * component.
 */
public record WaterContent(double litres, String source, boolean boiled, double heat, double boilMinutes) {

    public static final Codec<WaterContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 1000).fieldOf("litres").forGetter(WaterContent::litres),
            Codec.STRING.fieldOf("source").forGetter(WaterContent::source),
            Codec.BOOL.optionalFieldOf("boiled", false).forGetter(WaterContent::boiled),
            Codec.doubleRange(0, 1).optionalFieldOf("heat", 0.0).forGetter(WaterContent::heat),
            Codec.doubleRange(0, 1000).optionalFieldOf("boil_min", 0.0).forGetter(WaterContent::boilMinutes)
    ).apply(i, WaterContent::new));

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YearOfIce.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WaterContent>> WATER =
            COMPONENTS.registerComponentType("water", b -> b.persistent(CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CODEC)));

    public Vessel vessel() {
        WaterPurity.Source s;
        try {
            s = WaterPurity.Source.valueOf(source.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            s = WaterPurity.Source.RIVER;
        }
        return new Vessel(litres, s, boiled, heat, boilMinutes);
    }

    public static WaterContent of(Vessel v) {
        return new WaterContent(v.litres(), v.source().name().toLowerCase(Locale.ROOT), v.boiled(), v.heat(), v.boilMinutes());
    }

    /** The vessel in a container stack, or an empty vessel. */
    public static Vessel read(ItemStack stack) {
        WaterContent c = stack.get(WATER.get());
        return c == null ? Vessel.empty() : c.vessel();
    }

    /** Stores {@code v} in the stack; an empty vessel removes the component. */
    public static void write(ItemStack stack, Vessel v) {
        if (v.isEmpty()) stack.remove(WATER.get());
        else stack.set(WATER.get(), of(v));
    }
}
