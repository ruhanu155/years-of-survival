package com.yearofice.client.travel;

import com.yearofice.core.YearOfIce;
import com.yearofice.travel.canoe.CanoeRegistry;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The canoe draws with the vanilla birch boat model until it has art of its own (G0 placeholder). */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class CanoeClientEvents {
    private CanoeClientEvents() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(CanoeRegistry.CANOE_ENTITY.get(), ctx -> new BoatRenderer(ctx, false));
    }
}
