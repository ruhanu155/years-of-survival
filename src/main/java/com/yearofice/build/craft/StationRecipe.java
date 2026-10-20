package com.yearofice.build.craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.sim.build.Effort;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.build.Tier;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/**
 * The recipe type {@code yearofice:station} (Crafting and building in depth › Data and tests): a recipe done at a
 * station, costing time and calories. Files are {@code data/yearofice/recipe/station/<id>.json}.
 *
 * <pre>
 * { "type": "yearofice:station", "tier": "bushcraft", "station": "yearofice:workbench",
 *   "ingredients": [ { "item": "yearofice:tool_handle", "count": 1 } ], "tool": "#yearofice:knives",
 *   "work_min": 240, "effort": "light", "kcal": 400, "skill": "woodcraft", "taught_by": "yearofice:logbook_07",
 *   "result": { "id": "yearofice:longbow", "count": 1 } }
 * </pre>
 *
 * {@code station} defaults to the Hands; {@code tool}, {@code skill} and {@code taught_by} are optional. {@code kcal}
 * is the effort rate over the base time, before practice, and a JUnit test holds each file to that. A recipe whose
 * skill is knapping rolls the core's shatter chance (Practice: 25% down to 5%) when it finishes.
 */
public record StationRecipe(Tier tier, ResourceLocation station, List<SizedIngredient> ingredients, Optional<ToolReq> tool,
                            double workMin, Effort effort, double kcal, Optional<Skill> skill, Optional<ResourceLocation> takenBy,
                            ItemStack result) implements Recipe<StationInput> {

    public static final ResourceLocation HANDS = ResourceLocation.fromNamespaceAndPath("yearofice", "hands");
    public static final ResourceLocation WORKBENCH = ResourceLocation.fromNamespaceAndPath("yearofice", "workbench");

    static final Codec<Tier> TIER = Codec.STRING.comapFlatMap(
            s -> Tier.byId(s).map(com.mojang.serialization.DataResult::success).orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "unknown tier " + s)),
            Tier::id);
    static final Codec<Effort> EFFORT = Codec.STRING.comapFlatMap(
            s -> Effort.byId(s).map(com.mojang.serialization.DataResult::success).orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "unknown effort " + s)),
            Effort::id);
    static final Codec<Skill> SKILL = Codec.STRING.comapFlatMap(
            s -> Skill.byId(s).map(com.mojang.serialization.DataResult::success).orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "unknown skill " + s)),
            Skill::id);

    public static final MapCodec<StationRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TIER.fieldOf("tier").forGetter(StationRecipe::tier),
            ResourceLocation.CODEC.optionalFieldOf("station", HANDS).forGetter(StationRecipe::station),
            SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(StationRecipe::ingredients),
            ToolReq.CODEC.optionalFieldOf("tool").forGetter(StationRecipe::tool),
            Codec.doubleRange(0, 100_000).fieldOf("work_min").forGetter(StationRecipe::workMin),
            EFFORT.fieldOf("effort").forGetter(StationRecipe::effort),
            Codec.doubleRange(0, 1_000_000).fieldOf("kcal").forGetter(StationRecipe::kcal),
            SKILL.optionalFieldOf("skill").forGetter(StationRecipe::skill),
            ResourceLocation.CODEC.optionalFieldOf("taught_by").forGetter(StationRecipe::takenBy),
            ItemStack.CODEC.fieldOf("result").forGetter(StationRecipe::result)
    ).apply(i, StationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StationRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public static final RecipeSerializer<StationRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override public MapCodec<StationRecipe> codec() { return CODEC; }

        @Override public StreamCodec<RegistryFriendlyByteBuf, StationRecipe> streamCodec() { return STREAM_CODEC; }
    };

    /** True for a recipe learned only from a found page (the doc's marked recipes). */
    public boolean taught() { return takenBy.isPresent(); }

    @Override public boolean matches(StationInput in, Level level) { return false; }

    @Override public ItemStack assemble(StationInput in, HolderLookup.Provider registries) { return result.copy(); }

    @Override public boolean canCraftInDimensions(int w, int h) { return true; }

    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> out = NonNullList.create();
        for (SizedIngredient s : ingredients) out.add(s.ingredient());
        return out;
    }

    /** Never offered to the vanilla recipe book. */
    @Override public boolean isSpecial() { return true; }

    @Override public RecipeSerializer<?> getSerializer() { return CraftRegistry.STATION_SERIALIZER.get(); }

    @Override public RecipeType<?> getType() { return CraftRegistry.STATION.get(); }
}
