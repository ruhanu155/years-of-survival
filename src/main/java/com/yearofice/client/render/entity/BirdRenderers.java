package com.yearofice.client.render.entity;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.bird.BirdEntity;
import com.yearofice.ecosystem.bird.BirdRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registration for the M6 birds: the shared grouse geometry, each species' own texture and animations, scaled. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class BirdRenderers {
    private BirdRenderers() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (var en : BirdRegistry.TYPES.entrySet()) {
            float scale = BirdRegistry.SCALE.getOrDefault(en.getKey(), 1f);
            e.registerEntityRenderer(en.getValue().get(), ctx -> new Renderer(ctx, scale));
        }
    }

    static final class Model extends YoiAnimalModel<BirdEntity> {
        @Override
        public ResourceLocation getModelResource(BirdEntity b) { return Looks.rl("geo/entity/rig_grouse.geo.json"); }

        @Override
        public ResourceLocation getTextureResource(BirdEntity b) { return Looks.rl("textures/entity/" + b.speciesPath() + "/adult_all.png"); }

        @Override
        public ResourceLocation getAnimationResource(BirdEntity b) { return Looks.rl("animations/entity/" + b.speciesPath() + ".animation.json"); }
    }

    static final class Renderer extends software.bernie.geckolib.renderer.GeoEntityRenderer<BirdEntity> {
        Renderer(EntityRendererProvider.Context ctx, float scale) {
            super(ctx, new Model());
            this.shadowRadius = 0.25f * scale;
            withScale(scale);
        }

        @Override
        protected float getDeathMaxRotation(BirdEntity a) { return 0f; }
    }
}
