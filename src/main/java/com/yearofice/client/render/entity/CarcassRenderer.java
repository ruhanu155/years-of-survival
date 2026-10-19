package com.yearofice.client.render.entity;

import com.yearofice.ecosystem.carcass.CarcassEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * {@code CarcassRenderer} (Bestiary › In Minecraft: carcasses): the species' own geometry and texture, playing its
 * collapse once and holding the last frame as the dead pose.
 */
public class CarcassRenderer extends GeoEntityRenderer<CarcassEntity> {
    public CarcassRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(CarcassEntity c) { return Looks.model(c.speciesPath()); }

            @Override
            public ResourceLocation getTextureResource(CarcassEntity c) { return Looks.texture(c.speciesPath(), c.coat(), false); }

            @Override
            public ResourceLocation getAnimationResource(CarcassEntity c) { return Looks.animation(c.speciesPath()); }
        });
        this.shadowRadius = 0.2f;
    }
}
