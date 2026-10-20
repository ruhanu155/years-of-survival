package com.yearofice.api.env;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * One row of the item data map {@code yearofice:light} ({@code data_maps/item/light.json}): a carried light's block
 * light level, its cone and how long it burns (Minecraft development › Gameplay light and Held light). {@code env.light}
 * reads it for gameplay light and {@code client.light.HeldLight} for drawing, so the two never disagree. The map is
 * synced to clients.
 *
 * @param level   light 0–15 at the carrier (the Engine's table: headlamp wide 12, bark torch 10, pitch torch 8, tin
 *                lantern 8, candle 6, messenger screen 1)
 * @param coneDeg full cone angle, 360 for a light that shines all round
 * @param burnMin game minutes the item burns, or battery minutes for a lamp; 0 for no limit
 * @param rain    how rain treats it: {@code out} (any rain puts it out), {@code holds} (light rain does not; heavy rain
 *                has a chance) or {@code never} (wind-proof and rain-proof, a lantern)
 * @param needsLit true when the item shines only while lit (everything but the messenger screen)
 */
public record LightData(int level, int coneDeg, int burnMin, String rain, boolean needsLit) {

    public static final Codec<LightData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 15).fieldOf("level").forGetter(LightData::level),
            Codec.intRange(1, 360).optionalFieldOf("cone_deg", 360).forGetter(LightData::coneDeg),
            Codec.intRange(0, 1_000_000).optionalFieldOf("burn_min", 0).forGetter(LightData::burnMin),
            Codec.STRING.optionalFieldOf("rain", "out").forGetter(LightData::rain),
            Codec.BOOL.optionalFieldOf("needs_lit", true).forGetter(LightData::needsLit)
    ).apply(i, LightData::new));

    public static final DataMapType<Item, LightData> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "light"), Registries.ITEM, CODEC).synced(CODEC, false).build();
}
