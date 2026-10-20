package com.yearofice.api.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * One row of the item data map {@code yearofice:container} ({@code data_maps/item/container.json}): a thing that
 * holds water (Items › Containers; Fire, water and light › Carrying water). The fire ring reads {@code heat} to know
 * whether it may stand on the fire, and the container items read {@code capacity_l} to fill.
 *
 * @param capacityL litres it holds (steel bottle 1, pot 2, bark bucket 4, tin can 0.4, jerrycan 15)
 * @param heat      where it may stand to boil: {@code fire} (on a burning fire or coals), {@code coals} (only on coals:
 *                  a bottle or a can sits in the coals) or {@code never} (a birch-bark bucket burns)
 */
public record ContainerData(double capacityL, String heat) {

    public static final Codec<ContainerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0.01, 100).fieldOf("capacity_l").forGetter(ContainerData::capacityL),
            Codec.STRING.optionalFieldOf("heat", "never").forGetter(ContainerData::heat)
    ).apply(i, ContainerData::new));

    public static final DataMapType<Item, ContainerData> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "container"), Registries.ITEM, CODEC).synced(CODEC, false).build();

    public boolean onFire() { return heat.equals("fire"); }

    public boolean onCoals() { return heat.equals("fire") || heat.equals("coals"); }
}
