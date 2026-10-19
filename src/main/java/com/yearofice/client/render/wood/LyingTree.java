package com.yearofice.client.render.wood;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yearofice.build.wood.WoodBlocks;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.worldgen.TreePlan;
import com.yearofice.worldgen.TreeShapes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Draws a felled tree from the worldgen's own builders ({@link TreeShapes}, the same crown the tree stood with),
 * tipped over by {@code angle} degrees toward its fall bearing about the top of its stump. The trunk above the stump
 * and, unless limbed, its crown; {@code lengthM} trims the trunk from the top as rounds are bucked off.
 */
final class LyingTree {
    private LyingTree() {}

    record Part(int dx, int dy, int dz, boolean log) {}

    private static final Map<Long, List<Part>> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<Part>> e) { return size() > 64; }
    };

    static synchronized List<Part> parts(TreeKind k, int height, int seed) {
        long key = ((long) k.ordinal() << 56) ^ ((long) height << 40) ^ (seed & 0xFFFFFFFFL);
        return CACHE.computeIfAbsent(key, x -> {
            List<Part> out = new ArrayList<>();
            TreeShapes.build(new TreePlan.Tree(0, 0, WoodBlocks.coverSpecies(k), height, TreePlan.Kind.LIVE, -1, seed), (dx, dy, dz, part) -> {
                if (dy < 1) return;
                if (part == TreeShapes.Part.LOG) out.add(new Part(dx, dy - 1, dz, true));
                else if (part == TreeShapes.Part.LEAVES) out.add(new Part(dx, dy - 1, dz, false));
            });
            return out;
        });
    }

    /**
     * @param lift height of the pivot over the entity's position (1 for the stump top while it falls, 0.5 lying)
     */
    static void render(PoseStack pose, MultiBufferSource buffers, int light, TreeKind k, int height, int seed, float bearing, float angle,
                       float lift, boolean crown, double lengthM) {
        if (k == null) return;
        double[] d = Felling.unit(bearing);
        BlockRenderDispatcher br = Minecraft.getInstance().getBlockRenderer();
        BlockState log = WoodBlocks.log(k).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState leaves = WoodBlocks.leaves(k).defaultBlockState();
        pose.pushPose();
        pose.translate(0, lift, 0);
        pose.mulPose(Axis.of(new Vector3f((float) d[1], 0, (float) -d[0])).rotationDegrees(angle));
        int whole = (int) Math.floor(lengthM + 1e-6);
        double part = lengthM - whole;
        for (Part p : parts(k, height, seed)) {
            if (p.log()) {
                if (p.dy() > whole || (p.dy() == whole && part < 0.05)) continue;
                pose.pushPose();
                pose.translate(p.dx() - 0.5, p.dy(), p.dz() - 0.5);
                if (p.dy() == whole) pose.scale(1, (float) part, 1);
                br.renderSingleBlock(log, pose, buffers, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            } else if (crown) {
                pose.pushPose();
                pose.translate(p.dx() - 0.5, p.dy(), p.dz() - 0.5);
                br.renderSingleBlock(leaves, pose, buffers, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        pose.popPose();
    }
}
