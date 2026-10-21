package com.yearofice.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yearofice.core.YearOfIce;
import com.yearofice.story.radio.RadioRegistry;
import com.yearofice.story.radio.SupplyParachute;
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

/** The supply parachute: a barrel under a flat white canopy, no entity art needed. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class ParachuteRenderer extends EntityRenderer<SupplyParachute> {
    ParachuteRenderer(EntityRendererProvider.Context c) { super(c); }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) { e.registerEntityRenderer(RadioRegistry.SUPPLY_PARACHUTE.get(), ParachuteRenderer::new); }

    @Override
    public void render(SupplyParachute e, float yaw, float pt, PoseStack pose, MultiBufferSource buf, int light) {
        pose.pushPose();
        pose.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.BARREL.defaultBlockState(), pose, buf, light, OverlayTexture.NO_OVERLAY);
        pose.translate(-1.0, 3.0, -1.0);
        pose.scale(3f, 0.25f, 3f);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.WHITE_WOOL.defaultBlockState(), pose, buf, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SupplyParachute e) { return TextureAtlas.LOCATION_BLOCKS; }
}
