package com.yearofice.worldgen.structure;

import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.gen.WorldgenRegistry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * {@code yearofice:template_piece}: one set-piece template, turned and standing with its rotated box's lowest
 * corner on the pad. Its processors are the {@code yearofice:setpiece} processor list (loot seed, then the
 * placed mask, which runs last). Saved with its template, position and rotation.
 */
public final class TemplatePiece extends TemplateStructurePiece {
    static final ResourceKey<StructureProcessorList> PROCESSORS =
            ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "setpiece"));
    public static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};

    private final int rotation;

    private TemplatePiece(StructureTemplateManager mgr, RegistryAccess reg, String template, BlockPos at, int rotation) {
        super(WorldgenRegistry.TEMPLATE_PIECE.get(), 0, mgr, ResourceLocation.parse(template), template, settings(reg, rotation), at);
        this.rotation = rotation;
    }

    public TemplatePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(WorldgenRegistry.TEMPLATE_PIECE.get(), tag, ctx.structureTemplateManager(), settingsFor(ctx.registryAccess(), tag.getInt("Rot")));
        this.rotation = tag.getInt("Rot");
    }

    private static Function<ResourceLocation, StructurePlaceSettings> settingsFor(RegistryAccess reg, int rot) {
        return id -> settings(reg, rot);
    }

    /** A piece whose rotated box's minimum corner is {@code min}. */
    public static TemplatePiece at(StructureTemplateManager mgr, RegistryAccess reg, String template, BlockPos min, int rotation) {
        StructureTemplate t = mgr.getOrCreate(ResourceLocation.parse(template));
        BoundingBox box = t.getBoundingBox(new StructurePlaceSettings().setRotation(ROTATIONS[rotation]), BlockPos.ZERO);
        BlockPos origin = min.subtract(new Vec3i(box.minX(), box.minY(), box.minZ()));
        return new TemplatePiece(mgr, reg, template, origin, rotation);
    }

    static StructurePlaceSettings settings(RegistryAccess reg, int rotation) {
        StructurePlaceSettings s = new StructurePlaceSettings().setRotation(ROTATIONS[rotation]).setIgnoreEntities(true);
        var list = reg.registry(Registries.PROCESSOR_LIST).flatMap(r -> r.getOptional(PROCESSORS));
        if (list.isPresent()) {
            for (StructureProcessor p : list.get().list()) s.addProcessor(p);
        } else {
            s.addProcessor(MarkPlacedProcessor.INSTANCE);
        }
        return s;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super.addAdditionalSaveData(ctx, tag);
        tag.putInt("Rot", rotation);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {}
}
