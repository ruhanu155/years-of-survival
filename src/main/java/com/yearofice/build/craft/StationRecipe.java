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
 * { "type": "yearofice:craft_station", "tier": "bushcraft", "station": "yearofice:workbench",
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
                            ItemStack result, int workMinPerKg, int yieldPct) implements Recipe<StationInput> {

    /** The pre-fold form: no extra fields (a per-kg recipe is the crop quern's shape, see {@link #LEGACY}). */
    public StationRecipe(Tier tier, ResourceLocation station, List<SizedIngredient> ingredients, Optional<ToolReq> tool,
                         double workMin, Effort effort, double kcal, Optional<Skill> skill, Optional<ResourceLocation> takenBy, ItemStack result) {
        this(tier, station, ingredients, tool, workMin, effort, kcal, skill, takenBy, result, 0, 100);
    }

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

    public static final MapCodec<StationRecipe> FULL = RecordCodecBuilder.mapCodec(i -> i.group(
            TIER.fieldOf("tier").forGetter(StationRecipe::tier),
            ResourceLocation.CODEC.optionalFieldOf("station", HANDS).forGetter(StationRecipe::station),
            SizedIngredient.FLAT_CODEC.listOf().fieldOf("ingredients").forGetter(StationRecipe::ingredients),
            ToolReq.CODEC.optionalFieldOf("tool").forGetter(StationRecipe::tool),
            Codec.doubleRange(0, 100_000).optionalFieldOf("work_min", 0.0).forGetter(StationRecipe::workMin),
            EFFORT.fieldOf("effort").forGetter(StationRecipe::effort),
            Codec.doubleRange(0, 1_000_000).fieldOf("kcal").forGetter(StationRecipe::kcal),
            SKILL.optionalFieldOf("skill").forGetter(StationRecipe::skill),
            ResourceLocation.CODEC.optionalFieldOf("taught_by").forGetter(StationRecipe::takenBy),
            ItemStack.CODEC.fieldOf("result").forGetter(StationRecipe::result),
            Codec.intRange(0, 100000).optionalFieldOf("work_min_per_kg", 0).forGetter(StationRecipe::workMinPerKg),
            Codec.intRange(1, 1000).optionalFieldOf("yield_pct", 100).forGetter(StationRecipe::yieldPct)
    ).apply(i, StationRecipe::new));

    /**
     * The shape crop's {@code yearofice:station} recipes were shipped in (M5): {@code inputs} item ids, a {@code tool} id,
     * {@code work_min_per_kg} and a bare {@code result} id. It loads as a Start-tier light-work recipe with no kcal.
     * Kept so old data and packs still load (RECONCILE 282/287: one type, an alias loader).
     */
    static final MapCodec<StationRecipe> LEGACY = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("station").forGetter(StationRecipe::station),
            ResourceLocation.CODEC.listOf().fieldOf("inputs").forGetter(r -> List.of()),
            ResourceLocation.CODEC.fieldOf("tool").forGetter(r -> r.tool.map(ToolReq::id).orElseThrow()),
            Codec.intRange(1, 100000).fieldOf("work_min_per_kg").forGetter(StationRecipe::workMinPerKg),
            ResourceLocation.CODEC.fieldOf("result").forGetter(r -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(r.result.getItem())),
            Codec.intRange(1, 1000).optionalFieldOf("yield_pct", 100).forGetter(StationRecipe::yieldPct)
    ).apply(i, (st, ins, tool, perKg, res, y) -> new StationRecipe(Tier.START, st,
            ins.stream().map(id -> SizedIngredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id), 1)).toList(),
            Optional.of(new ToolReq(tool, false)), 0, Effort.LIGHT, 0, Optional.empty(), Optional.empty(),
            new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(res)), perKg, y)));

    /** Reads either shape; writes the full one. */
    public static final MapCodec<StationRecipe> CODEC = Codec.mapEither(FULL, LEGACY).xmap(
            e -> e.map(r -> r, r -> r), com.mojang.datafixers.util.Either::left);

    public static final StreamCodec<RegistryFriendlyByteBuf, StationRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public static final RecipeSerializer<StationRecipe> SERIALIZER = serializer();

    /** A fresh serializer over the one codec (a registry holds each object once, so the {@code yearofice:station} alias gets its own). */
    static RecipeSerializer<StationRecipe> serializer() {
        return new RecipeSerializer<>() {
            @Override public MapCodec<StationRecipe> codec() { return CODEC; }

            @Override public StreamCodec<RegistryFriendlyByteBuf, StationRecipe> streamCodec() { return STREAM_CODEC; }
        };
    }

    /** The first recipe at {@code stationId} that takes {@code input} (any ingredient matches it), or null. */
    public static @org.jetbrains.annotations.Nullable StationRecipe find(net.minecraft.server.level.ServerLevel level, String stationId, ItemStack input) {
        for (var h : StationCraft.all(level)) {
            StationRecipe r = h.value();
            if (!r.station.toString().equals(stationId)) continue;
            for (SizedIngredient in : r.ingredients) if (in.ingredient().test(input)) return r;
        }
        return null;
    }

    /** Minutes for {@code grams} of input at a per-kg recipe. */
    public int minutesFor(int grams) { return (int) Math.round(workMinPerKg * grams / 1000.0); }

    public net.minecraft.world.item.Item resultItem() { return result.getItem(); }

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
