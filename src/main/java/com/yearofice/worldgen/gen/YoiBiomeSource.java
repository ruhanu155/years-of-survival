package com.yearofice.worldgen.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.worldgen.ValleyWorld;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * {@code yearofice:valley} biome source (YoiBiomeSource and the biome list). It ignores y and the climate sampler:
 * a quart's biome is its cover class's biome, read from the bound layers through a per-class array, so the call
 * is constant time and lock-free. Biomes set only the look; gameplay reads the cover class.
 *
 * <p>Before the generator binds the layers (or on a client that never does) every quart reads the last biome in
 * the list, so nothing fails while the server starts.
 */
public final class YoiBiomeSource extends BiomeSource {
    public static final MapCodec<YoiBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(YoiBiomeSource::biomes))
            .apply(i, YoiBiomeSource::new));

    private final HolderSet<Biome> biomes;
    private volatile ValleyWorld world;
    private volatile Holder<Biome>[] byClass;

    public YoiBiomeSource(HolderSet<Biome> biomes) { this.biomes = biomes; }

    public HolderSet<Biome> biomes() { return biomes; }

    @Override
    protected MapCodec<? extends BiomeSource> codec() { return CODEC; }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() { return biomes.stream(); }

    @SuppressWarnings("unchecked")
    void bind(ValleyWorld w) {
        List<Holder<Biome>> all = biomes.stream().toList();
        Holder<Biome> fallback = all.get(all.size() - 1);
        int n = w.p.cover().classes().size();
        Holder<Biome>[] arr = new Holder[n];
        for (int c = 0; c < n; c++) {
            ResourceLocation want = ResourceLocation.parse(w.classDef(c).biome());
            arr[c] = fallback;
            for (Holder<Biome> h : all) if (h.is(want)) { arr[c] = h; break; }
        }
        this.byClass = arr;
        this.world = w;
    }

    public Holder<Biome> forClass(int cls) {
        Holder<Biome>[] a = byClass;
        if (a == null) return biomes.get(biomes.size() - 1);
        return a[cls];
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        ValleyWorld w = world;
        if (w == null) return biomes.get(biomes.size() - 1);
        return forClass(w.classAt((qx << 2) + 2, (qz << 2) + 2));
    }
}
