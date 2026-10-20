package com.yearofice.worldgen.gen;

import com.mojang.serialization.MapCodec;
import com.yearofice.core.YearOfIce;
import com.yearofice.world.attach.WorldAttachments;
import com.yearofice.worldgen.block.WorldBlocks;
import com.yearofice.worldgen.structure.LootSeedProcessor;
import com.yearofice.worldgen.structure.MarkPlacedProcessor;
import com.yearofice.worldgen.structure.PlacesLayerPlacement;
import com.yearofice.worldgen.structure.SetPieceStructure;
import com.yearofice.worldgen.structure.TemplatePiece;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every worldgen registration (Registry table: worldgen, structures and data): the chunk generator and biome
 * source {@code yearofice:valley}, the {@code layer_trees} feature, the set-piece structure type, placement and
 * piece, the processors, the world blocks and the chunk attachments. Also adds the built-in data pack that holds
 * the GameTest dimension {@code yearofice:slice_test}: it is never selected in a normal world, and the GameTest
 * server turns on every available pack.
 */
public final class WorldgenRegistry {
    private WorldgenRegistry() {}

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, YearOfIce.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, YearOfIce.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, YearOfIce.MOD_ID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, YearOfIce.MOD_ID);
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, YearOfIce.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, YearOfIce.MOD_ID);
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, YearOfIce.MOD_ID);

    public static final Supplier<MapCodec<YoiChunkGenerator>> VALLEY_GENERATOR = GENERATORS.register("valley", () -> YoiChunkGenerator.CODEC);
    public static final Supplier<MapCodec<YoiBiomeSource>> VALLEY_BIOMES = BIOME_SOURCES.register("valley", () -> YoiBiomeSource.CODEC);
    public static final Supplier<Feature<NoneFeatureConfiguration>> LAYER_TREES = FEATURES.register("layer_trees", LayerTreesFeature::new);
    public static final Supplier<Feature<NoneFeatureConfiguration>> LAYER_PLANTS = FEATURES.register("layer_plants", LayerPlantsFeature::new);
    public static final Supplier<StructureType<SetPieceStructure>> SETPIECE = STRUCTURE_TYPES.register("setpiece", () -> () -> SetPieceStructure.CODEC);
    public static final Supplier<StructurePlacementType<PlacesLayerPlacement>> PLACES_LAYER =
            PLACEMENTS.register("places_layer", () -> () -> PlacesLayerPlacement.CODEC);
    public static final Supplier<StructurePieceType> TEMPLATE_PIECE = PIECES.register("template_piece", () -> TemplatePiece::new);
    public static final Supplier<StructureProcessorType<MarkPlacedProcessor>> MARK_PLACED =
            PROCESSORS.register("mark_placed", () -> () -> MarkPlacedProcessor.CODEC);
    public static final Supplier<StructureProcessorType<LootSeedProcessor>> LOOT_SEED =
            PROCESSORS.register("loot_seed", () -> () -> LootSeedProcessor.CODEC);

    public static void init(IEventBus modBus) {
        WorldBlocks.register(modBus);
        WorldAttachments.register(modBus);
        GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
        FEATURES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        PLACEMENTS.register(modBus);
        PIECES.register(modBus);
        PROCESSORS.register(modBus);
        modBus.addListener(WorldgenRegistry::packs);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ValleyWorlds.clear());
        com.yearofice.world.WorldHooks.init();
    }

    /** The test dimension pack, {@code packs/slice_test} in the jar. */
    static void packs(AddPackFindersEvent e) {
        e.addPackFinders(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "packs/slice_test"), PackType.SERVER_DATA,
                Component.literal("Year of Ice test dimension"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }
}
