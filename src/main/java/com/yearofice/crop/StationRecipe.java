package com.yearofice.crop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@code yearofice:station} recipe (Crafting in depth, Data and tests): what a station or hand tool turns a stack into.
 * Minimal form: a station id, accepted input items, a tool, minutes per kilogram of input and a result item that takes the
 * input's weight times {@code yield_pct}. Station block entities and menus come with the M9 stations; the hand quern uses it now.
 */
public record StationRecipe(ResourceLocation station, List<ResourceLocation> inputs, ResourceLocation tool,
                            int workMinPerKg, ResourceLocation result, int yieldPct) implements Recipe<RecipeInput> {

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, YearOfIce.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<StationRecipe>> TYPE = TYPES.register("station", () -> new RecipeType<>() {
        @Override public String toString() { return "yearofice:station"; }
    });

    public static final MapCodec<StationRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("station").forGetter(StationRecipe::station),
            ResourceLocation.CODEC.listOf().fieldOf("inputs").forGetter(StationRecipe::inputs),
            ResourceLocation.CODEC.fieldOf("tool").forGetter(StationRecipe::tool),
            Codec.intRange(1, 100000).fieldOf("work_min_per_kg").forGetter(StationRecipe::workMinPerKg),
            ResourceLocation.CODEC.fieldOf("result").forGetter(StationRecipe::result),
            Codec.intRange(1, 1000).optionalFieldOf("yield_pct", 100).forGetter(StationRecipe::yieldPct)
    ).apply(i, StationRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StationRecipe> STREAM = StreamCodec.of(
            (b, r) -> { b.writeResourceLocation(r.station); b.writeCollection(r.inputs, (x, v) -> x.writeResourceLocation(v));
                b.writeResourceLocation(r.tool); b.writeVarInt(r.workMinPerKg); b.writeResourceLocation(r.result); b.writeVarInt(r.yieldPct); },
            b -> new StationRecipe(b.readResourceLocation(), b.readList(x -> x.readResourceLocation()), b.readResourceLocation(),
                    b.readVarInt(), b.readResourceLocation(), b.readVarInt()));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StationRecipe>> SERIALIZER = SERIALIZERS.register("station", () -> new RecipeSerializer<>() {
        @Override public MapCodec<StationRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, StationRecipe> streamCodec() { return STREAM; }
    });

    /** The first station recipe at {@code stationId} that takes {@code input}, or null. */
    public static StationRecipe find(ServerLevel level, String stationId, ItemStack input) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(input.getItem());
        for (var h : level.getServer().getRecipeManager().getAllRecipesFor(TYPE.get()))
            if (h.value().station.toString().equals(stationId) && h.value().inputs.contains(id)) return h.value();
        return null;
    }

    public int minutesFor(int grams) { return (int) Math.round(workMinPerKg * grams / 1000.0); }

    public net.minecraft.world.item.Item resultItem() { return BuiltInRegistries.ITEM.get(result); }

    @Override public boolean matches(RecipeInput in, Level l) { return false; }
    @Override public ItemStack assemble(RecipeInput in, HolderLookup.Provider p) { return ItemStack.EMPTY; }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider p) { return new ItemStack(resultItem()); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
}
