package com.yearofice.client.minigame.fishing;

import com.yearofice.core.YearOfIce;
import com.yearofice.hunt.fishing.FishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Draws a live fish: the resident rig for grayling, char, pike, burbot, whitefish and lake trout, the school rig for
 * salmon, each with its own original skin ({@code textures/entity/fish/<species>.png}) and one swim loop.
 */
public final class FishRenderer extends GeoEntityRenderer<FishEntity> {
    public FishRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0f;
    }

    @Override
    protected float getDeathMaxRotation(FishEntity f) { return 0f; }

    static final class Model extends GeoModel<FishEntity> {
        @Override
        public ResourceLocation getModelResource(FishEntity f) {
            return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "geo/entity/" + (f.school() ? "rig_fish_school" : "rig_fish_resident") + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(FishEntity f) {
            return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "textures/entity/fish/" + (f.species().isEmpty() ? "arctic_grayling" : f.species()) + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(FishEntity f) {
            return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "animations/entity/fish.animation.json");
        }
    }
}
