package com.yearofice.build.wood;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.wood.ToolStats;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The data map {@code yearofice:tool} ({@code data_maps/item/tool.json}; Trees, felling and firewood › Build: "Tool
 * numbers (S, minutes a swing, limb, buck and split rates) are in data_maps/item/tool.json"). Synced, so the
 * client's preview reads the same numbers.
 */
public final class ToolData {
    private ToolData() {}

    public static final Codec<ToolStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 10_000).optionalFieldOf("fell_max_cm", 0.0).forGetter(ToolStats::fellMaxCm),
            Codec.intRange(0, 1000).optionalFieldOf("swings_25cm", 0).forGetter(ToolStats::swings25),
            Codec.doubleRange(0, 1000).optionalFieldOf("swing_min", 0.0).forGetter(ToolStats::swingMin),
            Codec.doubleRange(0, 1000).optionalFieldOf("limb_min", 0.0).forGetter(ToolStats::limbMin),
            Codec.doubleRange(0, 1000).optionalFieldOf("buck_min", 0.0).forGetter(ToolStats::buckMin),
            Codec.intRange(0, 100).optionalFieldOf("split_strikes", 0).forGetter(ToolStats::splitStrikes),
            Codec.intRange(0, 100).optionalFieldOf("split_crack_strikes", 1).forGetter(ToolStats::splitCrackStrikes)
    ).apply(i, ToolStats::new));

    public static final DataMapType<Item, ToolStats> TOOL = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "tool"), Registries.ITEM, CODEC).synced(CODEC, false).build();

    static void register(RegisterDataMapTypesEvent e) { e.register(TOOL); }

    /** The stack's tool row, or null for anything that is not a wood tool. */
    public static @Nullable ToolStats of(ItemStack stack) { return stack.isEmpty() ? null : stack.getItemHolder().getData(TOOL); }
}
