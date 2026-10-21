package com.yearofice.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yearofice.build.raft.RaftEntity;
import com.yearofice.build.raft.RaftRegistry;
import com.yearofice.core.YearOfIce;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The raft: six spruce log blocks laid side by side, no entity art needed. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class RaftRenderer extends EntityRenderer<RaftEntity> {
    RaftRenderer(EntityRendererProvider.Context c) { super(c); }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) { e.registerEntityRenderer(RaftRegistry.RAFT.get(), RaftRenderer::new); }

    @Override
    public void render(RaftEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        var state = Blocks.SPRUCE_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Z);
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            pose.translate(-0.9 + i * 0.3, 0, -0.5);
            pose.scale(0.3f, 0.3f, 1f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buf, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RaftEntity e) { return TextureAtlas.LOCATION_BLOCKS; }
}
