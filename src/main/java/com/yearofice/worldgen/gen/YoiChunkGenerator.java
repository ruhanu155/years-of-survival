package com.yearofice.worldgen.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.worldgen.TerrainPalette;
import com.yearofice.worldgen.ValleyWorld;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/**
 * {@code yearofice:valley} (Minecraft development › The generator: YoiChunkGenerator). Our own height field, no
 * density functions: every column comes from the bound layers ({@link ValleyWorld#columns}), written into the
 * chunk's sections directly. Sections wholly below every column's soil or shell are single-value deep ground or
 * deep rock; sections above the surface stay air, so the 1,968-block height costs memory only near the surface.
 *
 * <p>Carvers, original mobs and vanilla features are empty. Structures run vanilla's loop over the sets our
 * biomes allow, which are only ours (the set pieces). Trees are the {@code yearofice:layer_trees} feature.
 */
public final class YoiChunkGenerator extends ChunkGenerator {
    public static final MapCodec<YoiChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            YoiBiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.source),
            GenSettings.CODEC.fieldOf("settings").forGetter(YoiChunkGenerator::settings)).apply(i, YoiChunkGenerator::new));

    public static final int SEA_LEVEL = 100, MIN_Y = 64, GEN_DEPTH = 1968;

    private final YoiBiomeSource source;
    private final GenSettings settings;
    private volatile ValleyWorld world;
    private volatile BlockState[] states;

    public YoiChunkGenerator(YoiBiomeSource source, GenSettings settings) {
        super(source);
        this.source = source;
        this.settings = settings;
    }

    public GenSettings settings() { return settings; }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    // ------------------------------------------------------------------ binding

    /** Builds (or reuses) the layers for this level's seed. Called once from {@link #createState}. */
    public ValleyWorld bind(long levelSeed) {
        ValleyWorld w = world;
        if (w != null) return w;
        w = ValleyWorlds.get(settings.seedFor(levelSeed));
        BlockState[] st = new BlockState[w.palette.size()];
        for (int k = 0; k < st.length; k++) st[k] = state(w.palette.name(k));
        this.states = st;
        source.bind(w);
        ValleyWorlds.bindLevelSeed(levelSeed, w);
        this.world = w;
        return w;
    }

    static BlockState state(String name) {
        if (name.equals("air")) return Blocks.AIR.defaultBlockState();
        if (name.equals("water")) return Blocks.WATER.defaultBlockState();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("yearofice", name);
        Block b = BuiltInRegistries.BLOCK.get(id);
        if (b == Blocks.AIR) throw new IllegalStateException("terrain palette names unknown block " + id);
        return b.defaultBlockState();
    }

    /** The bound layers, or null before the level is created. */
    public ValleyWorld world() { return world; }

    private ValleyWorld need() {
        ValleyWorld w = world;
        if (w == null) throw new IllegalStateException("yearofice:valley generator used before its level bound the layers");
        return w;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> sets, RandomState random, long seed) {
        bind(seed);
        return super.createState(sets, random, seed);
    }

    // ------------------------------------------------------------------ chunk steps

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState random, Blender blender, StructureManager structures, ChunkAccess chunk) {
        ValleyWorld w = world;
        if (w == null) return super.createBiomes(random, blender, structures, chunk);
        ChunkPos cp = chunk.getPos();
        int qx0 = cp.getMinBlockX() >> 2, qz0 = cp.getMinBlockZ() >> 2;
        @SuppressWarnings("unchecked")
        Holder<Biome>[] at = new Holder[16];
        for (int k = 0; k < 4; k++) for (int i = 0; i < 4; i++) {
            at[k * 4 + i] = source.forClass(w.classAt(cp.getMinBlockX() + i * 4 + 2, cp.getMinBlockZ() + k * 4 + 2));
        }
        chunk.fillBiomesFromNoise((qx, qy, qz, s) -> at[((qz - qz0) & 3) * 4 + ((qx - qx0) & 3)], random.sampler());
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        fill(need(), states, chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    /** Writes one chunk's columns. Public for the chunk-budget GameTest, which times it on bare proto-chunks. */
    public void fill(ChunkAccess chunk) { fill(need(), states, chunk); }

    static void fill(ValleyWorld w, BlockState[] pal, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        ValleyWorld.Columns c = w.columns(x0, z0, 16, 16);
        int soilN = w.p.terrain().soilBlocks();
        int minY = chunk.getMinBuildHeight();
        LevelChunkSection[] sections = chunk.getSections();
        BlockState deepGround = pal[TerrainPalette.DEEP_GROUND], deepRock = pal[TerrainPalette.DEEP_ROCK];

        // Bottom of the written part of each column, and which deep block lies under it.
        int[] bottom = new int[256];
        int low = Integer.MAX_VALUE;
        boolean anyRock = false, anyGround = false;
        for (int i = 0; i < 256; i++) {
            bottom[i] = c.kind[i] == ValleyWorld.ROCK ? c.shellBottom[i] : c.ground[i] - soilN;
            low = Math.min(low, bottom[i]);
            if (c.kind[i] == ValleyWorld.ROCK) anyRock = true; else anyGround = true;
        }
        // Single-value sections wholly below every column's written part.
        int single = Math.max(0, Math.min(sections.length, (low - minY) / 16));
        if (anyRock != anyGround) {
            BlockState fill = anyRock ? deepRock : deepGround;
            for (int s = 0; s < single; s++) {
                LevelChunkSection old = sections[s];
                sections[s] = new LevelChunkSection(new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, fill,
                        PalettedContainer.Strategy.SECTION_STATES), old.getBiomes());
            }
        } else {
            single = 0;
        }
        int start = minY + single * 16;
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int i = lz * 16 + lx;
                byte kind = c.kind[i];
                int g = c.ground[i];
                boolean rock = kind == ValleyWorld.ROCK;
                BlockState deep = rock ? deepRock : deepGround;
                for (int y = start; y < bottom[i]; y++) set(sections, minY, lx, y, lz, deep);
                if (rock) {
                    BlockState face = pal[TerrainPalette.FACE_ROCK];
                    for (int y = bottom[i]; y <= g; y++) set(sections, minY, lx, y, lz, face);
                    if (c.has(i, ValleyWorld.F_COAL)) {
                        for (int y = c.coalY[i]; y <= Math.min(g, c.coalY[i] + 1); y++) set(sections, minY, lx, y, lz, pal[TerrainPalette.COAL_SEAM]);
                    }
                    if (c.has(i, ValleyWorld.F_IRON)) set(sections, minY, lx, g, lz, pal[TerrainPalette.IRON_OUTCROP]);
                } else {
                    for (int k = 0; k < soilN; k++) {
                        int y = g - soilN + k;
                        set(sections, minY, lx, y, lz, pal[k == 0 ? c.lower[i] : c.soil[i]]);
                    }
                    set(sections, minY, lx, g, lz, pal[c.surface[i]]);
                    if (kind == ValleyWorld.WATER) {
                        BlockState water = pal[TerrainPalette.WATER];
                        for (int y = g + 1; y <= c.water[i]; y++) set(sections, minY, lx, y, lz, water);
                        if (c.has(i, ValleyWorld.F_FLOW)) chunk.markPosForPostprocessing(mp.set(x0 + lx, c.water[i], z0 + lz));
                    } else if (kind == ValleyWorld.LIP) {
                        set(sections, minY, lx, c.lip[i], lz, pal[TerrainPalette.RIFFLE_COBBLE]);
                    }
                }
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
    }

    private static void set(LevelChunkSection[] sections, int minY, int x, int y, int z, BlockState s) {
        int si = (y - minY) >> 4;
        if (si < 0 || si >= sections.length) return;
        sections[si].setBlockState(x, y & 15, z, s, false);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
        // The surface is written with the columns in fillFromNoise: one pass, one read of the layers.
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // No caves, no ravines (Vanilla worldgen switched off).
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Animals come from populations (Ecology), never from generation.
    }

    // ------------------------------------------------------------------ queries

    @Override
    public int getGenDepth() { return GEN_DEPTH; }

    @Override
    public int getSeaLevel() { return SEA_LEVEL; }

    @Override
    public int getMinY() { return MIN_Y; }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        ValleyWorld w = world;
        if (w == null) return SEA_LEVEL;
        ValleyWorld.Columns c = w.columns(x, z, 1, 1);
        boolean floor = type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
        int top = floor ? (c.kind[0] == ValleyWorld.LIP ? c.lip[0] : c.ground[0]) : c.top(0);
        return top + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int minY = level.getMinBuildHeight(), h = level.getHeight();
        BlockState[] col = new BlockState[h];
        java.util.Arrays.fill(col, Blocks.AIR.defaultBlockState());
        ValleyWorld w = world;
        if (w != null) {
            ValleyWorld.Columns c = w.columns(x, z, 1, 1);
            BlockState[] pal = states;
            int g = c.ground[0];
            boolean rock = c.kind[0] == ValleyWorld.ROCK;
            for (int y = minY; y <= g && y - minY < h; y++) {
                BlockState s = rock ? (y >= c.shellBottom[0] ? pal[TerrainPalette.FACE_ROCK] : pal[TerrainPalette.DEEP_ROCK])
                        : (y == g ? pal[c.surface[0]] : (y > g - w.p.terrain().soilBlocks() ? pal[c.soil[0]] : pal[TerrainPalette.DEEP_GROUND]));
                col[y - minY] = s;
            }
            if (c.kind[0] == ValleyWorld.WATER) for (int y = g + 1; y <= c.water[0] && y - minY < h; y++) col[y - minY] = pal[TerrainPalette.WATER];
            if (c.kind[0] == ValleyWorld.LIP && c.lip[0] - minY < h) col[c.lip[0] - minY] = pal[TerrainPalette.RIFFLE_COBBLE];
        }
        return new NoiseColumn(minY, col);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState random, BlockPos pos) {
        ValleyWorld w = world;
        if (w == null) return;
        lines.add(String.join(" | ", debugLines(w, pos.getX(), pos.getZ())));
    }

    /** The F3 and {@code /yoi show current} lines for a column. */
    public static List<String> debugLines(ValleyWorld w, int x, int z) {
        ValleyWorld.Columns c = w.columns(x, z, 1, 1);
        int cell = w.cellOf(x, z);
        String kind = switch (c.kind[0]) {
            case ValleyWorld.ROCK -> "rock";
            case ValleyWorld.WATER -> "water to " + c.water[0];
            case ValleyWorld.LIP -> "riffle lip " + c.lip[0];
            default -> "soil";
        };
        return List.of(
                "Year of Ice " + w.regionCode(x, z) + (cell >= 0 ? " core cell " + cell : " coarse"),
                "cover " + w.classDef(c.cover[0] & 0xFF).id() + " (" + w.classDef(c.cover[0] & 0xFF).biome() + ")",
                String.format(Locale.ROOT, "ground %d, %s, surface %s, soil %s%s", c.ground[0], kind, w.palette.name(c.surface[0]),
                        w.palette.name(c.soil[0]), c.has(0, ValleyWorld.F_PERMAFROST) ? ", permafrost" : ""),
                w.layerLine(x, z));
    }
}
