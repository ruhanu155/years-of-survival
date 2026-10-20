package com.yearofice.worldgen.structure;

import com.yearofice.core.YearOfIce;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Loads and stamps set-piece templates ({@code structure/setpiece/<id>/<variant>.nbt}) with the mask processor. */
public final class SetPieceTemplates {
    private SetPieceTemplates() {}

    /** {@code "plane_wreck/nose_down_bar"} to {@code yearofice:setpiece/plane_wreck/nose_down_bar}. */
    public static ResourceLocation id(String template) {
        String path = template.startsWith(YearOfIce.MOD_ID + ":") ? template.substring(YearOfIce.MOD_ID.length() + 1) : template;
        if (!path.startsWith("setpiece/")) path = "setpiece/" + path;
        return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path);
    }

    public static Optional<StructureTemplate> load(ServerLevel level, String template) {
        return level.getStructureManager().get(id(template));
    }

    public static Rotation rotation(String name) {
        return switch (name) {
            case "clockwise_90" -> Rotation.CLOCKWISE_90;
            case "clockwise_180" -> Rotation.CLOCKWISE_180;
            case "counterclockwise_90" -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** Places a template turned by {@code rot} so its box's lowest corner lands on {@code min}. */
    public static Vec3i placeWithin(ServerLevel level, StructureTemplate t, BlockPos min, Rotation rot) {
        BoundingBox box = t.getBoundingBox(new StructurePlaceSettings().setRotation(rot), min);
        BlockPos shift = min.subtract(new BlockPos(box.minX(), box.minY(), box.minZ()));
        return place(level, t, min.offset(shift), rot);
    }

    /** Places a template with its origin at {@code corner}; every placed block is marked. Returns its size. */
    public static Vec3i place(ServerLevel level, StructureTemplate t, BlockPos corner, Rotation rot) {
        StructurePlaceSettings s = new StructurePlaceSettings().setRotation(rot).setIgnoreEntities(true)
                .addProcessor(MarkPlacedProcessor.INSTANCE);
        t.placeInWorld(level, corner, corner, s, RandomSource.create(0L), Block.UPDATE_CLIENTS);
        return t.getSize(rot);
    }

    /**
     * The share (0 to 1) of a template's non-air blocks that stand in the world with the same block, when the
     * template's rotated box has its lowest corner on {@code min}. The same placement the structure piece makes.
     */
    public static double coverage(net.minecraft.world.level.LevelReader level, StructureTemplate t, BlockPos min, Rotation rot) {
        StructurePlaceSettings s = new StructurePlaceSettings().setRotation(rot);
        BoundingBox box = t.getBoundingBox(s, BlockPos.ZERO);
        BlockPos origin = min.subtract(new Vec3i(box.minX(), box.minY(), box.minZ()));
        net.minecraft.nbt.CompoundTag tag = t.save(new net.minecraft.nbt.CompoundTag());
        net.minecraft.nbt.ListTag palette = tag.getList("palette", 10);
        net.minecraft.nbt.ListTag blocks = tag.getList("blocks", 10);
        int total = 0, present = 0;
        for (int i = 0; i < blocks.size(); i++) {
            net.minecraft.nbt.CompoundTag b = blocks.getCompound(i);
            String name = palette.getCompound(b.getInt("state")).getString("Name");
            if (name.equals("minecraft:air")) continue;
            net.minecraft.nbt.ListTag p = b.getList("pos", 3);
            BlockPos at = StructureTemplate.calculateRelativePosition(s, new BlockPos(p.getInt(0), p.getInt(1), p.getInt(2))).offset(origin);
            total++;
            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).toString().equals(name)) present++;
        }
        return total == 0 ? 1.0 : present / (double) total;
    }
}
