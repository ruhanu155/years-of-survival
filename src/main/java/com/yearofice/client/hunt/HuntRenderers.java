package com.yearofice.client.hunt;

import com.yearofice.core.YearOfIce;
import com.yearofice.hunt.HuntRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registration for the hunt: the arrow draws as its item until the shaft model lands. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class HuntRenderers {
    private HuntRenderers() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(HuntRegistry.ARROW_ENTITY.get(), ctx -> new ThrownItemRenderer<>(ctx, 0.8f, true));
    }
}
