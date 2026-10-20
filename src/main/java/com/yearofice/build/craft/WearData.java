package com.yearofice.build.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jetbrains.annotations.Nullable;

/**
 * One row of the item data map {@code yearofice:tool_wear} ({@code data_maps/item/tool_wear.json}): how a tool's Edge wears
 * (Crafting and building in depth › Tool durability). It is its own map, not the felling team's {@code tool} map and not the garment map {@code wear}. Durability itself is vanilla damage against the item's max
 * damage, and {@code max_condition} in {@code props.json} repeats that number as data.
 *
 * @param edgeUses the Edge column: uses from sharp (100) to dull (0); 0 for a tool with no edge
 * @param material {@code wood}, {@code stone} or {@code iron}: wood and stone wear twice as fast under -30 C, and an iron edge chips under -40 C
 */
public record WearData(double edgeUses, String material) {
    public static final Codec<WearData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 100_000).optionalFieldOf("edge_uses", 0.0).forGetter(WearData::edgeUses),
            Codec.STRING.optionalFieldOf("material", "iron").forGetter(WearData::material)
    ).apply(i, WearData::new));

    public static final DataMapType<Item, WearData> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "tool_wear"), Registries.ITEM, CODEC).synced(CODEC, false).build();

    public boolean hasEdge() { return edgeUses > 0; }

    public boolean woodOrStone() { return material.equals("wood") || material.equals("stone"); }

    public static @Nullable WearData of(ItemStack stack) { return stack.isEmpty() ? null : stack.getItemHolder().getData(MAP); }
}
