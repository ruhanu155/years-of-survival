package com.yearofice.client.render.entity;

import com.yearofice.ecosystem.YoiAnimal;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Draws a slice animal. GeckoLib's tip-over is off: a death is a carcass, never a falling body. */
public class YoiAnimalRenderer<T extends YoiAnimal> extends GeoEntityRenderer<T> {
    public YoiAnimalRenderer(EntityRendererProvider.Context ctx, float shadow) {
        super(ctx, new YoiAnimalModel<>());
        this.shadowRadius = shadow;
    }

    @Override
    protected float getDeathMaxRotation(T animal) { return 0f; }
}
