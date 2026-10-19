package com.yearofice.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yearofice.food.traps.SnareBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** A caught hare in the loop, in its dead pose (Minecraft engine › Traps and nets: "a dead animal model on the block entity's renderer"). */
public class SnareRenderer extends GeoBlockRenderer<SnareBlockEntity> {
    public SnareRenderer() {
        super(new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(SnareBlockEntity be) { return Looks.model("snowshoe_hare"); }

            @Override
            public ResourceLocation getTextureResource(SnareBlockEntity be) { return Looks.texture("snowshoe_hare", be.clientCoat(), false); }

            @Override
            public ResourceLocation getAnimationResource(SnareBlockEntity be) { return Looks.animation("snowshoe_hare"); }
        });
    }

    @Override
    public void render(SnareBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!be.clientCatch()) return;
        super.render(be, partialTick, pose, buffers, light, overlay);
    }
}
