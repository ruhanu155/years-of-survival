package com.yearofice.client.render.entity;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.food.traps.TrapRegistry;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registration for the ecosystem slice: the animal, carcass and snare renderers. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class EcoRenderers {
    private EcoRenderers() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(EcosystemRegistry.SNOWSHOE_HARE.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.25f));
        e.registerEntityRenderer(EcosystemRegistry.WILLOW_PTARMIGAN.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.25f));
        e.registerEntityRenderer(EcosystemRegistry.MOOSE.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.9f));
        e.registerEntityRenderer(EcosystemRegistry.CARCASS_SMALL.get(), CarcassRenderer::new);
        // M5 lane A
        for (var t : java.util.List.of(EcosystemRegistry.RED_SQUIRREL, EcosystemRegistry.MARTEN, EcosystemRegistry.ERMINE,
                EcosystemRegistry.MINK, EcosystemRegistry.MUSKRAT, EcosystemRegistry.WOOD_FROG, EcosystemRegistry.YELLOWJACKET,
                EcosystemRegistry.CRITTER)) e.registerEntityRenderer(t.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.15f));
        e.registerEntityRenderer(EcosystemRegistry.BEAVER.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.4f));
        e.registerEntityRenderer(EcosystemRegistry.PORCUPINE.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.3f));
        for (var t : java.util.List.of(EcosystemRegistry.SPRUCE_GROUSE, EcosystemRegistry.RUFFED_GROUSE, EcosystemRegistry.ROCK_PTARMIGAN))
            e.registerEntityRenderer(t.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.25f));
        e.registerEntityRenderer(EcosystemRegistry.CARCASS_TINY.get(), CarcassRenderer::new);
        e.registerEntityRenderer(EcosystemRegistry.CARCASS_MEDIUM.get(), CarcassRenderer::new);
        // M6 lane A
        e.registerEntityRenderer(EcosystemRegistry.CARIBOU.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.6f));
        e.registerEntityRenderer(EcosystemRegistry.MULE_DEER.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.5f));
        e.registerEntityRenderer(EcosystemRegistry.BLACK_BEAR.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.6f));
        e.registerEntityRenderer(EcosystemRegistry.GRIZZLY.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.8f));
        for (var t : java.util.List.of(EcosystemRegistry.WOLF, EcosystemRegistry.COYOTE, EcosystemRegistry.LYNX))
            e.registerEntityRenderer(t.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.4f));
        e.registerEntityRenderer(EcosystemRegistry.RED_FOX.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.3f));
        e.registerEntityRenderer(EcosystemRegistry.WOLVERINE.get(), ctx -> new YoiAnimalRenderer<>(ctx, 0.35f));
        e.registerEntityRenderer(EcosystemRegistry.CARCASS_LARGE.get(), CarcassRenderer::new);
        e.registerEntityRenderer(EcosystemRegistry.CARCASS_HUGE.get(), CarcassRenderer::new);
        e.registerBlockEntityRenderer(TrapRegistry.SNARE_ENTITY.get(), ctx -> new SnareRenderer());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        Butchering.CLIENT_LEVEL = () -> Minecraft.getInstance().level;
    }
}
