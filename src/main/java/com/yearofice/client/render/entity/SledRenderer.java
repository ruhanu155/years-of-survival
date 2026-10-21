package com.yearofice.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yearofice.ecosystem.companion.SledEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/** The dog sled until its model lands: a spruce-plank bed on the ground (placeholder art, vanilla texture). */
public class SledRenderer extends EntityRenderer<SledEntity> {
    public SledRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(SledEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        ps.translate(-0.5, 0, -1.0);
        ps.scale(1.0f, 0.25f, 2.0f);
        net.minecraft.client.Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.SPRUCE_PLANKS.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(e, yaw, partial, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SledEntity e) { return ResourceLocation.withDefaultNamespace("textures/block/spruce_planks.png"); }
}
