package com.yearofice.client.render.wood;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yearofice.build.wood.ChoppingBlockEntity;
import com.yearofice.build.wood.WoodRegistry;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.wood.Bucking;
import com.yearofice.sim.wood.Splitting;
import com.yearofice.world.logs.FallingTreeEntity;
import com.yearofice.world.logs.FelledLogEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The wood renderers: the falling tree turning about its stump, the felled log lying with its crown until limbed,
 * and the round on a chopping block with the crack in its end grain. Block models only, so no new entity art.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class WoodRenderers {
    private WoodRenderers() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(WoodRegistry.FALLING_TREE.get(), Falling::new);
        e.registerEntityRenderer(WoodRegistry.FELLED_LOG.get(), Felled::new);
        e.registerBlockEntityRenderer(WoodRegistry.CHOPPING_BLOCK_ENTITY.get(), ctx -> new Block());
    }

    static final class Falling extends EntityRenderer<FallingTreeEntity> {
        Falling(EntityRendererProvider.Context c) { super(c); }

        @Override
        public void render(FallingTreeEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
            float a = e.angle(pt);
            float lift = 1.0f - 0.5f * (a / 90f);
            LyingTree.render(pose, buf, light, e.kind(), e.height(), e.seed(), e.bearing(), a, lift, true, Bucking.usableM(e.height()));
        }

        @Override
        public ResourceLocation getTextureLocation(FallingTreeEntity e) { return TextureAtlas.LOCATION_BLOCKS; }
    }

    static final class Felled extends EntityRenderer<FelledLogEntity> {
        Felled(EntityRendererProvider.Context c) { super(c); }

        @Override
        public void render(FelledLogEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
            LyingTree.render(pose, buf, light, e.kind(), e.treeHeight(), e.seed(), e.bearing(), 90f, 0.5f, !e.limbed(), e.lengthM());
        }

        @Override
        public ResourceLocation getTextureLocation(FelledLogEntity e) { return TextureAtlas.LOCATION_BLOCKS; }
    }

    static final class Block implements BlockEntityRenderer<ChoppingBlockEntity> {
        @Override
        public void render(ChoppingBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
            ItemStack round = be.round();
            if (round.isEmpty() || be.getLevel() == null) return;
            pose.pushPose();
            pose.translate(0.5, 0.64, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale(0.75f, 0.75f, 0.75f);
            Minecraft.getInstance().getItemRenderer().renderStatic(round, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buf,
                    be.getLevel(), (int) be.getBlockPos().asLong());
            pose.popPose();
            // The crack across the end grain, along compass bearing 0°, 45°, 90° or 135°.
            double a = Math.toRadians(Splitting.crackDeg(be.crack()));
            float dx = (float) (Math.sin(a) * 0.32), dz = (float) (-Math.cos(a) * 0.32);
            VertexConsumer vc = buf.getBuffer(RenderType.lines());
            PoseStack.Pose p = pose.last();
            float y = 0.68f;
            vc.addVertex(p, 0.5f - dx, y, 0.5f - dz).setColor(26, 21, 18, 255).setNormal(p, dx, 0, dz);
            vc.addVertex(p, 0.5f + dx, y, 0.5f + dz).setColor(26, 21, 18, 255).setNormal(p, dx, 0, dz);
        }
    }
}
