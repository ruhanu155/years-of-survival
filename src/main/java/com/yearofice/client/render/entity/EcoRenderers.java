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
        e.registerBlockEntityRenderer(TrapRegistry.SNARE_ENTITY.get(), ctx -> new SnareRenderer());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        Butchering.CLIENT_LEVEL = () -> Minecraft.getInstance().level;
    }
}
